package com.gfi.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.StudentEnrollment;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {
    List<StudentEnrollment> findByStudentIdOrderBySchoolYearIdDescIdDesc(Long studentId);
    Optional<StudentEnrollment> findByStudentIdAndSchoolYearId(Long studentId, Long schoolYearId);
    List<StudentEnrollment> findByClassroomIdAndDeletedFlagOrderByStudentFullNameAsc(Long classroomId, Integer deletedFlag);
    void deleteByStudentId(Long studentId);

    // ── Dashboard queries ────────────────────────────────────────────────────

    /** Phân bố học sinh theo khối trong năm học (unrestricted) */
    @Query("SELECT e.classroom.gradeLevel.gradeNumber, COUNT(e) FROM StudentEnrollment e " +
           "WHERE e.schoolYear.id = :schoolYearId AND e.deletedFlag = 0 " +
           "GROUP BY e.classroom.gradeLevel.gradeNumber ORDER BY e.classroom.gradeLevel.gradeNumber ASC")
    List<Object[]> countStudentsByGradeAndSchoolYear(@Param("schoolYearId") Long schoolYearId);

    /** Phân bố học sinh theo khối trong năm học (scoped by unit) */
    @Query("SELECT e.classroom.gradeLevel.gradeNumber, COUNT(e) FROM StudentEnrollment e " +
           "WHERE e.schoolYear.id = :schoolYearId AND e.student.unit.id IN :unitIds AND e.deletedFlag = 0 " +
           "GROUP BY e.classroom.gradeLevel.gradeNumber ORDER BY e.classroom.gradeLevel.gradeNumber ASC")
    List<Object[]> countStudentsByGradeAndSchoolYearAndUnitIdIn(
            @Param("schoolYearId") Long schoolYearId,
            @Param("unitIds") List<Long> unitIds);

    /** Xu hướng nhập học theo năm học (unrestricted) */
    @Query("SELECT e.schoolYear.name, COUNT(e) FROM StudentEnrollment e " +
           "WHERE e.deletedFlag = 0 " +
           "GROUP BY e.schoolYear.name, e.schoolYear.id ORDER BY e.schoolYear.id ASC")
    List<Object[]> countStudentsBySchoolYear();

    /** Xu hướng nhập học theo năm học (scoped by unit) */
    @Query("SELECT e.schoolYear.name, COUNT(e) FROM StudentEnrollment e " +
           "WHERE e.student.unit.id IN :unitIds AND e.deletedFlag = 0 " +
           "GROUP BY e.schoolYear.name, e.schoolYear.id ORDER BY e.schoolYear.id ASC")
    List<Object[]> countStudentsBySchoolYearAndUnitIdIn(@Param("unitIds") List<Long> unitIds);
}
