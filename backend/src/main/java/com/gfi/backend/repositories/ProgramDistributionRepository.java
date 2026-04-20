package com.gfi.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.ProgramDistribution;

@Repository
public interface ProgramDistributionRepository extends JpaRepository<ProgramDistribution, Long>, JpaSpecificationExecutor<ProgramDistribution> {
    List<ProgramDistribution> findBySchoolYearIdAndUnitIdAndClassroomIdAndSubjectIdAndDeletedFlagOrderByOrderNumberAscIdAsc(
            Long schoolYearId,
            Long unitId,
            Long classroomId,
            Long subjectId,
            Integer deletedFlag);
}
