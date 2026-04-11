package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.user.UserCreateRequest;
import com.gfi.backend.models.dtos.user.UserDetailDto;
import com.gfi.backend.models.dtos.user.UserFilterDto;
import com.gfi.backend.models.dtos.user.UserListItemDto;
import com.gfi.backend.models.dtos.user.UserUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller quản lý người dùng.
 * Xử lý HTTP requests cho các phép CRUD user.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Quản lý người dùng - User")
public class UserController extends ApiBaseController {

    private final UserService userService;

    /**
     * Tìm kiếm và phân trang users với filter.
     * Trả về list view chứa thông tin tối thiểu.
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResult<PageResponseDto<UserListItemDto, UserFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<UserFilterDto> request) {
        PageRequestDto<UserFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(
                () -> ApiResult.success(userService.search(safeRequest), "Hiển thị danh sách người dùng thành công"));
    }

    /**
     * Lấy chi tiết user theo ID.
     * Trả về đầy đủ thông tin user bao gồm quan hệ.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết người dùng", description = "Lấy thông tin người dùng theo id.")
    public ResponseEntity<ApiResult<UserDetailDto>> getById(@PathVariable Long id) {
        return executeApiResult(
                () -> ApiResult.success(userService.getById(id), "Hiển thị chi tiết người dùng thành công"));
    }

    /**
     * Tạo user mới.
     */
    @PostMapping
    @Operation(summary = "Thêm người dùng", description = "Tạo mới người dùng.")
    public ResponseEntity<ApiResult<UserDetailDto>> create(@Valid @RequestBody UserCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(userService.create(request), "Thêm người dùng thành công"));
    }

    /**
     * Cập nhật user hiện có.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Sửa người dùng", description = "Cập nhật người dùng theo id.")
    public ResponseEntity<ApiResult<UserDetailDto>> update(@PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(userService.update(id, request), "Cập nhật người dùng thành công"));
    }

    /**
     * Xóa (xóa mềm) user theo ID.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa người dùng", description = "Xóa người dùng theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            userService.delete(id);
            return ApiResult.success(null, "Xóa người dùng thành công");
        });
    }
}
