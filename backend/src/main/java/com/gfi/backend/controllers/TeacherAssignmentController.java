package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.controllers.annotations.DataScoped;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentCreateRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentDetailRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentDetailResponse;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentFilterDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentItemDto;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.TeacherAssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/teacher-assignments")
@RequiredArgsConstructor
@Tag(name = "Quản lý phân công giáo viên - Teacher Assignment")
public class TeacherAssignmentController extends ApiBaseController {

    private final TeacherAssignmentService teacherAssignmentService;

    @PostMapping("/search")
    @DataScoped(feature = "ASSIGNMENT_LIST", action = ActionType.VIEW)
    @Operation(summary = "Danh sách phân công", description = "Lấy danh sách phân công giáo viên dạy học.")
    public ResponseEntity<ApiResult<PageResponseDto<TeacherAssignmentItemDto, TeacherAssignmentFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<TeacherAssignmentFilterDto> request) {
        PageRequestDto<TeacherAssignmentFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(
                () -> ApiResult.success(teacherAssignmentService.search(safeRequest),
                        "Hiển thị danh sách phân công thành công"));
    }

    @PostMapping("/detail")
    @DataScoped(feature = "ASSIGNMENT_LIST", action = ActionType.VIEW)
    @Operation(summary = "Chi tiết phân công",
            description = "Lấy thông tin chi tiết phân công theo đơn vị, cán bộ, môn học và năm học.")
    public ResponseEntity<ApiResult<TeacherAssignmentDetailResponse>> getDetail(
            @Valid @RequestBody TeacherAssignmentDetailRequest request) {
        return executeApiResult(
                () -> ApiResult.success(teacherAssignmentService.getDetail(request),
                        "Hiển thị chi tiết phân công thành công"));
    }

    @PostMapping
    @DataScoped(feature = "ASSIGNMENT_LIST", action = ActionType.ADD)
    @Operation(summary = "Thêm phân công", description = "Tạo danh sách phân công dạy học mới theo giáo viên.")
    public ResponseEntity<ApiResult<List<TeacherAssignmentItemDto>>> create(
            @Valid @RequestBody TeacherAssignmentCreateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(teacherAssignmentService.create(request), "Thêm phân công thành công"));
    }

    @PutMapping
    @DataScoped(feature = "ASSIGNMENT_LIST", action = ActionType.EDIT)
    @Operation(summary = "Sửa phân công", description = "Cập nhật danh sách phân công dạy học theo giáo viên.")
    public ResponseEntity<ApiResult<List<TeacherAssignmentItemDto>>> update(
            @Valid @RequestBody TeacherAssignmentCreateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(teacherAssignmentService.update(request),
                        "Cập nhật phân công thành công"));
    }

    @DeleteMapping("/{id}")
    @DataScoped(feature = "ASSIGNMENT_LIST", action = ActionType.DELETE)
    @Operation(summary = "Xóa phân công", description = "Xóa phân công theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            teacherAssignmentService.delete(id);
            return ApiResult.success(null, "Xóa phân công thành công");
        });
    }
}
