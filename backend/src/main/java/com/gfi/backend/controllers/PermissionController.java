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
import com.gfi.backend.models.dtos.permission.PermissionCreateRequest;
import com.gfi.backend.models.dtos.permission.PermissionFilterDto;
import com.gfi.backend.models.dtos.permission.PermissionItemDto;
import com.gfi.backend.models.dtos.permission.PermissionUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Tag(name = "Quản lý quyền - Permission")
public class PermissionController extends ApiBaseController {

    private final PermissionService permissionService;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách quyền", description = "Lấy danh sách quyền có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<PermissionItemDto, PermissionFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<PermissionFilterDto> request) {
        PageRequestDto<PermissionFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(permissionService.search(safeRequest), "Lấy danh sách quyền thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chi tiet permission", description = "Lấy chi tiết quyền theo ID.")
    public ResponseEntity<ApiResult<PermissionItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(permissionService.getById(id), "Lấy chi tiết quyền thành công"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm quyền", description = "Thêm quyền mới cho role và menu.")
    public ResponseEntity<ApiResult<PermissionItemDto>> create(@Valid @RequestBody PermissionCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(permissionService.create(request), "Thêm quyền thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật quyền", description = "Cập nhật quyền cho role và menu.")
    public ResponseEntity<ApiResult<PermissionItemDto>> update(@PathVariable Long id, @Valid @RequestBody PermissionUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(permissionService.update(id, request), "Cập nhật quyền thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa quyền", description = "Xóa quyền khỏi role và menu.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            permissionService.delete(id);
            return ApiResult.success(null, "Xóa quyền thành công");
        });
    }
}
