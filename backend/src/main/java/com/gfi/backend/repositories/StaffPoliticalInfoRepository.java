package com.gfi.backend.repositories;

import com.gfi.backend.models.entities.StaffPoliticalInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffPoliticalInfoRepository extends JpaRepository<StaffPoliticalInfo, Long> {
    Optional<StaffPoliticalInfo> findByStaffId(Long staffId);
}
