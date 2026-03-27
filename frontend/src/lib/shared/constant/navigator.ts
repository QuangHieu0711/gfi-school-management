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

  // Modules
  TAI_LIEU_NGUYEN_THUY: {
    TAI_LIEU_NGUYEN_THUY: 'tai-lieu-nguyen-thuy',
    CHI_TIET_KHU_MO: 'khu-mo',
    LO_KHOAN: 'lo-khoan',
    CONG_TRINH_KHAI_DAO: 'cong-trinh-khai-dao',
    CONG_TAC_DO_VE: 'cong-tac-do-ve',
    HO_SO_LO_KHOAN: 'ho-so-lo-khoan',
    VUNG_MO: 'vung-mo',
    KHOANG_SAN: 'khoang-san',
    HO_SO_CONG_TRINH_KHAI_DAO: 'ho-so-cong-trinh-khai-dao',
    HO_SO_CONG_TAC_DO_VE: 'ho-so-cong-tac-do-ve',
  },

  BAO_CAO_DIA_CHAT: {
    BAO_CAO_DIA_CHAT: 'bao-cao-dia-chat',
    KHOANG_SAN: 'khoang-san',
    BAO_CAO_VUNG_MO: 'vung-mo',
    BAO_CAO: 'bao-cao',
    BAO_CAO_TONG_HOP_THEO_KHU_MO: 'khu-mo',
    BAO_CAO_DU_LIEU: 'bao-cao-du-lieu',
    BAO_CAO_DANH_MUC_DU_LIEU: 'danh-muc',
    BAO_CAO_DANH_MUC_CHI_TIET_DU_LIEU: 'danh-muc-chi-tiet',
    DANH_SACH_TUYEN: 'danh-sach-tuyen',
    PHU_LUC: 'phu-luc',
    PHU_LUC_CHI_TIET: 'phu-luc-chi-tiet',
    HO_SO_PHU_LUC: 'ho-so-phu-luc',
  },

  ADMIN: {
    BASE_PATH: 'Admin',
    NGUOI_DUNG: {
      BASE_PATH: 'NguoiDung',
    },
    DON_VI: {
      BASE_PATH: 'DonVi',
    },
    CHUC_NANG: {
      BASE_PATH: 'ChucNang',
    },
    NHOM_QUYEN: {
      BASE_PATH: 'NhomQuyen',
    },
    DANH_MUC_VUNG_MO: {
      BASE_PATH: 'DanhMucVungMo',
    },
    DANH_MUC_KHU_MO: {
      BASE_PATH: 'DanhMucKhuMo',
    },
    DANH_MUC_BAO_CAO_TAI_NGUYEN: {
      BASE_PATH: 'DanhMucBaoCaoTaiNguyen',
    },
    DANH_MUC_BAO_CAO_DIA_CHAT: {
      BASE_PATH: 'DanhMucBaoCaoDiaChat',
    },
    DANH_MUC_KHOANG_SAN: {
      BASE_PATH: 'DanhMucKhoangSan',
    },
    DANH_MUC_DE_AN_DONG_CUA_MO: {
      BASE_PATH: 'DanhMucDeAnDongCuaMo',
    },
    DU_AN_KHAI_THAC: {
      BASE_PATH: 'DuAnKhaiThac',
    },
    DANH_MUC_TAI_LIEU_NGUYEN_THUY: {
      BASE_PATH: 'DanhMucTaiLieuNguyenThuy',
    },
    DANH_MUC_DE_AN_THAM_DO: {
      BASE_PATH: 'DanhMucDeAnThamDo',
    },
    DANH_MUC_HANG_MUC: {
      BASE_PATH: 'HangMucThanhToan',
    },
    TIEN_DO_THANH_TOAN: {
      BASE_PATH: 'TienDoThanhToan',
    },
    TIEN_DO_THI_CONG: {
      BASE_PATH: 'TienDoThiCong',
    },
    DANH_SACH_BAN_DO: {
      BASE_PATH: 'DanhSachBanDo',
    },
    DANH_SACH_UNG_DUNG: {
      BASE_PATH: 'DanhSachUngDung',
      EDIT: 'ChinhSuDanhSachUngDung',
    },
    SAO_LUU_DU_LIEU: {
      BASE_PATH: 'SaoLuuDuLieu',
    },
    LICH_SU_TRUY_CAP_NGUOI_DUNG: {
      BASE_PATH: 'LichSuTruyCapNguoiDung',
    },
    CANH_BAO_DAPA: {
      BASE_PATH: 'CanhBaoDeAn',
    },
    CANH_BAO_QTTN: {
      BASE_PATH: 'CanhBaoQTTN',
    },
  },
  DE_AN_PHUONG_AN: {
    BASE_PATH: 'de-an-phuong-an',
    VUNG_MO: 'vung-mo',
    THONG_TIN_DE_AN: 'thong-tin',
    DE_AN: 'de-an',
    KHU_MO: 'khu-mo',
    KHOANG_SAN: 'khoang-san',
    HO_SO: 'ho-so',
    LOAI_TAI_LIEU: 'loai-tai-lieu',
    QUAN_LY_TIEN_DO_THI_CONG: 'quan-ly-tien-do-thi-cong',
    QUAN_LY_TIEN_DO_THANH_TOAN: 'quan-ly-tien-do-thanh-toan',
    HO_SO_TIEN_DO_THI_CONG: 'dapa-ho-so-tien-do-thi-cong',
    HO_SO_TIEN_DO_THANH_TOAN: 'dapa-ho-so-tien-do-thanh-toan',
  },
  QUAN_TRI_TAI_NGUYEN: {
    BASE_PATH: 'quan-tri-tai-nguyen',
    BAO_CAO: 'bao-cao',
    TRU_LUONG_GOI_THAU: 'tru-luong-goi-thau',
    TRU_LUONG_KHAI_THAC: 'tru-luong-khai-thac',
    DE_AN: 'de-an',
    DON_VI: 'don-vi',
    KHOANG_SAN: 'khoang-san',
    HO_SO: 'ho-so',
  },

  DU_AN_KHAI_THAC: {
    BASE_PATH: 'du-an-khai-thac',
    VUNG_MO: 'vung-mo',
    KHU_MO: 'khu-mo',
    THONG_TIN: 'thong-tin',
    LOAI_TAI_LIEU: 'loai-tai-lieu',
    BAO_CAO_DINH_KY: 'bao-cao-dinh-ky',
    BAO_CAO: 'bao-cao',
    TRU_LUONG_GOI_THAU: 'tru-luong-goi-thau',
    TRU_LUONG_KHAI_THAC: 'tru-luong-khai-thac',
    DE_AN: 'de-an',
    HO_SO: 'ho-so',
  },
  DONG_CUA_MO: {
    BASE_PATH: 'dong-cua-mo',
    VUNG_MO: 'vung-mo',
    THONG_TIN_DE_AN: 'thong-tin',
    DE_AN: 'de-an',
    KHU_MO: 'khu-mo',
    HO_SO: 'ho-so',
    LOAI_TAI_LIEU: 'loai-tai-lieu',
  },
  MAP: {
    BASE_PATH: 'map',
    LOGIN: 'login',
    DASHBOARD: 'dashboard',
  },
} as const;

export type NavigatorEndpoint = typeof NAVIGATOR_ENDPOINT;
