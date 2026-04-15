package com.gfi.backend.repositories;

import com.gfi.backend.models.entities.StaffJobInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffJobInfoRepository extends JpaRepository<StaffJobInfo, Long> {
    Optional<StaffJobInfo> findByStaffId(Long staffId);
}
