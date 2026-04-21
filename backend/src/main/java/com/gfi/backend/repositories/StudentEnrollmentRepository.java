package com.gfi.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.StudentEnrollment;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {
    List<StudentEnrollment> findByStudentIdOrderBySchoolYearIdDescIdDesc(Long studentId);
    Optional<StudentEnrollment> findByStudentIdAndSchoolYearId(Long studentId, Long schoolYearId);
    List<StudentEnrollment> findByClassroomIdAndDeletedFlagOrderByStudentFullNameAsc(Long classroomId, Integer deletedFlag);
    void deleteByStudentId(Long studentId);
}
