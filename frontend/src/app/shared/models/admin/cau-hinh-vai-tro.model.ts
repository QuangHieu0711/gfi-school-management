import { TEXT_CONTROL } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';

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

export const CAU_HINH_VAI_TRO_SEARCH_ITEMS = [
  TEXT_CONTROL({
    controlName: 'keyword',
    placeholder: 'Tìm theo tên, mã hoặc đường dẫn chức năng',
    required: false,
    showLabel: false,
    maxLength: 255,
  }),
];
