import { CommonModule } from '@angular/common';
import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { finalize, forkJoin } from 'rxjs';

import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { COMMON_TABLE_KEY } from '@model/table.model';
import { ComponentBaseAbstract } from '@layout';
import { MATERIAL_MODULE } from '@modules';
import { ID_TYPE, IResponse, ITableResponse } from '@model/response.model';

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

type PermissionToggleKey =
  | PERMISSION_KEY.IS_VIEW
  | PERMISSION_KEY.IS_ADD
  | PERMISSION_KEY.IS_EDIT
  | PERMISSION_KEY.IS_DELETE
  | PERMISSION_KEY.IS_APPROVE
  | PERMISSION_KEY.IS_DOWNLOAD;

interface PermissionMatrixRow extends MenuResponse {
  permissionId?: ID_TYPE;
  roleId: ID_TYPE;
  level: number;
  assigned: boolean;
  permissionCount: number;
  isView: number;
  isAdd: number;
  isEdit: number;
  isDelete: number;
  isApprove: number;
  isDownload: number;
  originalState: Record<PermissionToggleKey, number>;
}

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
    ...MATERIAL_MODULE,
  ],
})
export class CauHinhVaiTroComponent extends ComponentBaseAbstract {
  @ViewChild('menuNameTpl', { static: true })
  menuNameTpl!: TemplateRef<unknown>;

  @ViewChild('pathTpl', { static: true })
  pathTpl!: TemplateRef<unknown>;

  @ViewChild('toggleTpl', { static: true })
  toggleTpl!: TemplateRef<unknown>;

  readonly tableConfig = {
    showPaginator: false,
  };
  readonly filterForm = new FormGroup({
    keyword: new FormControl('', { nonNullable: true }),
    assignedOnly: new FormControl(false, { nonNullable: true }),
  });
  readonly columns: MtxGridColumn[] = [];
  readonly permissionKeys: PermissionToggleKey[] = [
    PERMISSION_KEY.IS_VIEW,
    PERMISSION_KEY.IS_ADD,
    PERMISSION_KEY.IS_EDIT,
    PERMISSION_KEY.IS_DELETE,
    PERMISSION_KEY.IS_APPROVE,
    PERMISSION_KEY.IS_DOWNLOAD,
  ];

  roleId: ID_TYPE | null = null;
  roleDetail?: VaiTroResponse;
  loading = false;
  saving = false;
  dataSource: PermissionMatrixRow[] = [];

