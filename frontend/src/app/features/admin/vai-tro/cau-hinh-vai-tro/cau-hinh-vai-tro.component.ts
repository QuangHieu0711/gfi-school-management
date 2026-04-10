/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { finalize, forkJoin, takeUntil } from 'rxjs';

import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { COMMON_TABLE_KEY } from '@model/table.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { ID_TYPE, ITableResponse } from '@model/response.model';

import {
  CAU_HINH_VAI_TRO_SEARCH_ITEMS,
  PermissionMatrixRow,
  PermissionToggleKey,
} from '@app/model/admin/cau-hinh-vai-tro.model';
import {
  PERMISSION_KEY,
  PermissionFormRequest,
  PermissionResponse,
} from '@app/model/admin/permission.model';
import { MENU_KEY, MenuResponse } from '@app/model/admin/menu.model';
import { VaiTroResponse } from '@app/model/admin/vai-tro.model';
import { MenuService } from '@app/service/admin/menu.service';
import { PermissionAdminService } from '@app/service/admin/permission.service';
import { VaiTroService } from '@app/service/admin/vai-tro.service';

@Component({
  selector: 'cau-hinh-vai-tro',
  templateUrl: './cau-hinh-vai-tro.component.html',
  styleUrls: ['./cau-hinh-vai-tro.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    AppTableComponent,
    IconComponent,
    ...FORM_CONTROL_MODULE,
    ...MATERIAL_MODULE,
  ],
})
export class CauHinhVaiTroComponent extends ComponentBaseAbstract {
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
  readonly columns: MtxGridColumn[] = [];
  readonly permissionKeys: PermissionToggleKey[] = [
    PERMISSION_KEY.IS_VIEW,
    PERMISSION_KEY.IS_ADD,
    PERMISSION_KEY.IS_EDIT,
    PERMISSION_KEY.IS_DELETE,
    PERMISSION_KEY.IS_DOWNLOAD,
    PERMISSION_KEY.IS_CONFIG,
  ];

  roleId: ID_TYPE | null = null;
  roleDetail?: VaiTroResponse;
  loading = false;
  saving = false;
  dataSource: PermissionMatrixRow[] = [];

  private allRows: PermissionMatrixRow[] = [];
  private expandedRowIds = new Set<ID_TYPE>();
  private dirtyRowIds = new Set<ID_TYPE>();

  constructor(
    protected override injector: Injector,
    private readonly activatedRoute: ActivatedRoute,
    private readonly vaiTroService: VaiTroService,
    private readonly menuService: MenuService,
    private readonly permissionService: PermissionAdminService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.roleId = this.activatedRoute.snapshot.paramMap.get('id');

    this.columns.push(
      {
        header: 'STT',
        class: 'text-center',
        field: COMMON_TABLE_KEY.STT,
      },
      {
        header: 'Chức năng',
        field: MENU_KEY.NAME,
        cellTemplate: this.menuNameTpl,
      },
      {
        header: 'Mã chức năng',
        field: MENU_KEY.CODE,
        cellTemplate: this.menuCodeTpl,
      },
      {
        header: 'Đường dẫn',
        field: MENU_KEY.URL,
        cellTemplate: this.pathTpl,
      },
      ...this.permissionKeys.map((field) => ({
        header: this.getPermissionHeader(field),
        field,
        class: 'text-center',
        cellTemplate: this.toggleTpl,
      })),
      {
        header: 'Tất cả',
        field: 'allPermissions',
        class: 'text-center',
        cellTemplate: this.toggleTpl,
      }
    );

    if (this.roleId == null) {
      this.toastr.error('Không tìm thấy vai trò cần cấu hình', 'Thất bại');
      return;
    }

    this.searchForm.controls.keyword.valueChanges
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(() => this.applyClientFilter());

    this.refreshSnapshot();
  }

  onTogglePermission(
    displayRow: PermissionMatrixRow,
    field: PermissionToggleKey | 'allPermissions',
    event: MatSlideToggleChange
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
    this.applyClientFilter();
  }

  resetSearch() {
    this.searchForm.reset({ keyword: '' });
    this.applyClientFilter();
  }

