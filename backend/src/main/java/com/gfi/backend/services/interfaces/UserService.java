package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.user.UserCreateRequest;
import com.gfi.backend.models.dtos.user.UserFilterDto;
import com.gfi.backend.models.dtos.user.UserItemDto;
import com.gfi.backend.models.dtos.user.UserUpdateRequest;

public interface UserService {
    PageResponseDto<UserItemDto, UserFilterDto> search(PageRequestDto<UserFilterDto> request);
    UserItemDto getById(Long id);
    UserItemDto create(UserCreateRequest request);
    UserItemDto update(Long id, UserUpdateRequest request);
    void delete(Long id);
}
