import { SELECT_CONTROL, TEXT_CONTROL } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum NGUOI_DUNG_KEY {
  NO = 'no',
  ID = 'id',
  FULL_NAME = 'fullName',
  USERNAME = 'username',
  EMAIL = 'email',
  STATUS = 'status',
  PHONE = 'phone',
  ROLE_ID = 'roleId',
  ROLE_NAME = 'roleName',
  PASSWORD = 'password',
  AVATAR = 'avatar',
  UNIT_ID = 'unitId',
  UNIT_NAME = 'unitName',
}

export const NGUOI_DUNG_API_ENDPOINT = {
  BASE_PATH: 'users',
  FILTER: 'search',
  OPTIONS: 'unit-options',
  ROLE: 'role-options',
};

export interface NguoiDungFilterRequest extends TableRequest {
  pageNow?: number;
  filter?: {
    [NGUOI_DUNG_KEY.FULL_NAME]?: string;
    [NGUOI_DUNG_KEY.ROLE_ID]?: ID_TYPE;
    [NGUOI_DUNG_KEY.UNIT_ID]?: ID_TYPE | ID_TYPE[];
    [NGUOI_DUNG_KEY.STATUS]?: number | number[];
  };
}

export interface NguoiDungResponse extends TableDataSource {
  [NGUOI_DUNG_KEY.ID]: ID_TYPE;
  [NGUOI_DUNG_KEY.FULL_NAME]?: string;
  [NGUOI_DUNG_KEY.USERNAME]?: string;
  [NGUOI_DUNG_KEY.PHONE]?: string;
  [NGUOI_DUNG_KEY.EMAIL]?: string | null;
  [NGUOI_DUNG_KEY.ROLE_ID]?: string;
  [NGUOI_DUNG_KEY.ROLE_NAME]?: string;
  [NGUOI_DUNG_KEY.AVATAR]?: string;
  [NGUOI_DUNG_KEY.UNIT_ID]?: ID_TYPE;
  [NGUOI_DUNG_KEY.UNIT_NAME]?: string;
  [NGUOI_DUNG_KEY.STATUS]: number;
}

export interface NguoiDungFormRequest {
  [NGUOI_DUNG_KEY.ID]?: ID_TYPE;
  [NGUOI_DUNG_KEY.USERNAME]?: string;
  [NGUOI_DUNG_KEY.PASSWORD]?: string;
  [NGUOI_DUNG_KEY.ROLE_ID]?: string;
  [NGUOI_DUNG_KEY.AVATAR]?: string;
  [NGUOI_DUNG_KEY.STATUS]?: number;
}

export const NGUOI_DUNG_FORM = (requiredPassword = false) => [
  TEXT_CONTROL({
    controlName: NGUOI_DUNG_KEY.USERNAME,
    label: 'Tên tài khoản',
    placeholder: 'Tên tài khoản',
    required: true,
    regex: /^[a-zA-Z0-9!@#$%^&*()_+'";:.,]+$/g,
    maxLength: 255,
  }),
  TEXT_CONTROL({
    controlName: NGUOI_DUNG_KEY.PASSWORD,
    label: 'Mật khẩu',
    placeholder: 'Mật khẩu',
    required: requiredPassword,
    type: 'password',
    regex: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_])[A-Za-z\d\W_]{8,}$/,
  }),
  SELECT_CONTROL({
    controlName: NGUOI_DUNG_KEY.STATUS,
    label: 'Trạng thái',
    placeholder: 'Trạng thái',
    required: true,
    clearable: true,
    listOption: [
      { value: 1, label: 'Hoạt động' },
      { value: 0, label: 'Không hoạt động' },
    ],
  }),
  SELECT_CONTROL({
    controlName: NGUOI_DUNG_KEY.ROLE_ID,
    label: 'Vai trò',
    placeholder: 'Vai trò',
    required: true,
    clearable: true,
    listOption: [],
  }),
];
