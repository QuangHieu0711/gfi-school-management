import { TEXT_CONTROL } from '@model/form-control.model';
import { HeaderRows, TableColumns } from '@model/table.model';
import { ID_TYPE } from '@model/response.model';
import { TemplateRef } from '@angular/core';

import { DATA_PERMISSION_SCOPE_OPTIONS } from '@app/model/admin/data-permission.model';
import { MenuResponse } from '@app/model/admin/menu.model';
import { PERMISSION_KEY } from '@app/model/admin/permission.model';

export type PermissionToggleKey =
  | PERMISSION_KEY.IS_VIEW
  | PERMISSION_KEY.IS_ADD
  | PERMISSION_KEY.IS_EDIT
  | PERMISSION_KEY.IS_DELETE
  | PERMISSION_KEY.IS_DOWNLOAD
  | PERMISSION_KEY.IS_CONFIG;

export interface PermissionMatrixRow extends MenuResponse {
  permissionId?: ID_TYPE;
  roleId: ID_TYPE;
  level: number;
  childCount: number;
  assigned: boolean;
  permissionCount: number;
  isView: number;
  isAdd: number;
  isEdit: number;
  isDelete: number;
  isApprove: number;
  isDownload: number;
  isConfig: number;
  originalState: Record<PermissionToggleKey, number>;
}

export interface DataPermissionScopeMatrixItem {
  id?: ID_TYPE;
  scopeType: string;
  status: number;
  originalStatus: number;
}

export interface DataPermissionMatrixRow extends MenuResponse {
  dataPermissionId?: ID_TYPE;
  roleId: ID_TYPE;
  level: number;
  childCount: number;
  status: number;
  originalStatus: number;
  scopes: DataPermissionScopeMatrixItem[];
}

export type RolePermissionTab = 'function' | 'data';

export interface RolePermissionTemplates {
  menuNameTpl: TemplateRef<unknown>;
  menuCodeTpl: TemplateRef<unknown>;
  pathTpl: TemplateRef<unknown>;
  toggleTpl: TemplateRef<unknown>;
}

export const DATA_PERMISSION_SCOPE_FIELDS = DATA_PERMISSION_SCOPE_OPTIONS.map(
  (item) => item.scopeType
);

export const DATA_PERMISSION_GROUPED_HEADER_ROWS: HeaderRows[] = [
  [
    { title: 'Chức năng', rowspan: 2, slotField: 'name' },
    { title: 'Mã chức năng', rowspan: 2, slotField: 'code' },
    { title: 'Đường dẫn', rowspan: 2, slotField: 'url' },
    { title: 'Phạm vi dữ liệu', colspan: DATA_PERMISSION_SCOPE_OPTIONS.length },
  ],
];

export function buildPermissionColumns(
  templates: RolePermissionTemplates,
  getPermissionHeader: (field: PermissionToggleKey) => string
): TableColumns[] {
  const permissionKeys: PermissionToggleKey[] = [
    PERMISSION_KEY.IS_VIEW,
    PERMISSION_KEY.IS_ADD,
    PERMISSION_KEY.IS_EDIT,
    PERMISSION_KEY.IS_DELETE,
    PERMISSION_KEY.IS_DOWNLOAD,
    PERMISSION_KEY.IS_CONFIG,
  ];

  return [
    {
      header: 'Chức năng',
      field: 'name',
      cellTemplate: templates.menuNameTpl,
    },
    {
      header: 'Mã chức năng',
      field: 'code',
      cellTemplate: templates.menuCodeTpl,
    },
    {
      header: 'Đường dẫn',
      field: 'url',
      cellTemplate: templates.pathTpl,
      minWidth: 150,
    },
    ...permissionKeys.map((field) => ({
      header: getPermissionHeader(field),
      field,
      class: 'text-center',
      cellTemplate: templates.toggleTpl,
    })),
    {
      header: 'Tất cả',
      field: 'allPermissions',
      class: 'text-center',
      cellTemplate: templates.toggleTpl,
    },
  ];
}

export function buildDataPermissionColumns(
  templates: RolePermissionTemplates
): TableColumns[] {
  return [
    {
      header: 'Chức năng',
      field: 'name',
      cellTemplate: templates.menuNameTpl,
      rowspan: 2,
    },
    {
      header: 'Mã chức năng',
      field: 'code',
      cellTemplate: templates.menuCodeTpl,
      rowspan: 2,
    },
    {
      header: 'Đường dẫn',
      field: 'url',
      cellTemplate: templates.pathTpl,
      rowspan: 2,
      minWidth: 180,
    },
    ...DATA_PERMISSION_SCOPE_OPTIONS.map((scope) => ({
      header: scope.label,
      field: scope.scopeType,
      class: 'text-center',
      cellTemplate: templates.toggleTpl,
      width:
        scope.scopeType === 'SELF'
          ? '190px'
          : scope.scopeType === 'UNIT'
            ? '170px'
            : '120px',
    })),
  ];
}

export const CAU_HINH_VAI_TRO_SEARCH_ITEMS = [
  TEXT_CONTROL({
    controlName: 'keyword',
    placeholder: 'Tìm theo tên, mã hoặc đường dẫn chức năng',
    required: false,
    showLabel: false,
    maxLength: 255,
  }),
];
