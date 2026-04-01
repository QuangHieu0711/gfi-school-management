package com.gfi.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.Semester;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long>, JpaSpecificationExecutor<Semester> {
    long countBySchoolYearId(Long schoolYearId);
    Optional<Semester> findBySchoolYearIdAndCode(Long schoolYearId, String code);
    Optional<Semester> findBySchoolYearIdAndName(Long schoolYearId, String name);
    Optional<Semester> findBySchoolYearIdAndSemesterOrder(Long schoolYearId, Integer semesterOrder);

    @Modifying
    @Query("update Semester s set s.isCurrent = false where s.isCurrent = true and s.id <> :id")
    void clearCurrentExcept(Long id);
}
