package com.gfi.backend.repositories;

import com.gfi.backend.models.entities.StaffEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffEducationRepository extends JpaRepository<StaffEducation, Long>, JpaSpecificationExecutor<StaffEducation> {
    List<StaffEducation> findByStaffId(Long staffId);
    Optional<StaffEducation> findByIdAndStaffId(Long id, Long staffId);
    Optional<StaffEducation> findByIdAndEducationType(Long id, String educationType);
}
