package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.datapermission.DataPermissionItemDto;
import com.gfi.backend.models.dtos.datapermission.DataPermissionSaveRequest;
import com.gfi.backend.models.dtos.datapermission.DataScopeContext;

public interface DataPermissionService {
    List<DataPermissionItemDto> getByRoleId(Long roleId);
    List<DataPermissionItemDto> savePermissions(List<DataPermissionSaveRequest> requests);
    DataScopeContext resolve(Long roleId, Long menuId, Long userId);
}
