package com.gfi.backend.repositories;

import com.gfi.backend.models.entities.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long>, JpaSpecificationExecutor<Staff> {
    Optional<Staff> findByStaffCode(String staffCode);
    Optional<Staff> findByIdentityCode(String identityCode);
    Optional<Staff> findByUserId(Long userId);
}
