package com.gfi.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.GradeLevelSubject;

@Repository
public interface GradeLevelSubjectRepository extends JpaRepository<GradeLevelSubject, Long> {
    List<GradeLevelSubject> findByGradeLevelId(Long gradeLevelId);
    long countByGradeLevelId(Long gradeLevelId);
    long countBySubjectId(Long subjectId);
    void deleteByGradeLevelId(Long gradeLevelId);
}
