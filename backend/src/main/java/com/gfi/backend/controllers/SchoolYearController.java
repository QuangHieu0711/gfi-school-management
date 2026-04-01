package com.gfi.backend.controllers;

import java.util.List;

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

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearCreateRequest;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearFilterDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearItemDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.SchoolYearService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/school-years")
@RequiredArgsConstructor
@Tag(name = "School Year")
public class SchoolYearController extends ApiBaseController {

    private final SchoolYearService schoolYearService;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách năm học", description = "lấy danh sách năm học có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<SchoolYearItemDto, SchoolYearFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<SchoolYearFilterDto> request) {
        PageRequestDto<SchoolYearFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(schoolYearService.search(safeRequest), "Hiển thị danh sách năm học thành công"));
    }

    @GetMapping("/options")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách năm học cho combobox", description = "Lấy danh sách id và tên năm học.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions() {
        return executeApiResult(() -> ApiResult.success(schoolYearService.getOptions(), "Hiển thị danh sách năm học thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chi tiết năm học", description = "Lấy thông tin năm học theo id.")
    public ResponseEntity<ApiResult<SchoolYearItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(schoolYearService.getById(id), "Hiển thị chi tiết năm học thành công"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm năm học", description = "Tạo mới năm học.")
    public ResponseEntity<ApiResult<SchoolYearItemDto>> create(@Valid @RequestBody SchoolYearCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(schoolYearService.create(request), "Thêm năm học thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa năm học", description = "Cập nhật năm học theo id.")
    public ResponseEntity<ApiResult<SchoolYearItemDto>> update(@PathVariable Long id, @Valid @RequestBody SchoolYearUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(schoolYearService.update(id, request), "Cập nhật năm học thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa năm học", description = "Xóa năm học theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            schoolYearService.delete(id);
            return ApiResult.success(null, "Xóa năm học thành công");
        });
    }
}
