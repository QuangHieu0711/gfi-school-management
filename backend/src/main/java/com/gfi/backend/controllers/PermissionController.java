package com.gfi.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.permission.PermissionItemDto;
import com.gfi.backend.models.dtos.permission.PermissionSaveRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Tag(name = "Permission Management")
public class PermissionController extends ApiBaseController {

    private final PermissionService permissionService;

    @GetMapping("/{roleId}")
    @Operation(summary = "Get permissions by role", description = "Get permission list by role id.")
    public ResponseEntity<ApiResult<List<PermissionItemDto>>> getByRoleId(@PathVariable Long roleId) {
        return executeApiResult(() -> ApiResult.success(permissionService.getByRoleId(roleId), "Lay chi tiet quyen thanh cong"));
    }

    @PostMapping("/save")
    @Operation(summary = "Save permissions", description = "Insert, update or delete permissions in one request.")
    public ResponseEntity<ApiResult<List<PermissionItemDto>>> savePermissions(
            @RequestBody List<@Valid PermissionSaveRequest> requests) {
        return executeApiResult(() -> ApiResult.success(permissionService.savePermissions(requests), "Luu phan quyen thanh cong"));
    }
}
