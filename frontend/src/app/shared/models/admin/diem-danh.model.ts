import { ID_TYPE } from '@model/response.model';

export const DIEM_DANH_API_ENDPOINT = {
  BASE_PATH: 'attendance',
  DAILY_SHEET: 'daily-sheet',
  MONTHLY_SHEET: 'monthly-sheet',
};

export const LOP_KHOI_NHOM_API_ENDPOINT = {
  BASE_PATH: 'classes',
  GRADE_CLASS_GROUPS: 'grade-class-groups',
};

export const HOC_SINH_DIEM_DANH_API_ENDPOINT = {
  BASE_PATH: 'students',
  BY_CLASSROOM: 'by-classroom',
};

export interface DiemDanhLopItem {
  id: ID_TYPE;
  name: string;
}

export interface DiemDanhKhoiNhomItem {
  gradeLevelId: ID_TYPE;
  gradeLevelName: string;
  gradeNumber?: number;
  classes: DiemDanhLopItem[];
}

export interface DiemDanhHocSinhOption {
  id: ID_TYPE;
  code?: string;
  name: string;
}

export interface DiemDanhNgayHocSinh {
  studentId: ID_TYPE;
  studentCode?: string;
  fullName: string;
  attendanceStatus?: string;
  note?: string;
}

export interface DiemDanhNgayResponse {
  classroom?: {
    id?: ID_TYPE;
    name?: string;
  };
  attendanceDate?: string;
  sessionType?: string;
  students?: DiemDanhNgayHocSinh[];
}

/** Một ô điểm danh trong tháng: ngày + trạng thái */
export interface DiemDanhThangOHocSinh {
  date: string;        // 'YYYY-MM-DD'
  status: string;      // 'P','K','X','C' hoặc ''
  note?: string;
}

/** View-model hàng học sinh trong bảng tháng */
export interface DiemDanhThangRowViewModel {
  studentId: ID_TYPE;
  studentCode?: string;
  fullName: string;
  /** key = 'YYYY-MM-DD', value = trạng thái */
  statusMap: Record<string, string>;
  countP: number;
  countK: number;
  countX: number;
  totalAbsent: number;
}

/** Response từ API monthly-sheet */
export interface DiemDanhThangHocSinhApi {
  studentId?: ID_TYPE;
  studentCode?: string;
  fullName?: string;
  attendances?: Array<{ date: string; status: string; note?: string }>;
}

export interface DiemDanhThangResponse {
  classroom?: { id?: ID_TYPE; name?: string };
  month?: string;
  sessionType?: string;
  days?: unknown[];
  students?: DiemDanhThangHocSinhApi[];
}

export interface DiemDanhItemSaveRequest {
  classroomId: number;
  studentId: number;
  attendanceDate: string;
  sessionType: string;
  status: string;
  note?: string;
}

/** ViewModel cũ (ngày đơn) – giữ lại cho tương thích ngược */
export interface DiemDanhRowViewModel {
  studentId: ID_TYPE;
  studentCode?: string;
  fullName: string;
  attendanceStatus: string;
  note: string;
}
