import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EmbeddedViewRef,
  EventEmitter,
  HostListener,
  Input,
  OnDestroy,
  Output,
  Renderer2,
  TemplateRef,
  ViewChild,
} from '@angular/core';
import { MtxGridModule } from '@ng-matero/extensions/grid';
import { PageEvent } from '@angular/material/paginator';
import {
  TableColumns,
  TableConfig,
  TableDataSource,
  TableQueryEvent,
} from '@model/table.model';
import { Sort } from '@angular/material/sort';
import { MATERIAL_MODULE } from '@modules';
import { ID_TYPE } from '@model/response.model';
import { AppPaginatorComponent } from '@components/app-paginator/app-paginator.component';
import {
  DEFAULT_TABLE_CONFIG,
  PAGINATION_DEFAULT_PAGE_SIZE,
} from '@constant/constant';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { Subscription } from 'rxjs';
import { RafSchedulerService } from '@service';

@Component({
  selector: 'app-table-merge',
  templateUrl: './app-table-merge.component.html',
  styleUrls: ['./app-table-merge.component.scss'],
  imports: [
    MtxGridModule,
    ...MATERIAL_MODULE,
    AppPaginatorComponent,
    TranslateModule,
    IconComponent,
  ],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
/**
 * A table wrapper that PREPENDS custom header rows into the native Mtx/Mat <thead>
 * using a provided template (allows merged/sticky header rows)
 * supports "visual rowspan" by covering selected native leaf header cells
 * and computes sticky offsets so each header row stacks at 56px increments.
 *
 * How it works:
 * - Renders the provided `gridHeaderTpl` off-DOM to get one or more <tr> rows (or a <thead>).
 * - Finds the native <thead> produced by Mtx/Mat and PREPENDS those <tr> rows into it.
 * - Watches the table DOM with a MutationObserver; if Mtx re-renders its header,
 *   re-inserts our custom rows to keep them authoritative.
 */
export class AppTableMergeComponent implements AfterViewInit, OnDestroy {
  /** Optional title displayed above the table */
  @Input() tableTitle = '';

  /** Height of each sticky header row (px) */
  @Input() stickyRowHeight = 48;

  /** Internal dataSource with 1-based rowIndex injected for display */
  private _dataSource: TableDataSource[] = [];

  /** Table rows input; rowIndex is attached on assignment */
  @Input()
  set dataSource(value: TableDataSource[]) {
    this._dataSource = this.attachRowIndex(value);
  }
  get dataSource(): TableDataSource[] {
    return this._dataSource;
  }

  /** MtxGrid column definitions */
  @Input() tableColumns: TableColumns[] = [];

  /** Loading flag to show table-level progress UI */
  @Input() loading = false;

  /** Optional feature toggles and behavior config */
  @Input() tableConfig: TableConfig = {};

  /** Row click event hook */
  @Output() rowClick = new EventEmitter<TableDataSource>();

  /** Query (paging, sorting) changed: let parent refetch */
  @Output() queryChange = new EventEmitter<TableQueryEvent>();

  /** Selection changed (single/multi) */
  @Output() rowSelectedChange = new EventEmitter<TableDataSource[]>();

  /** Toolbar actions */
  @Output() createEvent = new EventEmitter<void>();
  @Output() importEvent = new EventEmitter<void>();
  @Output() exportEvent = new EventEmitter<void>();
  @Output() deleteEvent = new EventEmitter<void>();

  /** Current query snapshot propagated to parent */
  query: TableQueryEvent = {
    pageIndex: 0,
    pageSize: PAGINATION_DEFAULT_PAGE_SIZE,
  };

  /** Filter accordion state (if used in template) */
  panelOpenState = false;

  /** Template that produces a header (either <thead> or bare <tr> rows) */
  @Input() gridHeaderTpl!: TemplateRef<unknown>;

  /** Root wrapper that contains the Mtx table */
  @ViewChild('gridWrapper', { static: true }) gridWrapperRef!: ElementRef;

  /** References used by the header injection logic */
  private headerViewRef: EmbeddedViewRef<unknown> | null = null;
  private insertedRows: HTMLTableRowElement[] = []; /** rows we injected */
  private tableEl: HTMLTableElement | null = null;
  private domObserver: MutationObserver | null = null;

  /** Merge defaults with incoming config (cheap getter; values are primitives) */
  get config(): Required<TableConfig> {
    return { ...DEFAULT_TABLE_CONFIG, ...this.tableConfig };
  }

  /** Feature flags derived from config (kept for template clarity) */
  get hasImport(): boolean {
    return this.config?.hasImport;
  }
  get hasExport(): boolean {
    return this.config?.hasExport;
  }
  get hasCreate(): boolean {
    return this.config?.hasCreate;
  }
  get hasDelete(): boolean {
    return this.config?.hasDelete;
  }
  get hasFilterPanel(): boolean {
    return this.config?.hasFilterPanel;
  }
  get hasFilterPanelButton(): boolean {
    return this.config?.hasFilterPanelButton;
  }

  /** Paging inputs and helpers */
  @Input() total = 0;
  @Input() pageIndex = 0;
  @Input() pageSize = PAGINATION_DEFAULT_PAGE_SIZE;
  get showPaginator(): boolean {
    return this.config?.showPaginator;
  }
  get pageSizeOptions(): number[] {
    return this.config?.pageSizeOptions;
  }
  get columnResizable(): boolean {
    return this.config?.columnResizable;
  }

  /** Sorting flags */
  get sortDisabled(): boolean {
    return this.config?.sortDisabled;
  }

  /** Row selectable */
  get rowSelectable(): boolean {
    return this.config?.rowSelectable;
  }
  get multiSelectable(): boolean {
    return this.config?.multiSelectable;
  }

  /** Language change subscription for dynamic labels */
  private readonly languageSubscription?: Subscription;

  /** Resize throttle flag */
  private resizeScheduled = false;

  constructor(
    private readonly translateService: TranslateService,
    private readonly renderer: Renderer2,
    private readonly raf: RafSchedulerService
  ) {
    /** Initialize query with current paging so parent can rely on a stable shape */
    this.query = { pageIndex: 0, pageSize: this.pageSize };

    /** Update header rows on language change */
    this.languageSubscription = this.translateService.onLangChange.subscribe(
      () => {
        this.installCustomHeader(); /** refresh translated header rows */
        this.applyRowspanFromColumns();
        this.updateStickyOffsets(); /** and recompute tops */
      }
    );
  }

  /** Lifecycle: render and lock-in our custom header after the table appears */
  ngAfterViewInit(): void {
    if (!this.gridHeaderTpl) return;

    this.tableEl = this.gridWrapperRef.nativeElement.querySelector('table');
    if (!this.tableEl || !this.gridHeaderTpl) return;

    this.installCustomHeader();
    this.applyRowspanFromColumns();
    this.updateStickyOffsets();
    this.observeTableDom(); /** keep our header rows installed across re-renders */
  }

  /** Cleanup: destroy the embedded view and stop observing DOM */
  ngOnDestroy(): void {
    this.languageSubscription?.unsubscribe();
    this.removeInsertedRows();
    this.destroyHeaderView();
    this.domObserver?.disconnect();
    this.domObserver = null;
    this.tableEl = null;

    this.raf.flush(); /** 👇 ensure pending tasks run (or are cleared) */
  }

  /** ===================== Core header injection logic ====================== */

  /** Finds the native THEAD rendered by Mtx/Mat */
  private getNativeThead(): HTMLTableSectionElement | null {
    return this.tableEl?.querySelector('thead') ?? null;
  }

  /** Extracts header <tr> rows from the embedded view (template may be <thead> or bare <tr>s). */
  private extractHeaderRowsFromView(): HTMLTableRowElement[] {
    if (!this.headerViewRef) return [];
    const rows: HTMLTableRowElement[] = [];

    for (const node of this.headerViewRef.rootNodes) {
      /** Template rooted in <thead>: take its immediate rows */
      if (node instanceof HTMLTableSectionElement && node.tagName === 'THEAD') {
        rows.push(
          ...Array.from(node.querySelectorAll(':scope > tr')).filter(
            (el): el is HTMLTableRowElement => el instanceof HTMLTableRowElement
          )
        );
        continue;
      }
      /** Template gives bare <tr> nodes */
      if (node instanceof HTMLTableRowElement && node.tagName === 'TR') {
        rows.push(node);
        continue;
      }
      /** Fallback: nested <thead> > <tr> or top-level <tr> under a wrapper */
      if (node instanceof HTMLElement) {
        rows.push(
          ...Array.from(
            node.querySelectorAll('thead > tr, :scope > tr')
          ).filter(
            (el): el is HTMLTableRowElement => el instanceof HTMLTableRowElement
          )
        );
      }
    }
    return rows;
  }

  /** Removes any rows previously injected into the native <thead>. */
  private removeInsertedRows(): void {
    if (!this.tableEl || !this.insertedRows.length) return;
    const thead = this.getNativeThead();
    for (const row of this.insertedRows) {
      if (thead && row.parentElement === thead) {
        this.renderer.removeChild(thead, row);
      }
    }
    this.insertedRows = [];
  }

  /**
   * Renders `gridHeaderTpl` off-DOM, extracts <tr> rows, and PREPENDS them into the
   * native <thead>. If <thead> isn't there yet, the MutationObserver will retry.
   */
  private installCustomHeader(): void {
    if (!this.tableEl || !this.gridHeaderTpl) return;

    const nativeThead = this.getNativeThead();
    if (!nativeThead) return; /** wait for Mtx to render header */

    /** Clean up any previous injected rows and view */
    this.removeInsertedRows();
    this.destroyHeaderView();

    /** Render our template off-DOM */
    this.headerViewRef = this.gridHeaderTpl.createEmbeddedView({});
    this.headerViewRef.detectChanges();

    /** Extract <tr> rows to inject */
    const rows = this.extractHeaderRowsFromView();
    if (!rows.length) return;

    /** PREPEND rows in order so final visual order matches the template */
    for (const row of rows) {
      this.renderer.insertBefore(
        nativeThead,
        row,
        nativeThead.lastElementChild
      );
      this.insertedRows.push(row);
    }

    /** Apply rowspan styles to header cells */
    this.applyRowspanFromColumns();

    /** Retarget the observer to the new THEAD */
    this.retargetObserverToThead();
  }

  /**
   * Computes and applies sticky offsets:
   * - For each inserted row: top = index * stickyRowHeight
   * - For each native (non-inserted) row in THEAD: top = (insertedCount + nativeIndex) * stickyRowHeight
   * Also sets z-index so earlier rows overlay later ones.
   */
  private updateStickyOffsets(): void {
    const thead = this.getNativeThead();
    if (!thead) return;

    const H = this.stickyRowHeight;
    const allHeadRows = Array.from(
      thead.querySelectorAll(':scope > tr')
    ) as HTMLTableRowElement[];

    /** Partition rows into inserted vs native */
    const insertedSet = new Set(this.insertedRows);
    const nativeRows: HTMLTableRowElement[] = [];
    for (const r of allHeadRows) {
      if (!insertedSet.has(r)) nativeRows.push(r);
    }

    const nativeThead = this.getNativeThead();
    if (!nativeThead) return; /** wait for Mtx to render header */

    /** Apply offsets to inserted rows */
    nativeThead.querySelectorAll('tr').forEach((row, i) => {
      const topPx = i * H;
      this.applyStickyToRow(
        row,
        topPx,
        /*zBase*/ 100 + (allHeadRows.length - i)
      );
    });
  }

  /** Ensures the row and its header cells are sticky at a given top, with a z-index. */
  private applyStickyToRow(
    row: HTMLTableRowElement,
    topPx: number,
    zIndex: number
  ): void {
    /** Apply to the <tr> itself (works in most modern browsers) */
    this.renderer.setStyle(row, 'position', 'sticky');
    this.renderer.setStyle(row, 'top', `${topPx}px`);
    this.renderer.setStyle(row, 'z-index', `${zIndex}`);

    /** Also apply to each <th> for maximum compatibility with Material/MDC behaviors */
    const cells = Array.from(row.children) as HTMLElement[];
    for (const cell of cells) {
      if (cell.tagName === 'TH' || cell.tagName === 'TD') {
        this.renderer.setStyle(cell, 'position', 'sticky');
        this.renderer.setStyle(cell, 'top', `${topPx}px`);
        this.renderer.setStyle(cell, 'z-index', `${zIndex}`);
        /** Ensure sticky cells have a background; otherwise they appear transparent over rows */
        if (!cell.style.backgroundColor && !cell.style.background) {
          this.renderer.setStyle(cell, 'background', 'inherit');
        }
      }
    }
  }

  /**
   * Observes the table DOM; if our rows are missing/relocated or <thead> is swapped,
   * re-inserts our custom rows and recomputes sticky offsets.
   */
  private observeTableDom(): void {
    if (!this.tableEl) return;

    this.domObserver = new MutationObserver(() => {
      this.raf.schedule('table-header-sync', this.onMutations);
    });

    this.domObserver.observe(this.tableEl, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['class', 'style'],
    });
  }

  /** Disposes the current header view */
  private destroyHeaderView(): void {
    if (this.headerViewRef) {
      this.headerViewRef.destroy();
      this.headerViewRef = null;
    }
  }

  /** MutationObserver callback (debounced via RafScheduler) */
  private onMutations = () => {
    const thead = this.getNativeThead();

    const needsReinstall =
      !thead ||
      !this.insertedRows.length ||
      this.insertedRows.some(
        (r) => !r.isConnected || r.parentElement !== thead
      );

    if (needsReinstall) {
      /** Avoid re-entrancy */
      Promise.resolve().then(() => {
        this.installCustomHeader();
        this.applyRowspanFromColumns();
        this.updateStickyOffsets();
      });
    } else {
      this.applyRowspanFromColumns();
      /** Structure might have changed (e.g., Mtx added a sort row), recompute offsets */
      this.updateStickyOffsets();
    }
  };

  /** Retargets the MutationObserver to a new THEAD element */
  private retargetObserverToThead() {
    if (!this.domObserver) return;
    this.domObserver.disconnect();
    const thead = this.getNativeThead();
    if (thead) {
      this.domObserver.observe(thead, {
        childList: true,
        subtree: true,
        attributes: true,
        attributeFilter: ['class', 'style'],
      });
    }
  }

  /** ===================== Table API passthroughs =========================== */

  /** TrackBy: use stable `id` when present, otherwise index */
  public trackBy(index: number, item: TableDataSource): ID_TYPE | number {
    return item && item.id != null ? item.id : index;
  }

  /** Bubble row click to parent */
  public onRowClick({ rowData }: { rowData: TableDataSource }): void {
    this.rowClick.emit(rowData);
  }

  /**
   * Paging changed from paginator:
   * - update local state
   * - emit query to parent so it can fetch
   */
  public onPageChange(event: PageEvent): void {
    this.query = {
      ...this.query,
      pageIndex: event.pageIndex,
      pageSize: event.pageSize,
    };
    this.queryChange.emit(this.query);
  }

  /**
   * Sort changed:
   * - normalize empty direction to `undefined` if your backend expects that
   * - emit query so parent can refetch
   */
  public onSortChange(event: Sort) {
    this.query = {
      ...this.query,
      sortField: event.active,
      sortDirection: event.direction,
    };
    this.queryChange.emit(this.query);
  }

  /** Selection changed passthrough */
  public onRowSelected(rowSelected: TableDataSource[]) {
    this.rowSelectedChange.emit(rowSelected);
  }

  /** Toolbar actions passthrough */
  public onCreate() {
    this.createEvent.emit();
  }

  public onImport() {
    this.importEvent.emit();
  }

  public onExport() {
    this.exportEvent.emit();
  }

  public onDelete() {
    this.deleteEvent.emit();
  }

  /**
   * Reset query to first page and clear sorting.
   * Keeps current pageSize.
   */
  public resetQuery(): void {
    this.query = {
      pageIndex: 0,
      pageSize: this.pageSize,
      sortField: undefined,
      sortDirection: undefined,
    };
    /** 🔧 also refresh rowIndex after reset */
    this._dataSource = this.attachRowIndex(this._dataSource);

    this.queryChange.emit(this.query);
  }

  /**
   * Attach a 1-based `rowIndex` to each row for display,
   * computed from the current paging state.
   * Preserves existing rowIndex for summary/total rows.
   */
  private attachRowIndex(_dataSource: TableDataSource[]): TableDataSource[] {
    const page = this.pageIndex ?? 0;
    const size = this.pageSize ?? PAGINATION_DEFAULT_PAGE_SIZE;
    return _dataSource.map((row, idx) => {
      // Giữ nguyên rowIndex nếu là summary row hoặc total row
      if (row['isSummaryRow'] || row['isTotalRow']) {
        return row;
      }
      return { ...row, rowIndex: page * size + idx + 1 };
    });
  }

  /** Recalculate sticky positions on viewport size changes */
  @HostListener('window:resize')
  onResize() {
    if (this.resizeScheduled) return;
    this.resizeScheduled = true;
    this.raf.schedule('table-header-resize', () => {
      this.resizeScheduled = false;
      this.updateStickyOffsets();
    });
  }

  /** Move mtx header cells upward based on `tableColumns[].rowspan` (>=2)
   *  and place them into the header placeholder with `data-slot="<field>"`.
   */
  private applyRowspanFromColumns(): void {
    const thead = this.getNativeThead();
    if (!thead || !this.insertedRows.length || !this.tableColumns?.length)
      return;

    // All header rows, top..bottom (bottom is mtx leaf row)
    const allRows = Array.from(
      thead.querySelectorAll(':scope > tr')
    ) as HTMLTableRowElement[];
    if (!allRows.length) return;
    const mtxRow = allRows[allRows.length - 1];

    // Map mtx <th> by column id from class: mat-column-<id> or cdk-column-<id>
    const thByColId = new Map<string, HTMLTableCellElement>();
    for (const th of Array.from(mtxRow.cells) as HTMLTableCellElement[]) {
      const cls = th.className || '';
      const m = cls.match(/(?:^|\s)(?:mat|cdk)-column-([^\s]+)/);
      if (m && m[1]) thByColId.set(m[1], th);
    }

    const totalRows = allRows.length;

    for (const col of this.tableColumns) {
      const span = Number(col.rowspan ?? 1);
      if (span < 2) continue; // only act on 2+

      // Use `field` as the canonical id/slot key
      const field = col.field?.toString();
      if (!field) continue;

      const th = thByColId.get(field);
      if (!th) continue; // hidden/not rendered yet
      if (th.hasAttribute('data-moved-rowspan')) {
        // keep rowspan in sync if config changed
        if (th.getAttribute('rowspan') !== String(span)) {
          this.renderer.setAttribute(th, 'rowspan', String(span));
        }
        continue;
      }

      // Compute target row: for span=S, target index = totalRows - S (clamped)
      const targetIdx = Math.max(totalRows - span, 0);
      const targetRow = allRows[targetIdx];
      if (!targetRow) continue;

      // Find placeholder slot in that row: data-slot="<field>"
      const anchor = targetRow.querySelector(`th[data-slot="${field}"]`);

      if (anchor && anchor.parentElement === targetRow) {
        // insert before placeholder, then remove placeholder
        this.renderer.insertBefore(targetRow, th, anchor);
        this.renderer.removeChild(targetRow, anchor);
      } else {
        // fallback: append to end (if no placeholder provided)
        this.renderer.appendChild(targetRow, th);
      }

      // Give it the actual rowspan and mark moved (idempotent)
      this.renderer.setAttribute(th, 'rowspan', String(span));
      this.renderer.setAttribute(th, 'data-moved-rowspan', '1');
      th.classList.add('moved-rowspan-cell');
    }
  }
}
