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
    @Operation(summary = "Danh sách role", description = "Lấy danh sách role có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<RoleItemDto, RoleFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<RoleFilterDto> request) {
        PageRequestDto<RoleFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(roleService.search(safeRequest), "Hiển thị danh sách role thành công"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm role", description = "Tạo mới role.")
    public ResponseEntity<ApiResult<RoleItemDto>> create(@Valid @RequestBody RoleCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(roleService.create(request), "Thêm role thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa role", description = "Cập nhật role theo id.")
    public ResponseEntity<ApiResult<RoleItemDto>> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(roleService.update(id, request), "Cập nhật role thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa role", description = "Xóa role theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            roleService.delete(id);
            return ApiResult.success(null, "Xóa role thành công");
        });
    }
}
