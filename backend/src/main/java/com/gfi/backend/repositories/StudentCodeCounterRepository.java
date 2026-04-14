package com.gfi.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.StudentCodeCounter;

import jakarta.persistence.LockModeType;

/**
 * Repository để quản lý student code counters.
 * Sử dụng PESSIMISTIC_WRITE lock để tránh race condition khi sinh mã đồng thời.
 */
@Repository
public interface StudentCodeCounterRepository extends JpaRepository<StudentCodeCounter, Long> {
    
    /**
     * Tìm counter với lock pessimistic để đảm bảo không bị race condition.
     * Dùng cho việc tăng last_number.
     * 
     * @param unitId ID của đơn vị
     * @param year Năm sinh mã
     * @return Optional chứa counter
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM StudentCodeCounter c WHERE c.unit.id = :unitId AND c.year = :year")
    Optional<StudentCodeCounter> findByUnitIdAndYearForUpdate(
            @Param("unitId") Long unitId,
            @Param("year") Integer year);

    /**
     * Tìm counter mà không lock (dùng cho các truy vấn chỉ đọc).
     * 
     * @param unitId ID của đơn vị
     * @param year Năm sinh mã
     * @return Optional chứa counter
     */
    @Query("SELECT c FROM StudentCodeCounter c WHERE c.unit.id = :unitId AND c.year = :year")
    Optional<StudentCodeCounter> findByUnitIdAndYear(
            @Param("unitId") Long unitId,
            @Param("year") Integer year);
}
