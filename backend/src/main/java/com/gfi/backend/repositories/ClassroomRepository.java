package com.gfi.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.Classroom;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long>, JpaSpecificationExecutor<Classroom> {
    long countByUnitId(Long unitId);
    long countByGradeLevelId(Long gradeLevelId);
    long countBySchoolYearId(Long schoolYearId);
    Optional<Classroom> findByUnitIdAndGradeLevelIdAndSchoolYearIdAndCode(Long unitId, Long gradeLevelId, Long schoolYearId, String code);
    Optional<Classroom> findByUnitIdAndGradeLevelIdAndSchoolYearIdAndName(Long unitId, Long gradeLevelId, Long schoolYearId, String name);
    Optional<Classroom> findByUnitIdAndSchoolYearIdAndName(Long unitId, Long schoolYearId, String name);

    /**
     * Find classrooms by unit IDs (for data scope filtering)
     */
    List<Classroom> findByUnitIdIn(List<Long> unitIds);

    /**
     * Find classrooms by unit IDs with pagination (for data scope filtering)
     */
    Page<Classroom> findByUnitIdIn(List<Long> unitIds, Pageable pageable);

    /**
     * Find single classroom by ID and allowed unit IDs (for data scope enforcement)
     */
    Optional<Classroom> findByIdAndUnitIdIn(Long id, List<Long> unitIds);

    /**
     * Find classrooms by unit, grade level, and school year (for options/dropdown)
     */
    List<Classroom> findByUnitIdAndGradeLevelIdAndSchoolYearId(Long unitId, Long gradeLevelId, Long schoolYearId);

    List<Classroom> findByUnitIdAndSchoolYearIdAndDeletedFlagOrderByGradeLevelGradeNumberAscNameAsc(
            Long unitId,
            Long schoolYearId,
            Integer deletedFlag);

    // ── Dashboard queries ────────────────────────────────────────────────────

    /** Đếm lớp học theo unit IDs và năm học */
    @Query("SELECT COUNT(c) FROM Classroom c WHERE c.unit.id IN :unitIds AND c.schoolYear.id = :schoolYearId AND c.deletedFlag = 0")
    long countByUnitIdInAndSchoolYearId(@Param("unitIds") List<Long> unitIds, @Param("schoolYearId") Long schoolYearId);

    /** Phân bố lớp học theo khối (unrestricted) */
    @Query("SELECT c.gradeLevel.gradeNumber, COUNT(c) FROM Classroom c WHERE c.schoolYear.id = :schoolYearId AND c.deletedFlag = 0 GROUP BY c.gradeLevel.gradeNumber ORDER BY c.gradeLevel.gradeNumber ASC")
    List<Object[]> countGroupByGradeAndSchoolYear(@Param("schoolYearId") Long schoolYearId);

    /** Phân bố lớp học theo khối (scoped) */
    @Query("SELECT c.gradeLevel.gradeNumber, COUNT(c) FROM Classroom c WHERE c.unit.id IN :unitIds AND c.schoolYear.id = :schoolYearId AND c.deletedFlag = 0 GROUP BY c.gradeLevel.gradeNumber ORDER BY c.gradeLevel.gradeNumber ASC")
    List<Object[]> countGroupByGradeAndSchoolYearAndUnitIdIn(@Param("schoolYearId") Long schoolYearId, @Param("unitIds") List<Long> unitIds);
}
