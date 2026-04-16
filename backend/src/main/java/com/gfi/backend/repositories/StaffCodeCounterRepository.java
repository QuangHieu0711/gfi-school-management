package com.gfi.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.StaffCodeCounter;

import jakarta.persistence.LockModeType;

@Repository
public interface StaffCodeCounterRepository extends JpaRepository<StaffCodeCounter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM StaffCodeCounter c WHERE c.unit.id = :unitId AND c.year = :year")
    Optional<StaffCodeCounter> findByUnitIdAndYearForUpdate(
            @Param("unitId") Long unitId,
            @Param("year") Integer year);

    @Query("SELECT c FROM StaffCodeCounter c WHERE c.unit.id = :unitId AND c.year = :year")
    Optional<StaffCodeCounter> findByUnitIdAndYear(
            @Param("unitId") Long unitId,
            @Param("year") Integer year);
}
