package com.gfi.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.controllers.annotations.DataScoped;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.role.RoleCreateRequest;
import com.gfi.backend.models.dtos.role.RoleDetailDto;
import com.gfi.backend.models.dtos.role.RoleFilterDto;
import com.gfi.backend.models.dtos.role.RoleListItemDto;
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

    /**
     * Danh sách vai trò với phân trang và filter.
     *
     * @param request yêu cầu tìm kiếm chứa điều kiện lọc và phân trang
     * @return trang danh sách vai trò cơ bản (id, code, roleName, status)
     */
    @PostMapping("/search")
    @DataScoped(feature = "ROLE_MANAGEMENT", action = ActionType.VIEW)
    @Operation(summary = "Danh sách vai trò", description = "Lấy danh sách vai trò có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<RoleListItemDto, RoleFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<RoleFilterDto> request) {
        PageRequestDto<RoleFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(roleService.search(safeRequest), "Hiển thị danh sách vai trò thành công"));
    }

    /**
     * Danh sách vai trò cho dropdown/combobox.
     *
     * @return danh sách id và tên vai trò
     */
    @GetMapping("/options")
    // @DataScoped(menuCode = "ROLE_MANAGEMENT")
    @Operation(summary = "Danh sách vai trò cho combobox", description = "Lấy danh sách id và tên vai trò.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions() {
        return executeApiResult(() -> ApiResult.success(roleService.getOptions(), "Hiển thị danh sách vai trò thành công"));
    }

    /**
     * Chi tiết vai trò theo id.
     *
     * @param id ID của vai trò
     * @return thông tin chi tiết vai trò (tất cả trường)
     */
    @GetMapping("/{id}")
    @DataScoped(feature = "ROLE_MANAGEMENT", action = ActionType.VIEW, scopeExpression = "#id")
    @Operation(summary = "Chi tiết vai trò", description = "Lấy thông tin vai trò theo id.")
    public ResponseEntity<ApiResult<RoleDetailDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(roleService.getById(id), "Hiển thị chi tiết vai trò thành công"));
    }

    /**
     * Tạo mới vai trò.
     *
     * @param request dữ liệu vai trò cần tạo
     * @return thông tin chi tiết vai trò vừa tạo
     */
    @PostMapping
    @DataScoped(feature = "ROLE_MANAGEMENT", action = ActionType.ADD)
    @Operation(summary = "Thêm vai trò", description = "Tạo mới vai trò.")
    public ResponseEntity<ApiResult<RoleDetailDto>> create(@Valid @RequestBody RoleCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(roleService.create(request), "Thêm vai trò thành công"));
    }

    /**
     * Cập nhật vai trò theo id.
     *
     * @param id ID của vai trò
     * @param request dữ liệu vai trò cần cập nhật
     * @return thông tin chi tiết vai trò sau cập nhật
     */
    @PutMapping("/{id}")
    @DataScoped(feature = "ROLE_MANAGEMENT", action = ActionType.EDIT, scopeExpression = "#id")
    @Operation(summary = "Sửa vai trò", description = "Cập nhật vai trò theo id.")
    public ResponseEntity<ApiResult<RoleDetailDto>> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(roleService.update(id, request), "Cập nhật vai trò thành công"));
    }

    /**
     * Xóa vai trò theo id.
     *
     * @param id ID của vai trò
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/{id}")
    @DataScoped(feature = "ROLE_MANAGEMENT", action = ActionType.DELETE, scopeExpression = "#id")
    @Operation(summary = "Xóa vai trò", description = "Xóa vai trò theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            roleService.delete(id);
            return ApiResult.success(null, "Xóa vai trò thành công");
        });
    }
}
