package com.gfi.backend.models.dtos.dashboard;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO tổng hợp thống kê cho Dashboard.
 * Được trả về từ GET /api/dashboard/stats
 */
@Data
@Builder
public class DashboardStatsDto {

    /** Tổng số học sinh đang hoạt động trong phạm vi quyền */
    private long totalStudents;

    /** Tổng số cán bộ/giáo viên */
    private long totalStaffs;

    /** Tổng số lớp học trong năm học hiện tại */
    private long totalClassrooms;

    /** Tổng số tài khoản người dùng đang hoạt động */
    private long totalUsers;

    /** Tỷ lệ điểm danh tháng hiện tại (%) */
    private double attendanceRateCurrentMonth;

    /** Tên năm học hiện tại */
    private String currentSchoolYearName;

    /** Phân bố học sinh theo trạng thái */
    private List<LabelValueDto> studentStatusDistribution;

    /** Phân bố cán bộ theo giới tính */
    private List<LabelValueDto> staffGenderDistribution;

    /** Phân bố học sinh theo khối */
    private List<LabelValueDto> studentsByGradeLevel;

    /** Phân bố lớp học theo khối */
    private List<LabelValueDto> classroomsByGradeLevel;

    /** Thống kê điểm danh 6 tháng gần đây (có/vắng) */
    private List<AttendanceMonthlyStatsDto> attendanceLast6Months;

    /** Thống kê học sinh nhập học theo từng năm học */
    private List<LabelValueDto> studentEnrollmentTrend;

    /** Top 5 đơn vị có nhiều học sinh nhất */
    private List<LabelValueDto> topUnitsByStudentCount;

    /** Phân bố giáo viên theo vai trò/phân công */
    private List<LabelValueDto> staffByAssignment;
}