  private allRows: PermissionMatrixRow[] = [];

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
        header: 'Đường dẫn',
        field: MENU_KEY.URL,
        cellTemplate: this.pathTpl,
      },
      {
        header: 'Tất cả',
        field: 'allPermissions',
        class: 'text-center',
        cellTemplate: this.toggleTpl,
      },
      ...this.permissionKeys.map((field) => ({
        header: this.getPermissionHeader(field),
        field,
        class: 'text-center',
        cellTemplate: this.toggleTpl,
      })),
    );

    if (this.roleId == null) {
      this.toastr.error('Không tìm thấy vai trò cần cấu hình', 'Thất bại');
      return;
    }

    this.refreshSnapshot();
  }

  onKeywordChange() {
    this.applyClientFilter();
  }

  onAssignedOnlyChange() {
    this.refreshSnapshot();
  }

  onTogglePermission(
    row: PermissionMatrixRow,
    field: PermissionToggleKey | 'allPermissions',
    event: MatSlideToggleChange
  ) {
    if (field === 'allPermissions') {
      this.permissionKeys.forEach((key) => {
        row[key] = event.checked ? 1 : 0;
      });
    } else {
      row[field] = event.checked ? 1 : 0;
    }

    this.syncRowState(row);
  }

  saveAllChanges() {
    if (this.roleId == null || !this.hasDirtyRows()) return;

    const dirtyRows = this.allRows.filter((row) => this.isRowDirty(row));
    const requests = dirtyRows.map((row) => this.buildSaveRequest(row));

    this.saving = true;

    forkJoin(requests)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.toastr.success('Cập nhật quyền thành công', 'Thành công');
          this.refreshSnapshot(false);
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
    this.allRows = this.allRows.map((row) => {
      const nextRow: PermissionMatrixRow = {
        ...row,
        isView: row.originalState.isView,
        isAdd: row.originalState.isAdd,
        isEdit: row.originalState.isEdit,
        isDelete: row.originalState.isDelete,
        isApprove: row.originalState.isApprove,
        isDownload: row.originalState.isDownload,
      };

      this.syncRowState(nextRow);
      return nextRow;
    });

    this.applyClientFilter();
  }

  hasDirtyRows(): boolean {
    return this.allRows.some((row) => this.isRowDirty(row));
  }

  getDirtyCount(): number {
    return this.allRows.filter((row) => this.isRowDirty(row)).length;
  }

  isRowDirty(row: PermissionMatrixRow): boolean {
    return this.permissionKeys.some(
      (key) => Number(row[key]) !== Number(row.originalState[key])
    );
  }

  isAllSelected(row: PermissionMatrixRow): boolean {
    return this.permissionKeys.every((key) => Number(row[key]) === 1);
  }

  getRoleName(): string {
    return this.roleDetail?.roleName ?? this.roleDetail?.code ?? `Vai trò #${this.roleId}`;
  }

  private refreshSnapshot(showLoading = true) {
    if (this.roleId == null) return;

    if (showLoading) {
      this.loading = true;
    }

    forkJoin({
      role: this.vaiTroService.getById(this.roleId),
      menus: this.menuService.filter({}),
      permissions: this.permissionService.filter({
        pageNow: 1,
        pageSize: 1000,
        filter: {
          roleId: this.roleId,
        },
      }),
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ role, menus, permissions }) => {
          this.roleDetail = role.data;
          const permissionItems = this.normalizePermissionItems(permissions.data);
          this.allRows = this.buildMatrixRows(this.roleId!, menus.data ?? [], permissionItems);
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
    data: ITableResponse<PermissionResponse> | PermissionResponse[] | null | undefined
  ): PermissionResponse[] {
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

      rows.push(this.createMatrixRow(roleId, menu, permissionMap.get(menu.id), level));

      const children = [...(childrenMap.get(menu.id) ?? [])].sort((a, b) =>
        `${a.name}`.localeCompare(`${b.name}`, 'vi')
      );

      children.forEach((child) => visitNode(child, level + 1));
    };

    [...roots]
      .sort((a, b) => `${a.name}`.localeCompare(`${b.name}`, 'vi'))
      .forEach((root) => visitNode(root, 0));

    menus
      .filter((item) => !visited.has(item.id))
      .forEach((item) => visitNode(item, 0));

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
      assigned: false,
      permissionCount: 0,
      isView: permission?.isView ?? 0,
      isAdd: permission?.isAdd ?? 0,
      isEdit: permission?.isEdit ?? 0,
      isDelete: permission?.isDelete ?? 0,
      isApprove: permission?.isApprove ?? 0,
      isDownload: permission?.isDownload ?? 0,
      originalState: {
        isView: permission?.isView ?? 0,
        isAdd: permission?.isAdd ?? 0,
        isEdit: permission?.isEdit ?? 0,
        isDelete: permission?.isDelete ?? 0,
        isApprove: permission?.isApprove ?? 0,
        isDownload: permission?.isDownload ?? 0,
      },
    };

    this.syncRowState(row);
    return row;
  }

  private applyClientFilter() {
    const keyword = this.filterForm.controls.keyword.value.trim().toLowerCase();
    const assignedOnly = this.filterForm.controls.assignedOnly.value;
    let rows = [...this.allRows];

    if (assignedOnly) {
      rows = rows.filter((row) => row.assigned);
    }

    if (keyword) {
      rows = this.filterRowsByKeyword(rows, keyword);
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

  private countEnabledPermissions(row: PermissionMatrixRow): number {
    return this.permissionKeys.filter((key) => Number(row[key]) === 1).length;
  }

  private syncRowState(row: PermissionMatrixRow) {
    row.permissionCount = this.countEnabledPermissions(row);
    row.assigned = row.permissionCount > 0 || row.permissionId != null;
  }

  private buildSaveRequest(row: PermissionMatrixRow) {
    const payload: PermissionFormRequest = {
      roleId: this.roleId!,
      menuId: row.id,
      isView: row.isView,
      isAdd: row.isAdd,
      isEdit: row.isEdit,
      isDelete: row.isDelete,
      isApprove: row.isApprove,
      isDownload: row.isDownload,
    };

    return row.permissionId != null
      ? this.permissionService.update(row.permissionId, payload)
      : this.permissionService.create(payload);
  }

  private getPermissionHeader(field: PermissionToggleKey): string {
    const headers: Record<PermissionToggleKey, string> = {
      isView: 'Xem',
      isAdd: 'Thêm',
      isEdit: 'Sửa',
      isDelete: 'Xóa',
      isApprove: 'Duyệt',
      isDownload: 'Tải',
    };

    return headers[field];
  }
}
