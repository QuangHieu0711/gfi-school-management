/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import {
  Component,
  Input,
  Output,
  EventEmitter,
  Injector,
  TemplateRef,
  ViewChild,
  OnInit,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { finalize, takeUntil } from 'rxjs';

import { AppTableMergeComponent } from '@components/app-table-merge/app-table-merge.component';
import { AppTableHeaderComponent } from '@components/app-table-header/app-table-header.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { HeaderRows } from '@model/table.model';
import { ID_TYPE } from '@model/response.model';

import {
  CAU_HINH_VAI_TRO_SEARCH_ITEMS,
  DATA_PERMISSION_GROUPED_HEADER_ROWS,
  DataPermissionMatrixRow,
  DataPermissionScopeMatrixItem,
  buildDataPermissionColumns,
} from '@app/model/admin/cau-hinh-vai-tro.model';
import {
  DATA_PERMISSION_SCOPE_OPTIONS,
  DataPermissionFormRequest,
  DataPermissionResponse,
} from '@app/model/admin/data-permission.model';
import { MenuResponse } from '@app/model/admin/menu.model';
import { VaiTroResponse } from '@app/model/admin/vai-tro.model';
import { DataPermissionService } from '@app/service/admin/data-permission.service';
import { MenuService } from '@app/service/admin/menu.service';

@Component({
  selector: 'phan-quyen-du-lieu',
  templateUrl: './phan-quyen-du-lieu.component.html',
  styleUrls: ['./phan-quyen-du-lieu.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AppTableMergeComponent,
    AppTableHeaderComponent,
    IconComponent,
    ...FORM_CONTROL_MODULE,
    ...MATERIAL_MODULE,
  ],
})
export class PhanQuyenDuLieuComponent
  extends ComponentBaseAbstract
  implements OnInit, OnChanges
{
  @Input() roleId: ID_TYPE | null = null;
  @Input() roleDetail?: VaiTroResponse;
  @Input() menusSnapshot: MenuResponse[] = [];

  @Output() dirtyStateChange = new EventEmitter<{
    count: number;
    hasDirty: boolean;
  }>();

  @ViewChild('menuNameTpl', { static: true })
  menuNameTpl!: TemplateRef<unknown>;

  @ViewChild('menuCodeTpl', { static: true })
  menuCodeTpl!: TemplateRef<unknown>;

  @ViewChild('pathTpl', { static: true })
  pathTpl!: TemplateRef<unknown>;

  @ViewChild('toggleTpl', { static: true })
  toggleTpl!: TemplateRef<unknown>;

  @ViewChild('dataPermissionHeaderTpl', { static: true })
  dataPermissionHeaderTpl!: TemplateRef<unknown>;

  readonly tableConfig = {
    showPaginator: false,
  };
  readonly dataPermissionHeaderRows: HeaderRows[] =
    DATA_PERMISSION_GROUPED_HEADER_ROWS;
  readonly searchForm = new FormGroup({
    keyword: new FormControl('', { nonNullable: true }),
  });
  readonly searchItems = CAU_HINH_VAI_TRO_SEARCH_ITEMS;
  readonly dataPermissionColumns: MtxGridColumn[] = [];

  loading = false;
  saving = false;
  dataPermissionSource: DataPermissionMatrixRow[] = [];

  private keyword = '';
  private allDataPermissionRows: DataPermissionMatrixRow[] = [];
  private dataExpandedRowIds = new Set<ID_TYPE>();
  private dirtyDataRowIds = new Set<ID_TYPE>();

  constructor(
    protected override injector: Injector,
    private readonly menuService: MenuService,
    private readonly dataPermissionService: DataPermissionService
  ) {
    super(injector);
  }

  override ngOnInit() {
    const templates = {
      menuNameTpl: this.menuNameTpl,
      menuCodeTpl: this.menuCodeTpl,
      pathTpl: this.pathTpl,
      toggleTpl: this.toggleTpl,
    };

    this.dataPermissionColumns.push(...buildDataPermissionColumns(templates));

    this.searchForm.controls.keyword.valueChanges
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((kw) => {
        this.keyword = kw;
        this.applyClientFilter();
      });

    super.ngOnInit();

    if (this.roleId && this.menusSnapshot.length > 0) {
      this.loadPermissions();
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (
      (changes['menusSnapshot'] || changes['roleId']) &&
      this.roleId &&
      this.menusSnapshot.length > 0
    ) {
      this.loadPermissions();
    }
  }

  get hasDirtyRows(): boolean {
    return this.dirtyDataRowIds.size > 0;
  }

  get dirtyCount(): number {
    return this.dirtyDataRowIds.size;
  }

  isDataRootMenu(row: DataPermissionMatrixRow): boolean {
    return Number(row.level) === 0;
  }

  hasDataChildren(row: DataPermissionMatrixRow): boolean {
    return Number(row.childCount) > 0;
  }

  isDataExpanded(row: DataPermissionMatrixRow): boolean {
    return this.dataExpandedRowIds.has(row.id);
  }

  toggleDataRow(row: DataPermissionMatrixRow, event?: Event) {
    event?.stopPropagation();
    if (!this.hasDataChildren(row)) return;

    if (this.isDataExpanded(row)) {
      this.dataExpandedRowIds.delete(row.id);
    } else {
      this.dataExpandedRowIds.add(row.id);
    }

    this.applyClientFilter();
  }

  onToggleDataScope(
    row: DataPermissionMatrixRow,
    scopeType: string,
    event: MatSlideToggleChange | { checked: boolean }
  ) {
    if (this.hasDataChildren(row)) return;

    const sourceRow = this.allDataPermissionRows.find(
      (item) => item.id === row.id
    );
    if (!sourceRow) return;

    sourceRow.scopes = sourceRow.scopes.map((scope) => {
      // Bật "Tất cả dữ liệu" thì tự tắt hết các scope còn lại
      if (scopeType === 'ALL' && event.checked) {
        return {
          ...scope,
          status: scope.scopeType === 'ALL' ? 1 : 0,
        };
      }

      // Nếu đang bật scope khác, tắt "Tất cả dữ liệu"
      if (scope.scopeType === 'ALL' && scopeType !== 'ALL' && event.checked) {
        return {
          ...scope,
          status: 0,
        };
      }

      // Cập nhật trạng thái scope hiện tại
      if (scope.scopeType === scopeType) {
        return {
          ...scope,
          status: event.checked ? 1 : 0,
        };
      }

      // Giữ nguyên các scope khác (vì có thể chọn nhiều cùng lúc)
      return scope;
    });

    this.syncDataRowState(sourceRow);
    this.syncDataDirtyState(sourceRow);
    this.applyClientFilter();
  }

  submitSearch() {
    this.keyword = this.searchForm.controls.keyword.value.trim();
    this.applyClientFilter();
  }

  resetSearch() {
    this.keyword = '';
    this.searchForm.reset({ keyword: '' });
    this.loadPermissions();
  }

  saveChanges() {
    if (this.roleId == null) return;
    if (!this.dirtyDataRowIds.size) {
      this.toastr.warning('Chưa có thay đổi để lưu', 'Thông báo');
      return;
    }

    const payloads = this.allDataPermissionRows
      .filter((row) => this.dirtyDataRowIds.has(row.id))
      .map((row) => this.buildDataPermissionPayload(row));

    this.saving = true;

    this.dataPermissionService
      .save(payloads)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.toastr.success(
            'Cập nhật quyền dữ liệu thành công',
            'Thành công'
          );
          this.loadPermissions(false);
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Cập nhật quyền dữ liệu thất bại',
            'Thất bại'
          );
        },
      });
  }

  resetChanges() {
    if (!this.dirtyDataRowIds.size) {
      this.toastr.warning('Chưa có thay đổi để hủy', 'Thông báo');
      return;
    }

    this.allDataPermissionRows = this.allDataPermissionRows.map((row) => ({
      ...row,
      status: row.originalStatus,
      scopes: row.scopes.map((scope) => ({
        ...scope,
        status: scope.originalStatus,
      })),
    }));

    this.dirtyDataRowIds.clear();
    this.dirtyStateChange.emit({
      count: 0,
      hasDirty: false,
    });
    this.applyClientFilter();
    this.toastr.success('Đã hoàn tác các thay đổi', 'Thành công');
  }

  isDataScopeEnabled(row: DataPermissionMatrixRow, scopeType: string): boolean {
    return (
      row.scopes.find((scope) => scope.scopeType === scopeType)?.status === 1
    );
  }

  loadPermissions(showLoading = true) {
    if (this.roleId == null || !this.menusSnapshot.length) return;

    if (showLoading) {
      this.loading = true;
    }

    this.dataPermissionService
      .getByRoleId(this.roleId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (dataPermissions) => {
          const normalizedDataPermissions = this.normalizeDataPermissionItems(
            dataPermissions.data
          );
          this.allDataPermissionRows = this.buildDataPermissionRows(
            this.roleId!,
            this.menusSnapshot,
            normalizedDataPermissions
          );
          this.dirtyDataRowIds.clear();
          this.dataExpandedRowIds = new Set(
            this.allDataPermissionRows
              .filter((row) => this.hasDataChildren(row))
              .map((row) => row.id)
          );
          this.applyClientFilter();
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Không tải được quyền dữ liệu',
            'Thất bại'
          );
        },
      });
  }

  private normalizeDataPermissionItems(
    data: DataPermissionResponse[] | null | undefined
  ): DataPermissionResponse[] {
    return (data ?? []).map((item: any) => ({
      ...item,
      menuId: item.menuId ?? item.id,
      scopes: (item.scopes ?? []).map((scope: any) => ({
        id: scope.id,
        scopeType: scope.scopeType,
        status: Number(scope.status ?? 0),
      })),
      status: Number(item.status ?? 0),
    }));
  }

  private buildDataPermissionRows(
    roleId: ID_TYPE,
    menus: MenuResponse[],
    permissions: DataPermissionResponse[]
  ): DataPermissionMatrixRow[] {
    const permissionMap = new Map<ID_TYPE, DataPermissionResponse>();
    permissions.forEach((item) => permissionMap.set(item.menuId, item));

    return this.flattenMenuRows(menus, (menu, level, childCount) => {
      const permission = permissionMap.get(menu.id);
      const scopes = this.buildScopeMatrixItems(permission?.scopes ?? []);
      const row: DataPermissionMatrixRow = {
        ...menu,
        roleId,
        dataPermissionId: permission?.id,
        level,
        childCount,
        status: Number(permission?.status ?? 0),
        originalStatus: Number(permission?.status ?? 0),
        scopes,
      };

      this.syncDataRowState(row);
      return row;
    });
  }

  private flattenMenuRows<T extends { id: ID_TYPE }>(
    menus: MenuResponse[],
    createRow: (menu: MenuResponse, level: number, childCount: number) => T
  ): T[] {
    const childrenMap = new Map<ID_TYPE | null, MenuResponse[]>();
    const itemMap = new Map(menus.map((item) => [item.id, item]));

    menus.forEach((item) => {
      const key = (item.parentId ?? null) as ID_TYPE | null;
      const group = childrenMap.get(key) ?? [];
      group.push(item);
      childrenMap.set(key, group);
    });

    const roots = menus.filter(
      (item) => item.parentId == null || !itemMap.has(item.parentId)
    );
    const rows: T[] = [];
    const visited = new Set<ID_TYPE>();

    const visitNode = (menu: MenuResponse, level: number) => {
      if (visited.has(menu.id)) return;
      visited.add(menu.id);

      const children = [...(childrenMap.get(menu.id) ?? [])].sort(
        (a, b) => (a.ordinal ?? 0) - (b.ordinal ?? 0)
      );

      rows.push(createRow(menu, level, children.length));
      children.forEach((child) => visitNode(child, level + 1));
    };

    [...roots]
      .sort((a, b) => (a.ordinal ?? 0) - (b.ordinal ?? 0))
      .forEach((root) => visitNode(root, 0));

    menus
      .filter((item) => !visited.has(item.id))
      .forEach((item) => visitNode(item, 0));

    return rows;
  }

  private normalizeScope(scope: any): DataPermissionScopeMatrixItem {
    return {
      id: scope?.id,
      scopeType: scope?.scopeType ?? '',
      status: Number(scope?.status ?? 0),
      originalStatus: Number(scope?.status ?? 0),
    };
  }

  private buildScopeMatrixItems(
    scopes: any[]
  ): DataPermissionScopeMatrixItem[] {
    const scopeMap = new Map<string, DataPermissionScopeMatrixItem>();

    (scopes ?? []).forEach((scope) => {
      const normalized = this.normalizeScope(scope);
      if (!normalized.scopeType) return;
      scopeMap.set(normalized.scopeType, normalized);
    });

    return DATA_PERMISSION_SCOPE_OPTIONS.map((scopeOption) => {
      const matched = scopeMap.get(scopeOption.scopeType);
      return (
        matched ?? {
          scopeType: scopeOption.scopeType,
          status: 0,
          originalStatus: 0,
        }
      );
    });
  }

  private syncDataDirtyState(row: DataPermissionMatrixRow) {
    const hasScopeChanged = row.scopes.some(
      (scope) => Number(scope.status) !== Number(scope.originalStatus)
    );
    const hasChanged =
      hasScopeChanged || Number(row.status) !== Number(row.originalStatus);

    if (hasChanged) {
      this.dirtyDataRowIds.add(row.id);
    } else {
      this.dirtyDataRowIds.delete(row.id);
    }

    this.dirtyStateChange.emit({
      count: this.dirtyDataRowIds.size,
      hasDirty: this.dirtyDataRowIds.size > 0,
    });
  }

  private syncDataRowState(row: DataPermissionMatrixRow) {
    row.status = row.scopes.some((scope) => Number(scope.status) === 1) ? 1 : 0;
  }

  private applyClientFilter() {
    const keyword = this.keyword.trim().toLowerCase();

    const rows = keyword
      ? this.filterRowsByKeyword(this.allDataPermissionRows, keyword)
      : this.filterRowsByExpandState(
          this.allDataPermissionRows,
          this.dataExpandedRowIds
        );
    this.dataPermissionSource = rows;
    this.dataSourceTotal = rows.length;
    this.pageSize = Math.max(rows.length, 1);
  }

  private filterRowsByKeyword<T extends MenuResponse>(
    rows: T[],
    keyword: string
  ): T[] {
    const rowMap = new Map(rows.map((row) => [row.id, row]));
    const resultIds = new Set<ID_TYPE>();

    rows.forEach((row) => {
      const matched =
        `${row.name ?? ''}`.toLowerCase().includes(keyword) ||
        `${row.code ?? ''}`.toLowerCase().includes(keyword) ||
        `${row.url ?? ''}`.toLowerCase().includes(keyword);

      if (!matched) return;

      let current: T | undefined = row;
      while (current) {
        resultIds.add(current.id);
        current =
          current.parentId != null
            ? (rowMap.get(current.parentId as ID_TYPE) as T | undefined)
            : undefined;
      }
    });

    return rows.filter((row) => resultIds.has(row.id));
  }

  private filterRowsByExpandState<T extends MenuResponse>(
    rows: T[],
    expandedIds: Set<ID_TYPE>
  ): T[] {
    const rowMap = new Map(rows.map((row) => [row.id, row]));

    return rows.filter((row) => {
      let parentId = row.parentId as ID_TYPE | null | undefined;

      while (parentId != null) {
        const parent = rowMap.get(parentId);
        if (!parent) break;
        if (!expandedIds.has(parent.id)) return false;
        parentId = parent.parentId as ID_TYPE | null | undefined;
      }

      return true;
    });
  }

  private buildDataPermissionPayload(
    row: DataPermissionMatrixRow
  ): DataPermissionFormRequest {
    return {
      menuId: row.id,
      roleId: row.roleId,
      status: row.status,
      scopes: row.scopes.map((scope) => ({
        scopeType: scope.scopeType,
        status: scope.status,
      })),
    };
  }
}
