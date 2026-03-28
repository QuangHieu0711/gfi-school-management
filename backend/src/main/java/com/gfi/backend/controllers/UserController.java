package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.user.UserCreateRequest;
import com.gfi.backend.models.dtos.user.UserFilterDto;
import com.gfi.backend.models.dtos.user.UserItemDto;
import com.gfi.backend.models.dtos.user.UserUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Quản lý người dùng - User")
public class UserController extends ApiBaseController {

    private final UserService userService;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách user", description = "Lấy danh sách user có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<UserItemDto, UserFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<UserFilterDto> request) {
        PageRequestDto<UserFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(userService.search(safeRequest), "Hiển thị danh sách người dùng thành công"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm user", description = "Tạo mới user.")
    public ResponseEntity<ApiResult<UserItemDto>> create(@Valid @RequestBody UserCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(userService.create(request), "Thêm người dùng thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa user", description = "Cập nhật user theo id.")
    public ResponseEntity<ApiResult<UserItemDto>> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(userService.update(id, request), "Cập nhật người dùng thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa user", description = "Xóa user theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            userService.delete(id);
            return ApiResult.success(null, "Xóa người dùng thành công");
        });
    }
}
