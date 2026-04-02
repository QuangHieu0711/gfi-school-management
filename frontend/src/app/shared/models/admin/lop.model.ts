import {
  SELECT_CONTROL,
  TEXTAREA_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum LOP_KEY {
  ID = 'id',
  CODE = 'code',
  NAME = 'name',
  UNIT_ID = 'unitId',
  UNIT_NAME = 'unitName',
  GRADE_LEVEL_ID = 'gradeLevelId',
  GRADE_LEVEL_NAME = 'gradeLevelName',
  GRADE_NUMBER = 'gradeNumber',
  SCHOOL_YEAR_ID = 'schoolYearId',
  SCHOOL_YEAR_NAME = 'schoolYearName',
  STATUS = 'status',
  DESCRIPTION = 'description',
}

export const LOP_API_ENDPOINT = {
  BASE_PATH: 'classes',
  FILTER: 'search',
  OPTIONS: 'options',
};

export interface LopFilter {
  className?: string;
  unitId?: ID_TYPE;
  gradeLevelId?: ID_TYPE;
  schoolYearId?: ID_TYPE;
  status?: number;
}

export interface LopFilterRequest extends TableRequest {
  pageNow?: number;
  filter?: LopFilter;
}

export interface LopFormRequest {
  [LOP_KEY.ID]?: ID_TYPE;
  [LOP_KEY.CODE]: string;
  [LOP_KEY.NAME]: string;
  [LOP_KEY.UNIT_ID]: ID_TYPE;
  [LOP_KEY.GRADE_LEVEL_ID]: ID_TYPE;
  [LOP_KEY.SCHOOL_YEAR_ID]: ID_TYPE;
  [LOP_KEY.STATUS]: number;
  [LOP_KEY.DESCRIPTION]?: string;
}

export interface LopResponse extends TableDataSource {
  [LOP_KEY.ID]: ID_TYPE;
  [LOP_KEY.CODE]: string;
  [LOP_KEY.NAME]: string;
  [LOP_KEY.UNIT_ID]: ID_TYPE;
  [LOP_KEY.UNIT_NAME]?: string;
  [LOP_KEY.GRADE_LEVEL_ID]: ID_TYPE;
  [LOP_KEY.GRADE_LEVEL_NAME]?: string;
  [LOP_KEY.GRADE_NUMBER]?: number;
  [LOP_KEY.SCHOOL_YEAR_ID]: ID_TYPE;
  [LOP_KEY.SCHOOL_YEAR_NAME]?: string;
  [LOP_KEY.STATUS]: number;
  [LOP_KEY.DESCRIPTION]?: string;
}

export const LOP_STATUS_OPTIONS = [
  { value: 1, label: 'Hoạt động' },
  { value: 0, label: 'Không hoạt động' },
];

export const LOP_FILTER_FORM = [
  TEXT_CONTROL({
    controlName: LOP_KEY.NAME,
    placeholder: 'Tìm kiếm theo mã hoặc tên lớp',
    required: false,
    maxLength: 255,
  }),
  SELECT_CONTROL({
    controlName: LOP_KEY.UNIT_ID,
    placeholder: 'Đơn vị',
    required: false,
    clearable: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: LOP_KEY.GRADE_LEVEL_ID,
    placeholder: 'Khối',
    required: false,
    clearable: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: LOP_KEY.SCHOOL_YEAR_ID,
    placeholder: 'Năm học',
    required: false,
    clearable: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: LOP_KEY.STATUS,
    placeholder: 'Trạng thái',
    required: false,
    clearable: true,
    listOption: LOP_STATUS_OPTIONS,
  }),
];

export const LOP_FORM = [
  TEXT_CONTROL({
    controlName: LOP_KEY.CODE,
    label: 'Mã lớp',
    placeholder: 'Ví dụ: 1A1',
    required: true,
    maxLength: 50,
  }),
  TEXT_CONTROL({
    controlName: LOP_KEY.NAME,
    label: 'Tên lớp',
    placeholder: 'Ví dụ: Lớp 1A1',
    required: true,
    maxLength: 255,
  }),
  SELECT_CONTROL({
    controlName: LOP_KEY.UNIT_ID,
    label: 'Đơn vị',
    placeholder: 'Chọn đơn vị',
    required: true,
    clearable: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: LOP_KEY.GRADE_LEVEL_ID,
    label: 'Khối',
    placeholder: 'Chọn khối',
    required: true,
    clearable: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: LOP_KEY.SCHOOL_YEAR_ID,
    label: 'Năm học',
    placeholder: 'Chọn năm học',
    required: true,
    clearable: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: LOP_KEY.STATUS,
    label: 'Trạng thái',
    placeholder: 'Chọn trạng thái',
    required: true,
    clearable: true,
    listOption: LOP_STATUS_OPTIONS,
  }),
  TEXTAREA_CONTROL({
    controlName: LOP_KEY.DESCRIPTION,
    label: 'Mô tả',
    placeholder: 'Nhập mô tả ngắn cho lớp',
    required: false,
    maxLength: 255,
    rows: 4,
  }),
];
