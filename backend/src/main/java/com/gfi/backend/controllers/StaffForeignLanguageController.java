package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
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
import com.gfi.backend.models.dtos.staff.StaffForeignLanguageCreateRequest;
import com.gfi.backend.models.dtos.staff.StaffForeignLanguageDto;
import com.gfi.backend.models.dtos.staff.StaffForeignLanguageFilterDto;
import com.gfi.backend.models.dtos.staff.StaffForeignLanguageUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.StaffForeignLanguageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staff-foreign-languages")
@RequiredArgsConstructor
@Tag(name = "Thông tin ngoại ngữ cán bộ - Staff Foreign Language")
public class StaffForeignLanguageController extends ApiBaseController {

    private final StaffForeignLanguageService staffForeignLanguageService;

    @PostMapping("/search")
    @Operation(summary = "Danh sách thông tin ngoại ngữ", description = "Lấy danh sách thông tin ngoại ngữ của cán bộ theo điều kiện lọc và phân trang.")
    public ResponseEntity<ApiResult<PageResponseDto<StaffForeignLanguageDto, StaffForeignLanguageFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<StaffForeignLanguageFilterDto> request) {
        PageRequestDto<StaffForeignLanguageFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(staffForeignLanguageService.search(safeRequest),
                "Hiển thị danh sách thông tin ngoại ngữ thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết thông tin ngoại ngữ", description = "Lấy chi tiết thông tin ngoại ngữ theo id.")
    public ResponseEntity<ApiResult<StaffForeignLanguageDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(staffForeignLanguageService.getById(id),
                "Hiển thị chi tiết thông tin ngoại ngữ thành công"));
    }

    @PostMapping
    @Operation(summary = "Thêm thông tin ngoại ngữ", description = "Thêm mới thông tin ngoại ngữ cho cán bộ.")
    public ResponseEntity<ApiResult<StaffForeignLanguageDto>> create(
            @Valid @RequestBody StaffForeignLanguageCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(staffForeignLanguageService.create(request),
                "Thêm thông tin ngoại ngữ thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin ngoại ngữ", description = "Cập nhật thông tin ngoại ngữ theo id.")
    public ResponseEntity<ApiResult<StaffForeignLanguageDto>> update(@PathVariable Long id,
            @Valid @RequestBody StaffForeignLanguageUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(staffForeignLanguageService.update(id, request),
                "Cập nhật thông tin ngoại ngữ thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa thông tin ngoại ngữ", description = "Xóa thông tin ngoại ngữ theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            staffForeignLanguageService.delete(id);
            return ApiResult.success(null, "Xóa thông tin ngoại ngữ thành công");
        });
    }
}
