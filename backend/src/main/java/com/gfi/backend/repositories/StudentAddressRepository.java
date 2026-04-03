package com.gfi.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.StudentAddress;

@Repository
public interface StudentAddressRepository extends JpaRepository<StudentAddress, Long> {
    List<StudentAddress> findByStudentIdOrderByIdAsc(Long studentId);
    void deleteByStudentId(Long studentId);
}
