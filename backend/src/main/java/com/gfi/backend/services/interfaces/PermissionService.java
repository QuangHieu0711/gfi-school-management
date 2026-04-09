package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.permission.PermissionCreateRequest;
import com.gfi.backend.models.dtos.permission.PermissionFilterDto;
import com.gfi.backend.models.dtos.permission.PermissionItemDto;
import com.gfi.backend.models.dtos.permission.PermissionUpdateRequest;

public interface PermissionService {
    PageResponseDto<PermissionItemDto, PermissionFilterDto> search(PageRequestDto<PermissionFilterDto> request);
    PermissionItemDto getById(Long id);
    PermissionItemDto create(PermissionCreateRequest request);
    PermissionItemDto update(Long id, PermissionUpdateRequest request);
    void delete(Long id);
}
