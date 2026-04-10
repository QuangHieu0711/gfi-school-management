package com.gfi.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.Unit;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long>, JpaSpecificationExecutor<Unit> {
    /**
     * Kiểm tra code unit có tồn tại không.
     */
    boolean existsByCode(String code);
    
    /**
     * Tìm unit theo code.
     */
    Optional<Unit> findByCode(String code);
    
    /**
     * Kiểm tra code có trùng không, loại trừ một unit cụ thể theo ID.
     * Dùng cho validate khi update để tránh báo false duplicate.
     */
    @Query("SELECT COUNT(u) > 0 FROM Unit u WHERE u.code = :code AND u.id <> :excludeId")
    boolean existsByCodeAndIdNot(@Param("code") String code, @Param("excludeId") Long excludeId);
}

