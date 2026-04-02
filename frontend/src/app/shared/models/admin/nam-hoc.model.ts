import {
  DATE_CONTROL,
  SELECT_CONTROL,
  TEXTAREA_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum NAM_HOC_KEY {
  ID = 'id',
  CODE = 'code',
  NAME = 'name',
  START_DATE = 'startDate',
  END_DATE = 'endDate',
  STATUS = 'status',
  IS_CURRENT = 'isCurrent',
  DESCRIPTION = 'description',
}

export const NAM_HOC_API_ENDPOINT = {
  BASE_PATH: 'school-years',
  FILTER: 'search',
  OPTIONS: 'options',
};

export interface NamHocOptionResponse {
  id: ID_TYPE;
  name: string;
}

export const SCHOOL_YEAR_STATUS_OPTIONS = [
  { value: 0, label: 'Lập kế hoạch' },
  { value: 1, label: 'Đang diễn ra' },
  { value: 2, label: 'Đã kết thúc' },
];

export const BOOLEAN_OPTIONS = [
  { value: true, label: 'Có' },
  { value: false, label: 'Không' },
];

export interface NamHocFilter {
  [NAM_HOC_KEY.CODE]?: string;
  [NAM_HOC_KEY.NAME]?: string;
  [NAM_HOC_KEY.STATUS]?: number;
  [NAM_HOC_KEY.IS_CURRENT]?: boolean;
}

export interface NamHocFilterRequest extends TableRequest {
  pageNow?: number;
  filter?: NamHocFilter;
}

export interface NamHocFormRequest {
  [NAM_HOC_KEY.ID]?: ID_TYPE;
  [NAM_HOC_KEY.CODE]: string;
  [NAM_HOC_KEY.NAME]: string;
  [NAM_HOC_KEY.START_DATE]: string;
  [NAM_HOC_KEY.END_DATE]: string;
  [NAM_HOC_KEY.STATUS]: number;
  [NAM_HOC_KEY.IS_CURRENT]: boolean;
  [NAM_HOC_KEY.DESCRIPTION]?: string;
}

export interface NamHocResponse extends TableDataSource {
  [NAM_HOC_KEY.ID]: ID_TYPE;
  [NAM_HOC_KEY.CODE]?: string;
  [NAM_HOC_KEY.NAME]?: string;
  [NAM_HOC_KEY.START_DATE]?: string;
  [NAM_HOC_KEY.END_DATE]?: string;
  [NAM_HOC_KEY.STATUS]?: number;
  [NAM_HOC_KEY.IS_CURRENT]?: boolean;
  [NAM_HOC_KEY.DESCRIPTION]?: string;
}

export const NAM_HOC_FILTER_FORM = [
  TEXT_CONTROL({
    controlName: NAM_HOC_KEY.CODE,
    placeholder: 'Tìm theo mã năm học',
    required: false,
    maxLength: 50,
  }),
  TEXT_CONTROL({
    controlName: NAM_HOC_KEY.NAME,
    placeholder: 'Tìm theo tên năm học',
    required: false,
    maxLength: 255,
  }),
  SELECT_CONTROL({
    controlName: NAM_HOC_KEY.STATUS,
    placeholder: 'Trạng thái',
    required: false,
    clearable: true,
    listOption: SCHOOL_YEAR_STATUS_OPTIONS,
  }),
  SELECT_CONTROL({
    controlName: NAM_HOC_KEY.IS_CURRENT,
    placeholder: 'Đang áp dụng',
    required: false,
    clearable: true,
    listOption: BOOLEAN_OPTIONS,
  }),
];

export const NAM_HOC_FORM = [
  TEXT_CONTROL({
    controlName: NAM_HOC_KEY.CODE,
    label: 'Mã năm học',
    placeholder: 'Ví dụ: NH-2026',
    required: true,
    maxLength: 50,
  }),
  TEXT_CONTROL({
    controlName: NAM_HOC_KEY.NAME,
    label: 'Tên năm học',
    placeholder: 'Ví dụ: Năm học 2026 - 2027',
    required: true,
    maxLength: 255,
  }),
  DATE_CONTROL({
    controlName: NAM_HOC_KEY.START_DATE,
    label: 'Ngày bắt đầu',
    placeholder: 'Chọn ngày bắt đầu',
    required: true,
    dateType: 'date',
  }),
  DATE_CONTROL({
    controlName: NAM_HOC_KEY.END_DATE,
    label: 'Ngày kết thúc',
    placeholder: 'Chọn ngày kết thúc',
    required: true,
    dateType: 'date',
  }),
  SELECT_CONTROL({
    controlName: NAM_HOC_KEY.STATUS,
    label: 'Trạng thái',
    placeholder: 'Chọn trạng thái',
    required: true,
    listOption: SCHOOL_YEAR_STATUS_OPTIONS,
  }),
  SELECT_CONTROL({
    controlName: NAM_HOC_KEY.IS_CURRENT,
    label: 'Năm học hiện hành',
    placeholder: 'Chọn trạng thái áp dụng',
    required: true,
    listOption: BOOLEAN_OPTIONS,
  }),
  TEXTAREA_CONTROL({
    controlName: NAM_HOC_KEY.DESCRIPTION,
    label: 'Mô tả',
    placeholder: 'Nhập mô tả ngắn cho năm học',
    required: false,
    maxLength: 255,
    rows: 4,
  }),
];
