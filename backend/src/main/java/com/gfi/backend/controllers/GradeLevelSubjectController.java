package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.gradelevelsubject.GradeLevelSubjectAssignRequest;
import com.gfi.backend.models.dtos.gradelevelsubject.GradeLevelSubjectConfigDto;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.GradeLevelSubjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/grade-level-subjects")
@RequiredArgsConstructor
@Tag(name = "Quản lý cấu hình môn học theo khối - GradeLevelSubject")
public class GradeLevelSubjectController extends ApiBaseController {

    private final GradeLevelSubjectService gradeLevelSubjectService;

    @GetMapping("/{gradeLevelId}")
    @Operation(summary = "Chi tiết cấu hình môn học theo khối", description = "Lấy thông tin cấu hình danh sách môn học theo khối.")
    public ResponseEntity<ApiResult<GradeLevelSubjectConfigDto>> getByGradeLevelId(@PathVariable Long gradeLevelId) {
        return executeApiResult(() -> ApiResult.success(
                gradeLevelSubjectService.getByGradeLevelId(gradeLevelId),
                "Hiển thị cấu hình môn học theo khối thành công"));
    }

    @PostMapping("/assign")
    @Operation(summary = "Gán môn học cho khối", description = "Lưu cấu hình danh sách môn học theo khối.")
    public ResponseEntity<ApiResult<GradeLevelSubjectConfigDto>> assignSubjects(
            @Valid @RequestBody GradeLevelSubjectAssignRequest request) {
        return executeApiResult(() -> ApiResult.success(
                gradeLevelSubjectService.assignSubjects(request),
                "Gán cấu hình môn học theo khối thành công"));
    }
}
