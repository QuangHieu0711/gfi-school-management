import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from '@constant/constant';
import { ID_TYPE } from '@model/response.model';

export const USER_INFO_KEY = 'userInfo';

export enum AUTH_KEY {
  USERNAME = 'username',
  PASSWORD = 'password',
  EMAIL = 'email',
  REMEMBER_ME = 'rememberMe',
}

export enum AUTH_API_ENDPOINT {
  AUTH_TOKEN = 'auth/login',
  REFRESH_TOKEN = 'auth/refresh',
  AUTH_USER = 'auth/user',
  CHANGE_PASSWORD = 'change-password',
  CAPTCHA = 'public/auth/captcha',
}

export interface ILoginResponse {
  [ACCESS_TOKEN_KEY]: string;
  [REFRESH_TOKEN_KEY]: string;
  isFirstLogin?: number | string | boolean;
}

/**
 * Interface cho Captcha response từ API
 */
export interface ICaptchaResponse {
  imageBase64: string;
  key: string;
}

/**
 * Interface cho Login request khi có captcha
 */
export interface ILoginWithCaptchaPayload {
  [AUTH_KEY.USERNAME]: string;
  [AUTH_KEY.PASSWORD]: string;
  captchaCode: string;
  captchaKey: string;
}

/**
 * Interface cho Login request không có captcha
 */
export interface ILoginPayload {
  [AUTH_KEY.USERNAME]: string;
  [AUTH_KEY.PASSWORD]: string;
}

/**
 * Interface cho Login error response có yêu cầu captcha
 */
export interface ILoginErrorWithCaptcha {
  requireCaptcha: boolean;
  loginFailCount: number;
}

export interface IRefreshTokenResponse extends ILoginResponse {
  [ACCESS_TOKEN_KEY]: string;
  [REFRESH_TOKEN_KEY]: string;
}

/**
 * Interface cho đơn vị khu mỏ (lấy từ mảng donVi trong API)
 */
export interface IDonViKhuMo {
  id?: ID_TYPE;
  maDonViId?: string;
  idVungMo?: string;
  tenVungMo?: string;
  idKhuMo?: string;
  maKhuMo?: string;
  tenKhuMo?: string;
  allow?: number;
  makhoangsan?: string;
}

/**
 * Interface cho Rule phân quyền (lấy từ role.rules trong API)
 */
export interface IRule {
  ruleId: number; // ID gốc từ API
  roleId: number;
  moduleId: ID_TYPE;
  isView: number;
  isAdd: number;
  isEdit: number;
  isDelete: number;
  isDownload: number;
  isApprove: number;
  name: string;
  url: string;
  pid?: number; // ID cha
  pathId: string;
  ordinal: number;
  icon: string;

  // Các trường quan trọng để check quyền màn hình
  module?: string; // VD: "TLNT"
  categoryId?: string; // VD: "LK"
  idKhumo?: string; // VD: "KM0025" (Lưu ý: API viết chữ m thường)

  isViewMap?: number;
  isEditMap?: number;
}

export interface IRole {
  id: ID_TYPE;
  name: string;
  rules?: IRule[];
}

/**
 * Interface User đầy đủ map với API mới
 */
export interface ICurrentUser {
  id: ID_TYPE;
  username: string;
  name: string;
  email: string;
  role: IRole;
  donVi: {
    maDonVi: string;
    tenDonVi: string;
    tenVietTat: string;
    role: IDonViKhuMo[];
  };
  rememberMe?: boolean;
}

export enum UserRole {
  ADMIN = 'admin',
}

export type AuthMessage =
  | {
      type: 'LOGIN';
      accessToken: string;
      refreshToken: string;
      uuid: string;
      rememberMe: boolean;
    }
  | { type: 'LOGOUT'; uuid: string }
  | { type: 'REQUEST_SESSION'; uuid: string }
  | {
      type: 'SYNC_SESSION';
      accessToken: string;
      refreshToken: string;
      uuid: string;
    };

// --- Helper Functions giữ nguyên logic cũ nhưng update Type ---

export const saveUserInfoToStorage = (
  storageService: {
    set: (key: string, value: ICurrentUser, type: string) => void;
  },
  userInfo: ICurrentUser,
  rememberMe = false
): void => {
  storageService.set(USER_INFO_KEY, userInfo, rememberMe ? 'local' : 'session');
};

export const getUserInfoFromStorage = (storageService: {
  get: (key: string, type: string) => ICurrentUser | null;
}): ICurrentUser | null => {
  return storageService.get(USER_INFO_KEY, 'all');
};

export const removeUserInfoFromStorage = (storageService: {
  remove: (key: string, type: string) => void;
}): void => {
  storageService.remove(USER_INFO_KEY, 'all');
};

export interface IChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
  confirmPassword?: string;
}

export interface IChangePasswordResponse {
  success?: boolean;
  message?: string;
}
