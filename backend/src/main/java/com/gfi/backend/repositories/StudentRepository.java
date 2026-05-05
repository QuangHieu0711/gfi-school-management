package com.gfi.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    Optional<Student> findByStudentCode(String studentCode);

    // ── Dashboard queries ────────────────────────────────────────────────────

    /** Đếm học sinh theo unit IDs */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.unit.id IN :unitIds AND s.deletedFlag = 0")
    long countByUnitIdIn(@Param("unitIds") List<Long> unitIds);

    /** Phân bố học sinh theo trạng thái (unrestricted) */
    @Query("SELECT s.studentStatus, COUNT(s) FROM Student s WHERE s.deletedFlag = 0 GROUP BY s.studentStatus")
    List<Object[]> countGroupByStatus();

    /** Phân bố học sinh theo trạng thái (scoped by unit) */
    @Query("SELECT s.studentStatus, COUNT(s) FROM Student s WHERE s.unit.id IN :unitIds AND s.deletedFlag = 0 GROUP BY s.studentStatus")
    List<Object[]> countGroupByStatusAndUnitIdIn(@Param("unitIds") List<Long> unitIds);

    /** Top đơn vị theo số học sinh (unrestricted) */
    @Query("SELECT s.unit.name, COUNT(s) FROM Student s WHERE s.deletedFlag = 0 GROUP BY s.unit.name ORDER BY COUNT(s) DESC")
    List<Object[]> countGroupByUnit();

    /** Top đơn vị theo số học sinh (scoped by unit) */
    @Query("SELECT s.unit.name, COUNT(s) FROM Student s WHERE s.unit.id IN :unitIds AND s.deletedFlag = 0 GROUP BY s.unit.name ORDER BY COUNT(s) DESC")
    List<Object[]> countGroupByUnitAndUnitIdIn(@Param("unitIds") List<Long> unitIds);
}
