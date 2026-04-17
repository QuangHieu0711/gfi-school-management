import {
  DATE_CONTROL,
  IOptions,
  SELECT_CONTROL,
  TEXT_CONTROL,
  TEXTAREA_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE, ITableResponse } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export const DEPARTMENT_OPTIONS: IOptions[] = [
  { value: 'Ban giám hiệu', label: 'Ban giám hiệu' },
  { value: 'Tổ Khối 1', label: 'Tổ Khối 1' },
  { value: 'Tổ Khối 2', label: 'Tổ Khối 2' },
  { value: 'Tổ Khối 3', label: 'Tổ Khối 3' },
  { value: 'Tổ Khối 4', label: 'Tổ Khối 4' },
  { value: 'Tổ Khối 5', label: 'Tổ Khối 5' },
  { value: 'Tổ chuyên biệt', label: 'Tổ chuyên biệt' },
  { value: 'Tổ Văn phòng', label: 'Tổ Văn phòng' },
  { value: 'Thư viện - Thiết bị', label: 'Thư viện - Thiết bị' },
  { value: 'Y tế học đường', label: 'Y tế học đường' },
  { value: 'Công tác Đội', label: 'Công tác Đội' },
];

export const WORKING_POSITION_OPTIONS: IOptions[] = [
  { value: 'Giáo viên chủ nhiệm', label: 'Giáo viên chủ nhiệm' },
  { value: 'Giáo viên bộ môn', label: 'Giáo viên bộ môn' },
  { value: 'Giáo viên Tiếng Anh', label: 'Giáo viên Tiếng Anh' },
  { value: 'Giáo viên Tin học', label: 'Giáo viên Tin học' },
  { value: 'Giáo viên Âm nhạc', label: 'Giáo viên Âm nhạc' },
  { value: 'Giáo viên Mỹ thuật', label: 'Giáo viên Mỹ thuật' },
  { value: 'Giáo viên Thể dục', label: 'Giáo viên Thể dục' },
  { value: 'Tổng phụ trách Đội', label: 'Tổng phụ trách Đội' },
  { value: 'Nhân viên thư viện', label: 'Nhân viên thư viện' },
  { value: 'Nhân viên thiết bị', label: 'Nhân viên thiết bị' },
  { value: 'Nhân viên kế toán', label: 'Nhân viên kế toán' },
  { value: 'Nhân viên văn thư', label: 'Nhân viên văn thư' },
  { value: 'Nhân viên y tế học đường', label: 'Nhân viên y tế học đường' },
  { value: 'Cán bộ quản lý', label: 'Cán bộ quản lý' },
];

export const TITLE_OPTIONS: IOptions[] = [
  { value: 'Hiệu trưởng', label: 'Hiệu trưởng' },
  { value: 'Phó Hiệu trưởng', label: 'Phó Hiệu trưởng' },
  { value: 'Tổ trưởng chuyên môn', label: 'Tổ trưởng chuyên môn' },
  { value: 'Tổ phó chuyên môn', label: 'Tổ phó chuyên môn' },
  { value: 'Giáo viên', label: 'Giáo viên' },
  { value: 'Tổng phụ trách Đội', label: 'Tổng phụ trách Đội' },
  { value: 'Nhân viên', label: 'Nhân viên' },
  { value: 'Kế toán', label: 'Kế toán' },
  { value: 'Văn thư', label: 'Văn thư' },
  { value: 'Thư viện', label: 'Thư viện' },
  { value: 'Y tế học đường', label: 'Y tế học đường' },
];

export const EMPLOYMENT_TYPE_OPTIONS: IOptions[] = [
  { value: 'Biên chế', label: 'Biên chế' },
  { value: 'Hợp đồng xác định thời hạn', label: 'Hợp đồng xác định thời hạn' },
  { value: 'Hợp đồng không xác định thời hạn', label: 'Hợp đồng không xác định thời hạn' },
  { value: 'Thử việc', label: 'Thử việc' },
  { value: 'Thỉnh giảng', label: 'Thỉnh giảng' },
  { value: 'Kiêm nhiệm', label: 'Kiêm nhiệm' },
];

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
  SELECT_CONTROL({
    controlName: STAFF_JOB_HISTORY_KEY.DEPARTMENT_ID,
    label: 'Phòng ban / Tổ / Bộ phận',
    placeholder: 'Chọn phòng ban / tổ / bộ phận',
    required: false,
    clearable: true,
    listOption: DEPARTMENT_OPTIONS,
  }),
  SELECT_CONTROL({
    controlName: STAFF_JOB_HISTORY_KEY.WORKING_POSITION_ID,
    label: 'Chức danh',
    placeholder: 'Chọn chức danh',
    required: false,
    clearable: true,
    listOption: WORKING_POSITION_OPTIONS,
  }),
  SELECT_CONTROL({
    controlName: STAFF_JOB_HISTORY_KEY.TITLE_ID,
    label: 'Chức vụ',
    placeholder: 'Chọn chức vụ',
    required: false,
    clearable: true,
    listOption: TITLE_OPTIONS,
  }),
  SELECT_CONTROL({
    controlName: STAFF_JOB_HISTORY_KEY.EMPLOYMENT_TYPE_ID,
    label: 'Loại tuyển dụng',
    placeholder: 'Chọn loại tuyển dụng',
    required: false,
    clearable: true,
    listOption: EMPLOYMENT_TYPE_OPTIONS,
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
