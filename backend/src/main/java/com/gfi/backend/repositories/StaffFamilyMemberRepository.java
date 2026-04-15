package com.gfi.backend.repositories;

import com.gfi.backend.models.entities.StaffFamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffFamilyMemberRepository extends JpaRepository<StaffFamilyMember, Long> {
    List<StaffFamilyMember> findByStaffId(Long staffId);
}
