package com.gfi.backend.repositories;

import com.gfi.backend.models.entities.StaffAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffAddressRepository extends JpaRepository<StaffAddress, Long> {
    List<StaffAddress> findByStaffId(Long staffId);
    Optional<StaffAddress> findByStaffIdAndAddressType(Long staffId, String addressType);
}
