package com.gfi.backend.services.implement;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.attendance.AttendanceBulkItemRequest;
import com.gfi.backend.models.dtos.attendance.AttendanceBulkUpsertRequest;
import com.gfi.backend.models.dtos.attendance.AttendanceClassroomDto;
import com.gfi.backend.models.dtos.attendance.AttendanceDailySheetDto;
import com.gfi.backend.models.dtos.attendance.AttendanceDailyStudentDto;
import com.gfi.backend.models.dtos.attendance.AttendanceDayDto;
import com.gfi.backend.models.dtos.attendance.AttendanceMonthlySheetDto;
import com.gfi.backend.models.dtos.attendance.AttendanceRecordDto;
import com.gfi.backend.models.dtos.attendance.AttendanceStudentRowDto;
import com.gfi.backend.models.dtos.attendance.AttendanceSummaryDto;
import com.gfi.backend.models.dtos.attendance.AttendanceUpsertRequest;
import com.gfi.backend.models.entities.AttendanceRecord;
import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.entities.Student;
import com.gfi.backend.models.entities.StudentEnrollment;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.AttendanceRecordRepository;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.StudentEnrollmentRepository;
import com.gfi.backend.repositories.StudentRepository;
import com.gfi.backend.services.interfaces.AttendanceService;
import com.gfi.backend.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private static final Set<String> VALID_SESSION_TYPES = Set.of("SANG", "CHIEU");
    private static final Set<String> VALID_STATUSES = Set.of("C", "P", "K", "X");

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ClassroomRepository classroomRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public AttendanceMonthlySheetDto getMonthlySheet(Long classroomId, String month, String sessionType) {
        Classroom classroom = findClassroom(classroomId);
        String normalizedSessionType = normalizeSessionType(sessionType);
        YearMonth yearMonth = parseMonth(month);
        List<StudentEnrollment> enrollments = getActiveEnrollments(classroomId);
        List<Student> students = enrollments.stream().map(StudentEnrollment::getStudent).toList();

        LocalDate fromDate = yearMonth.atDay(1);
        LocalDate toDate = yearMonth.atEndOfMonth();
        List<AttendanceRecord> records = attendanceRecordRepository
                .findByClassroomIdAndAttendanceDateBetweenAndSessionTypeAndDeletedFlagOrderByAttendanceDateAscStudentIdAsc(
                        classroomId, fromDate, toDate, normalizedSessionType, 0);

        Map<Long, Map<String, AttendanceRecord>> recordsByStudent = new LinkedHashMap<>();
        for (AttendanceRecord record : records) {
            recordsByStudent
                    .computeIfAbsent(record.getStudent().getId(), ignored -> new LinkedHashMap<>())
                    .put(record.getAttendanceDate().toString(), record);
        }

        List<AttendanceDayDto> days = buildDays(yearMonth);
        List<AttendanceStudentRowDto> rows = new ArrayList<>();
        for (Student student : students) {
            Map<String, String> attendanceByDay = new LinkedHashMap<>();
            Map<String, AttendanceRecord> studentRecords = recordsByStudent.getOrDefault(student.getId(), Map.of());
            long presentCount = 0;
            long excusedCount = 0;
            long unexcusedCount = 0;
            long lateCount = 0;

            for (AttendanceDayDto day : days) {
                AttendanceRecord record = studentRecords.get(day.getAttendanceDate().toString());
                String status = record == null ? null : record.getAttendanceStatus();
                attendanceByDay.put(day.getAttendanceDate().toString(), status);
                if ("C".equals(status)) presentCount++;
                if ("P".equals(status)) excusedCount++;
                if ("K".equals(status)) unexcusedCount++;
                if ("X".equals(status)) lateCount++;
            }

            rows.add(AttendanceStudentRowDto.builder()
                    .studentId(student.getId())
                    .studentCode(student.getStudentCode())
                    .fullName(student.getFullName())
                    .attendanceByDay(attendanceByDay)
                    .summary(AttendanceSummaryDto.builder()
                            .presentCount(presentCount)
                            .excusedCount(excusedCount)
                            .unexcusedCount(unexcusedCount)
                            .lateCount(lateCount)
                            .totalAbsentCount(excusedCount + unexcusedCount)
                            .build())
                    .build());
        }

        return AttendanceMonthlySheetDto.builder()
                .classroom(toClassroomDto(classroom))
                .month(yearMonth.toString())
                .sessionType(normalizedSessionType)
                .days(days)
                .students(rows)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceDailySheetDto getDailySheet(Long classroomId, LocalDate attendanceDate, String sessionType) {
        Classroom classroom = findClassroom(classroomId);
        String normalizedSessionType = normalizeSessionType(sessionType);
        List<StudentEnrollment> enrollments = getActiveEnrollments(classroomId);
        List<AttendanceRecord> records = attendanceRecordRepository
                .findByClassroomIdAndAttendanceDateAndSessionTypeAndDeletedFlagOrderByStudentIdAsc(
                        classroomId, attendanceDate, normalizedSessionType, 0);
        Map<Long, AttendanceRecord> recordsByStudentId = new LinkedHashMap<>();
        for (AttendanceRecord record : records) {
            recordsByStudentId.put(record.getStudent().getId(), record);
        }

        List<AttendanceDailyStudentDto> students = enrollments.stream()
                .map(StudentEnrollment::getStudent)
                .map(student -> {
                    AttendanceRecord record = recordsByStudentId.get(student.getId());
                    return AttendanceDailyStudentDto.builder()
                            .studentId(student.getId())
                            .studentCode(student.getStudentCode())
                            .fullName(student.getFullName())
                            .attendanceStatus(record == null ? null : record.getAttendanceStatus())
                            .note(record == null ? null : record.getNote())
                            .build();
                })
                .toList();

        return AttendanceDailySheetDto.builder()
                .classroom(toClassroomDto(classroom))
                .attendanceDate(attendanceDate)
                .sessionType(normalizedSessionType)
                .students(students)
                .build();
    }

    @Override
    @Transactional
    public AttendanceRecordDto upsert(AttendanceUpsertRequest request) {
        Classroom classroom = findClassroom(request.getClassroomId());
        Student student = findStudent(request.getStudentId());
        validateStudentInClassroom(classroom.getId(), student.getId());
        String normalizedSessionType = normalizeSessionType(request.getSessionType());
        String normalizedStatus = normalizeStatus(request.getAttendanceStatus());

        AttendanceRecord record = attendanceRecordRepository
                .findByClassroomIdAndStudentIdAndAttendanceDateAndSessionType(
                        classroom.getId(),
                        student.getId(),
                        request.getAttendanceDate(),
                        normalizedSessionType)
                .orElse(null);

        if (!StringUtils.hasText(normalizedStatus)) {
            if (record != null) {
                record.setDeletedFlag(1);
                record.setDeletedAt(java.time.LocalDateTime.now());
                record.setDeletedBy(SecurityUtils.getCurrentUsername());
                attendanceRecordRepository.save(record);
            }
            return AttendanceRecordDto.builder()
                    .classroomId(classroom.getId())
                    .studentId(student.getId())
                    .attendanceDate(request.getAttendanceDate())
                    .sessionType(normalizedSessionType)
                    .attendanceStatus(null)
                    .note(null)
                    .build();
        }

        if (record == null) {
            record = new AttendanceRecord();
            record.setClassroom(classroom);
            record.setStudent(student);
            record.setAttendanceDate(request.getAttendanceDate());
            record.setSessionType(normalizedSessionType);
            record.setCreatedBy(SecurityUtils.getCurrentUsername());
            record.setDeletedFlag(0);
        } else {
            record.setUpdatedBy(SecurityUtils.getCurrentUsername());
            record.setDeletedFlag(0);
            record.setDeletedAt(null);
            record.setDeletedBy(null);
        }
        record.setAttendanceStatus(normalizedStatus);
        record.setNote(normalizeNullable(request.getNote()));
        AttendanceRecord saved = attendanceRecordRepository.save(record);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void bulkUpsert(AttendanceBulkUpsertRequest request) {
        String normalizedSessionType = normalizeSessionType(request.getSessionType());
        findClassroom(request.getClassroomId());
        for (AttendanceBulkItemRequest item : request.getItems()) {
            AttendanceUpsertRequest single = new AttendanceUpsertRequest();
            single.setClassroomId(request.getClassroomId());
            single.setStudentId(item.getStudentId());
            single.setAttendanceDate(item.getAttendanceDate());
            single.setSessionType(normalizedSessionType);
            single.setAttendanceStatus(item.getAttendanceStatus());
            single.setNote(item.getNote());
            upsert(single);
        }
    }

    private List<StudentEnrollment> getActiveEnrollments(Long classroomId) {
        return studentEnrollmentRepository.findByClassroomIdAndDeletedFlagOrderByStudentFullNameAsc(classroomId, 0);
    }

    private List<AttendanceDayDto> buildDays(YearMonth yearMonth) {
        List<AttendanceDayDto> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            DayOfWeek dow = date.getDayOfWeek();
            days.add(AttendanceDayDto.builder()
                    .attendanceDate(date)
                    .dayOfMonth(day)
                    .dayOfWeek(dow.getDisplayName(TextStyle.SHORT, new Locale("vi", "VN")))
                    .weekend(dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY)
                    .build());
        }
        return days;
    }

    private AttendanceClassroomDto toClassroomDto(Classroom classroom) {
        return AttendanceClassroomDto.builder()
                .id(classroom.getId())
                .code(classroom.getCode())
                .name(classroom.getName())
                .build();
    }

    private AttendanceRecordDto toDto(AttendanceRecord record) {
        return AttendanceRecordDto.builder()
                .id(record.getId())
                .classroomId(record.getClassroom().getId())
                .studentId(record.getStudent().getId())
                .attendanceDate(record.getAttendanceDate())
                .sessionType(record.getSessionType())
                .attendanceStatus(record.getAttendanceStatus())
                .note(record.getNote())
                .build();
    }

    private Classroom findClassroom(Long id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.STUDENT_NOT_FOUND));
    }

    private void validateStudentInClassroom(Long classroomId, Long studentId) {
        boolean exists = studentEnrollmentRepository
                .findByClassroomIdAndDeletedFlagOrderByStudentFullNameAsc(classroomId, 0)
                .stream()
                .anyMatch(enrollment -> enrollment.getStudent().getId().equals(studentId));
        if (!exists) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Học sinh không thuộc lớp đã chọn");
        }
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (Exception ex) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Tháng không đúng định dạng yyyy-MM");
        }
    }

    private String normalizeSessionType(String sessionType) {
        String normalized = normalize(sessionType);
        if (!StringUtils.hasText(normalized)) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Buổi điểm danh là bắt buộc");
        }
        if (!VALID_SESSION_TYPES.contains(normalized)) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Buổi điểm danh chỉ hỗ trợ SANG hoặc CHIEU");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = normalize(status);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (!VALID_STATUSES.contains(normalized)) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Trạng thái điểm danh chỉ hỗ trợ C, P, K, X");
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
