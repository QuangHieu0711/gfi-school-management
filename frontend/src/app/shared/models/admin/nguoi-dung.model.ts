import { REGEX } from '@constant/constant';
import { SELECT_CONTROL, TEXT_CONTROL } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum NGUOI_DUNG_KEY {
  STT = 'no',
  ID = 'id',
  TEN_NGUOI_DUNG = 'name',
  TEN_TAI_KHOAN = 'username',
  EMAIL = 'email',
  STATUS = 'status',
  SDT = 'phone',
  NHOM_QUYEN = 'nhomQuyen',
  ROLEID = 'roleId',
  NHOM_QUYEN_NAME = 'roleName',
  HO = 'lastName',
  TEN = 'firstName',
  MAT_KHAU = 'pwd',
  AVATAR = 'avatar',
  UNITID = 'unitId',
  TEN_DON_VI = 'unitName',
  TEN_VIET_TAT = 'shortName',
  ID_VUNG_MO = 'idVungMo',
  MA_DON_VI_CHA = 'parentUnitId',
  ID_LOAI_DON_VI = 'unitTypeId',
}

export const NGUOI_DUNG_API_ENDPOINT = {
  BASE_PATH: 'account',
  FILTER: 'filter',
  FILTER_ALL: 'filter-all',
  GET_BY_ID: 'get-by-id',
  CREATE: 'create',
  UPDATE: 'update',
};

export interface NguoiDungFilterRequest extends TableRequest {
  [NGUOI_DUNG_KEY.TEN_NGUOI_DUNG]?: string;
}

export interface NguoiDungResponse extends TableDataSource {
  [NGUOI_DUNG_KEY.ID]: ID_TYPE;
  [NGUOI_DUNG_KEY.TEN]?: string;
  [NGUOI_DUNG_KEY.HO]?: string;
  [NGUOI_DUNG_KEY.TEN_TAI_KHOAN]?: string;
  [NGUOI_DUNG_KEY.SDT]?: number;
  [NGUOI_DUNG_KEY.EMAIL]?: string;
  [NGUOI_DUNG_KEY.ROLEID]?: string;
  [NGUOI_DUNG_KEY.NHOM_QUYEN_NAME]?: string;
  [NGUOI_DUNG_KEY.AVATAR]?: string;
  [NGUOI_DUNG_KEY.UNITID]?: ID_TYPE;
  [NGUOI_DUNG_KEY.TEN_DON_VI]?: string;
  [NGUOI_DUNG_KEY.TEN_VIET_TAT]?: string;
  [NGUOI_DUNG_KEY.STATUS]: number;
}

export interface NguoiDungFormRequest {
  [NGUOI_DUNG_KEY.ID]?: ID_TYPE;
  [NGUOI_DUNG_KEY.TEN]?: string;
  [NGUOI_DUNG_KEY.HO]?: string;
  [NGUOI_DUNG_KEY.TEN_TAI_KHOAN]?: string;
  [NGUOI_DUNG_KEY.EMAIL]?: string;
  [NGUOI_DUNG_KEY.SDT]?: number;
  [NGUOI_DUNG_KEY.MAT_KHAU]?: string;
  [NGUOI_DUNG_KEY.ROLEID]?: string;
  [NGUOI_DUNG_KEY.AVATAR]?: string;
  [NGUOI_DUNG_KEY.UNITID]?: ID_TYPE;
  [NGUOI_DUNG_KEY.STATUS]?: number;
}

export const NGUOI_DUNG_FORM = (requiredPassword = false) => [
  TEXT_CONTROL({
    controlName: NGUOI_DUNG_KEY.HO,
    label: 'Ho',
    placeholder: 'Ho',
    required: true,
    regex: REGEX.VIETNAMESE_NAME,
    maxLength: 255,
  }),
  TEXT_CONTROL({
    controlName: NGUOI_DUNG_KEY.TEN,
    label: 'Ten',
    placeholder: 'Ten',
    required: true,
    regex: REGEX.VIETNAMESE_NAME,
    maxLength: 255,
  }),
  TEXT_CONTROL({
    controlName: NGUOI_DUNG_KEY.TEN_TAI_KHOAN,
    label: 'Ten tai khoan',
    placeholder: 'Ten tai khoan',
    required: true,
    regex: /^[a-zA-Z0-9!@#$%^&*()_+'";:.,]+$/g,
    maxLength: 255,
  }),
  TEXT_CONTROL({
    controlName: NGUOI_DUNG_KEY.MAT_KHAU,
    label: 'Mat khau',
    placeholder: 'Mat khau',
    required: requiredPassword,
    type: 'password',
    regex: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_])[A-Za-z\d\W_]{8,}$/,
  }),
  TEXT_CONTROL({
    controlName: NGUOI_DUNG_KEY.SDT,
    label: 'So dien thoai',
    placeholder: 'So dien thoai',
    type: 'tel',
    required: true,
    regex: /^[0-9]+$/g,
    maxLength: 12,
  }),
  TEXT_CONTROL({
    controlName: NGUOI_DUNG_KEY.EMAIL,
    label: 'Email',
    placeholder: 'Email',
    required: true,
    type: 'text',
    regex: REGEX.EMAIL,
    maxLength: 255,
  }),
  SELECT_CONTROL({
    controlName: NGUOI_DUNG_KEY.UNITID,
    label: 'Don vi',
    placeholder: 'Don vi',
    required: true,
  }),
  SELECT_CONTROL({
    controlName: NGUOI_DUNG_KEY.STATUS,
    label: 'Trang thai',
    placeholder: 'Trang thai',
    required: true,
    listOption: [
      { value: 1, label: 'Hoat dong' },
      { value: 0, label: 'Khong hoat dong' },
    ],
  }),
  SELECT_CONTROL({
    controlName: NGUOI_DUNG_KEY.ROLEID,
    label: 'Nhom quyen',
    placeholder: 'Nhom quyen',
    required: true,
    listOption: [
      { value: 'admin', label: 'Quan tri vien' },
      { value: 'editor', label: 'Bien tap vien' },
      { value: 'viewer', label: 'Nguoi xem' },
    ],
  }),
];
