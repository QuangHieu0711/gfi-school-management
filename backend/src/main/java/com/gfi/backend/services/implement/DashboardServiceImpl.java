package com.gfi.backend.services.implement;

import com.gfi.backend.models.dtos.dashboard.AttendanceMonthlyStatsDto;
import com.gfi.backend.models.dtos.dashboard.DashboardStatsDto;
import com.gfi.backend.models.dtos.dashboard.LabelValueDto;
import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.models.security.ResolvedScope;
import com.gfi.backend.repositories.*;
import com.gfi.backend.services.interfaces.DashboardService;
import com.gfi.backend.services.interfaces.DataScopeFilterService;
import com.gfi.backend.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation của DashboardService.
 * Tất cả số liệu đều được lọc theo phạm vi quyền dữ liệu (DataScope) của người dùng.
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final UnitRepository unitRepository;
    private final DataScopeFilterService dataScopeFilterService;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    @Override
    public DashboardStatsDto getStats(Long unitId) {
        // Lấy danh sách unit IDs theo phân quyền dữ liệu
        List<Long> allowedUnitIds = resolveAllowedUnitIds(unitId);
        boolean isUnrestricted = allowedUnitIds == null;

        // Lấy năm học hiện tại
        String currentSchoolYearName = schoolYearRepository.findCurrentSchoolYear()
                .map(sy -> sy.getName())
                .orElse("Chưa xác định");
        Long currentSchoolYearId = schoolYearRepository.findCurrentSchoolYear()
                .map(sy -> sy.getId())
                .orElse(null);

        // ── Số liệu tổng quan ─────────────────────────────────────────────────
        long totalStudents = countStudents(isUnrestricted, allowedUnitIds);
        long totalStaffs = countStaffs(isUnrestricted, allowedUnitIds);
        long totalClassrooms = countClassrooms(isUnrestricted, allowedUnitIds, currentSchoolYearId);
        long totalUsers = countUsers(isUnrestricted, allowedUnitIds);

        // ── Tỷ lệ điểm danh tháng hiện tại ───────────────────────────────────
        double attendanceRate = computeAttendanceRateCurrentMonth(isUnrestricted, allowedUnitIds, currentSchoolYearId);

        // ── Phân bố học sinh theo trạng thái ─────────────────────────────────
        List<LabelValueDto> studentStatusDist = buildStudentStatusDistribution(isUnrestricted, allowedUnitIds);

        // ── Phân bố cán bộ theo giới tính ─────────────────────────────────────
        List<LabelValueDto> staffGenderDist = buildStaffGenderDistribution(isUnrestricted, allowedUnitIds);

        // ── Phân bố học sinh theo khối ─────────────────────────────────────────
        List<LabelValueDto> studentsByGrade = buildStudentsByGrade(isUnrestricted, allowedUnitIds, currentSchoolYearId);

        // ── Phân bố lớp học theo khối ──────────────────────────────────────────
        List<LabelValueDto> classroomsByGrade = buildClassroomsByGrade(isUnrestricted, allowedUnitIds, currentSchoolYearId);

        // ── Điểm danh 6 tháng gần đây ─────────────────────────────────────────
        List<AttendanceMonthlyStatsDto> attendanceLast6Months = buildAttendanceLast6Months(isUnrestricted, allowedUnitIds, currentSchoolYearId);

        // ── Xu hướng nhập học theo năm học ───────────────────────────────────
        List<LabelValueDto> enrollmentTrend = buildEnrollmentTrend(isUnrestricted, allowedUnitIds);

        // ── Top đơn vị theo số học sinh ───────────────────────────────────────
        List<LabelValueDto> topUnits = buildTopUnitsByStudentCount(isUnrestricted, allowedUnitIds);

        // ── Phân bố giáo viên theo phân công ─────────────────────────────────
        List<LabelValueDto> staffByAssignment = buildStaffByAssignment(isUnrestricted, allowedUnitIds);

        return DashboardStatsDto.builder()
                .totalStudents(totalStudents)
                .totalStaffs(totalStaffs)
                .totalClassrooms(totalClassrooms)
                .totalUsers(totalUsers)
                .attendanceRateCurrentMonth(attendanceRate)
                .currentSchoolYearName(currentSchoolYearName)
                .studentStatusDistribution(studentStatusDist)
                .staffGenderDistribution(staffGenderDist)
                .studentsByGradeLevel(studentsByGrade)
                .classroomsByGradeLevel(classroomsByGrade)
                .attendanceLast6Months(attendanceLast6Months)
                .studentEnrollmentTrend(enrollmentTrend)
                .topUnitsByStudentCount(topUnits)
                .staffByAssignment(staffByAssignment)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers: Phân quyền
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Trả về danh sách unit IDs được phép truy cập.
     * null = không bị giới hạn (toàn quyền).
     */
    private List<Long> resolveAllowedUnitIds(Long requestedUnitId) {
        List<ResolvedScope> scopes = dataScopeFilterService.getResolvedScopes("STUDENT_PROFILE", ActionType.VIEW);

        // Nếu không có cấu hình hoặc có scope unrestricted → không giới hạn
        boolean hasUnrestricted = scopes.isEmpty() || scopes.stream().anyMatch(ResolvedScope::isUnrestricted);
        if (hasUnrestricted) {
            if (requestedUnitId != null) {
                return List.of(requestedUnitId);
            }
            return null; // unrestricted
        }

        // Gom unit IDs từ các UNIT scope
        Set<Long> unitIds = new HashSet<>();
        for (ResolvedScope scope : scopes) {
            if (scope.getScopeType() == ScopeType.UNIT && scope.getScopeIds() != null) {
                unitIds.addAll(scope.getScopeIds());
            }
        }

        // Nếu có filter theo unitId cụ thể, giao với allowedUnitIds
        if (requestedUnitId != null && unitIds.contains(requestedUnitId)) {
            return List.of(requestedUnitId);
        } else if (requestedUnitId != null) {
            return List.of(); // unitId yêu cầu không được phép
        }

        return new ArrayList<>(unitIds);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers: Số liệu tổng quan
    // ─────────────────────────────────────────────────────────────────────────

    private long countStudents(boolean unrestricted, List<Long> unitIds) {
        if (unrestricted) return studentRepository.count();
        if (unitIds == null || unitIds.isEmpty()) return 0;
        return studentRepository.countByUnitIdIn(unitIds);
    }

    private long countStaffs(boolean unrestricted, List<Long> unitIds) {
        if (unrestricted) return staffRepository.count();
        if (unitIds == null || unitIds.isEmpty()) return 0;
        return staffRepository.countByUnitIdIn(unitIds);
    }

    private long countClassrooms(boolean unrestricted, List<Long> unitIds, Long schoolYearId) {
        if (schoolYearId == null) return 0;
        if (unrestricted) return classroomRepository.countBySchoolYearId(schoolYearId);
        if (unitIds == null || unitIds.isEmpty()) return 0;
        return classroomRepository.countByUnitIdInAndSchoolYearId(unitIds, schoolYearId);
    }

    private long countUsers(boolean unrestricted, List<Long> unitIds) {
        if (unrestricted) return userRepository.count();
        if (unitIds == null || unitIds.isEmpty()) return 0;
        return userRepository.countByStaffUnitIdIn(unitIds);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers: Điểm danh
    // ─────────────────────────────────────────────────────────────────────────

    private double computeAttendanceRateCurrentMonth(boolean unrestricted, List<Long> unitIds, Long schoolYearId) {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.withDayOfMonth(1);
        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());

        long total = unrestricted
                ? attendanceRecordRepository.countByAttendanceDateBetween(firstDay, lastDay)
                : attendanceRecordRepository.countByAttendanceDateBetweenAndUnitIdIn(firstDay, lastDay, unitIds);

        if (total == 0) return 0.0;

        long present = unrestricted
                ? attendanceRecordRepository.countByAttendanceDateBetweenAndAttendanceStatus(firstDay, lastDay, "P")
                : attendanceRecordRepository.countByAttendanceDateBetweenAndAttendanceStatusAndUnitIdIn(firstDay, lastDay, "P", unitIds);

        return Math.round((double) present / total * 10000.0) / 100.0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers: Phân bố
    // ─────────────────────────────────────────────────────────────────────────

    private List<LabelValueDto> buildStudentStatusDistribution(boolean unrestricted, List<Long> unitIds) {
        List<Object[]> rows = unrestricted
                ? studentRepository.countGroupByStatus()
                : studentRepository.countGroupByStatusAndUnitIdIn(unitIds);

        return rows.stream()
                .map(row -> {
                    Integer status = (Integer) row[0];
                    long count = ((Number) row[1]).longValue();
                    String label = switch (status == null ? -1 : status) {
                        case 0 -> "Đang học";
                        case 1 -> "Đã tốt nghiệp";
                        case 2 -> "Đã thôi học";
                        default -> "Không xác định";
                    };
                    return new LabelValueDto(label, count);
                })
                .collect(Collectors.toList());
    }

    private List<LabelValueDto> buildStaffGenderDistribution(boolean unrestricted, List<Long> unitIds) {
        List<Object[]> rows = unrestricted
                ? staffRepository.countGroupByGender()
                : staffRepository.countGroupByGenderAndUnitIdIn(unitIds);

        return rows.stream()
                .map(row -> {
                    String gender = (String) row[0];
                    long count = ((Number) row[1]).longValue();
                    String label = "NAM".equalsIgnoreCase(gender) ? "Nam" :
                            "NU".equalsIgnoreCase(gender) || "NỮ".equalsIgnoreCase(gender) ? "Nữ" :
                                    (gender == null ? "Không xác định" : gender);
                    return new LabelValueDto(label, count);
                })
                .collect(Collectors.toList());
    }

    private List<LabelValueDto> buildStudentsByGrade(boolean unrestricted, List<Long> unitIds, Long schoolYearId) {
        if (schoolYearId == null) return List.of();
        List<Object[]> rows = unrestricted
                ? studentEnrollmentRepository.countStudentsByGradeAndSchoolYear(schoolYearId)
                : studentEnrollmentRepository.countStudentsByGradeAndSchoolYearAndUnitIdIn(schoolYearId, unitIds);

        return rows.stream()
                .map(row -> new LabelValueDto("Khối " + row[0], ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    private List<LabelValueDto> buildClassroomsByGrade(boolean unrestricted, List<Long> unitIds, Long schoolYearId) {
        if (schoolYearId == null) return List.of();
        List<Object[]> rows = unrestricted
                ? classroomRepository.countGroupByGradeAndSchoolYear(schoolYearId)
                : classroomRepository.countGroupByGradeAndSchoolYearAndUnitIdIn(schoolYearId, unitIds);

        return rows.stream()
                .map(row -> new LabelValueDto("Khối " + row[0], ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    private List<AttendanceMonthlyStatsDto> buildAttendanceLast6Months(boolean unrestricted, List<Long> unitIds, Long schoolYearId) {
        LocalDate today = LocalDate.now();
        List<AttendanceMonthlyStatsDto> result = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDate month = today.minusMonths(i);
            LocalDate from = month.withDayOfMonth(1);
            LocalDate to = month.withDayOfMonth(month.lengthOfMonth());
            String label = month.format(MONTH_FORMATTER);

            long present, absent;
            if (unrestricted) {
                present = attendanceRecordRepository.countByAttendanceDateBetweenAndAttendanceStatus(from, to, "P");
                absent = attendanceRecordRepository.countByAttendanceDateBetweenAndAttendanceStatus(from, to, "A");
            } else {
                present = attendanceRecordRepository.countByAttendanceDateBetweenAndAttendanceStatusAndUnitIdIn(from, to, "P", unitIds);
                absent = attendanceRecordRepository.countByAttendanceDateBetweenAndAttendanceStatusAndUnitIdIn(from, to, "A", unitIds);
            }

            result.add(new AttendanceMonthlyStatsDto(label, present, absent));
        }

        return result;
    }

    private List<LabelValueDto> buildEnrollmentTrend(boolean unrestricted, List<Long> unitIds) {
        List<Object[]> rows = unrestricted
                ? studentEnrollmentRepository.countStudentsBySchoolYear()
                : studentEnrollmentRepository.countStudentsBySchoolYearAndUnitIdIn(unitIds);

        return rows.stream()
                .map(row -> new LabelValueDto(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    private List<LabelValueDto> buildTopUnitsByStudentCount(boolean unrestricted, List<Long> unitIds) {
        List<Object[]> rows = unrestricted
                ? studentRepository.countGroupByUnit()
                : studentRepository.countGroupByUnitAndUnitIdIn(unitIds);

        return rows.stream()
                .limit(5)
                .map(row -> new LabelValueDto(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    private List<LabelValueDto> buildStaffByAssignment(boolean unrestricted, List<Long> unitIds) {
        // Cán bộ có tài khoản vs chưa có
        long withUser = unrestricted
                ? staffRepository.countByUserIsNotNull()
                : staffRepository.countByUserIsNotNullAndUnitIdIn(unitIds);
        long withoutUser = unrestricted
                ? staffRepository.countByUserIsNull()
                : staffRepository.countByUserIsNullAndUnitIdIn(unitIds);

        List<LabelValueDto> list = new ArrayList<>();
        if (withUser > 0) list.add(new LabelValueDto("Có tài khoản", withUser));
        if (withoutUser > 0) list.add(new LabelValueDto("Chưa có tài khoản", withoutUser));
        return list;
    }
}
