import {
  SELECT_CONTROL,
  TEXTAREA_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum KHOI_KEY {
  ID = 'id',
  CODE = 'code',
  NAME = 'name',
  GRADE_NUMBER = 'gradeNumber',
  STATUS = 'status',
  DESCRIPTION = 'description',
}

export const KHOI_API_ENDPOINT = {
  BASE_PATH: 'grade-levels',
  FILTER: 'search',
  OPTIONS: 'options',
};

export interface KhoiOptionResponse {
  id: ID_TYPE;
  name: string;
}

export interface KhoiFilter {
  gradeLevel?: string;
  status?: number;
}

export interface KhoiFilterRequest extends TableRequest {
  pageNow?: number;
  filter?: KhoiFilter;
}

export interface KhoiFormRequest {
  [KHOI_KEY.ID]?: ID_TYPE;
  [KHOI_KEY.CODE]: string;
  [KHOI_KEY.NAME]: string;
  [KHOI_KEY.GRADE_NUMBER]: number;
  [KHOI_KEY.STATUS]: number;
  [KHOI_KEY.DESCRIPTION]?: string;
}

export interface KhoiResponse extends TableDataSource {
  [KHOI_KEY.ID]: ID_TYPE;
  [KHOI_KEY.CODE]: string;
  [KHOI_KEY.NAME]: string;
  [KHOI_KEY.GRADE_NUMBER]: number;
  [KHOI_KEY.STATUS]: number;
  [KHOI_KEY.DESCRIPTION]?: string;
}

export const KHOI_STATUS_OPTIONS = [
  { value: 1, label: 'Hoạt động' },
  { value: 0, label: 'Không hoạt động' },
];

export const KHOI_FILTER_FORM = [
  TEXT_CONTROL({
    controlName: KHOI_KEY.NAME,
    placeholder: 'Tìm kiếm theo mã hoặc tên khối',
    required: false,
    maxLength: 255,
  }),
  SELECT_CONTROL({
    controlName: KHOI_KEY.STATUS,
    placeholder: 'Trạng thái',
    required: false,
    clearable: true,
    listOption: KHOI_STATUS_OPTIONS,
  }),
];

export const KHOI_FORM = [
  TEXT_CONTROL({
    controlName: KHOI_KEY.CODE,
    label: 'Mã khối',
    placeholder: 'Ví dụ: KHOI_1',
    required: true,
    maxLength: 50,
  }),
  TEXT_CONTROL({
    controlName: KHOI_KEY.NAME,
    label: 'Tên khối',
    placeholder: 'Ví dụ: Khối 1',
    required: true,
    maxLength: 255,
  }),
  TEXT_CONTROL({
    controlName: KHOI_KEY.GRADE_NUMBER,
    label: 'Số khối',
    placeholder: 'Ví dụ: 1',
    type: 'number',
    required: true,
    regex: /^[0-9]+$/,
  }),
  SELECT_CONTROL({
    controlName: KHOI_KEY.STATUS,
    label: 'Trạng thái',
    placeholder: 'Chọn trạng thái',
    required: true,
    clearable: true,
    listOption: KHOI_STATUS_OPTIONS,
  }),
  TEXTAREA_CONTROL({
    controlName: KHOI_KEY.DESCRIPTION,
    label: 'Mô tả',
    placeholder: 'Nhập mô tả ngắn cho khối',
    required: false,
    maxLength: 255,
    rows: 4,
  }),
];
