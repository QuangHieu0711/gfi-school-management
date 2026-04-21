import { SELECT_CONTROL, TEXT_CONTROL } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';
import { REGEX } from '@constant/constant';

export enum DON_VI_KEY {
  STT = 'no',
  ID = 'id',
  CODE = 'code',
  NAME = 'name',
  ADDRESS = 'address',
  PHONE = 'phone',
  EMAIL = 'email',
  STATUS = 'status',
}

export const DON_VI_API_ENDPOINT = {
  BASE_PATH: 'units',
  FILTER: 'search',
  EXPORT: 'export',
  EXCEL_TEMPLATE: 'excel-template',
  IMPORT_EXCEL: 'import-excel',
  IMPORT_ERROR_FILE: 'import-error-file',
  OPTIONS: 'options',
  USER_CREATION_OPTIONS: 'user-creation-options',
};

export interface DonViOptionResponse {
  id: ID_TYPE;
  name: string;
}

export interface DonViFilter {
  unitName?: string;
  status?: number;
}

export interface DonViFilterRequest extends TableRequest {
  pageNow?: number;
  filter?: DonViFilter;
}

export interface DonViExportRequest extends DonViFilterRequest {
  exportType?: 'PDF' | 'EXCEL';
}

export interface DonViImportResponseData {
  successCount: number;
  failedCount: number;
  hasErrorFile?: boolean;
  errorFileName?: string;
  errorFileToken?: string;
}

export interface DonViFormRequest {
  [DON_VI_KEY.ID]?: ID_TYPE;
  [DON_VI_KEY.CODE]: string;
  [DON_VI_KEY.NAME]: string;
  [DON_VI_KEY.ADDRESS]?: string;
  [DON_VI_KEY.PHONE]?: string;
  [DON_VI_KEY.EMAIL]?: string;
  [DON_VI_KEY.STATUS]?: number;
}

export interface DonViResponse extends TableDataSource {
  [DON_VI_KEY.ID]: ID_TYPE;
  [DON_VI_KEY.CODE]: string;
  [DON_VI_KEY.NAME]: string;
  [DON_VI_KEY.ADDRESS]?: string;
  [DON_VI_KEY.PHONE]?: string;
  [DON_VI_KEY.EMAIL]?: string;
  [DON_VI_KEY.STATUS]?: number;
}

export const DON_VI_STATUS_OPTIONS = [
  { value: 1, label: 'Hoạt động' },
  { value: 0, label: 'Không hoạt động' },
];

export const DON_VI_FILTER_FORM = [
  TEXT_CONTROL({
    controlName: DON_VI_KEY.NAME,
    placeholder: 'Tìm kiếm theo mã hoặc tên đơn vị',
    required: false,
    maxLength: 255,
  }),
  SELECT_CONTROL({
    controlName: DON_VI_KEY.STATUS,
    placeholder: 'Trạng thái',
    required: false,
    clearable: true,
    listOption: DON_VI_STATUS_OPTIONS,
  }),
];

export const DON_VI_FORM = [
  TEXT_CONTROL({
    controlName: DON_VI_KEY.CODE,
    label: 'Mã đơn vị',
    placeholder: 'Mã đơn vị',
    required: true,
    maxLength: 50,
  }),
  TEXT_CONTROL({
    controlName: DON_VI_KEY.NAME,
    label: 'Tên đơn vị',
    placeholder: 'Tên đơn vị',
    required: true,
    maxLength: 255,
  }),
  TEXT_CONTROL({
    controlName: DON_VI_KEY.ADDRESS,
    label: 'Địa chỉ',
    placeholder: 'Địa chỉ',
    required: false,
    maxLength: 255,
  }),
  TEXT_CONTROL({
    controlName: DON_VI_KEY.PHONE,
    label: 'Số điện thoại',
    placeholder: 'Số điện thoại',
    required: false,
    maxLength: 20,
  }),
  TEXT_CONTROL({
    controlName: DON_VI_KEY.EMAIL,
    label: 'Email',
    placeholder: 'Email',
    required: false,
    regex: REGEX.EMAIL,
    maxLength: 255,
  }),
  SELECT_CONTROL({
    controlName: DON_VI_KEY.STATUS,
    label: 'Trạng thái',
    placeholder: 'Chọn trạng thái',
    required: true,
    clearable: true,
    listOption: DON_VI_STATUS_OPTIONS,
  }),
];
