package com.gfi.backend.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.AttendanceRecord;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByClassroomIdAndStudentIdAndAttendanceDateAndSessionType(
            Long classroomId, Long studentId, LocalDate attendanceDate, String sessionType);

    List<AttendanceRecord> findByClassroomIdAndAttendanceDateBetweenAndSessionTypeAndDeletedFlagOrderByAttendanceDateAscStudentIdAsc(
            Long classroomId, LocalDate fromDate, LocalDate toDate, String sessionType, Integer deletedFlag);

    List<AttendanceRecord> findByClassroomIdAndAttendanceDateAndSessionTypeAndDeletedFlagOrderByStudentIdAsc(
            Long classroomId, LocalDate attendanceDate, String sessionType, Integer deletedFlag);

    List<AttendanceRecord> findByClassroomIdAndStudentIdAndDeletedFlag(
            Long classroomId, Long studentId, Integer deletedFlag);
}
