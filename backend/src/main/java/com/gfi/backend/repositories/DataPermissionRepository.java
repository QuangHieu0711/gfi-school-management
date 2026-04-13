package com.gfi.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.DataPermission;

@Repository
public interface DataPermissionRepository extends JpaRepository<DataPermission, Long>, JpaSpecificationExecutor<DataPermission> {
    List<DataPermission> findAllByRoleIdOrderByIdAsc(Long roleId);
    Optional<DataPermission> findByRoleIdAndMenuId(Long roleId, Long menuId);
    long countByRoleId(Long roleId);
    long countByMenuId(Long menuId);
    
    /**
     * Find all data permissions by roleId with scopes eagerly fetched
     * Avoids LazyInitializationException when accessing scopes in filter/security context
     */
    @Query("""
        select distinct dp
        from DataPermission dp
        left join fetch dp.scopes
        where dp.role.id = :roleId
        order by dp.id asc
    """)
    List<DataPermission> findAllByRoleIdWithScopesOrderByIdAsc(@Param("roleId") Long roleId);

    /**
     * Kiểm tra xem data permission đã tồn tại cho role và menu hay chưa.
     */
    boolean existsByRoleIdAndMenuId(Long roleId, Long menuId);
}
