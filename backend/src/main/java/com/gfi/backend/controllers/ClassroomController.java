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

import com.gfi.backend.models.dtos.classroom.ClassroomCreateRequest;
import com.gfi.backend.models.dtos.classroom.ClassroomFilterDto;
import com.gfi.backend.models.dtos.classroom.ClassroomItemDto;
import com.gfi.backend.models.dtos.classroom.ClassroomUpdateRequest;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.ClassroomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Tag(name = "Quản lý lớp - Classroom")
public class ClassroomController extends ApiBaseController {

    private final ClassroomService classroomService;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách lớp", description = "Lấy danh sách lớp có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<ClassroomItemDto, ClassroomFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<ClassroomFilterDto> request) {
        PageRequestDto<ClassroomFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(classroomService.search(safeRequest), "Hiển thị danh sách lớp thành công"));
    }

    @GetMapping("/options")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách lớp cho combobox", description = "Lấy danh sách id và tên lớp.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions(
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long gradeLevelId,
            @RequestParam(required = false) Long schoolYearId) {
        return executeApiResult(() -> ApiResult.success(classroomService.getOptions(unitId, gradeLevelId, schoolYearId), "Hiển thị danh sách lớp thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chi tiết lớp", description = "Lấy thông tin lớp theo id.")
    public ResponseEntity<ApiResult<ClassroomItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(classroomService.getById(id), "Hiển thị chi tiết lớp thành công"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm lớp", description = "Tạo mới lớp.")
    public ResponseEntity<ApiResult<ClassroomItemDto>> create(@Valid @RequestBody ClassroomCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(classroomService.create(request), "Thêm lớp thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa lớp", description = "Cập nhật lớp theo id.")
    public ResponseEntity<ApiResult<ClassroomItemDto>> update(@PathVariable Long id, @Valid @RequestBody ClassroomUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(classroomService.update(id, request), "Cập nhật lớp thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa lớp", description = "Xóa lớp theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            classroomService.delete(id);
            return ApiResult.success(null, "Xóa lớp thành công");
        });
    }
}
