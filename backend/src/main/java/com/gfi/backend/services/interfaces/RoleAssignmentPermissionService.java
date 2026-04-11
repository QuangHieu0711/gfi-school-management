package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.roleassignment.RoleAssignmentPermissionResponse;
import com.gfi.backend.models.dtos.roleassignment.RoleAssignmentPermissionSaveRequest;

public interface RoleAssignmentPermissionService {
    RoleAssignmentPermissionResponse getByCreatorRoleId(Long creatorRoleId);
    RoleAssignmentPermissionResponse save(RoleAssignmentPermissionSaveRequest request);
}
