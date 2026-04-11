package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gfi.backend.models.dtos.roleassignment.RoleAssignmentPermissionResponse;
import com.gfi.backend.models.dtos.roleassignment.RoleAssignmentPermissionSaveRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.RoleAssignmentPermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/role-assignment-permissions")
@RequiredArgsConstructor
@Tag(name = "Phân quyền gán vai trò - Role Assignment Permission Management")
public class RoleAssignmentPermissionController extends ApiBaseController {

    private final RoleAssignmentPermissionService roleAssignmentPermissionService;

    @GetMapping("/{creatorRoleId}")
    @Operation(summary = "Lấy cấu hình gán vai trò theo role", description = "Lấy danh sách vai trò đích được phép tạo/cập nhật từ một role nguồn")
    public ResponseEntity<ApiResult<RoleAssignmentPermissionResponse>> getByCreatorRoleId(
            @PathVariable Long creatorRoleId) {
        return executeApiResult(() ->
                ApiResult.success(
                        roleAssignmentPermissionService.getByCreatorRoleId(creatorRoleId),
                        "Lấy cấu hình gán vai trò thành công"
                )
        );
    }

    @PostMapping("/save")
    @Operation(summary = "Lưu cấu hình gán vai trò", description = "Lưu cấu hình vai trò được phép tạo/cập nhật cho một role nguồn")
    public ResponseEntity<ApiResult<RoleAssignmentPermissionResponse>> save(
            @Valid @RequestBody RoleAssignmentPermissionSaveRequest request) {
        return executeApiResult(() ->
                ApiResult.success(
                        roleAssignmentPermissionService.save(request),
                        "Lưu cấu hình gán vai trò thành công"
                )
        );
    }
}
