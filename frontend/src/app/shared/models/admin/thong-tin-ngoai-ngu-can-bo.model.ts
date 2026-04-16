import {
  DATE_CONTROL,
  TEXT_CONTROL,
  TEXTAREA_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE, ITableResponse } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum STAFF_FOREIGN_LANGUAGE_KEY {
  ID = 'id',
  STAFF_ID = 'staffId',
  LANGUAGE_NAME = 'languageName',
  LANGUAGE_LEVEL = 'languageLevel',
  ISSUE_DATE = 'issueDate',
  SCORE = 'score',
  NOTE = 'note',
}

export const STAFF_FOREIGN_LANGUAGE_API_ENDPOINT = {
  BASE_PATH: 'staff-foreign-languages',
  FILTER: 'search',
};

export interface StaffForeignLanguageResponse extends TableDataSource {
  id: ID_TYPE;
  staffId?: ID_TYPE;
  languageName?: string;
  languageLevel?: string;
  issueDate?: string;
  score?: string;
  note?: string;
}

export interface StaffForeignLanguageFormRequest {
  staffId?: ID_TYPE;
  languageName?: string;
  languageLevel?: string;
  issueDate?: string;
  score?: string;
  note?: string;
}

export interface StaffForeignLanguageFilter {
  staffId?: ID_TYPE;
  languageName?: string;
  languageLevel?: string;
  issueDate?: string;
  score?: string;
}

export interface StaffForeignLanguageFilterRequest extends TableRequest {
  pageNow?: number;
  filter?: StaffForeignLanguageFilter;
}

export type StaffForeignLanguageListResponse =
  | StaffForeignLanguageResponse[]
  | ITableResponse<StaffForeignLanguageResponse>;

export const STAFF_FOREIGN_LANGUAGE_FORM = [
  TEXT_CONTROL({
    controlName: STAFF_FOREIGN_LANGUAGE_KEY.LANGUAGE_NAME,
    label: 'Ngoại ngữ',
    placeholder: 'Ngoại ngữ',
    required: true,
  }),
  TEXT_CONTROL({
    controlName: STAFF_FOREIGN_LANGUAGE_KEY.LANGUAGE_LEVEL,
    label: 'Trình độ',
    placeholder: 'Trình độ',
    required: false,
  }),
  DATE_CONTROL({
    controlName: STAFF_FOREIGN_LANGUAGE_KEY.ISSUE_DATE,
    label: 'Ngày cấp',
    placeholder: 'Ngày cấp',
    required: false,
  }),
  TEXT_CONTROL({
    controlName: STAFF_FOREIGN_LANGUAGE_KEY.SCORE,
    label: 'Điểm số',
    placeholder: 'Điểm số',
    required: false,
  }),
  TEXTAREA_CONTROL({
    controlName: STAFF_FOREIGN_LANGUAGE_KEY.NOTE,
    label: 'Ghi chú',
    placeholder: 'Ghi chú',
    required: false,
    rows: 4,
  }),
];
