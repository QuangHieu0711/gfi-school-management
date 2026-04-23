package com.gfi.backend.services.implement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
import com.gfi.backend.models.dtos.attendance.AttendanceBulkStudentRequest;
import com.gfi.backend.models.dtos.attendance.AttendanceBulkUpsertRequest;
import com.gfi.backend.models.dtos.attendance.AttendanceMonthlyTableDto;
import com.gfi.backend.models.dtos.attendance.AttendanceMonthlyTableStudentDto;
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
    public AttendanceMonthlyTableDto getMonthlyTable(Long classroomId, Integer year, Integer month, String sessionType) {
        Classroom classroom = findClassroom(classroomId);
        String normalizedSessionType = normalizeSessionType(sessionType);
        YearMonth yearMonth = parseMonth(year, month);
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

        List<AttendanceMonthlyTableStudentDto> studentRows = new ArrayList<>();
        for (Student student : students) {
            Map<String, String> attendance = new LinkedHashMap<>();
            Map<String, AttendanceRecord> studentRecords = recordsByStudent.getOrDefault(student.getId(), Map.of());
            for (AttendanceRecord record : studentRecords.values()) {
                attendance.put(record.getAttendanceDate().toString(), record.getAttendanceStatus());
            }

            studentRows.add(AttendanceMonthlyTableStudentDto.builder()
                    .studentId(student.getId())
                    .studentCode(student.getStudentCode())
                    .studentName(student.getFullName())
                    .attendance(attendance)
                    .build());
        }

        return AttendanceMonthlyTableDto.builder()
                .classroomId(classroom.getId())
                .classroomName(classroom.getName())
                .sessionType(normalizedSessionType)
                .year(yearMonth.getYear())
                .month(yearMonth.getMonthValue())
                .students(studentRows)
                .build();
    }

    @Override
    @Transactional
    public void bulkUpsert(AttendanceBulkUpsertRequest request) {
        String normalizedSessionType = normalizeSessionType(request.getSessionType());
        findClassroom(request.getClassroomId());
        for (AttendanceBulkItemRequest item : request.getItems()) {
            for (AttendanceBulkStudentRequest student : item.getStudents()) {
                upsertAttendance(
                        request.getClassroomId(),
                        student.getStudentId(),
                        item.getAttendanceDate(),
                        normalizedSessionType,
                        student.getStatus(),
                        student.getNote());
            }
        }
    }

    private void upsertAttendance(Long classroomId, Long studentId, LocalDate attendanceDate, String sessionType,
            String status, String note) {
        Classroom classroom = findClassroom(classroomId);
        Student student = findStudent(studentId);
        validateStudentInClassroom(classroom.getId(), student.getId());
        String normalizedStatus = normalizeStatus(status);

        AttendanceRecord record = attendanceRecordRepository
                .findByClassroomIdAndStudentIdAndAttendanceDateAndSessionType(
                        classroom.getId(),
                        student.getId(),
                        attendanceDate,
                        sessionType)
                .orElse(null);

        if (!StringUtils.hasText(normalizedStatus)) {
            if (record != null) {
                record.setDeletedFlag(1);
                record.setDeletedAt(LocalDateTime.now());
                record.setDeletedBy(SecurityUtils.getCurrentUsername());
                attendanceRecordRepository.save(record);
            }
            return;
        }

        if (record == null) {
            record = new AttendanceRecord();
            record.setClassroom(classroom);
            record.setStudent(student);
            record.setAttendanceDate(attendanceDate);
            record.setSessionType(sessionType);
            record.setCreatedBy(SecurityUtils.getCurrentUsername());
            record.setDeletedFlag(0);
        } else {
            record.setUpdatedBy(SecurityUtils.getCurrentUsername());
            record.setDeletedFlag(0);
            record.setDeletedAt(null);
            record.setDeletedBy(null);
        }

        record.setAttendanceStatus(normalizedStatus);
        record.setNote(normalizeNullable(note));
        attendanceRecordRepository.save(record);
    }

    private List<StudentEnrollment> getActiveEnrollments(Long classroomId) {
        return studentEnrollmentRepository.findByClassroomIdAndDeletedFlagOrderByStudentFullNameAsc(classroomId, 0);
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
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Hoc sinh khong thuoc lop da chon");
        }
    }

    private YearMonth parseMonth(Integer year, Integer month) {
        try {
            return YearMonth.of(year, month);
        } catch (Exception ex) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Nam/thang khong hop le");
        }
    }

    private String normalizeSessionType(String sessionType) {
        String normalized = normalize(sessionType);
        if (!StringUtils.hasText(normalized)) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Buoi diem danh la bat buoc");
        }
        if (!VALID_SESSION_TYPES.contains(normalized)) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Buoi diem danh chi ho tro SANG hoac CHIEU");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = normalize(status);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (!VALID_STATUSES.contains(normalized)) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Trang thai diem danh chi ho tro C, P, K, X");
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
