package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.staff.*;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.StaffService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staffs")
@RequiredArgsConstructor
@Tag(name = "Quản lý cán bộ/giáo viên - Staff")
public class StaffController extends ApiBaseController {

    private final StaffService staffService;

    @PostMapping("/search")
    @Operation(summary = "Danh sách cán bộ", description = "Lấy danh sách cán bộ có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<StaffItemDto, StaffFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<StaffFilterDto> request) {
        PageRequestDto<StaffFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(
                () -> ApiResult.success(staffService.search(safeRequest), "Hiển thị danh sách cán bộ thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết cán bộ", description = "Lấy thông tin chi tiết cán bộ theo id.")
    public ResponseEntity<ApiResult<StaffDetailDto>> getById(@PathVariable Long id) {
        return executeApiResult(
                () -> ApiResult.success(staffService.getById(id), "Hiển thị chi tiết cán bộ thành công"));
    }

    @PostMapping
    @Operation(summary = "Thêm cán bộ", description = "Tạo mới cán bộ.")
    public ResponseEntity<ApiResult<StaffDetailDto>> create(@Valid @RequestBody StaffCreateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(staffService.create(request), "Thêm cán bộ thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa cán bộ", description = "Cập nhật thông tin cán bộ.")
    public ResponseEntity<ApiResult<StaffDetailDto>> update(@PathVariable Long id,
            @Valid @RequestBody StaffUpdateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(staffService.update(id, request), "Cập nhật cán bộ thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa cán bộ", description = "Xóa cán bộ theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            staffService.delete(id);
            return ApiResult.success(null, "Xóa cán bộ thành công");
        });
    }
}
