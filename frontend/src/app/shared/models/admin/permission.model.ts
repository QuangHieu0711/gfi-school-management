import { ID_TYPE } from '@model/response.model';
import { TableRequest } from '@model/table.model';

export enum PERMISSION_KEY {
  ID = 'id',
  RULE_ID = 'ruleId',
  ROLE_ID = 'roleId',
  MENU_ID = 'menuId',
  MENU_CODE = 'menuCode',
  MENU_NAME = 'menuName',
  MENU_URL = 'menuUrl',
  PARENT_ID = 'parentId',
  IS_VIEW = 'isView',
  IS_ADD = 'isAdd',
  IS_EDIT = 'isEdit',
  IS_DELETE = 'isDelete',
  IS_DOWNLOAD = 'isDownload',
  IS_CONFIG = 'isConfig',
}

export const PERMISSION_API_ENDPOINT = {
  BASE_PATH: 'permissions',
  FILTER: 'search',
};

export interface PermissionFilter {
  roleId?: ID_TYPE;
  menuId?: ID_TYPE;
}

export interface PermissionFilterRequest extends TableRequest {
  filter?: PermissionFilter;
}

export interface PermissionFormRequest {
  [PERMISSION_KEY.ROLE_ID]: ID_TYPE;
  [PERMISSION_KEY.MENU_ID]: ID_TYPE;
  [PERMISSION_KEY.IS_VIEW]: number;
  [PERMISSION_KEY.IS_ADD]: number;
  [PERMISSION_KEY.IS_EDIT]: number;
  [PERMISSION_KEY.IS_DELETE]: number;
  [PERMISSION_KEY.IS_DOWNLOAD]: number;
  [PERMISSION_KEY.IS_CONFIG]: number;
}

export type PermissionBulkFormRequest = PermissionFormRequest[];

export interface PermissionResponse {
  [PERMISSION_KEY.ID]?: ID_TYPE;
  [PERMISSION_KEY.RULE_ID]?: ID_TYPE;
  [PERMISSION_KEY.ROLE_ID]: ID_TYPE;
  [PERMISSION_KEY.MENU_ID]: ID_TYPE;
  [PERMISSION_KEY.MENU_CODE]?: string | null;
  [PERMISSION_KEY.MENU_NAME]?: string | null;
  [PERMISSION_KEY.MENU_URL]?: string | null;
  [PERMISSION_KEY.PARENT_ID]?: ID_TYPE | null;
  [PERMISSION_KEY.IS_VIEW]: number;
  [PERMISSION_KEY.IS_ADD]: number;
  [PERMISSION_KEY.IS_EDIT]: number;
  [PERMISSION_KEY.IS_DELETE]: number;
  [PERMISSION_KEY.IS_DOWNLOAD]: number;
  [PERMISSION_KEY.IS_CONFIG]: number;
}
