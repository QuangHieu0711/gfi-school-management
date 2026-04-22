package com.gfi.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
