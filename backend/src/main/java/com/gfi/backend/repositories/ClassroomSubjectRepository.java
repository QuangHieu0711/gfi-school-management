package com.gfi.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.ClassroomSubject;

@Repository
public interface ClassroomSubjectRepository extends JpaRepository<ClassroomSubject, Long> {
    List<ClassroomSubject> findByClassroomId(Long classroomId);
    long countBySubjectId(Long subjectId);
    boolean existsByClassroomIdAndSubjectId(Long classroomId, Long subjectId);
    void deleteByClassroomId(Long classroomId);
}
