package com.gfi.backend.controllers;

import com.gfi.backend.controllers.annotations.DataScoped;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentCreateRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentDetailRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentDetailResponse;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentImportResultDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentItemDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentSearchRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentSearchResponse;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentStaffClassResponse;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.TeacherAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
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
    public ResponseEntity<ApiResult<TeacherAssignmentSearchResponse>> search(
            @RequestBody(required = false) TeacherAssignmentSearchRequest request) {
        TeacherAssignmentSearchRequest safeRequest = request == null ? new TeacherAssignmentSearchRequest() : request;
        return executeApiResult(
                () -> ApiResult.success(teacherAssignmentService.search(safeRequest),
                        "Hiển thị danh sách phân công thành công"));
    }

    @PostMapping("/detail")
    @DataScoped(feature = "ASSIGNMENT_LIST", action = ActionType.VIEW)
    @Operation(summary = "Chi tiết phân công", description = "Lấy thông tin chi tiết phân công theo đơn vị, cán bộ, môn học, năm học và học kỳ.")
    public ResponseEntity<ApiResult<TeacherAssignmentDetailResponse>> getDetail(
            @Valid @RequestBody TeacherAssignmentDetailRequest request) {
        return executeApiResult(
                () -> ApiResult.success(teacherAssignmentService.getDetail(request),
                        "Hiển thị chi tiết phân công thành công"));
    }

    @GetMapping("/staff/{staffId}/classes")
    @DataScoped(feature = "ASSIGNMENT_LIST", action = ActionType.VIEW)
    @Operation(summary = "Danh sách lớp và môn theo giáo viên", description = "Lấy danh sách lớp giáo viên được phân công và các môn giáo viên dạy trong từng lớp.")
    public ResponseEntity<ApiResult<List<TeacherAssignmentStaffClassResponse>>> getClassesByStaff(
            @PathVariable Long staffId) {
        return executeApiResult(
                () -> ApiResult.success(teacherAssignmentService.getClassesByStaff(staffId),
                        "Hiển thị danh sách lớp phân công theo giáo viên thành công"));
    }

    @PostMapping
    @DataScoped(feature = "ASSIGNMENT_LIST", action = ActionType.ADD)
    @Operation(summary = "Lưu phân công", description = "Tạo mới hoặc cập nhật danh sách phân công dạy học theo giáo viên.")
    public ResponseEntity<ApiResult<List<TeacherAssignmentItemDto>>> create(
            @Valid @RequestBody TeacherAssignmentCreateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(teacherAssignmentService.create(request), "Lưu phân công thành công"));
    }

    @PostMapping(value = "/excel-template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @DataScoped(feature = "ASSIGNMENT_LIST", action = ActionType.DOWNLOAD)
    @Operation(summary = "Tạo mẫu Excel phân công giảng dạy", description = "Tạo file Excel mẫu import phân công giảng dạy theo đơn vị và năm học.")
    public ResponseEntity<byte[]> exportExcelTemplate(
            @RequestParam Long schoolYearId,
            @RequestParam Long unitId) {
        byte[] content = teacherAssignmentService.exportExcelTemplate(schoolYearId, unitId);
        String fileName = "phan-cong-giang-day-" + unitId + "-" + schoolYearId + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @DataScoped(feature = "ASSIGNMENT_LIST", action = ActionType.ADD)
    @Operation(summary = "Import Excel phân công giảng dạy", description = "Đọc file Excel và lưu phân công giảng dạy theo học kỳ.")
    public ResponseEntity<ApiResult<TeacherAssignmentImportResultDto>> importExcel(
            @RequestParam Long schoolYearId,
            @RequestParam Long unitId,
            @RequestParam MultipartFile file) {
        return executeApiResult(
                () -> ApiResult.success(teacherAssignmentService.importExcel(schoolYearId, unitId, file),
                        "Import phân công giảng dạy thành công"));
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
