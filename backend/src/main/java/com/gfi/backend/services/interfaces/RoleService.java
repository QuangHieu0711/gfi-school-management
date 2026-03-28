package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.role.RoleCreateRequest;
import com.gfi.backend.models.dtos.role.RoleFilterDto;
import com.gfi.backend.models.dtos.role.RoleItemDto;
import com.gfi.backend.models.dtos.role.RoleUpdateRequest;

public interface RoleService {
    PageResponseDto<RoleItemDto, RoleFilterDto> search(PageRequestDto<RoleFilterDto> request);
    RoleItemDto create(RoleCreateRequest request);
    RoleItemDto update(Long id, RoleUpdateRequest request);
    void delete(Long id);
}
