import {
  SELECT_CONTROL,
  TEXTAREA_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum MON_HOC_KEY {
  ID = 'id',
  CODE = 'code',
  NAME = 'name',
  TYPE = 'type',
  DESCRIPTION = 'description',
  STATUS = 'status',
}

export const MON_HOC_API_ENDPOINT = {
  BASE_PATH: 'subjects',
  FILTER: 'search',
  OPTIONS: 'options',
};

export interface MonHocOptionResponse {
  id: ID_TYPE;
  name: string;
}

export const MON_HOC_TYPE_OPTIONS = [
  { value: 0, label: 'Bắt buộc' },
  { value: 1, label: 'Tự chọn' },
];

export const MON_HOC_STATUS_OPTIONS = [
  { value: 1, label: 'Hoạt động' },
  { value: 0, label: 'Không hoạt động' },
];

export interface MonHocFilter {
  subject?: string;
  type?: number;
  status?: number;
}

export interface MonHocFilterRequest extends TableRequest {
  pageNow?: number;
  filter?: MonHocFilter;
}

export interface MonHocFormRequest {
  [MON_HOC_KEY.ID]?: ID_TYPE;
  [MON_HOC_KEY.CODE]: string;
  [MON_HOC_KEY.NAME]: string;
  [MON_HOC_KEY.TYPE]: number;
  [MON_HOC_KEY.DESCRIPTION]?: string;
  [MON_HOC_KEY.STATUS]: number;
}

export interface MonHocResponse extends TableDataSource {
  [MON_HOC_KEY.ID]: ID_TYPE;
  [MON_HOC_KEY.CODE]: string;
  [MON_HOC_KEY.NAME]: string;
  [MON_HOC_KEY.TYPE]: number;
  [MON_HOC_KEY.DESCRIPTION]?: string;
  [MON_HOC_KEY.STATUS]: number;
}

export const MON_HOC_FILTER_FORM = [
  TEXT_CONTROL({
    controlName: MON_HOC_KEY.NAME,
    placeholder: 'Tìm kiếm theo mã hoặc tên môn học',
    required: false,
    maxLength: 255,
  }),
  SELECT_CONTROL({
    controlName: MON_HOC_KEY.STATUS,
    placeholder: 'Trạng thái',
    required: false,
    clearable: true,
    listOption: MON_HOC_STATUS_OPTIONS,
  }),
  SELECT_CONTROL({
    controlName: MON_HOC_KEY.TYPE,
    placeholder: 'Loại môn học',
    required: false,
    clearable: true,
    listOption: MON_HOC_TYPE_OPTIONS,
  }),
];

export const MON_HOC_FORM = [
  TEXT_CONTROL({
    controlName: MON_HOC_KEY.CODE,
    label: 'Mã môn học',
    placeholder: 'Ví dụ: TOAN',
    required: true,
    maxLength: 50,
  }),
  TEXT_CONTROL({
    controlName: MON_HOC_KEY.NAME,
    label: 'Tên môn học',
    placeholder: 'Ví dụ: Toán',
    required: true,
    maxLength: 255,
  }),
  SELECT_CONTROL({
    controlName: MON_HOC_KEY.TYPE,
    label: 'Loại môn học',
    placeholder: 'Chọn loại môn học',
    required: true,
    clearable: true,
    listOption: MON_HOC_TYPE_OPTIONS,
  }),
  SELECT_CONTROL({
    controlName: MON_HOC_KEY.STATUS,
    label: 'Trạng thái',
    placeholder: 'Chọn trạng thái',
    required: true,
    clearable: true,
    listOption: MON_HOC_STATUS_OPTIONS,
  }),
  TEXTAREA_CONTROL({
    controlName: MON_HOC_KEY.DESCRIPTION,
    label: 'Mô tả',
    placeholder: 'Nhập mô tả ngắn cho môn học',
    required: false,
    maxLength: 255,
    rows: 4,
  }),
];
