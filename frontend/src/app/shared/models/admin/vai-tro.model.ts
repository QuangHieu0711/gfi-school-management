import {
  SELECT_CONTROL,
  TEXTAREA_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum VAI_TRO_KEY {
  ID = 'id',
  CODE = 'code',
  ROLE_NAME = 'roleName',
  DESCRIPTION = 'description',
  STATUS = 'status',
}

export const VAI_TRO_API_ENDPOINT = {
  BASE_PATH: 'roles',
  FILTER: 'search',
  OPTIONS: 'options',
};

export interface VaiTroOptionResponse {
  id: ID_TYPE;
  name: string;
  code: string;
}

export interface VaiTroFilter {
  [VAI_TRO_KEY.ROLE_NAME]?: string;
  [VAI_TRO_KEY.STATUS]?: number;
  [VAI_TRO_KEY.CODE]?: string;
}

export interface VaiTroFilterRequest extends TableRequest {
  filter?: VaiTroFilter;
}

export interface VaiTroFormRequest {
  [VAI_TRO_KEY.ID]?: ID_TYPE;
  [VAI_TRO_KEY.CODE]: string;
  [VAI_TRO_KEY.ROLE_NAME]: string;
  [VAI_TRO_KEY.DESCRIPTION]?: string;
  [VAI_TRO_KEY.STATUS]?: number;
}

export interface VaiTroResponse extends TableDataSource {
  [VAI_TRO_KEY.ID]: ID_TYPE;
  [VAI_TRO_KEY.CODE]: string;
  [VAI_TRO_KEY.ROLE_NAME]?: string;
  [VAI_TRO_KEY.DESCRIPTION]?: string;
  [VAI_TRO_KEY.STATUS]?: number;
}

export const VAI_TRO_FILTER_FORM = [
  TEXT_CONTROL({
    controlName: VAI_TRO_KEY.ROLE_NAME,
    placeholder: 'Tìm kiếm theo tên vai trò',
    required: false,
    maxLength: 255,
  }),
  SELECT_CONTROL({
    controlName: VAI_TRO_KEY.STATUS,
    placeholder: 'Trạng thái',
    required: false,
    clearable: true,
    listOption: [
      { value: 1, label: 'Hoạt động' },
      { value: 0, label: 'Không hoạt động' },
    ],
  }),
];

export const VAI_TRO_FORM = [
  TEXT_CONTROL({
    controlName: VAI_TRO_KEY.CODE,
    label: 'Mã vai trò',
    placeholder: 'Ví dụ: ROLE_ADMIN',
    required: true,
    maxLength: 50,
  }),
  TEXT_CONTROL({
    controlName: VAI_TRO_KEY.ROLE_NAME,
    label: 'Tên vai trò',
    placeholder: 'Tên vai trò',
    required: true,
    maxLength: 100,
  }),
  TEXTAREA_CONTROL({
    controlName: VAI_TRO_KEY.DESCRIPTION,
    label: 'Mô tả',
    placeholder: 'Mô tả',
    required: false,
    maxLength: 255,
    rows: 4,
  }),
  SELECT_CONTROL({
    controlName: VAI_TRO_KEY.STATUS,
    label: 'Trạng thái',
    placeholder: 'Chọn trạng thái',
    required: true,
    clearable: true,
    listOption: [
      { value: 1, label: 'Hoạt động' },
      { value: 0, label: 'Không hoạt động' },
    ],
  }),
];
