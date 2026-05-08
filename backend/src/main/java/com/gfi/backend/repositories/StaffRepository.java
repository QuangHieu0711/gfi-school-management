package com.gfi.backend.repositories;

import com.gfi.backend.models.entities.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long>, JpaSpecificationExecutor<Staff> {
    Optional<Staff> findByStaffCode(String staffCode);
    Optional<Staff> findByIdentityCode(String identityCode);

    // User.staff_id is the owning column (primary key in the relationship)

    /**
     * Used for phase 1B to identify staffs needing user creation
     */
    @Query("SELECT s FROM Staff s WHERE s.user IS NULL AND s.deletedFlag = 0")
    Page<Staff> findStaffsWithoutUserAccount(Pageable pageable);

    @Query("SELECT s FROM Staff s WHERE s.deletedFlag = 0 AND s.status = 'ACTIVE' " +
           "AND s.id NOT IN (SELECT u.staff.id FROM User u WHERE u.deletedFlag = 0 AND u.staff IS NOT NULL)")
    List<Staff> findActiveStaffsWithoutUser();

    /**
     * Used for data verification and auditing
     */
    @Query("SELECT s FROM Staff s WHERE s.user IS NOT NULL AND s.deletedFlag = 0")
    Page<Staff> findStaffsWithUserAccount(Pageable pageable);

    @Query("SELECT s FROM Staff s WHERE s.gradeLevel.id = ?1 AND s.deletedFlag = 0")
    List<Staff> findByGradeLevelId(Long gradeLevelId);

    @Query("SELECT s FROM Staff s WHERE s.unit.id = ?1 AND s.gradeLevel.id = ?2 AND s.deletedFlag = 0")
    List<Staff> findByUnitIdAndGradeLevelId(Long unitId, Long gradeLevelId);

    // ── Dashboard queries ────────────────────────────────────────────────────

    /** Đếm cán bộ theo unit IDs */
    @Query("SELECT COUNT(s) FROM Staff s WHERE s.unit.id IN :unitIds AND s.deletedFlag = 0")
    long countByUnitIdIn(@Param("unitIds") List<Long> unitIds);

    /** Phân bố cán bộ theo giới tính (unrestricted) */
    @Query("SELECT s.gender, COUNT(s) FROM Staff s WHERE s.deletedFlag = 0 GROUP BY s.gender")
    List<Object[]> countGroupByGender();

    /** Phân bố cán bộ theo giới tính (scoped) */
    @Query("SELECT s.gender, COUNT(s) FROM Staff s WHERE s.unit.id IN :unitIds AND s.deletedFlag = 0 GROUP BY s.gender")
    List<Object[]> countGroupByGenderAndUnitIdIn(@Param("unitIds") List<Long> unitIds);

    /** Cán bộ có tài khoản (unrestricted) */
    @Query("SELECT COUNT(s) FROM Staff s WHERE s.user IS NOT NULL AND s.deletedFlag = 0")
    long countByUserIsNotNull();

    /** Cán bộ chưa có tài khoản (unrestricted) */
    @Query("SELECT COUNT(s) FROM Staff s WHERE s.user IS NULL AND s.deletedFlag = 0")
    long countByUserIsNull();

    /** Cán bộ có tài khoản (scoped) */
    @Query("SELECT COUNT(s) FROM Staff s WHERE s.user IS NOT NULL AND s.unit.id IN :unitIds AND s.deletedFlag = 0")
    long countByUserIsNotNullAndUnitIdIn(@Param("unitIds") List<Long> unitIds);

    /** Cán bộ chưa có tài khoản (scoped) */
    @Query("SELECT COUNT(s) FROM Staff s WHERE s.user IS NULL AND s.unit.id IN :unitIds AND s.deletedFlag = 0")
    long countByUserIsNullAndUnitIdIn(@Param("unitIds") List<Long> unitIds);
}