  saveAllChanges() {
    if (this.roleId == null) return;
    if (!this.hasDirtyRows()) {
      this.toastr.warning('Chưa có thay đổi để lưu', 'Thông báo');
      return;
    }

    const payloads = this.allRows
      .filter((row) => this.isRowDirty(row))
      .map((row) => this.buildPermissionPayload(row));

    this.saving = true;

    this.permissionService
      .save(payloads)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.toastr.success('Cập nhật quyền thành công', 'Thành công');
          this.router.navigate([
            '/',
            this.navigatorEndpoint.ADMIN.BASE_PATH,
            this.navigatorEndpoint.ADMIN.VAI_TRO.BASE_PATH,
          ]);
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Cập nhật quyền thất bại',
            'Thất bại'
          );
        },
      });
  }

  resetChanges() {
    if (!this.hasDirtyRows()) {
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

  hasDirtyRows(): boolean {
    return this.dirtyRowIds.size > 0;
  }

  getDirtyCount(): number {
    return this.dirtyRowIds.size;
  }

  isRowDirty(row: PermissionMatrixRow): boolean {
    return this.dirtyRowIds.has(row.id);
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

  getRoleName(): string {
    return (
      this.roleDetail?.roleName ??
      this.roleDetail?.code ??
      `Vai trò #${this.roleId}`
    );
  }

  private refreshSnapshot(showLoading = true) {
    if (this.roleId == null) return;

    if (showLoading) {
      this.loading = true;
    }

    forkJoin({
      role: this.vaiTroService.getById(this.roleId),
      menus: this.menuService.filter({}),
      permissions: this.permissionService.getByRoleId(this.roleId),
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ role, menus, permissions }) => {
          this.roleDetail = role.data;
          const permissionItems = this.normalizePermissionItems(
            permissions.data
          );
          const normalizedMenus = this.normalizeMenus(menus.data ?? []);

          this.allRows = this.buildMatrixRows(
            this.roleId!,
            normalizedMenus,
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
              'Không tải được dữ liệu cấu hình vai trò',
            'Thất bại'
          );
        },
      });
  }

  private normalizePermissionItems(
    data:
      | ITableResponse<PermissionResponse>
      | PermissionResponse[]
      | null
      | undefined
  ): PermissionResponse[] {
    if (Array.isArray(data)) {
      return data;
    }

    return data?.items ?? data?.data ?? [];
  }

  private normalizeMenus(items: any[]): MenuResponse[] {
    // Map parentCode → parentId
    const codeToId = new Map((items ?? []).map((item) => [item.code, item.id]));

    return (items ?? []).map((raw) => ({
      ...raw,
      parentId: raw.parentCode ? (codeToId.get(raw.parentCode) ?? null) : null,
    }));
  }

  private buildMatrixRows(
    roleId: ID_TYPE,
    menus: MenuResponse[],
    permissions: PermissionResponse[]
  ): PermissionMatrixRow[] {
    const permissionMap = new Map<ID_TYPE, PermissionResponse>();
    permissions.forEach((item) => permissionMap.set(item.menuId, item));

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
    const rows: PermissionMatrixRow[] = [];
    const visited = new Set<ID_TYPE>();

    const visitNode = (menu: MenuResponse, level: number) => {
      if (visited.has(menu.id)) return;
      visited.add(menu.id);

      rows.push(
        this.createMatrixRow(roleId, menu, permissionMap.get(menu.id), level)
      );

      const children = [...(childrenMap.get(menu.id) ?? [])].sort(
        (a, b) => (a.ordinal ?? 0) - (b.ordinal ?? 0)
      );

      children.forEach((child) => visitNode(child, level + 1));
    };

    [...roots]
      .sort((a, b) => (a.ordinal ?? 0) - (b.ordinal ?? 0))
      .forEach((root) => visitNode(root, 0));

    menus
      .filter((item) => !visited.has(item.id))
      .forEach((item) => visitNode(item, 0));

    rows.forEach((row) => {
      row.childCount = menus.filter((item) => item.parentId === row.id).length;
    });

    return rows;
  }

  private createMatrixRow(
    roleId: ID_TYPE,
    menu: MenuResponse,
    permission?: PermissionResponse,
    level = 0
  ): PermissionMatrixRow {
    const row: PermissionMatrixRow = {
      ...menu,
      roleId,
      permissionId: permission?.id ?? permission?.ruleId,
      level,
      childCount: 0,
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
  }

  private applyClientFilter() {
    const keyword = this.searchForm.controls.keyword.value.trim().toLowerCase();
    let rows = [...this.allRows];

    if (keyword) {
      rows = this.filterRowsByKeyword(rows, keyword);
    } else {
      rows = this.filterRowsByExpandState(rows);
    }

    this.dataSource = rows;
    this.dataSourceTotal = rows.length;
    this.pageSize = Math.max(rows.length, 1);
  }

  private filterRowsByKeyword(
    rows: PermissionMatrixRow[],
    keyword: string
  ): PermissionMatrixRow[] {
    const rowMap = new Map(rows.map((row) => [row.id, row]));
    const resultIds = new Set<ID_TYPE>();

    rows.forEach((row) => {
      const matched =
        `${row.name ?? ''}`.toLowerCase().includes(keyword) ||
        `${row.code ?? ''}`.toLowerCase().includes(keyword) ||
        `${row.url ?? ''}`.toLowerCase().includes(keyword);

      if (!matched) return;

      let current: PermissionMatrixRow | undefined = row;
      while (current) {
        resultIds.add(current.id);
        current =
          current.parentId != null
            ? rowMap.get(current.parentId as ID_TYPE)
            : undefined;
      }
    });

    return rows.filter((row) => resultIds.has(row.id));
  }

  private filterRowsByExpandState(
    rows: PermissionMatrixRow[]
  ): PermissionMatrixRow[] {
    const rowMap = new Map(rows.map((row) => [row.id, row]));

    return rows.filter((row) => {
      let parentId = row.parentId as ID_TYPE | null | undefined;

      while (parentId != null) {
        const parent = rowMap.get(parentId);
        if (!parent) break;
        if (!this.expandedRowIds.has(parent.id)) return false;
        parentId = parent.parentId as ID_TYPE | null | undefined;
      }

      return true;
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
      return;
    }

    this.dirtyRowIds.delete(row.id);
  }

  private buildPermissionPayload(
    row: PermissionMatrixRow
  ): PermissionFormRequest {
    return {
      roleId: this.roleId!,
      menuId: row.id,
      isView: row.isView,
      isAdd: row.isAdd,
      isEdit: row.isEdit,
      isDelete: row.isDelete,
      isDownload: row.isDownload,
      isConfig: row.isConfig,
    };
  }

  private getPermissionHeader(field: PermissionToggleKey): string {
    const headers: Record<PermissionToggleKey, string> = {
      isView: 'Xem',
      isAdd: 'Thêm',
      isEdit: 'Sửa',
      isDelete: 'Xóa',
      isDownload: 'Tải',
      isConfig: 'Cấu hình',
    };

    return headers[field];
  }
}
