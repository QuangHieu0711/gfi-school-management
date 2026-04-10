package com.gfi.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.DataPermission;

@Repository
public interface DataPermissionRepository extends JpaRepository<DataPermission, Long>, JpaSpecificationExecutor<DataPermission> {
    List<DataPermission> findAllByRoleIdOrderByIdAsc(Long roleId);
    Optional<DataPermission> findByRoleIdAndMenuId(Long roleId, Long menuId);
    long countByRoleId(Long roleId);
    long countByMenuId(Long menuId);
}
