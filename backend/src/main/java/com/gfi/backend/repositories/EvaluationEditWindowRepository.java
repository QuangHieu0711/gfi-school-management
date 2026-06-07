package com.gfi.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.EvaluationEditWindow;

@Repository
public interface EvaluationEditWindowRepository extends JpaRepository<EvaluationEditWindow, Long> {
    Optional<EvaluationEditWindow> findBySemesterIdAndDeletedFlag(Long semesterId, Integer deletedFlag);
}
