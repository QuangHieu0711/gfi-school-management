import {
  DATE_CONTROL,
  SELECT_CONTROL,
  TEXT_CONTROL,
  TEXTAREA_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE, ITableResponse } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum STAFF_JOB_HISTORY_KEY {
  ID = 'id',
  STAFF_ID = 'staffId',
  FROM_DATE = 'fromDate',
  TO_DATE = 'toDate',
  UNIT_ID = 'unitId',
  DEPARTMENT_ID = 'departmentId',
  WORKING_POSITION_ID = 'workingPositionId',
  TITLE_ID = 'titleId',
  EMPLOYMENT_TYPE_ID = 'employmentTypeId',
  DECISION_NO = 'decisionNo',
  NOTE = 'note',
}

export const STAFF_JOB_HISTORY_API_ENDPOINT = {
  BASE_PATH: 'staff-job-histories',
  FILTER: 'search',
};

export interface StaffJobHistoryResponse extends TableDataSource {
  id: ID_TYPE;
  staffId?: ID_TYPE;
  fromDate?: string;
  toDate?: string;
  unitId?: ID_TYPE;
  departmentId?: ID_TYPE | string;
  workingPositionId?: ID_TYPE | string;
  titleId?: ID_TYPE | string;
  employmentTypeId?: ID_TYPE | string;
  decisionNo?: string;
  note?: string;
  unitName?: string;
}

export interface StaffJobHistoryFormRequest {
  staffId?: ID_TYPE;
  fromDate?: string;
  toDate?: string;
  unitId?: ID_TYPE | string;
  departmentId?: ID_TYPE | string;
  workingPositionId?: ID_TYPE | string;
  titleId?: ID_TYPE | string;
  employmentTypeId?: ID_TYPE | string;
  decisionNo?: string;
  note?: string;
}

export interface StaffJobHistoryFilter {
  staffId?: ID_TYPE;
}

export interface StaffJobHistoryFilterRequest extends TableRequest {
  pageNow?: number;
  filter?: StaffJobHistoryFilter;
}

export type StaffJobHistoryListResponse =
  | StaffJobHistoryResponse[]
  | ITableResponse<StaffJobHistoryResponse>;

export const STAFF_JOB_HISTORY_FORM = [
  DATE_CONTROL({
    controlName: STAFF_JOB_HISTORY_KEY.FROM_DATE,
    label: 'Từ ngày',
    placeholder: 'Từ ngày',
    required: true,
  }),
  DATE_CONTROL({
    controlName: STAFF_JOB_HISTORY_KEY.TO_DATE,
    label: 'Đến ngày',
    placeholder: 'Đến ngày',
    required: false,
  }),
  SELECT_CONTROL({
    controlName: STAFF_JOB_HISTORY_KEY.UNIT_ID,
    label: 'Đơn vị',
    placeholder: 'Đơn vị',
    required: false,
    clearable: true,
    listOption: [],
  }),
  TEXT_CONTROL({
    controlName: STAFF_JOB_HISTORY_KEY.DEPARTMENT_ID,
    label: 'Phòng ban',
    placeholder: 'Phòng ban',
    required: false,
  }),
  TEXT_CONTROL({
    controlName: STAFF_JOB_HISTORY_KEY.WORKING_POSITION_ID,
    label: 'Vị trí việc làm',
    placeholder: 'Vị trí việc làm',
    required: false,
  }),
  TEXT_CONTROL({
    controlName: STAFF_JOB_HISTORY_KEY.TITLE_ID,
    label: 'Chức danh',
    placeholder: 'Chức danh',
    required: false,
  }),
  TEXT_CONTROL({
    controlName: STAFF_JOB_HISTORY_KEY.EMPLOYMENT_TYPE_ID,
    label: 'Loại tuyển dụng',
    placeholder: 'Loại tuyển dụng',
    required: false,
  }),
  TEXT_CONTROL({
    controlName: STAFF_JOB_HISTORY_KEY.DECISION_NO,
    label: 'Số quyết định',
    placeholder: 'Số quyết định',
    required: false,
  }),
  TEXTAREA_CONTROL({
    controlName: STAFF_JOB_HISTORY_KEY.NOTE,
    label: 'Ghi chú',
    placeholder: 'Ghi chú',
    required: false,
    rows: 4,
  }),
];
