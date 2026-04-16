package com.gfi.backend.repositories;

import com.gfi.backend.models.entities.StaffJobHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffJobHistoryRepository extends JpaRepository<StaffJobHistory, Long>, JpaSpecificationExecutor<StaffJobHistory> {
    List<StaffJobHistory> findByStaffIdOrderByFromDateAscIdAsc(Long staffId);
    Optional<StaffJobHistory> findByIdAndStaffId(Long id, Long staffId);
}
