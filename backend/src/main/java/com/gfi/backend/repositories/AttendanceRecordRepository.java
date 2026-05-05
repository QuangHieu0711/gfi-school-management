package com.gfi.backend.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // ── Dashboard queries ────────────────────────────────────────────────────

    /** Tổng số lượt điểm danh trong khoảng ngày (unrestricted) */
    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.attendanceDate BETWEEN :from AND :to AND a.deletedFlag = 0")
    long countByAttendanceDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Tổng số lượt điểm danh theo trạng thái trong khoảng ngày (unrestricted) */
    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.attendanceDate BETWEEN :from AND :to AND a.attendanceStatus = :status AND a.deletedFlag = 0")
    long countByAttendanceDateBetweenAndAttendanceStatus(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("status") String status);

    /** Tổng số lượt điểm danh trong khoảng ngày theo unit IDs (scoped) */
    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.attendanceDate BETWEEN :from AND :to AND a.classroom.unit.id IN :unitIds AND a.deletedFlag = 0")
    long countByAttendanceDateBetweenAndUnitIdIn(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("unitIds") List<Long> unitIds);

    /** Tổng số lượt điểm danh theo trạng thái trong khoảng ngày và unit IDs (scoped) */
    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.attendanceDate BETWEEN :from AND :to AND a.attendanceStatus = :status AND a.classroom.unit.id IN :unitIds AND a.deletedFlag = 0")
    long countByAttendanceDateBetweenAndAttendanceStatusAndUnitIdIn(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("status") String status,
            @Param("unitIds") List<Long> unitIds);
}
