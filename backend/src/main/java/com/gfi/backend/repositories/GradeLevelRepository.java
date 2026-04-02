package com.gfi.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.GradeLevel;

@Repository
public interface GradeLevelRepository extends JpaRepository<GradeLevel, Long>, JpaSpecificationExecutor<GradeLevel> {
    Optional<GradeLevel> findByCode(String code);
    Optional<GradeLevel> findByName(String name);
    Optional<GradeLevel> findByGradeNumber(Integer gradeNumber);
}
