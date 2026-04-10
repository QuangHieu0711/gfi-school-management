package com.gfi.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.Role;

/**
 * Repository quản lý các truy vấn Role entity.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {
    
    /**
     * Tìm Role theo code.
     */
    Optional<Role> findByCode(String code);
    
    /**
     * Kiểm tra code có tồn tại hay không.
     */
    boolean existsByCode(String code);
    
    /**
     * Kiểm tra code có tồn tại ngoại trừ một Role cụ thể (dùng khi update).
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Role r WHERE r.code = :code AND r.id != :excludeId")
    boolean existsByCodeAndIdNot(@Param("code") String code, @Param("excludeId") Long excludeId);
    
    /**
     * Tìm Role theo tên vai trò.
     */
    Optional<Role> findByRoleName(String roleName);
    
    /**
     * Kiểm tra tên vai trò có tồn tại hay không.
     */
    boolean existsByRoleName(String roleName);
    
    /**
     * Kiểm tra tên vai trò có tồn tại ngoại trừ một Role cụ thể (dùng khi update).
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Role r WHERE r.roleName = :roleName AND r.id != :excludeId")
    boolean existsByRoleNameAndIdNot(@Param("roleName") String roleName, @Param("excludeId") Long excludeId);
}
