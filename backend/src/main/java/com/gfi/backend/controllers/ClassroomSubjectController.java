package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.classroomsubject.ClassroomSubjectAssignRequest;
import com.gfi.backend.models.dtos.classroomsubject.ClassroomSubjectConfigDto;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.ClassroomSubjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/classroom-subjects")
@RequiredArgsConstructor
@Tag(name = "Quản lý cấu hình môn học theo lớp -ClassroomSubject")
public class ClassroomSubjectController extends ApiBaseController {

    private final ClassroomSubjectService classroomSubjectService;

    @GetMapping("/{classroomId}")
    @Operation(summary = "Chi tiết cấu hình môn học của lớp", description = "Lấy danh sách môn học theo lớp, bao gồm cả môn học kế thừa từ khối và môn học được cấu hình riêng cho lớp.")
    public ResponseEntity<ApiResult<ClassroomSubjectConfigDto>> getByClassroomId(@PathVariable Long classroomId) {
        return executeApiResult(() -> ApiResult.success(
                classroomSubjectService.getByClassroomId(classroomId),
                "Hiển thị cấu hình môn học của lớp thành công"));
    }

    @PostMapping("/assign")
    @Operation(summary = "Cập nhật môn học của lớp", description = "Lưu danh sách môn học đang áp dụng cho lớp trong phạm vi môn học của khối.")
    public ResponseEntity<ApiResult<ClassroomSubjectConfigDto>> assignSubjects(
            @Valid @RequestBody ClassroomSubjectAssignRequest request) {
        return executeApiResult(() -> ApiResult.success(
                classroomSubjectService.assignSubjects(request),
                "Lưu cấu hình môn học của lớp thành công"));
    }
}
