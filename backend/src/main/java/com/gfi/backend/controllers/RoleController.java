package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.gfi.backend.models.dtos.role.RoleCreateRequest;
import com.gfi.backend.models.dtos.role.RoleFilterDto;
import com.gfi.backend.models.dtos.role.RoleItemDto;
import com.gfi.backend.models.dtos.role.RoleUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.RoleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Quản lý vai trò - Role")
public class RoleController extends ApiBaseController {

    private final RoleService roleService;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách vai trò", description = "Lấy danh sach vai trò có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<RoleItemDto, RoleFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<RoleFilterDto> request) {
        PageRequestDto<RoleFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(roleService.search(safeRequest), "Hiển thị danh sách vai trò thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chi tiết vai trò", description = "Lấy thông tin vai trò theo id.")
    public ResponseEntity<ApiResult<RoleItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(roleService.getById(id), "Hiển thị chi tiết vai trò thành công"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm vai trò", description = "Tạo mới vai trò.")
    public ResponseEntity<ApiResult<RoleItemDto>> create(@Valid @RequestBody RoleCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(roleService.create(request), "Thêm vai trò thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa vai trò", description = "Cập nhật vai trò theo id.")
    public ResponseEntity<ApiResult<RoleItemDto>> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(roleService.update(id, request), "Cập nhật vai trò thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa vai trò", description = "Xóa vai trò theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            roleService.delete(id);
            return ApiResult.success(null, "Xóa role thành công");
        });
    }
}
