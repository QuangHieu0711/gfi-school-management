export const PATH = {
  TAO_MOI: 'tao-moi',
  CAP_NHAT: 'cap-nhat',
  CHI_TIET: 'chi-tiet',
};
export const NAVIGATOR_ENDPOINT = {
  BASE_PATH: '',
  LOGIN: 'login',
  CHANGE_PASSWORD: 'change-password',
  FORGOT_PASSWORD: 'forgot-password',
  ACCESS_DENIED: 'access-denied',
  SERVER_ERROR: 'server-error',

  ADMIN: {
    BASE_PATH: 'admin',
    NGUOI_DUNG: {
      BASE_PATH: 'nguoi-dung',
    },
    DON_VI: {
      BASE_PATH: 'don-vi',
    },
    VAI_TRO: {
      BASE_PATH: 'vai-tro',
    },
    MENU: {
      BASE_PATH: 'menu',
    },
    NAM_HOC: {
      BASE_PATH: 'nam-hoc',
    },
    KHOI: {
      BASE_PATH: 'khoi',
    },
    LOP: {
      BASE_PATH: 'lop',
    },
    MON_HOC: {
      BASE_PATH: 'mon-hoc',
    },
    HOC_SINH: {
      BASE_PATH: 'hoc-sinh',
    },
  },
} as const;

export type NavigatorEndpoint = typeof NAVIGATOR_ENDPOINT;
