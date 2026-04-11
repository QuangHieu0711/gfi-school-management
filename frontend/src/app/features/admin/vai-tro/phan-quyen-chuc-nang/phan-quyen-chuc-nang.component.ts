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

import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { ID_TYPE } from '@model/response.model';

import {
  CAU_HINH_VAI_TRO_SEARCH_ITEMS,
  PermissionMatrixRow,
  PermissionToggleKey,
  buildPermissionColumns,
} from '@app/model/admin/cau-hinh-vai-tro.model';
import {
  PERMISSION_KEY,
  PermissionFormRequest,
  PermissionResponse,
} from '@app/model/admin/permission.model';
import { MenuResponse } from '@app/model/admin/menu.model';
import { VaiTroResponse } from '@app/model/admin/vai-tro.model';
import { PermissionAdminService } from '@app/service/admin/permission.service';
import { MenuService } from '@app/service/admin/menu.service';

@Component({
  selector: 'phan-quyen-chuc-nang',
  templateUrl: './phan-quyen-chuc-nang.component.html',
  styleUrls: ['./phan-quyen-chuc-nang.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AppTableComponent,
    IconComponent,
    ...FORM_CONTROL_MODULE,
    ...MATERIAL_MODULE,
  ],
})
export class PhanQuyenChucNangComponent
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

  readonly tableConfig = {
    showPaginator: false,
  };
  readonly searchForm = new FormGroup({
    keyword: new FormControl('', { nonNullable: true }),
  });
  readonly searchItems = CAU_HINH_VAI_TRO_SEARCH_ITEMS;
  readonly permissionColumns: MtxGridColumn[] = [];
  readonly permissionKeys: PermissionToggleKey[] = [
    PERMISSION_KEY.IS_VIEW,
    PERMISSION_KEY.IS_ADD,
    PERMISSION_KEY.IS_EDIT,
    PERMISSION_KEY.IS_DELETE,
    PERMISSION_KEY.IS_DOWNLOAD,
    PERMISSION_KEY.IS_CONFIG,
  ];

  loading = false;
  saving = false;
  dataSource: PermissionMatrixRow[] = [];

  private keyword = '';
  private allRows: PermissionMatrixRow[] = [];
  private expandedRowIds = new Set<ID_TYPE>();
  private dirtyRowIds = new Set<ID_TYPE>();

  constructor(
    protected override injector: Injector,
    private readonly menuService: MenuService,
    private readonly permissionService: PermissionAdminService
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

    this.permissionColumns.push(
      ...buildPermissionColumns(templates, (field) =>
        this.getPermissionHeader(field)
      )
    );

    this.searchForm.controls.keyword.valueChanges
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((kw) => {
        this.keyword = kw;
        this.applyClientFilter();
      });

    super.ngOnInit();

    // Load permissions if data is ready
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
    return this.dirtyRowIds.size > 0;
  }

  get dirtyCount(): number {
    return this.dirtyRowIds.size;
  }

  isAllSelected(row: PermissionMatrixRow): boolean {
    return this.permissionKeys.every((key) => Number(row[key]) === 1);
  }

  isRootMenu(row: PermissionMatrixRow): boolean {
    return Number(row.level) === 0;
  }

  hasChildren(row: PermissionMatrixRow): boolean {
    return Number(row.childCount) > 0;
  }

  isExpanded(row: PermissionMatrixRow): boolean {
    return this.expandedRowIds.has(row.id);
  }

  toggleRow(row: PermissionMatrixRow, event?: Event) {
    event?.stopPropagation();
    if (!this.hasChildren(row)) return;

    if (this.isExpanded(row)) {
      this.expandedRowIds.delete(row.id);
    } else {
      this.expandedRowIds.add(row.id);
    }

    this.applyClientFilter();
  }

  onTogglePermission(
    displayRow: PermissionMatrixRow,
    field: PermissionToggleKey | 'allPermissions',
    event: MatSlideToggleChange | { checked: boolean }
  ) {
    if (this.isRootMenu(displayRow)) return;

    const sourceRow = this.allRows.find((row) => row.id === displayRow.id);
    if (!sourceRow) return;

    if (field === 'allPermissions') {
      const nextValue = event.checked ? 1 : 0;
      this.permissionKeys.forEach((key) => {
        sourceRow[key] = nextValue;
      });
    } else {
      sourceRow[field] = event.checked ? 1 : 0;
    }

    this.syncRowState(sourceRow);
    this.syncDirtyState(sourceRow);
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
    if (!this.dirtyRowIds.size) {
      this.toastr.warning('Chưa có thay đổi để lưu', 'Thông báo');
      return;
    }

    const payloads = this.allRows
      .filter((row) => this.dirtyRowIds.has(row.id))
      .map((row) => this.buildPermissionPayload(row));

    this.saving = true;

    this.permissionService
      .save(payloads)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.toastr.success(
            'Cập nhật quyền chức năng thành công',
            'Thành công'
          );
          this.loadPermissions(false);
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Cập nhật quyền chức năng thất bại',
            'Thất bại'
          );
        },
      });
  }

  resetChanges() {
    if (!this.dirtyRowIds.size) {
      this.toastr.warning('Chưa có thay đổi để hủy', 'Thông báo');
      return;
    }

    this.allRows = this.allRows.map((row) => {
      const nextRow: PermissionMatrixRow = {
        ...row,
        isView: row.originalState.isView,
        isAdd: row.originalState.isAdd,
        isEdit: row.originalState.isEdit,
        isDelete: row.originalState.isDelete,
        isDownload: row.originalState.isDownload,
        isConfig: row.originalState.isConfig,
      };

      this.syncRowState(nextRow);
      return nextRow;
    });

    this.dirtyRowIds.clear();
    this.applyClientFilter();
    this.toastr.success('Đã hoàn tác các thay đổi', 'Thành công');
  }

  loadPermissions(showLoading = true) {
    if (this.roleId == null || !this.menusSnapshot.length) return;

    if (showLoading) {
      this.loading = true;
    }

    this.permissionService
      .getByRoleId(this.roleId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (permissions) => {
          const permissionItems = this.normalizePermissionItems(
            permissions.data
          );
          this.allRows = this.buildMatrixRows(
            this.roleId!,
            this.menusSnapshot,
            permissionItems
          );
          this.dirtyRowIds.clear();
          this.expandedRowIds = new Set(
            this.allRows
              .filter((row) => this.hasChildren(row))
              .map((row) => row.id)
          );
          this.applyClientFilter();
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Không tải được quyền chức năng',
            'Thất bại'
          );
        },
      });
  }

  private countEnabledPermissions(row: PermissionMatrixRow): number {
    return this.permissionKeys.filter((key) => Number(row[key]) === 1).length;
  }

  private syncRowState(row: PermissionMatrixRow) {
    row.permissionCount = this.countEnabledPermissions(row);
    row.assigned = row.permissionCount > 0 || row.permissionId != null;
  }

  private syncDirtyState(row: PermissionMatrixRow) {
    const hasChanged = this.permissionKeys.some(
      (key) => Number(row[key]) !== Number(row.originalState[key])
    );

    if (hasChanged) {
      this.dirtyRowIds.add(row.id);
    } else {
      this.dirtyRowIds.delete(row.id);
    }

    this.dirtyStateChange.emit({
      count: this.dirtyRowIds.size,
      hasDirty: this.dirtyRowIds.size > 0,
    });
  }

  private normalizePermissionItems(data: any): PermissionResponse[] {
    if (Array.isArray(data)) {
      return data;
    }

    return data?.items ?? data?.data ?? [];
  }

  private buildMatrixRows(
    roleId: ID_TYPE,
    menus: MenuResponse[],
    permissions: PermissionResponse[]
  ): PermissionMatrixRow[] {
    const permissionMap = new Map<ID_TYPE, PermissionResponse>();
    permissions.forEach((item) => permissionMap.set(item.menuId, item));

    return this.flattenMenuRows(menus, (menu, level, childCount) => {
      const permission = permissionMap.get(menu.id);
      const row: PermissionMatrixRow = {
        ...menu,
        roleId,
        permissionId: permission?.id ?? permission?.ruleId,
        level,
        childCount,
        assigned: false,
        permissionCount: 0,
        isView: permission?.isView ?? 0,
        isAdd: permission?.isAdd ?? 0,
        isEdit: permission?.isEdit ?? 0,
        isDelete: permission?.isDelete ?? 0,
        isApprove: 0,
        isDownload: permission?.isDownload ?? 0,
        isConfig: permission?.isConfig ?? 0,
        originalState: {
          isView: permission?.isView ?? 0,
          isAdd: permission?.isAdd ?? 0,
          isEdit: permission?.isEdit ?? 0,
          isDelete: permission?.isDelete ?? 0,
          isDownload: permission?.isDownload ?? 0,
          isConfig: permission?.isConfig ?? 0,
        },
      };

      this.syncRowState(row);
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

  private applyClientFilter() {
    const keyword = this.keyword.trim().toLowerCase();

    const rows = keyword
      ? this.filterRowsByKeyword(this.allRows, keyword)
      : this.filterRowsByExpandState(this.allRows, this.expandedRowIds);

    this.dataSource = rows;
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

  private buildPermissionPayload(
    row: PermissionMatrixRow
  ): PermissionFormRequest {
    return {
      roleId: row.roleId,
      menuId: row.id,
      isView: Number(row.isView),
      isAdd: Number(row.isAdd),
      isEdit: Number(row.isEdit),
      isDelete: Number(row.isDelete),
      isDownload: Number(row.isDownload),
      isConfig: Number(row.isConfig),
    };
  }

  private getPermissionHeader(field: PermissionToggleKey): string {
    const headerMap: Record<PermissionToggleKey, string> = {
      [PERMISSION_KEY.IS_VIEW]: 'Xem',
      [PERMISSION_KEY.IS_ADD]: 'Thêm',
      [PERMISSION_KEY.IS_EDIT]: 'Sửa',
      [PERMISSION_KEY.IS_DELETE]: 'Xóa',
      [PERMISSION_KEY.IS_DOWNLOAD]: 'Tải xuống',
      [PERMISSION_KEY.IS_CONFIG]: 'Cấu hình',
    };

    return headerMap[field] ?? '';
  }
}
