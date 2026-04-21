import {
  DATE_CONTROL,
  SELECT_CONTROL,
  TEXTAREA_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';
import { BOOLEAN_OPTIONS, SCHOOL_YEAR_STATUS_OPTIONS } from './nam-hoc.model';

export enum HOC_KY_KEY {
  ID = 'id',
  SCHOOL_YEAR_ID = 'schoolYearId',
  CODE = 'code',
  NAME = 'name',
  SEMESTER_ORDER = 'semesterOrder',
  START_DATE = 'startDate',
  END_DATE = 'endDate',
  STATUS = 'status',
  IS_CURRENT = 'isCurrent',
  DESCRIPTION = 'description',
}

export const HOC_KY_API_ENDPOINT = {
  BASE_PATH: 'semesters',
  FILTER: 'search',
  OPTIONS: 'options',
};

export interface HocKyFilter {
  [HOC_KY_KEY.SCHOOL_YEAR_ID]?: ID_TYPE;
}

export interface HocKyFilterRequest extends TableRequest {
  pageNow?: number;
  [HOC_KY_KEY.SCHOOL_YEAR_ID]?: ID_TYPE;
  filter?: HocKyFilter;
}

export interface HocKyFormRequest {
  [HOC_KY_KEY.ID]?: ID_TYPE;
  [HOC_KY_KEY.SCHOOL_YEAR_ID]: ID_TYPE;
  [HOC_KY_KEY.CODE]: string;
  [HOC_KY_KEY.NAME]: string;
  [HOC_KY_KEY.SEMESTER_ORDER]: number;
  [HOC_KY_KEY.START_DATE]: string;
  [HOC_KY_KEY.END_DATE]: string;
  [HOC_KY_KEY.STATUS]: number;
  [HOC_KY_KEY.IS_CURRENT]: boolean;
  [HOC_KY_KEY.DESCRIPTION]?: string;
}

export interface HocKyResponse extends TableDataSource {
  [HOC_KY_KEY.ID]: ID_TYPE;
  [HOC_KY_KEY.SCHOOL_YEAR_ID]?: ID_TYPE;
  [HOC_KY_KEY.CODE]?: string;
  [HOC_KY_KEY.NAME]?: string;
  [HOC_KY_KEY.SEMESTER_ORDER]?: number;
  [HOC_KY_KEY.START_DATE]?: string;
  [HOC_KY_KEY.END_DATE]?: string;
  [HOC_KY_KEY.STATUS]?: number;
  [HOC_KY_KEY.IS_CURRENT]?: boolean;
  [HOC_KY_KEY.DESCRIPTION]?: string;
}

export const HOC_KY_FORM = [
  TEXT_CONTROL({
    controlName: HOC_KY_KEY.CODE,
    label: 'Mã học kỳ',
    placeholder: 'Ví dụ: HK1',
    required: true,
    maxLength: 50,
  }),
  TEXT_CONTROL({
    controlName: HOC_KY_KEY.NAME,
    label: 'Tên học kỳ',
    placeholder: 'Ví dụ: Học kỳ 1',
    required: true,
    maxLength: 255,
  }),
  TEXT_CONTROL({
    controlName: HOC_KY_KEY.SEMESTER_ORDER,
    label: 'Thứ tự học kỳ',
    placeholder: 'Ví dụ: 1',
    type: 'number',
    required: true,
    regex: /^[0-9]+$/,
  }),
  DATE_CONTROL({
    controlName: HOC_KY_KEY.START_DATE,
    label: 'Ngày bắt đầu',
    placeholder: 'Chọn ngày bắt đầu',
    required: true,
    dateType: 'date',
  }),
  DATE_CONTROL({
    controlName: HOC_KY_KEY.END_DATE,
    label: 'Ngày kết thúc',
    placeholder: 'Chọn ngày kết thúc',
    required: true,
    dateType: 'date',
  }),
  SELECT_CONTROL({
    controlName: HOC_KY_KEY.STATUS,
    label: 'Trạng thái',
    placeholder: 'Chọn trạng thái',
    required: true,
    listOption: SCHOOL_YEAR_STATUS_OPTIONS,
  }),
  SELECT_CONTROL({
    controlName: HOC_KY_KEY.IS_CURRENT,
    label: 'Học kỳ hiện hành',
    placeholder: 'Chọn trạng thái áp dụng',
    required: true,
    listOption: BOOLEAN_OPTIONS,
  }),
  TEXTAREA_CONTROL({
    controlName: HOC_KY_KEY.DESCRIPTION,
    label: 'Mô tả',
    placeholder: 'Nhập mô tả ngắn cho học kỳ',
    required: false,
    maxLength: 255,
    rows: 4,
  }),
];
