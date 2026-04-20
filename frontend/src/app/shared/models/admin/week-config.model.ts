import {
  DATE_CONTROL,
  SELECT_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource } from '@model/table.model';

export enum WEEK_CONFIG_KEY {
  ID = 'id',
  SCHOOL_YEAR_ID = 'schoolYearId',
  SCHOOL_YEAR_NAME = 'schoolYearName',
  SEMESTER_ID = 'semesterId',
  SEMESTER_NAME = 'semesterName',
  WEEK_NUMBER = 'weekNumber',
  START_DATE = 'startDate',
  END_DATE = 'endDate',
}

export const WEEK_CONFIG_API_ENDPOINT = {
  BASE_PATH: 'week-configs',
  GENERATE: 'generate',
  BULK_UPDATE: 'bulk-update',
  BY_SEMESTER: 'by-semester',
  CBB: 'cbb',
};

export interface WeekConfigOptionResponse {
  id: ID_TYPE;
  name: string;
}

export interface WeekConfigResponse extends TableDataSource {
  [WEEK_CONFIG_KEY.ID]: ID_TYPE;
  [WEEK_CONFIG_KEY.SCHOOL_YEAR_ID]?: ID_TYPE;
  [WEEK_CONFIG_KEY.SCHOOL_YEAR_NAME]?: string;
  [WEEK_CONFIG_KEY.SEMESTER_ID]?: ID_TYPE;
  [WEEK_CONFIG_KEY.SEMESTER_NAME]?: string;
  [WEEK_CONFIG_KEY.WEEK_NUMBER]?: number;
  [WEEK_CONFIG_KEY.START_DATE]?: string;
  [WEEK_CONFIG_KEY.END_DATE]?: string;
}

export interface WeekConfigQueryRequest {
  [WEEK_CONFIG_KEY.SCHOOL_YEAR_ID]: ID_TYPE;
  [WEEK_CONFIG_KEY.SEMESTER_ID]?: ID_TYPE;
}

export interface WeekConfigGenerateRequest {
  [WEEK_CONFIG_KEY.SCHOOL_YEAR_ID]: ID_TYPE;
  [WEEK_CONFIG_KEY.SEMESTER_ID]: ID_TYPE;
}

export interface WeekConfigItemPayload {
  [WEEK_CONFIG_KEY.ID]?: ID_TYPE;
  [WEEK_CONFIG_KEY.WEEK_NUMBER]: number;
  [WEEK_CONFIG_KEY.START_DATE]: string;
  [WEEK_CONFIG_KEY.END_DATE]: string;
}

export interface WeekConfigBulkUpdateRequest {
  items: WeekConfigItemPayload[];
}

export type WeekConfigUpdateRequest = Omit<
  WeekConfigItemPayload,
  WEEK_CONFIG_KEY.ID
>;

export const WEEK_CONFIG_FILTER_FORM = [
  SELECT_CONTROL({
    controlName: WEEK_CONFIG_KEY.SCHOOL_YEAR_ID,
    placeholder: 'Năm học',
    required: false,
    clearable: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: WEEK_CONFIG_KEY.SEMESTER_ID,
    placeholder: 'Học kỳ',
    required: false,
    clearable: true,
    listOption: [],
  }),
];

export const WEEK_CONFIG_FORM = [
  TEXT_CONTROL({
    controlName: WEEK_CONFIG_KEY.WEEK_NUMBER,
    label: 'Tuần',
    placeholder: 'Nhập số tuần',
    required: true,
    type: 'number',
  }),
  DATE_CONTROL({
    controlName: WEEK_CONFIG_KEY.START_DATE,
    label: 'Ngày bắt đầu',
    placeholder: 'Chọn ngày bắt đầu',
    required: true,
    dateType: 'date',
  }),
  DATE_CONTROL({
    controlName: WEEK_CONFIG_KEY.END_DATE,
    label: 'Ngày kết thúc',
    placeholder: 'Chọn ngày kết thúc',
    required: true,
    dateType: 'date',
  }),
];
