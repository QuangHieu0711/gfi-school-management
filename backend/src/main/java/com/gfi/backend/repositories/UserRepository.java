package com.gfi.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    long countByRoleId(Long roleId);
    long countByUnitId(Long unitId);
    
    /**
     * Kiểm tra username có trùng không, loại trừ một user cụ thể theo ID.
     * Dùng cho validate khi update để tránh báo false duplicate.
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.id <> :excludeId AND u.deletedFlag = 0")
    boolean existsByUsernameAndIdNot(@Param("username") String username, @Param("excludeId") Long excludeId);
    
    /**
     * Kiểm tra email có trùng không (không loại trừ user nào).
     * Dùng cho validate khi create user.
     */
    boolean existsByEmailAndDeletedFlagEquals(String email, Integer deletedFlag);
    
    /**
     * Kiểm tra email có trùng không, loại trừ một user cụ thể theo ID.
     * Dùng cho validate khi update user.
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.id <> :excludeId AND u.deletedFlag = 0")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("excludeId") Long excludeId);
    
    /**
     * Kiểm tra số điện thoại có trùng không (không loại trừ user nào).
     * Dùng cho validate khi create user.
     */
    boolean existsByPhoneAndDeletedFlagEquals(String phone, Integer deletedFlag);
    
    /**
     * Kiểm tra số điện thoại có trùng không, loại trừ một user cụ thể theo ID.
     * Dùng cho validate khi update user.
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.phone = :phone AND u.id <> :excludeId AND u.deletedFlag = 0")
    boolean existsByPhoneAndIdNot(@Param("phone") String phone, @Param("excludeId") Long excludeId);
    
    /**
     * Find users by unit IDs (for data scope filtering)
     */
    List<User> findByUnitIdIn(List<Long> unitIds);
    
    /**
     * Find users by unit IDs with pagination (for data scope filtering)
     */
    Page<User> findByUnitIdIn(List<Long> unitIds, Pageable pageable);
    
    /**
     * Find single user by ID and allowed unit IDs (for data scope enforcement)
     */
    Optional<User> findByIdAndUnitIdIn(Long id, List<Long> unitIds);
    
    /**
     * Find user by username with role eagerly fetched
     * Used for scope loading in filter - must get role without lazy loading
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.username = :username")
    Optional<User> findByUsernameWithRole(@Param("username") String username);
}
