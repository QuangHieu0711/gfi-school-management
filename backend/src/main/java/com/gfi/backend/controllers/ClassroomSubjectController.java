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
    @Operation(summary = "Chi tiet cau hinh mon hoc cua lop", description = "Lay danh sach mon hoc lop duoc ke thua tu khoi va trang thai dang bat.")
    public ResponseEntity<ApiResult<ClassroomSubjectConfigDto>> getByClassroomId(@PathVariable Long classroomId) {
        return executeApiResult(() -> ApiResult.success(
                classroomSubjectService.getByClassroomId(classroomId),
                "Hien thi cau hinh mon hoc cua lop thanh cong"));
    }

    @PostMapping("/assign")
    @Operation(summary = "Cap nhat mon hoc cua lop", description = "Luu danh sach mon hoc dang ap dung cho lop trong pham vi mon hoc cua khoi.")
    public ResponseEntity<ApiResult<ClassroomSubjectConfigDto>> assignSubjects(
            @Valid @RequestBody ClassroomSubjectAssignRequest request) {
        return executeApiResult(() -> ApiResult.success(
                classroomSubjectService.assignSubjects(request),
                "Luu cau hinh mon hoc cua lop thanh cong"));
    }
}
