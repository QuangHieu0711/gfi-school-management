import {
  SELECT_CONTROL,
  TEXTAREA_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum PHAN_PHOI_CHUONG_TRINH_KEY {
  ID = 'id',
  ORDER_NUMBER = 'orderNumber',
  SCHOOL_YEAR_ID = 'schoolYearId',
  SEMESTER_ID = 'semesterId',
  CLASSROOM_ID = 'classroomId',
  WEEK = 'week',
  UNIT_ID = 'unitId',
  KHOI = 'khoi',
  CLASS_ID = 'classId',
  CLASS_NAME = 'className',
  SUBJECT_ID = 'subjectId',
  SUBJECT_NAME = 'subjectName',
  SUB_SUBJECT = 'subSubject',
  PERIOD = 'period',
  LESSON_NAME = 'lessonName',
  NOTE = 'note',
}

export const PHAN_PHOI_CHUONG_TRINH_API_ENDPOINT = {
  BASE_PATH: 'program-distributions',
  FILTER: 'search',
  IMPORT_EXCEL: 'import-excel',
  EXCEL_TEMPLATE: 'excel-template',
};

export interface PhanPhoiChuongTrinhFilter {
  week?: number | string;
  unitId?: ID_TYPE;
  khoi?: ID_TYPE;
  classId?: ID_TYPE;
  subjectId?: ID_TYPE;
}

export interface PhanPhoiChuongTrinhFilterRequest extends TableRequest {
  pageNow?: number;
  filter?: PhanPhoiChuongTrinhFilter;
}

export interface PhanPhoiChuongTrinhFormRequest {
  unitId?: ID_TYPE;
  week: number;
  weekNumber?: number;
  classId: ID_TYPE;
  classroomId?: ID_TYPE;
  subjectId: ID_TYPE;
  period?: string;
  periodPpct?: string;
  orderNumber?: number;
  lessonName: string;
  note?: string;
}

export interface PhanPhoiChuongTrinhUpdateRequest {
  weekNumber: number;
  orderNumber: number;
  periodPpct?: string;
  lessonName: string;
  note?: string;
}

export interface PhanPhoiChuongTrinhImportRequest {
  schoolYearId: ID_TYPE;
  unitId: ID_TYPE;
  classroomId: ID_TYPE;
  subjectId: ID_TYPE;
}

export interface PhanPhoiChuongTrinhResponse extends TableDataSource {
  [PHAN_PHOI_CHUONG_TRINH_KEY.ID]: ID_TYPE;
  [PHAN_PHOI_CHUONG_TRINH_KEY.ORDER_NUMBER]?: number;
  [PHAN_PHOI_CHUONG_TRINH_KEY.WEEK]?: number | string;
  [PHAN_PHOI_CHUONG_TRINH_KEY.CLASS_ID]?: ID_TYPE;
  [PHAN_PHOI_CHUONG_TRINH_KEY.CLASS_NAME]?: string;
  [PHAN_PHOI_CHUONG_TRINH_KEY.SUBJECT_ID]?: ID_TYPE;
  [PHAN_PHOI_CHUONG_TRINH_KEY.SUBJECT_NAME]?: string;
  [PHAN_PHOI_CHUONG_TRINH_KEY.SUB_SUBJECT]?: string;
  [PHAN_PHOI_CHUONG_TRINH_KEY.PERIOD]?: string;
  [PHAN_PHOI_CHUONG_TRINH_KEY.LESSON_NAME]?: string;
  [PHAN_PHOI_CHUONG_TRINH_KEY.NOTE]?: string;
}

export const PHAN_PHOI_CHUONG_TRINH_FILTER_FORM = [
  SELECT_CONTROL({
    controlName: PHAN_PHOI_CHUONG_TRINH_KEY.WEEK,
    placeholder: 'Tuần',
    required: false,
    clearable: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: PHAN_PHOI_CHUONG_TRINH_KEY.KHOI,
    placeholder: 'Khối',
    required: false,
    clearable: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: PHAN_PHOI_CHUONG_TRINH_KEY.CLASS_ID,
    placeholder: 'Tên lớp',
    required: false,
    clearable: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: PHAN_PHOI_CHUONG_TRINH_KEY.SUBJECT_ID,
    placeholder: 'Tên môn học',
    required: false,
    clearable: true,
    listOption: [],
  }),
];

export const PHAN_PHOI_CHUONG_TRINH_FORM = [
  SELECT_CONTROL({
    controlName: PHAN_PHOI_CHUONG_TRINH_KEY.WEEK,
    label: 'Tuần',
    placeholder: 'Chọn tuần',
    required: true,
    clearable: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: PHAN_PHOI_CHUONG_TRINH_KEY.CLASS_ID,
    label: 'Tên lớp',
    placeholder: 'Chọn lớp',
    required: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: PHAN_PHOI_CHUONG_TRINH_KEY.SUBJECT_ID,
    label: 'Tên môn học',
    placeholder: 'Chọn môn học',
    required: true,
    listOption: [],
  }),
  TEXT_CONTROL({
    controlName: PHAN_PHOI_CHUONG_TRINH_KEY.PERIOD,
    label: 'Tiết PPCT',
    placeholder: 'Nhập tiết PPCT',
    required: false,
    maxLength: 100,
  }),
  TEXT_CONTROL({
    controlName: PHAN_PHOI_CHUONG_TRINH_KEY.LESSON_NAME,
    label: 'Tên bài học',
    placeholder: 'Nhập tên bài học',
    required: true,
    maxLength: 255,
  }),
  TEXTAREA_CONTROL({
    controlName: PHAN_PHOI_CHUONG_TRINH_KEY.NOTE,
    label: 'Ghi chú',
    placeholder: 'Nhập ghi chú',
    required: false,
    rows: 3,
    maxLength: 500,
  }),
];
