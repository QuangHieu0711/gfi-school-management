/* eslint-disable @typescript-eslint/no-explicit-any */
import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
} from '@angular/core';
import {
  MtxGridColumn,
  MtxGridColumnPinOption,
  MtxGridModule,
} from '@ng-matero/extensions/grid';
import { PageEvent } from '@angular/material/paginator';
import {
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
import { LanguageService } from '@service';
import { Subscription } from 'rxjs';
import { MtxGridCellTemplate } from '@ng-matero/extensions/grid';

@Component({
  selector: 'app-table',
  templateUrl: './app-table.component.html',
  styleUrls: ['./app-table.component.scss'],
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
 * Reusable data table wrapper around MtxGrid:
 * - Exposes rich configuration via `TableConfig`
 * - Emits query changes (paging/sorting) to parent
 * - Provides toolbar actions (create/import/export/delete)
 * - Supports i18n for pin options and labels
 */
export class AppTableComponent implements OnDestroy {
  /** Title displayed above the table */
  @Input() tableTitle = '';
  @Input() headerTemplate: MtxGridCellTemplate | Record<string, any> = {};

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
  @Input() tableColumns: MtxGridColumn[] = [];

  private _rowSelected: TableDataSource[] = [];

  /** Preselected rows for checkbox selection */
  @Input()
  set rowSelected(value: TableDataSource[]) {
    this._rowSelected = this.attachRowIndex(value);
  }
  get rowSelected(): TableDataSource[] {
    return this._rowSelected;
  }

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

  /** Toolbar / column features */
  columnPinOptions: MtxGridColumnPinOption[] = [];
  get showToolbar(): boolean {
    return this.config?.showToolbar;
  }
  get columnHideable(): boolean {
    return this.config?.columnHideable;
  }
  get columnSortable(): boolean {
    return this.config?.columnSortable;
  }
  get columnPinnable(): boolean {
    return this.config?.columnPinnable;
  }

  /** Language change subscription for dynamic labels */
  private readonly languageSubscription?: Subscription;

  constructor(
    private readonly i18n: LanguageService,
    private readonly translateService: TranslateService
  ) {
    /** Initialize query with current paging so parent can rely on a stable shape */
    this.query = { pageIndex: 0, pageSize: this.pageSize };

    /** Build initial pin options */
    this.rebuildPinOptions();

    /** Update pin labels on language change */
    this.languageSubscription = this.translateService.onLangChange.subscribe(
      () => {
        /** You can reload content, re-translate labels, etc. here */
        this.rebuildPinOptions();
      }
    );
  }

  ngOnDestroy(): void {
    this.languageSubscription?.unsubscribe();
  }

  /** Rebuilds pin options using current language */
  private rebuildPinOptions(): void {
    this.columnPinOptions = [
      { label: this.i18n.instant('table.pin.pin_left'), value: 'left' },
      { label: this.i18n.instant('table.pin.pin_right'), value: 'right' },
      { label: this.i18n.instant('table.pin.no_pin'), value: null },
    ];
  }

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
   * - recompute rowIndex for current page
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
   */
  private attachRowIndex(_dataSource: TableDataSource[]): TableDataSource[] {
    const page = this.pageIndex ?? 0;
    const size = this.pageSize ?? PAGINATION_DEFAULT_PAGE_SIZE;
    return _dataSource.map((row, idx) => ({
      ...row,
      rowIndex: page * size + idx + 1,
    }));
  }
}
