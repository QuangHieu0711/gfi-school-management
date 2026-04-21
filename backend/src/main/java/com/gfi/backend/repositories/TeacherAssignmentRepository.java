package com.gfi.backend.repositories;

import com.gfi.backend.models.entities.TeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long>, JpaSpecificationExecutor<TeacherAssignment> {
    List<TeacherAssignment> findByStaffId(Long staffId);
    List<TeacherAssignment> findBySchoolYearId(Long schoolYearId);
    List<TeacherAssignment> findByStaffIdAndSchoolYearId(Long staffId, Long schoolYearId);
    List<TeacherAssignment> findByStaffIdAndSchoolYearIdAndClassroomIdIn(Long staffId, Long schoolYearId, List<Long> classroomIds);
}
