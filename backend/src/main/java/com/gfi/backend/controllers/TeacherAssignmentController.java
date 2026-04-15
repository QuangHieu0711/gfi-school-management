package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.staff.*;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.TeacherAssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/teacher-assignments")
@RequiredArgsConstructor
@Tag(name = "Quản lý phân công giáo viên - Teacher Assignment")
public class TeacherAssignmentController extends ApiBaseController {

    private final TeacherAssignmentService teacherAssignmentService;

    @PostMapping("/search")
    @Operation(summary = "Danh sách phân công", description = "Lấy danh sách phân công giáo viên dạy học.")
    public ResponseEntity<ApiResult<PageResponseDto<TeacherAssignmentItemDto, TeacherAssignmentFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<TeacherAssignmentFilterDto> request) {
        PageRequestDto<TeacherAssignmentFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(
                () -> ApiResult.success(teacherAssignmentService.search(safeRequest), "Hiển thị danh sách phân công thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết phân công", description = "Lấy thông tin chi tiết phân công theo id.")
    public ResponseEntity<ApiResult<TeacherAssignmentItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(
                () -> ApiResult.success(teacherAssignmentService.getById(id), "Hiển thị chi tiết phân công thành công"));
    }

    @PostMapping
    @Operation(summary = "Thêm phân công", description = "Tạo phân công dạy học mới.")
    public ResponseEntity<ApiResult<TeacherAssignmentItemDto>> create(@Valid @RequestBody TeacherAssignmentCreateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(teacherAssignmentService.create(request), "Thêm phân công thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa phân công", description = "Cập nhật thông tin phân công.")
    public ResponseEntity<ApiResult<TeacherAssignmentItemDto>> update(@PathVariable Long id,
            @Valid @RequestBody TeacherAssignmentCreateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(teacherAssignmentService.update(id, request), "Cập nhật phân công thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa phân công", description = "Xóa phân công theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            teacherAssignmentService.delete(id);
            return ApiResult.success(null, "Xóa phân công thành công");
        });
    }
}
