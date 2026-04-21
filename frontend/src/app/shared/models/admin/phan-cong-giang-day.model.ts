import {
  CHECKBOX_CONTROL,
  SELECT_CONTROL,
  TEXT_CONTROL,
  TEXTAREA_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum PHAN_CONG_GIANG_DAY_KEY {
  ID = 'id',
  UNIT_ID = 'unitId',
  STAFF_ID = 'staffId',
  STAFF_CODE = 'staffCode',
  STAFF_NAME = 'staffName',
  SCHOOL_YEAR_ID = 'schoolYearId',
  SCHOOL_YEAR_NAME = 'schoolYearName',
  SEMESTER_ID = 'semesterId',
  CLASS_ID = 'classId',
  CLASS_IDS = 'classIds',
  CLASS_NAME = 'className',
  CLASS_NAMES = 'classNames',
  SUBJECT_ID = 'subjectId',
  SUBJECT_NAME = 'subjectName',
  SUB_SUBJECT_ID = 'subSubjectId',
  SUB_SUBJECT_NAME = 'subSubjectName',
  DEPARTMENT_ID = 'departmentId',
  DEPARTMENT_NAME = 'departmentName',
  IS_HOMEROOM = 'isHomeroom',
  TEACHING_LOAD = 'teachingLoad',
  ASSIGNMENT_SUMMARY = 'assignmentSummary',
  NOTE = 'note',
}

export const PHAN_CONG_GIANG_DAY_API_ENDPOINT = {
  BASE_PATH: 'teacher-assignments',
  FILTER: 'search',
  DETAIL: 'detail',
};

export interface PhanCongGiangDayFilter {
  unitId?: ID_TYPE;
  schoolYearId?: ID_TYPE;
  semesterId?: ID_TYPE;
  staffId?: ID_TYPE;
  staffCode?: string;
  subjectId?: ID_TYPE;
}

export interface PhanCongGiangDayFilterRequest extends TableRequest {
  pageNow?: number;
  filter?: PhanCongGiangDayFilter;
}

export interface PhanCongGiangDayAssignmentItem {
  subjectId?: ID_TYPE;
  subjectName?: string;
  classIds?: ID_TYPE[];
  classNames?: string[];
}

export interface PhanCongGiangDayResponse extends TableDataSource {
  [PHAN_CONG_GIANG_DAY_KEY.ID]: ID_TYPE;
  [PHAN_CONG_GIANG_DAY_KEY.UNIT_ID]?: ID_TYPE;
  [PHAN_CONG_GIANG_DAY_KEY.STAFF_ID]?: ID_TYPE;
  [PHAN_CONG_GIANG_DAY_KEY.STAFF_CODE]?: string;
  [PHAN_CONG_GIANG_DAY_KEY.STAFF_NAME]?: string;
  [PHAN_CONG_GIANG_DAY_KEY.SCHOOL_YEAR_ID]?: ID_TYPE;
  [PHAN_CONG_GIANG_DAY_KEY.SCHOOL_YEAR_NAME]?: string;
  [PHAN_CONG_GIANG_DAY_KEY.SEMESTER_ID]?: ID_TYPE;
  [PHAN_CONG_GIANG_DAY_KEY.CLASS_ID]?: ID_TYPE;
  [PHAN_CONG_GIANG_DAY_KEY.CLASS_IDS]?: ID_TYPE[];
  [PHAN_CONG_GIANG_DAY_KEY.CLASS_NAME]?: string;
  [PHAN_CONG_GIANG_DAY_KEY.CLASS_NAMES]?: string[];
  [PHAN_CONG_GIANG_DAY_KEY.SUBJECT_ID]?: ID_TYPE;
  [PHAN_CONG_GIANG_DAY_KEY.SUBJECT_NAME]?: string;
  assignments?: PhanCongGiangDayAssignmentItem[];
  [PHAN_CONG_GIANG_DAY_KEY.SUB_SUBJECT_ID]?: ID_TYPE;
  [PHAN_CONG_GIANG_DAY_KEY.SUB_SUBJECT_NAME]?: string;
  [PHAN_CONG_GIANG_DAY_KEY.DEPARTMENT_ID]?: ID_TYPE;
  [PHAN_CONG_GIANG_DAY_KEY.DEPARTMENT_NAME]?: string;
  [PHAN_CONG_GIANG_DAY_KEY.IS_HOMEROOM]?: boolean;
  [PHAN_CONG_GIANG_DAY_KEY.TEACHING_LOAD]?: number | string;
  [PHAN_CONG_GIANG_DAY_KEY.NOTE]?: string;
  groupStt?: number;
  isGroupHead?: boolean;
  groupRowSpan?: number;
  isStripedGroup?: boolean;
}

export interface PhanCongGiangDayFormRequest {
  staffId?: ID_TYPE;
  schoolYearId?: ID_TYPE;
  semesterId?: ID_TYPE;
  classId?: ID_TYPE;
  classIds?: ID_TYPE[];
  subjectId?: ID_TYPE;
  departmentId?: ID_TYPE;
  isHomeroom?: boolean;
  teachingLoad?: number;
  note?: string;
}

export interface PhanCongGiangDayAssignmentRequest {
  subjectId: ID_TYPE;
  classIds: ID_TYPE[];
}

export interface PhanCongGiangDayUpsertRequest {
  unitId?: ID_TYPE;
  staffId?: ID_TYPE;
  schoolYearId?: ID_TYPE;
  semesterId?: ID_TYPE;
  assignments: PhanCongGiangDayAssignmentRequest[];
}

export interface PhanCongGiangDayDetailRequest {
  unitId?: ID_TYPE;
  staffId?: ID_TYPE;
  schoolYearId?: ID_TYPE;
  subjectId?: ID_TYPE;
}

export interface PhanCongGiangDayDetailResponse {
  unitId?: ID_TYPE;
  staffId?: ID_TYPE;
  schoolYearId?: ID_TYPE;
  subjectId?: ID_TYPE;
  classIds?: ID_TYPE[];
}

export const PHAN_CONG_GIANG_DAY_FORM = [
  SELECT_CONTROL({
    controlName: PHAN_CONG_GIANG_DAY_KEY.SCHOOL_YEAR_ID,
    label: 'Năm học (Chọn)',
    placeholder: 'Chọn năm học',
    required: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: PHAN_CONG_GIANG_DAY_KEY.SEMESTER_ID,
    label: 'Học kỳ',
    placeholder: 'Chọn học kỳ',
    required: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: PHAN_CONG_GIANG_DAY_KEY.CLASS_ID,
    label: 'Lớp học',
    placeholder: 'Chọn lớp học',
    required: true,
    listOption: [],
  }),
  TEXT_CONTROL({
    controlName: PHAN_CONG_GIANG_DAY_KEY.ASSIGNMENT_SUMMARY,
    label: 'Danh sách phân công',
    placeholder: 'Chưa chọn phân công',
    required: false,
    readOnly: true,
    disabled: true,
    maxLength: 1000,
  }),
  SELECT_CONTROL({
    controlName: PHAN_CONG_GIANG_DAY_KEY.SUBJECT_ID,
    label: 'Môn học',
    placeholder: 'Chọn môn học',
    required: false,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: PHAN_CONG_GIANG_DAY_KEY.DEPARTMENT_ID,
    label: 'Phân môn',
    placeholder: 'Lựa chọn',
    required: false,
    listOption: [],
  }),
  CHECKBOX_CONTROL({
    controlName: PHAN_CONG_GIANG_DAY_KEY.IS_HOMEROOM,
    label: 'Làm giáo viên chủ nhiệm',
    checked: false,
    required: false,
  }),
  TEXT_CONTROL({
    controlName: PHAN_CONG_GIANG_DAY_KEY.TEACHING_LOAD,
    label: 'Số tiết',
    type: 'number',
    placeholder: 'Số tiết (/tuần)',
    required: false,
  }),
  TEXTAREA_CONTROL({
    controlName: PHAN_CONG_GIANG_DAY_KEY.NOTE,
    label: 'Ghi chú',
    placeholder: 'Nhập ghi chú',
    required: false,
    maxLength: 500,
    rows: 3,
  }),
];
