package com.gfi.backend.repositories;

import com.gfi.backend.models.entities.StaffSalaryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffSalaryHistoryRepository extends JpaRepository<StaffSalaryHistory, Long> {
    List<StaffSalaryHistory> findByStaffId(Long staffId);
}
