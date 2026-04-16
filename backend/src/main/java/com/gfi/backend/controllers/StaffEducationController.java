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
import com.gfi.backend.models.dtos.staff.StaffEducationCreateRequest;
import com.gfi.backend.models.dtos.staff.StaffEducationDto;
import com.gfi.backend.models.dtos.staff.StaffEducationFilterDto;
import com.gfi.backend.models.dtos.staff.StaffEducationUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.StaffEducationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staff-educations")
@RequiredArgsConstructor
@Tag(name = "Thông tin đào tạo cán bộ - Staff Education")
public class StaffEducationController extends ApiBaseController {

    private final StaffEducationService staffEducationService;

    @PostMapping("/search")
    @Operation(summary = "Danh sách thông tin đào tạo", description = "Lấy danh sách thông tin đào tạo có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<StaffEducationDto, StaffEducationFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<StaffEducationFilterDto> request) {
        PageRequestDto<StaffEducationFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(staffEducationService.search(safeRequest),
                "Hiển thị danh sách thông tin đào tạo thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết thông tin đào tạo", description = "Lấy chi tiết thông tin đào tạo theo id.")
    public ResponseEntity<ApiResult<StaffEducationDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(staffEducationService.getById(id),
                "Hiển thị chi tiết thông tin đào tạo thành công"));
    }

    @PostMapping
    @Operation(summary = "Thêm thông tin đào tạo", description = "Thêm mới thông tin đào tạo cho cán bộ.")
    public ResponseEntity<ApiResult<StaffEducationDto>> create(@Valid @RequestBody StaffEducationCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(staffEducationService.create(request),
                "Thêm thông tin đào tạo thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin đào tạo", description = "Cập nhật thông tin đào tạo theo id.")
    public ResponseEntity<ApiResult<StaffEducationDto>> update(@PathVariable Long id,
            @Valid @RequestBody StaffEducationUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(staffEducationService.update(id, request),
                "Cập nhật thông tin đào tạo thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa thông tin đào tạo", description = "Xóa thông tin đào tạo theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            staffEducationService.delete(id);
            return ApiResult.success(null, "Xóa thông tin đào tạo thành công");
        });
    }
}
