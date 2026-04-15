package com.gfi.backend.repositories;

import com.gfi.backend.models.entities.StaffEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffEducationRepository extends JpaRepository<StaffEducation, Long> {
    List<StaffEducation> findByStaffId(Long staffId);
}
