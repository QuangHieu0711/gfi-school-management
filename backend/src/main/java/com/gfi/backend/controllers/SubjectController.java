package com.gfi.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
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
import com.gfi.backend.models.dtos.subject.SubjectCreateRequest;
import com.gfi.backend.models.dtos.subject.SubjectFilterDto;
import com.gfi.backend.models.dtos.subject.SubjectItemDto;
import com.gfi.backend.models.dtos.subject.SubjectUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.SubjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@Tag(name = "Quản lý môn học - Subject")
public class SubjectController extends ApiBaseController {

    private final SubjectService subjectService;

    @PostMapping("/search")
    @Operation(summary = "Danh sách môn học", description = "Lấy danh sách môn học có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<SubjectItemDto, SubjectFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<SubjectFilterDto> request) {
        PageRequestDto<SubjectFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(subjectService.search(safeRequest), "Hiển thị danh sách môn học thành công"));
    }

    @GetMapping("/options")
    @Operation(summary = "Danh sách môn học cho combobox", description = "Lấy danh sách id và tên môn học.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions() {
        return executeApiResult(() -> ApiResult.success(subjectService.getOptions(), "Hiển thị danh sách môn học thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết môn học", description = "Lấy thông tin môn học theo id.")
    public ResponseEntity<ApiResult<SubjectItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(subjectService.getById(id), "Hiển thị chi tiết môn học thành công"));
    }

    @PostMapping
    @Operation(summary = "Thêm môn học", description = "Tạo mới môn học.")
    public ResponseEntity<ApiResult<SubjectItemDto>> create(@Valid @RequestBody SubjectCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(subjectService.create(request), "Thêm môn học thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa môn học", description = "Cập nhật môn học theo id.")
    public ResponseEntity<ApiResult<SubjectItemDto>> update(@PathVariable Long id, @Valid @RequestBody SubjectUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(subjectService.update(id, request), "Cập nhật môn học thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa môn học", description = "Xóa môn học theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            subjectService.delete(id);
            return ApiResult.success(null, "Xóa môn học thành công");
        });
    }
}
