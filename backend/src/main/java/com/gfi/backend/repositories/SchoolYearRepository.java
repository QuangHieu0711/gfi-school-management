package com.gfi.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.SchoolYear;

@Repository
public interface SchoolYearRepository extends JpaRepository<SchoolYear, Long>, JpaSpecificationExecutor<SchoolYear> {
    Optional<SchoolYear> findByCode(String code);
    Optional<SchoolYear> findByName(String name);
    Optional<SchoolYear> findByIsCurrentTrueAndDeletedFlagEquals(Integer deletedFlag);

    /** Dashboard convenience: lấy năm học hiện tại (deletedFlag = 0) */
    default Optional<SchoolYear> findCurrentSchoolYear() {
        return findByIsCurrentTrueAndDeletedFlagEquals(0);
    }

    @Modifying
    @Query("update SchoolYear sy set sy.isCurrent = false where sy.isCurrent = true and sy.id <> :id")
    void clearCurrentExcept(Long id);
}
