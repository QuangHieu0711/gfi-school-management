package com.gfi.backend.repositories;

import com.gfi.backend.models.entities.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long>, JpaSpecificationExecutor<Staff> {
    Optional<Staff> findByStaffCode(String staffCode);
    Optional<Staff> findByIdentityCode(String identityCode);
    
    // ✅ REMOVED: findByUserId() - relationship now owned by User via staff_id FK
    // User.staff_id is the owning column (primary key in the relationship)
    
    /**
     * ✅ NEW: Find staffs that don't have a user account yet
     * Used for phase 1B to identify staffs needing user creation
     */
    @Query("SELECT s FROM Staff s WHERE s.user IS NULL AND s.deletedFlag = 0")
    Page<Staff> findStaffsWithoutUserAccount(Pageable pageable);
    
    /**
     * ✅ NEW: Find staffs that already have a user account
     * Used for data verification and auditing
     */
    @Query("SELECT s FROM Staff s WHERE s.user IS NOT NULL AND s.deletedFlag = 0")
    Page<Staff> findStaffsWithUserAccount(Pageable pageable);
}
