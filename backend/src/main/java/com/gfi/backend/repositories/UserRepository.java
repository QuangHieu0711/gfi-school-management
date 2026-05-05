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
    
    // ✅ NEW: Find user by staff ID (one-to-one, unique relationship)
    Optional<User> findByStaffId(Long staffId);
    boolean existsByStaffId(Long staffId);
    
    /**
     * Kiểm tra username có trùng không, loại trừ một user cụ thể theo ID.
     * Dùng cho validate khi update để tránh báo false duplicate.
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.id <> :excludeId AND u.deletedFlag = 0")
    boolean existsByUsernameAndIdNot(@Param("username") String username, @Param("excludeId") Long excludeId);
    
    /**
     * Kiểm tra email có trùng không (không loại trừ user nào).
     * Dùng cho validate khi create user.
     * NOTE: Email now comes from Staff, but kept here for backward compatibility during migration
     */
    @Query("SELECT COUNT(u) > 0 FROM User u LEFT JOIN u.staff s WHERE s.email = :email AND u.deletedFlag = 0")
    boolean existsByEmailAndDeletedFlagEquals(@Param("email") String email, @Param("flag") Integer deletedFlag);
    
    /**
     * Kiểm tra email có trùng không, loại trừ một user cụ thể theo ID.
     * Dùng cho validate khi update user.
     * NOTE: Email now comes from Staff relationship
     */
    @Query("SELECT COUNT(u) > 0 FROM User u LEFT JOIN u.staff s WHERE s.email = :email AND u.id <> :excludeId AND u.deletedFlag = 0")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("excludeId") Long excludeId);
    
    /**
     * Kiểm tra số điện thoại có trùng không (không loại trừ user nào).
     * Dùng cho validate khi create user.
     * NOTE: Phone now comes from Staff, but kept here for backward compatibility during migration
     */
    @Query("SELECT COUNT(u) > 0 FROM User u LEFT JOIN u.staff s WHERE s.phone = :phone AND u.deletedFlag = 0")
    boolean existsByPhoneAndDeletedFlagEquals(@Param("phone") String phone, @Param("flag") Integer deletedFlag);
    
    /**
     * Kiểm tra số điện thoại có trùng không, loại trừ một user cụ thể theo ID.
     * Dùng cho validate khi update user.
     * NOTE: Phone now comes from Staff relationship
     */
    @Query("SELECT COUNT(u) > 0 FROM User u LEFT JOIN u.staff s WHERE s.phone = :phone AND u.id <> :excludeId AND u.deletedFlag = 0")
    boolean existsByPhoneAndIdNot(@Param("phone") String phone, @Param("excludeId") Long excludeId);
    
    /**
     * Find users by staff unit IDs (for data scope filtering)
     * ✅ UPDATED: Uses Staff relationship instead of direct unit
     */
    @Query("SELECT u FROM User u LEFT JOIN u.staff s WHERE s.unit.id IN :unitIds AND u.deletedFlag = 0")
    List<User> findByUnitIdIn(@Param("unitIds") List<Long> unitIds);
    
    /**
     * Find users by staff unit IDs with pagination (for data scope filtering)
     * ✅ UPDATED: Uses Staff relationship instead of direct unit
     */
    @Query("SELECT u FROM User u LEFT JOIN u.staff s WHERE s.unit.id IN :unitIds AND u.deletedFlag = 0")
    Page<User> findByUnitIdIn(@Param("unitIds") List<Long> unitIds, Pageable pageable);
    
    /**
     * Find single user by ID and allowed unit IDs (for data scope enforcement)
     * ✅ UPDATED: Uses Staff relationship instead of direct unit
     */
    @Query("SELECT u FROM User u LEFT JOIN u.staff s WHERE u.id = :id AND s.unit.id IN :unitIds AND u.deletedFlag = 0")
    Optional<User> findByIdAndUnitIdIn(@Param("id") Long id, @Param("unitIds") List<Long> unitIds);
    
    /**
     * Find user by username with role, staff, and unit eagerly fetched.
     * Auth flow reads profile fields via user.staff and user.staff.unit, so all
     * of them must be initialized before leaving repository layer.
     */
    @Query("""
            SELECT u
            FROM User u
            LEFT JOIN FETCH u.role
            LEFT JOIN FETCH u.staff s
            LEFT JOIN FETCH s.unit
            WHERE u.username = :username AND u.deletedFlag = 0
            """)
    Optional<User> findByUsernameWithStaffAndRole(@Param("username") String username);
    
    /**
     * Legacy: Find user by username with role eagerly fetched
     * Kept for backward compatibility - new code should use findByUsernameWithStaffAndRole()
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.username = :username AND u.deletedFlag = 0")
    Optional<User> findByUsernameWithRole(@Param("username") String username);

    // ── Dashboard queries ────────────────────────────────────────────────────

    /** Đếm user theo staff unit IDs */
    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.staff s WHERE s.unit.id IN :unitIds AND u.deletedFlag = 0")
    long countByStaffUnitIdIn(@Param("unitIds") List<Long> unitIds);
}
