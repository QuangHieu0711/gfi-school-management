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

/**
 * Interface cho Token Response từ BE login
 */
export interface ITokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresAt: number;
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

/**
 * Interface cho Menu Actions từ response login
 */
export interface IMenuActions {
  isView?: number;
  isAdd?: number;
  isEdit?: number;
  isDelete?: number;
  isDownload?: number;
  isConfig?: number;
}

/**
 * Interface cho Data Scope từ response login
 */
export interface IDataScope {
  scopeType: string;
  scopeValues?: (ID_TYPE | number)[];
}

/**
 * Interface cho Menu/Permission từ response login
 */
export interface IMenuPermission {
  menuCode: string;
  menuName?: string;
  path: string | null;
  icon?: string | null;
  level?: number | null;
  parentMenuId?: ID_TYPE | null;
  actions: IMenuActions;
  dataScopes?: IDataScope[];
  children?: IMenuPermission[];
}

/**
 * Interface cho Permissions response từ login
 */
export interface IPermissionsResponse {
  menus: IMenuPermission[];
}

/**
 * Interface cho Login response data từ BE
 */
export interface ILoginDataResponse {
  token: ITokenResponse;
  user: ICurrentUser;
  permissions: IPermissionsResponse;
}

/**
 * Interface cho Full Login response từ BE (wrapper)
 */
export interface ILoginApiResponse {
  code: number;
  data: ILoginDataResponse;
  status: boolean;
  traceID: string;
  userMessage: string;
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
  menuCode?: string;
  isView: number;
  isAdd: number;
  isEdit: number;
  isDelete: number;
  isDownload: number;
  isConfig: number;
  isApprove: number;
  name: string;
  url: string;
  pid?: number; // ID cha
  pathId: string;
  ordinal: number;
  icon: string;

  // Dữ liệu phân quyền dữ liệu
  dataScopes?: {
    scopeType?: string;
    scopeValues?: number[];
  }[];
}

export interface IRole {
  id: ID_TYPE;
  code?: string; // VD: "SCHOOL_ADMIN"
  name: string;
  rules?: IRule[];
}

/**
 * Interface cho Unit (đơn vị/trường) từ response login
 */
export interface IUnit {
  id: ID_TYPE;
  code: string;
  name: string;
}

/**
 * Interface User đầy đủ map với API mới
 */
export interface ICurrentUser {
  id: ID_TYPE;
  username: string;
  fullName: string;
  email: string;
  phone?: string;
  status: number;
  role: IRole;
  unit: IUnit;
  permissions?: IPermissionsResponse;
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
