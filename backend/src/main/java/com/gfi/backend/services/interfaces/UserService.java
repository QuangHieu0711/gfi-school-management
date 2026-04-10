package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.user.UserCreateRequest;
import com.gfi.backend.models.dtos.user.UserDetailDto;
import com.gfi.backend.models.dtos.user.UserFilterDto;
import com.gfi.backend.models.dtos.user.UserListItemDto;
import com.gfi.backend.models.dtos.user.UserUpdateRequest;

/**
 * Interface service quản lý người dùng.
 * Định nghĩa contract cho các phép CRUD và business logic.
 */
public interface UserService {
    /**
     * Tìm kiếm và phân trang users với filter.
     * Trả về DTO list view chứa thông tin tối thiểu.
     */
    PageResponseDto<UserListItemDto, UserFilterDto> search(PageRequestDto<UserFilterDto> request);

    /**
     * Lấy chi tiết user theo ID.
     * Trả về DTO detail view chứa đầy đủ thông tin.
     */
    UserDetailDto getById(Long id);

    /**
     * Tạo user mới.
     * Trả về DTO detail view của user vừa tạo.
     */
    UserDetailDto create(UserCreateRequest request);

    /**
     * Cập nhật user hiện có.
     * Trả về DTO detail view của user vừa update.
     */
    UserDetailDto update(Long id, UserUpdateRequest request);

    /**
     * Xóa (xóa mềm) user theo ID.
     */
    void delete(Long id);
}
