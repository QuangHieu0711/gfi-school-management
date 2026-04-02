package com.gfi.backend.repositories;

import java.util.Optional;

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
}
