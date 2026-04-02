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
import com.gfi.backend.models.dtos.gradelevel.GradeLevelCreateRequest;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelFilterDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelItemDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.GradeLevelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/grade-levels")
@RequiredArgsConstructor
@Tag(name = "GradeLevel")
public class GradeLevelController extends ApiBaseController {

    private final GradeLevelService gradeLevelService;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách khối", description = "Lấy danh sách khối có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<GradeLevelItemDto, GradeLevelFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<GradeLevelFilterDto> request) {
        PageRequestDto<GradeLevelFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(gradeLevelService.search(safeRequest), "Hiển thị danh sách khối thành công"));
    }

    @GetMapping("/options")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách khối cho combobox", description = "Lấy danh sách id và tên khối.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions() {
        return executeApiResult(() -> ApiResult.success(gradeLevelService.getOptions(), "Hiển thị danh sách khối thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chi tiết khối", description = "Lấy thông tin khối theo id.")
    public ResponseEntity<ApiResult<GradeLevelItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(gradeLevelService.getById(id), "Hiển thị chi tiết khối thành công"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm khối", description = "Tạo mới khối.")
    public ResponseEntity<ApiResult<GradeLevelItemDto>> create(@Valid @RequestBody GradeLevelCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(gradeLevelService.create(request), "Thêm khối thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa khối", description = "Cập nhật khối theo id.")
    public ResponseEntity<ApiResult<GradeLevelItemDto>> update(@PathVariable Long id, @Valid @RequestBody GradeLevelUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(gradeLevelService.update(id, request), "Cập nhật khối thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa khối", description = "Xóa khối theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            gradeLevelService.delete(id);
            return ApiResult.success(null, "Xóa khối thành công");
        });
    }
}
