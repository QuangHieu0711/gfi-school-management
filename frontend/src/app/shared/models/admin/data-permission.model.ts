import { ID_TYPE } from '@model/response.model';

export enum DATA_PERMISSION_KEY {
  ID = 'id',
  ROLE_ID = 'roleId',
  ROLE_CODE = 'roleCode',
  ROLE_NAME = 'roleName',
  MENU_ID = 'menuId',
  MENU_CODE = 'menuCode',
  MENU_NAME = 'menuName',
  MENU_URL = 'menuUrl',
  ICON = 'icon',
  ORDINAL = 'ordinal',
  STATUS = 'status',
  SCOPES = 'scopes',
  SCOPE_TYPE = 'scopeType',
}

export const DATA_PERMISSION_API_ENDPOINT = {
  BASE_PATH: 'data-permissions',
  SAVE: 'save',
};

export interface DataPermissionScopeResponse {
  id?: ID_TYPE;
  [DATA_PERMISSION_KEY.SCOPE_TYPE]: string;
  [DATA_PERMISSION_KEY.STATUS]: number;
}

export interface DataPermissionResponse {
  [DATA_PERMISSION_KEY.ID]?: ID_TYPE;
  [DATA_PERMISSION_KEY.ROLE_ID]: ID_TYPE;
  [DATA_PERMISSION_KEY.ROLE_CODE]?: string | null;
  [DATA_PERMISSION_KEY.ROLE_NAME]?: string | null;
  [DATA_PERMISSION_KEY.MENU_ID]: ID_TYPE;
  [DATA_PERMISSION_KEY.MENU_CODE]?: string | null;
  [DATA_PERMISSION_KEY.MENU_NAME]?: string | null;
  [DATA_PERMISSION_KEY.MENU_URL]?: string | null;
  [DATA_PERMISSION_KEY.ICON]?: string | null;
  [DATA_PERMISSION_KEY.ORDINAL]?: number | null;
  [DATA_PERMISSION_KEY.STATUS]: number;
  [DATA_PERMISSION_KEY.SCOPES]: DataPermissionScopeResponse[];
}

export interface DataPermissionScopeFormRequest {
  [DATA_PERMISSION_KEY.SCOPE_TYPE]: string;
  [DATA_PERMISSION_KEY.STATUS]: number;
}

export interface DataPermissionFormRequest {
  [DATA_PERMISSION_KEY.MENU_ID]: ID_TYPE;
  [DATA_PERMISSION_KEY.ROLE_ID]: ID_TYPE;
  [DATA_PERMISSION_KEY.STATUS]: number;
  [DATA_PERMISSION_KEY.SCOPES]: DataPermissionScopeFormRequest[];
}

export type DataPermissionBulkFormRequest = DataPermissionFormRequest[];

export const DATA_PERMISSION_SCOPE_OPTIONS = [
  {
    scopeType: 'ALL',
    label: 'Tất cả dữ liệu',
  },
  {
    scopeType: 'SELF',
    label: 'Chỉ dữ liệu của chính mình',
  },
  {
    scopeType: 'UNIT',
    label: 'Theo đơn vị / trường',
  },
  {
    scopeType: 'GRADE',
    label: 'Theo khối',
  },
  {
    scopeType: 'CLASS',
    label: 'Theo lớp',
  },
] as const;
