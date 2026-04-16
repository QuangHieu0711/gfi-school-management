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
@Tag(name = "Thong tin ngoai ngu can bo - Staff Foreign Language")
public class StaffForeignLanguageController extends ApiBaseController {

    private final StaffForeignLanguageService staffForeignLanguageService;

    @PostMapping("/search")
    @Operation(summary = "Danh sach thong tin ngoai ngu", description = "Lay danh sach thong tin ngoai ngu co phan trang va filter.")
    public ResponseEntity<ApiResult<PageResponseDto<StaffForeignLanguageDto, StaffForeignLanguageFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<StaffForeignLanguageFilterDto> request) {
        PageRequestDto<StaffForeignLanguageFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(staffForeignLanguageService.search(safeRequest),
                "Hien thi danh sach thong tin ngoai ngu thanh cong"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiet thong tin ngoai ngu", description = "Lay chi tiet thong tin ngoai ngu theo id.")
    public ResponseEntity<ApiResult<StaffForeignLanguageDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(staffForeignLanguageService.getById(id),
                "Hien thi chi tiet thong tin ngoai ngu thanh cong"));
    }

    @PostMapping
    @Operation(summary = "Them thong tin ngoai ngu", description = "Them moi thong tin ngoai ngu cho can bo.")
    public ResponseEntity<ApiResult<StaffForeignLanguageDto>> create(
            @Valid @RequestBody StaffForeignLanguageCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(staffForeignLanguageService.create(request),
                "Them thong tin ngoai ngu thanh cong"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cap nhat thong tin ngoai ngu", description = "Cap nhat thong tin ngoai ngu theo id.")
    public ResponseEntity<ApiResult<StaffForeignLanguageDto>> update(@PathVariable Long id,
            @Valid @RequestBody StaffForeignLanguageUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(staffForeignLanguageService.update(id, request),
                "Cap nhat thong tin ngoai ngu thanh cong"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoa thong tin ngoai ngu", description = "Xoa thong tin ngoai ngu theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            staffForeignLanguageService.delete(id);
            return ApiResult.success(null, "Xoa thong tin ngoai ngu thanh cong");
        });
    }
}
