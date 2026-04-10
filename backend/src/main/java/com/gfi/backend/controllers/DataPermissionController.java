package com.gfi.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.datapermission.DataPermissionItemDto;
import com.gfi.backend.models.dtos.datapermission.DataPermissionSaveRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.DataPermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller quản lý phân quyền dữ liệu.
 * Xử lý HTTP requests liên quan đến phân quyền dữ liệu theo vai trò.
 */
@RestController
@RequestMapping("/api/data-permissions")
@RequiredArgsConstructor
@Tag(name = "Phân quyền dữ liệu - Data Permission Management")
public class DataPermissionController extends ApiBaseController {

    private final DataPermissionService dataPermissionService;

    /**
     * Lấy danh sách quyền dữ liệu theo ID vai trò.
     * Trả về danh sách quyền dữ liệu để hiển thị hoặc chỉnh sửa.
     *
     * @param roleId ID của vai trò cần lấy quyền
     * @return danh sách quyền dữ liệu thuộc vai trò
     */
    @GetMapping("/{roleId}")
    @Operation(summary = "Lấy danh sách quyền dữ liệu theo vai trò", description = "Get data permission list by role id.")
    public ResponseEntity<ApiResult<List<DataPermissionItemDto>>> getByRoleId(@PathVariable Long roleId) {
        return executeApiResult(() -> ApiResult.success(dataPermissionService.getByRoleId(roleId), "Lấy chi tiết quyền dữ liệu thành công"));
    }

    /**
     * Lưu danh sách quyền dữ liệu cho một vai trò.
     * Cho phép chèn mới, cập nhật hoặc xóa quyền dữ liệu trong một yêu cầu.
     *
     * @param requests danh sách yêu cầu lưu quyền dữ liệu (có thể chứa cả thêm, sửa, xóa)
     * @return danh sách quyền dữ liệu sau khi lưu
     */
    @PostMapping("/save")
    @Operation(summary = "Lưu quyền dữ liệu", description = "Chèn, cập nhật hoặc xóa quyền dữ liệu trong một yêu cầu.")
    public ResponseEntity<ApiResult<List<DataPermissionItemDto>>> savePermissions(
            @RequestBody List<@Valid DataPermissionSaveRequest> requests) {
        return executeApiResult(() -> ApiResult.success(dataPermissionService.savePermissions(requests), "Lưu phân quyền dữ liệu thành công"));
    }
}
