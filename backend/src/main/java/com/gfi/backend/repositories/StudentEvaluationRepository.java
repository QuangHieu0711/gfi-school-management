package com.gfi.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.StudentEvaluation;

@Repository
public interface StudentEvaluationRepository extends JpaRepository<StudentEvaluation, Long> {
    List<StudentEvaluation> findByClassroomIdAndSubjectIdAndSemesterIdAndDeletedFlagOrderByStudentIdAsc(
            Long classroomId, Long subjectId, Long semesterId, Integer deletedFlag);

    Optional<StudentEvaluation> findByClassroomIdAndSubjectIdAndSemesterIdAndStudentId(
            Long classroomId, Long subjectId, Long semesterId, Long studentId);
}
