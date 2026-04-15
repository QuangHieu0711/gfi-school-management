import {
  DATE_CONTROL,
  SELECT_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum CAN_BO_KEY {
  ID = 'id',
  STAFF_CODE = 'staffCode',
  FULL_NAME = 'fullName',
  ALIAS_NAME = 'aliasName',
  UNIT_ID = 'unitId',
  UNIT_NAME = 'unitName',
  GENDER = 'gender',
  DATE_OF_BIRTH = 'dateOfBirth',
  PHONE = 'phone',
  EMAIL = 'email',
  STATUS = 'status',
  CCCD_NO = 'cccdNo',
}

export const CAN_BO_API_ENDPOINT = {
  BASE_PATH: 'staffs',
  FILTER: 'search',
};

export interface CanBoFilter {
  staffCode?: string;
  fullName?: string;
  unitId?: ID_TYPE;
  status?: string;
  gender?: string;
  phone?: string;
  email?: string;
  dateOfBirth?: string;
}

export interface CanBoFilterRequest extends TableRequest {
  pageNow?: number;
  filter?: CanBoFilter;
}

export interface CanBoResponse extends TableDataSource {
  [CAN_BO_KEY.ID]: ID_TYPE;
  [CAN_BO_KEY.STAFF_CODE]?: string;
  [CAN_BO_KEY.FULL_NAME]?: string;
  [CAN_BO_KEY.ALIAS_NAME]?: string;
  [CAN_BO_KEY.UNIT_ID]?: ID_TYPE;
  [CAN_BO_KEY.UNIT_NAME]?: string;
  [CAN_BO_KEY.GENDER]?: string;
  [CAN_BO_KEY.DATE_OF_BIRTH]?: string;
  [CAN_BO_KEY.PHONE]?: string;
  [CAN_BO_KEY.EMAIL]?: string;
  [CAN_BO_KEY.STATUS]?: string;
  [CAN_BO_KEY.CCCD_NO]?: string;
}

export const CAN_BO_STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Đang làm việc' },
  { value: 'INACTIVE', label: 'Ngừng hoạt động' },
];

export const CAN_BO_GENDER_OPTIONS = [
  { value: 'Nam', label: 'Nam' },
  { value: 'Nu', label: 'Nữ' },
];

export const CAN_BO_FILTER_FORM = [
  TEXT_CONTROL({
    controlName: CAN_BO_KEY.STAFF_CODE,
    placeholder: 'Mã cán bộ',
    required: false,
    maxLength: 100,
  }),
  TEXT_CONTROL({
    controlName: CAN_BO_KEY.FULL_NAME,
    placeholder: 'Họ và tên',
    required: false,
    maxLength: 255,
  }),
  SELECT_CONTROL({
    controlName: CAN_BO_KEY.UNIT_ID,
    placeholder: 'Đơn vị',
    required: false,
    clearable: true,
    listOption: [],
  }),
  SELECT_CONTROL({
    controlName: CAN_BO_KEY.STATUS,
    placeholder: 'Trạng thái',
    required: false,
    clearable: true,
    listOption: CAN_BO_STATUS_OPTIONS,
  }),
  SELECT_CONTROL({
    controlName: CAN_BO_KEY.GENDER,
    placeholder: 'Giới tính',
    required: false,
    clearable: true,
    listOption: CAN_BO_GENDER_OPTIONS,
  }),
  TEXT_CONTROL({
    controlName: CAN_BO_KEY.PHONE,
    placeholder: 'Số điện thoại',
    required: false,
    maxLength: 50,
  }),
  TEXT_CONTROL({
    controlName: CAN_BO_KEY.EMAIL,
    placeholder: 'Email',
    required: false,
    maxLength: 255,
  }),
  DATE_CONTROL({
    controlName: CAN_BO_KEY.DATE_OF_BIRTH,
    placeholder: 'Ngày sinh',
    required: false,
  }),
];

export const CAN_BO_DETAIL_FALLBACK: CanBoResponse = {
  id: '',
  staffCode: '',
  fullName: 'Chưa có dữ liệu',
  aliasName: '',
  unitId: undefined,
  unitName: '',
  gender: '',
  dateOfBirth: '',
  phone: '',
  email: '',
  status: '',
  cccdNo: '',
};
