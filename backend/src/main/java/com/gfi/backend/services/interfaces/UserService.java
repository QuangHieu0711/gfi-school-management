package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.user.UserCreateRequest;
import com.gfi.backend.models.dtos.user.UserDetailDto;
import com.gfi.backend.models.dtos.user.UserFilterDto;
import com.gfi.backend.models.dtos.user.UserListItemDto;
import com.gfi.backend.models.dtos.user.UserUpdateRequest;
import com.gfi.backend.models.enums.ExportType;
import java.util.List;

/**
 * Interface service quản lý người dùng.
 * Định nghĩa contract cho các phép CRUD và business logic.
 */
public interface UserService {
    /**
     * Lấy danh sách unit options cho form tạo người dùng.
     * Chỉ trả về unit mà user hiện tại có quyền tạo.
     */
    List<LookupItemDto> getUnitOptionsForCreateUser();

    /**
     * Lấy danh sách vai trò mà user hiện tại được phép gán khi tạo user.
     */
    java.util.List<LookupItemDto> getRoleOptionsForCreateUser();

    /**
     * Tìm kiếm và phân trang users với filter.
     * Trả về DTO list view chứa thông tin tối thiểu.
     */
    PageResponseDto<UserListItemDto, UserFilterDto> search(PageRequestDto<UserFilterDto> request);

    /**
     * Export toàn bộ dữ liệu người dùng theo điều kiện filter.
     */
    byte[] export(PageRequestDto<UserFilterDto> request, ExportType exportType);

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
