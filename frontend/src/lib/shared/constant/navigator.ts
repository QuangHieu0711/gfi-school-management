export const PATH = {
  TAO_MOI: 'TaoMoi',
  CAP_NHAT: 'CapNhat',
  CHI_TIET: 'ChiTiet',
};
export const NAVIGATOR_ENDPOINT = {
  BASE_PATH: '',
  LOGIN: 'login',
  CHANGE_PASSWORD: 'change-password',
  FORGOT_PASSWORD: 'forgot-password',
  ACCESS_DENIED: 'access-denied',
  SERVER_ERROR: 'server-error',

  ADMIN: {
    BASE_PATH: 'Admin',
    NGUOI_DUNG: {
      BASE_PATH: 'NguoiDung',
    },
    DON_VI: {
      BASE_PATH: 'DonVi',
    },
    VAI_TRO: {
      BASE_PATH: 'VaiTro',
    },
    NAM_HOC: {
      BASE_PATH: 'NamHoc',
    },
    KHOI: {
      BASE_PATH: 'Khoi',
    },
    LOP: {
      BASE_PATH: 'Lop',
    },
    MON_HOC: {
      BASE_PATH: 'MonHoc',
    },
    HOC_SINH: {
      BASE_PATH: 'HocSinh',
    },
  },
} as const;

export type NavigatorEndpoint = typeof NAVIGATOR_ENDPOINT;
