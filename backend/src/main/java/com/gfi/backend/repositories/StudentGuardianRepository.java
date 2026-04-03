package com.gfi.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.StudentGuardian;

@Repository
public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, Long> {
    List<StudentGuardian> findByStudentIdOrderByIdAsc(Long studentId);
    void deleteByStudentId(Long studentId);
}
