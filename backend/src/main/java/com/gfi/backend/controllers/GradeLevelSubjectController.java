package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "GradeLevelSubject")
public class GradeLevelSubjectController extends ApiBaseController {

    private final GradeLevelSubjectService gradeLevelSubjectService;

    @GetMapping("/{gradeLevelId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chi tiet cau hinh mon hoc theo khoi", description = "Lay danh sach mon hoc duoc cau hinh cho khoi.")
    public ResponseEntity<ApiResult<GradeLevelSubjectConfigDto>> getByGradeLevelId(@PathVariable Long gradeLevelId) {
        return executeApiResult(() -> ApiResult.success(
                gradeLevelSubjectService.getByGradeLevelId(gradeLevelId),
                "Hien thi cau hinh mon hoc theo khoi thanh cong"));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Gan mon hoc cho khoi", description = "Luu cau hinh danh sach mon hoc theo khoi.")
    public ResponseEntity<ApiResult<GradeLevelSubjectConfigDto>> assignSubjects(
            @Valid @RequestBody GradeLevelSubjectAssignRequest request) {
        return executeApiResult(() -> ApiResult.success(
                gradeLevelSubjectService.assignSubjects(request),
                "Luu cau hinh mon hoc theo khoi thanh cong"));
    }
}
