import { ID_TYPE } from '@model/response.model';

export const DASHBOARD_API_ENDPOINT = {
  BASE_PATH: 'dashboard',
};

export interface DashboardDistributionItem {
  label: string;
  value: number;
}

export interface DashboardAttendanceItem {
  month: string;
  presentCount: number;
  absentCount: number;
}

export interface DashboardSummary {
  totalStudents: number;
  totalStaffs: number;
  totalClassrooms: number;
  totalUsers: number;
  attendanceRateCurrentMonth: number;
  currentSchoolYearName: string;
  studentStatusDistribution: DashboardDistributionItem[];
  staffGenderDistribution: DashboardDistributionItem[];
  studentsByGradeLevel: DashboardDistributionItem[];
  classroomsByGradeLevel: DashboardDistributionItem[];
  attendanceLast6Months: DashboardAttendanceItem[];
  studentEnrollmentTrend: DashboardDistributionItem[];
  topUnitsByStudentCount: DashboardDistributionItem[];
  staffByAssignment: DashboardDistributionItem[];
  id?: ID_TYPE;
}
