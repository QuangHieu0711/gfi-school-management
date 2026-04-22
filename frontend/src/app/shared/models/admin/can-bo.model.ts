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
  BY_GRADE: 'grade',
  EXPORT: 'export',
  IMPORT_EXCEL: 'import-excel',
  IMPORT_ERROR_FILE: 'import-error-file',
  EXCEL_TEMPLATE: 'excel-template',
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

export interface CanBoExportRequest extends CanBoFilterRequest {
  exportType?: 'EXCEL' | 'PDF';
}

export interface CanBoImportResponseData {
  successCount?: number;
  failedCount?: number;
  hasErrorFile?: boolean;
  errorFileToken?: string;
  errorFileName?: string;
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

export interface CanBoAddressResponse {
  provinceId?: number | string;
  provinceName?: string;
  districtId?: number | string;
  districtName?: string;
  wardId?: number | string;
  wardName?: string;
  hamletName?: string;
  detailAddress?: string;
  fullAddress?: string;
}

export interface CanBoFamilyInfoResponse {
  fullName?: string;
  birthYear?: number | string;
  placeOfBirth?: string;
  hometown?: string;
  occupation?: string;
  phone?: string;
  workplace?: string;
  address?: string;
  note?: string;
}

export interface CanBoDetailResponse extends CanBoResponse {
  aliasName?: string;
  avatarFileId?: ID_TYPE | null;
  avatarUrl?: string;
  identityCode?: string;
  hometown?: string;
  ethnicityId?: ID_TYPE | null;
  ethnicityName?: string;
  nationalityId?: ID_TYPE | null;
  nationalityName?: string;
  religionId?: ID_TYPE | null;
  religionName?: string;
  cccdNo?: string;
  cccdIssueDate?: string;
  cccdIssuePlace?: string;
  socialInsuranceNo?: string;
  healthStatus?: string;
  gradeId?: ID_TYPE | null;
  gradeName?: string;
  note?: string;
  status?: string;
  signatureFileId?: ID_TYPE | null;
  signatureUrl?: string;
  userId?: ID_TYPE | null;
  permanentAddress?: CanBoAddressResponse;
  temporaryAddress?: CanBoAddressResponse;
  birthPlaceAddress?: CanBoAddressResponse;
  fatherInfo?: CanBoFamilyInfoResponse;
  motherInfo?: CanBoFamilyInfoResponse;
  spouseInfo?: CanBoFamilyInfoResponse;
  spouseFatherInfo?: CanBoFamilyInfoResponse;
  spouseMotherInfo?: CanBoFamilyInfoResponse;
  childrenDetail?: string;
}

export interface CanBoAddressRequest {
  provinceId?: number | string;
  districtId?: number | string;
  wardId?: number | string;
  hamletName?: string;
  detailAddress?: string;
  fullAddress?: string;
}

export interface CanBoFamilyInfoRequest {
  fullName?: string;
  birthYear?: number | string;
  placeOfBirth?: string;
  hometown?: string;
  occupation?: string;
  phone?: string;
  workplace?: string;
  address?: string;
  note?: string;
}

export interface CanBoFormRequest {
  staffCode?: string;
  fullName?: string;
  unitId?: number | string;
  aliasName?: string;
  identityCode?: string;
  gender?: string;
  dateOfBirth?: string;
  ethnicityId?: string | number;
  religionId?: string | number;
  nationalityId?: string | number;
  cccdNo?: string;
  cccdIssueDate?: string;
  cccdIssuePlace?: string;
  phone?: string;
  email?: string;
  healthStatus?: string;
  socialInsuranceNo?: string;
  gradeId?: string | number;
  avatarFileId?: number | string;
  avatarUrl?: string;
  signatureFileId?: number | string;
  signatureUrl?: string;
  status?: string;
  note?: string;
  permanentAddress?: CanBoAddressRequest;
  temporaryAddress?: CanBoAddressRequest;
  birthPlaceAddress?: CanBoAddressRequest;
  fatherInfo?: CanBoFamilyInfoRequest;
  motherInfo?: CanBoFamilyInfoRequest;
  spouseInfo?: CanBoFamilyInfoRequest;
  spouseFatherInfo?: CanBoFamilyInfoRequest;
  spouseMotherInfo?: CanBoFamilyInfoRequest;
  childrenDetail?: string;
}

export const CAN_BO_STATUS_OPTIONS = [
  { value: 'ACTIVE', label: '\u0110ang l\u00e0m vi\u1ec7c' },
  { value: 'INACTIVE', label: 'Ng\u1eebng ho\u1ea1t \u0111\u1ed9ng' },
];

export const CAN_BO_GENDER_OPTIONS = [
  { value: 'MALE', label: 'Nam' },
  { value: 'FEMALE', label: 'Nữ' },
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

export const CAN_BO_PROFILE_FALLBACK: CanBoDetailResponse = {
  ...CAN_BO_DETAIL_FALLBACK,
  avatarFileId: null,
  avatarUrl: '',
  identityCode: '',
  ethnicityId: null,
  ethnicityName: '',
  nationalityId: null,
  nationalityName: '',
  religionId: null,
  religionName: '',
  cccdIssueDate: '',
  cccdIssuePlace: '',
  socialInsuranceNo: '',
  healthStatus: '',
  note: '',
  status: 'ACTIVE',
  signatureFileId: null,
  signatureUrl: '',
  userId: null,
  permanentAddress: {},
  temporaryAddress: {},
  birthPlaceAddress: {},
  fatherInfo: {},
  motherInfo: {},
  spouseInfo: {},
  spouseFatherInfo: {},
  spouseMotherInfo: {},
  childrenDetail: '',
};

