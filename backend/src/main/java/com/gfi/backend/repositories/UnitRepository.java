package com.gfi.backend.repositories;

import java.util.List;
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
    
    /**
     * Find units by IDs ordered by name (for dropdown/combobox with scope filtering)
     */
    @Query("SELECT u FROM Unit u WHERE u.id IN :ids ORDER BY u.name ASC")
    List<Unit> findByIdInOrderByName(@Param("ids") List<Long> ids);
    
    /**
     * Find all active non-deleted units ordered by name (for unrestricted scope)
     */
    @Query("SELECT u FROM Unit u WHERE u.status = :status AND u.deletedFlag = :deletedFlag ORDER BY u.name ASC")
    List<Unit> findByStatusAndDeletedFlagOrderByName(@Param("status") int status, @Param("deletedFlag") int deletedFlag);
    
    /**
     * Find units by IDs and status filters, ordered by name (for restricted scope)
     */
    @Query("SELECT u FROM Unit u WHERE u.id IN :ids AND u.status = :status AND u.deletedFlag = :deletedFlag ORDER BY u.name ASC")
    List<Unit> findByIdInAndStatusAndDeletedFlagOrderByName(@Param("ids") List<Long> ids, @Param("status") int status, @Param("deletedFlag") int deletedFlag);
}

