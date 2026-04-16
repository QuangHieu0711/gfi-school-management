import {
  DATE_CONTROL,
  SELECT_CONTROL,
  TEXT_CONTROL,
  TEXTAREA_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE, ITableResponse } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum STAFF_TRAINING_KEY {
  ID = 'id',
  STAFF_ID = 'staffId',
  SCHOOL_NAME = 'schoolName',
  MAJOR = 'major',
  TRAINING_FORM = 'trainingForm',
  CERTIFICATE = 'certificate',
  FROM_DATE = 'fromDate',
  TO_DATE = 'toDate',
  NOTE = 'note',
}

export const STAFF_TRAINING_API_ENDPOINT = {
  BASE_PATH: 'staff-educations',
  FILTER: 'search',
};

export interface StaffTrainingResponse extends TableDataSource {
  id: ID_TYPE;
  staffId?: ID_TYPE;
  schoolName?: string;
  major?: string;
  trainingForm?: string;
  certificate?: string;
  fromDate?: string;
  toDate?: string;
  note?: string;
}

export interface StaffTrainingFormRequest {
  staffId?: ID_TYPE;
  schoolName?: string;
  major?: string;
  trainingForm?: string;
  certificate?: string;
  fromDate?: string;
  toDate?: string;
  note?: string;
}

export interface StaffTrainingFilter {
  staffId?: ID_TYPE;
  schoolName?: string;
  major?: string;
  trainingForm?: string;
  certificate?: string;
  fromDate?: string;
  toDate?: string;
}

export interface StaffTrainingFilterRequest extends TableRequest {
  pageNow?: number;
  filter?: StaffTrainingFilter;
}

export type StaffTrainingListResponse =
  | StaffTrainingResponse[]
  | ITableResponse<StaffTrainingResponse>;

export const STAFF_TRAINING_FORM_OPTIONS = [
  'Chính quy',
  'Vừa làm vừa học',
  'Từ xa',
  'Liên thông',
  'Văn bằng 2',
  '9+3',
  'Bồi dưỡng ngắn hạn',
].map((item) => ({
  label: item,
  value: item,
}));

export const STAFF_TRAINING_FORM = [
  TEXT_CONTROL({
    controlName: STAFF_TRAINING_KEY.SCHOOL_NAME,
    label: 'Trường đào tạo',
    placeholder: 'Trường đào tạo',
    required: true,
  }),
  TEXT_CONTROL({
    controlName: STAFF_TRAINING_KEY.MAJOR,
    label: 'Chuyên ngành',
    placeholder: 'Chuyên ngành',
    required: true,
  }),
  SELECT_CONTROL({
    controlName: STAFF_TRAINING_KEY.TRAINING_FORM,
    label: 'Hình thức đào tạo',
    placeholder: 'Hình thức đào tạo',
    required: false,
    clearable: true,
    listOption: STAFF_TRAINING_FORM_OPTIONS,
  }),
  TEXT_CONTROL({
    controlName: STAFF_TRAINING_KEY.CERTIFICATE,
    label: 'Chứng chỉ',
    placeholder: 'Chứng chỉ',
    required: false,
  }),
  DATE_CONTROL({
    controlName: STAFF_TRAINING_KEY.FROM_DATE,
    label: 'Từ ngày',
    placeholder: 'Từ ngày',
    required: true,
  }),
  DATE_CONTROL({
    controlName: STAFF_TRAINING_KEY.TO_DATE,
    label: 'Đến ngày',
    placeholder: 'Đến ngày',
    required: false,
  }),
  TEXTAREA_CONTROL({
    controlName: STAFF_TRAINING_KEY.NOTE,
    label: 'Ghi chú',
    placeholder: 'Ghi chú',
    required: false,
    rows: 4,
  }),
];
