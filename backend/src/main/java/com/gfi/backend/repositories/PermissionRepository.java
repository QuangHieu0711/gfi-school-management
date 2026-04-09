package com.gfi.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {
    List<Permission> findAllByRoleIdOrderByIdAsc(Long roleId);
    Optional<Permission> findByRoleIdAndMenuId(Long roleId, Long menuId);
    boolean existsByRoleIdAndMenuId(Long roleId, Long menuId);
    long countByRoleId(Long roleId);
    long countByMenuId(Long menuId);
}
