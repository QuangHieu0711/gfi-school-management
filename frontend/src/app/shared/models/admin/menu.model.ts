import { SELECT_CONTROL, TEXT_CONTROL } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource } from '@model/table.model';

export enum MENU_KEY {
  ID = 'id',
  CODE = 'code',
  NAME = 'name',
  ICON = 'icon',
  URL = 'url',
  ORDINAL = 'ordinal',
  PARENT_ID = 'parentId',
  PARENT_NAME = 'parentName',
  PARENT_CODE = 'parentCode',
  LEVEL = 'level',
  CHILDREN_COUNT = 'childrenCount',
}

export const MENU_API_ENDPOINT = {
  BASE_PATH: 'menus',
  FILTER: 'search',
  OPTIONS: 'options',
};

export interface MenuOptionResponse {
  id: ID_TYPE;
  code?: string | null;
  name: string;
  parentId?: ID_TYPE | null;
}

export interface MenuFilterRequest {
  menu?: string;
}

export interface MenuFormRequest {
  [MENU_KEY.PARENT_ID]?: ID_TYPE | null;
  [MENU_KEY.CODE]: string;
  [MENU_KEY.NAME]: string;
  [MENU_KEY.ICON]?: string | null;
  [MENU_KEY.URL]?: string | null;
  [MENU_KEY.ORDINAL]: number;
}

export interface MenuResponse extends TableDataSource {
  [MENU_KEY.ID]: ID_TYPE;
  [MENU_KEY.CODE]: string;
  [MENU_KEY.NAME]: string;
  [MENU_KEY.ICON]?: string | null;
  [MENU_KEY.URL]?: string | null;
  [MENU_KEY.ORDINAL]?: number | null;
  [MENU_KEY.PARENT_ID]?: ID_TYPE | null;
  [MENU_KEY.PARENT_CODE]?: string | null;
}

export interface MenuTreeRow extends MenuResponse {
  [MENU_KEY.PARENT_NAME]?: string | null;
  [MENU_KEY.LEVEL]: number;
  [MENU_KEY.CHILDREN_COUNT]: number;
}

export const MENU_FILTER_FORM = [
  TEXT_CONTROL({
    controlName: 'menu',
    placeholder: 'Tìm kiếm theo tên hoặc mã menu',
    required: false,
    maxLength: 255,
  }),
];

export const MENU_FORM = [
  SELECT_CONTROL({
    controlName: MENU_KEY.PARENT_ID,
    label: 'Menu cha',
    placeholder: 'Chọn menu cha nếu có',
    required: false,
    clearable: true,
    listOption: [],
  }),
  TEXT_CONTROL({
    controlName: MENU_KEY.CODE,
    label: 'Mã chức năng',
    placeholder: 'Ví dụ: FUNCTION_MANAGEMENT',
    required: true,
    maxLength: 100,
  }),
  TEXT_CONTROL({
    controlName: MENU_KEY.NAME,
    label: 'Tên chức năng',
    placeholder: 'Ví dụ: Quản lý chức năng',
    required: true,
    maxLength: 255,
  }),
  TEXT_CONTROL({
    controlName: MENU_KEY.ORDINAL,
    label: 'Thứ tự',
    placeholder: 'Ví dụ: 3',
    required: true,
    type: 'number',
  }),
  SELECT_CONTROL({
    controlName: MENU_KEY.ICON,
    label: 'Icon',
    placeholder: 'Chọn icon cho menu',
    required: false,
    clearable: true,
    listOption: [],
  }),
  TEXT_CONTROL({
    controlName: MENU_KEY.URL,
    label: 'Đường dẫn',
    placeholder: 'Ví dụ: /admin/menu',
    required: false,
    maxLength: 255,
  }),
];
