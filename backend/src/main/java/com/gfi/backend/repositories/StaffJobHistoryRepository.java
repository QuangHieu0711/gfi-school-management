package com.gfi.backend.repositories;

import com.gfi.backend.models.entities.StaffJobHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffJobHistoryRepository extends JpaRepository<StaffJobHistory, Long> {
    List<StaffJobHistory> findByStaffId(Long staffId);
}
