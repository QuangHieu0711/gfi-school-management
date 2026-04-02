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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.semester.SemesterCreateRequest;
import com.gfi.backend.models.dtos.semester.SemesterFilterDto;
import com.gfi.backend.models.dtos.semester.SemesterItemDto;
import com.gfi.backend.models.dtos.semester.SemesterUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.SemesterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/semesters")
@RequiredArgsConstructor
@Tag(name = "Quản lý học kỳ - Semester")
public class SemesterController extends ApiBaseController {

    private final SemesterService semesterService;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách học kỳ", description = "Lấy toàn bộ danh sách học kỳ.")
    public ResponseEntity<ApiResult<List<SemesterItemDto>>> search(
            @RequestBody(required = false) SemesterFilterDto filter) {
        return executeApiResult(() -> ApiResult.success(semesterService.search(filter), "Hiển thị danh sách học kỳ thành công"));
    }

    @GetMapping("/options")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách học kỳ cho combobox", description = "Lấy danh sách id và tên học kỳ.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions(@RequestParam(required = false) Long schoolYearId) {
        return executeApiResult(() -> ApiResult.success(semesterService.getOptions(schoolYearId), "Hiển thị danh sách học kỳ thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chi tiết học kỳ", description = "Lấy thông tin học kỳ theo id.")
    public ResponseEntity<ApiResult<SemesterItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(semesterService.getById(id), "Hiển thị chi tiết học kỳ thành công"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm học kỳ", description = "Tạo mới học kỳ.")
    public ResponseEntity<ApiResult<SemesterItemDto>> create(@Valid @RequestBody SemesterCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(semesterService.create(request), "Thêm học kỳ thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa học kỳ", description = "Cập nhật học kỳ theo id.")
    public ResponseEntity<ApiResult<SemesterItemDto>> update(@PathVariable Long id, @Valid @RequestBody SemesterUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(semesterService.update(id, request), "Cập nhật học kỳ thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa học kỳ", description = "Xóa học kỳ theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            semesterService.delete(id);
            return ApiResult.success(null, "Xóa học kỳ thành công");
        });
    }
}
