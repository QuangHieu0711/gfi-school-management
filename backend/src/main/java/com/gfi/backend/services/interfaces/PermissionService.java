package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.permission.PermissionItemDto;
import com.gfi.backend.models.dtos.permission.PermissionSaveRequest;

public interface PermissionService {
    List<PermissionItemDto> getByRoleId(Long roleId);
    List<PermissionItemDto> savePermissions(List<PermissionSaveRequest> requests);
}
