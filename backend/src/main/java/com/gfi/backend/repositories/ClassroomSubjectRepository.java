package com.gfi.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.entities.ClassroomSubject;

@Repository
public interface ClassroomSubjectRepository extends JpaRepository<ClassroomSubject, Long> {
    List<ClassroomSubject> findByClassroomId(Long classroomId);
    List<ClassroomSubject> findByClassroomIdAndStatusAndDeletedFlagOrderBySubjectNameAsc(Long classroomId, Integer status,
            Integer deletedFlag);
    long countBySubjectId(Long subjectId);
    boolean existsByClassroomIdAndSubjectId(Long classroomId, Long subjectId);
    void deleteByClassroomId(Long classroomId);

    @Query("SELECT DISTINCT cs.classroom FROM ClassroomSubject cs " +
            "WHERE cs.subject.id = :subjectId " +
            "AND (:unitId IS NULL OR cs.classroom.unit.id = :unitId) " +
            "AND cs.status = 1 " +
            "AND cs.classroom.deletedFlag = 0 " +
            "ORDER BY cs.classroom.name ASC")
    List<Classroom> findActiveClassroomsBySubjectId(@Param("subjectId") Long subjectId,
            @Param("unitId") Long unitId);
}
