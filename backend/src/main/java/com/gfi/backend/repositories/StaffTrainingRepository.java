package com.gfi.backend.repositories;

import com.gfi.backend.models.entities.StaffTraining;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffTrainingRepository extends JpaRepository<StaffTraining, Long> {
    List<StaffTraining> findByStaffId(Long staffId);
}
