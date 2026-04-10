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

/**
 * REST Controller quản lý phân quyền chức năng.
 * Xử lý HTTP requests liên quan đến phân quyền chức năng theo vai trò.
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Tag(name = "Phân quyền chức năng - Permission Management")
public class PermissionController extends ApiBaseController {

    private final PermissionService permissionService;

    /**
     * Lấy danh sách quyền chức năng theo ID vai trò.
     * Trả về danh sách quyền chức năng để hiển thị hoặc chỉnh sửa.
     *
     * @param roleId ID của vai trò cần lấy quyền
     * @return danh sách quyền chức năng thuộc vai trò
     */
    @GetMapping("/{roleId}")
    @Operation(summary = "Lấy danh sách quyền theo vai trò", description = "Lấy danh sách quyền theo id vai trò.")
    public ResponseEntity<ApiResult<List<PermissionItemDto>>> getByRoleId(@PathVariable Long roleId) {
        return executeApiResult(() -> ApiResult.success(permissionService.getByRoleId(roleId), "Lấy chi tiết quyền thành công"));
    }

    /**
     * Lưu danh sách quyền chức năng cho một vai trò.
     * Cho phép chèn mới, cập nhật hoặc xóa quyền trong một yêu cầu.
     *
     * @param requests danh sách yêu cầu lưu quyền (có thể chứa cả thêm, sửa, xóa)
     * @return danh sách quyền chức năng sau khi lưu
     */
    @PostMapping("/save")
    @Operation(summary = "Lưu quyền", description = "Chèn, cập nhật hoặc xóa quyền trong một yêu cầu.")
    public ResponseEntity<ApiResult<List<PermissionItemDto>>> savePermissions(
            @RequestBody List<@Valid PermissionSaveRequest> requests) {
        return executeApiResult(() -> ApiResult.success(permissionService.savePermissions(requests), "Lưu phân quyền thành công"));
    }
}
