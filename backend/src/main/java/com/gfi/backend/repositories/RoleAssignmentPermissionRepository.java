package com.gfi.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gfi.backend.models.entities.Role;
import com.gfi.backend.models.entities.RoleAssignmentPermission;

public interface RoleAssignmentPermissionRepository extends JpaRepository<RoleAssignmentPermission, Long> {

    @Query("""
        select rap.targetRole
        from RoleAssignmentPermission rap
        where rap.creatorRole.id = :creatorRoleId
          and rap.canCreate = 1
          and rap.status = 1
          and rap.deletedFlag = 0
          and rap.targetRole.status = 1
        order by rap.targetRole.roleName asc
    """)
    List<Role> findAssignableRolesForCreate(@Param("creatorRoleId") Long creatorRoleId);

    @Query("""
        select rap
        from RoleAssignmentPermission rap
        join fetch rap.targetRole tr
        where rap.creatorRole.id = :creatorRoleId
          and rap.deletedFlag = 0
        order by tr.roleName asc
    """)
    List<RoleAssignmentPermission> findAllByCreatorRoleId(@Param("creatorRoleId") Long creatorRoleId);

    Optional<RoleAssignmentPermission> findByCreatorRoleIdAndTargetRoleIdAndDeletedFlag(
            Long creatorRoleId,
            Long targetRoleId,
            Integer deletedFlag
    );

    boolean existsByCreatorRoleIdAndTargetRoleIdAndCanCreateAndStatusAndDeletedFlag(
            Long creatorRoleId,
            Long targetRoleId,
            Integer canCreate,
            Integer status,
            Integer deletedFlag
    );

    boolean existsByCreatorRoleIdAndTargetRoleIdAndCanUpdateAndStatusAndDeletedFlag(
            Long creatorRoleId,
            Long targetRoleId,
            Integer canUpdate,
            Integer status,
            Integer deletedFlag
    );
}
