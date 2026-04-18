import {
  SELECT_CONTROL,
  TEXTAREA_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum PHAN_PHOI_CHUONG_TRINH_KEY {
  ID = 'id',
  WEEK = 'week',
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
  BASE_PATH: 'curriculum-distributions',
  FILTER: 'search',
};

export interface PhanPhoiChuongTrinhFilter {
  week?: number | string;
  classId?: ID_TYPE;
  subjectId?: ID_TYPE;
  lessonName?: string;
}

export interface PhanPhoiChuongTrinhFilterRequest extends TableRequest {
  pageNow?: number;
  filter?: PhanPhoiChuongTrinhFilter;
}

export interface PhanPhoiChuongTrinhFormRequest {
  week: number;
  classId: ID_TYPE;
  subjectId: ID_TYPE;
  subSubject?: string;
  period?: string;
  lessonName: string;
  note?: string;
}

export interface PhanPhoiChuongTrinhResponse extends TableDataSource {
  [PHAN_PHOI_CHUONG_TRINH_KEY.ID]: ID_TYPE;
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
  TEXT_CONTROL({
    controlName: PHAN_PHOI_CHUONG_TRINH_KEY.WEEK,
    placeholder: 'Tuần',
    required: false,
    type: 'number',
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
  TEXT_CONTROL({
    controlName: PHAN_PHOI_CHUONG_TRINH_KEY.LESSON_NAME,
    placeholder: 'Tên bài học',
    required: false,
    maxLength: 255,
  }),
];

export const PHAN_PHOI_CHUONG_TRINH_FORM = [
  TEXT_CONTROL({
    controlName: PHAN_PHOI_CHUONG_TRINH_KEY.WEEK,
    label: 'Tuần',
    placeholder: 'Nhập tuần',
    required: true,
    type: 'number',
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
    controlName: PHAN_PHOI_CHUONG_TRINH_KEY.SUB_SUBJECT,
    label: 'Phân môn',
    placeholder: 'Nhập phân môn',
    required: false,
    maxLength: 255,
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
