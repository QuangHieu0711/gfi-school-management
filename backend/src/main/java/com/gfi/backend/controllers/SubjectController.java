package com.gfi.backend.controllers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.dtos.subject.SubjectCreateRequest;
import com.gfi.backend.models.dtos.subject.SubjectDetailDto;
import com.gfi.backend.models.dtos.subject.SubjectFilterDto;
import com.gfi.backend.models.dtos.subject.SubjectImportResultDto;
import com.gfi.backend.models.dtos.subject.SubjectListItemDto;
import com.gfi.backend.models.dtos.subject.SubjectUpdateRequest;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.ClassroomSubjectService;
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
    private final ClassroomSubjectService classroomSubjectService;

    @PostMapping("/search")
    @Operation(summary = "Danh sách môn học", description = "Lấy danh sách môn học có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<SubjectListItemDto, SubjectFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<SubjectFilterDto> request) {
        PageRequestDto<SubjectFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(
                () -> ApiResult.success(subjectService.search(safeRequest), "Hiển thị danh sách môn học thành công"));
    }

    @PostMapping("/export")
    @Operation(summary = "Xuất danh sách môn học", description = "Xuất danh sách môn học theo điều kiện tìm kiếm, hỗ trợ EXCEL hoặc PDF.")
    public ResponseEntity<byte[]> export(
            @RequestBody(required = false) PageRequestDto<SubjectFilterDto> request,
            @RequestParam(defaultValue = "EXCEL") ExportType exportType) {
        PageRequestDto<SubjectFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        byte[] content = subjectService.export(safeRequest, exportType);
        String extension = exportType == ExportType.PDF ? "pdf" : "xlsx";
        String contentType = exportType == ExportType.PDF
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("danh-sach-mon-hoc." + extension, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }

    @PostMapping(value = "/excel-template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Tải mẫu Excel môn học", description = "Tải file mẫu Excel để import môn học.")
    public ResponseEntity<byte[]> exportExcelTemplate() {
        byte[] content = subjectService.exportExcelTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("mau-import-mon-hoc.xlsx", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import Excel môn học", description = "Đọc file Excel và tạo hoặc cập nhật danh sách môn học.")
    public ResponseEntity<ApiResult<SubjectImportResultDto>> importExcel(@RequestParam MultipartFile file) {
        return executeApiResult(() -> {
            SubjectImportResultDto result = subjectService.importExcel(file);
            String message = result.getFailedCount() > 0
                    ? String.format("Import hoàn tất: %d thành công, %d lỗi", result.getSuccessCount(), result.getFailedCount())
                    : String.format("Import môn học thành công: %d bản ghi", result.getSuccessCount());
            return ApiResult.success(result, message);
        });
    }

    @GetMapping("/import-error-file/{token}")
    @Operation(summary = "Tải file lỗi import môn học", description = "Tải file Excel phản hồi sau khi import môn học có lỗi.")
    public ResponseEntity<byte[]> downloadImportErrorFile(@PathVariable String token) {
        TemporaryFileDto file = subjectService.getImportErrorFile(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(file.getContent());
    }

    @GetMapping("/options")
    @Operation(summary = "Danh sách môn học cho combobox", description = "Lấy danh sách id và tên môn học.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions() {
        return executeApiResult(
                () -> ApiResult.success(subjectService.getOptions(), "Hiển thị danh sách môn học thành công"));
    }

    @GetMapping("/{subjectId}/classrooms")
    @Operation(summary = "Danh sách lớp theo môn học", description = "Lấy danh sách lớp đang cấu hình môn học được chọn.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getClassroomsBySubjectId(@PathVariable Long subjectId,
            @RequestParam(required = false) Long unitId) {
        return executeApiResult(() -> ApiResult.success(
                classroomSubjectService.getClassroomsBySubjectId(subjectId, unitId),
                "Hiển thị danh sách lớp theo môn học thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết môn học", description = "Lấy thông tin môn học theo id.")
    public ResponseEntity<ApiResult<SubjectDetailDto>> getById(@PathVariable Long id) {
        return executeApiResult(
                () -> ApiResult.success(subjectService.getById(id), "Hiển thị chi tiết môn học thành công"));
    }

    @PostMapping
    @Operation(summary = "Thêm môn học", description = "Tạo mới môn học.")
    public ResponseEntity<ApiResult<SubjectDetailDto>> create(@Valid @RequestBody SubjectCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(subjectService.create(request), "Thêm môn học thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa môn học", description = "Cập nhật môn học theo id.")
    public ResponseEntity<ApiResult<SubjectDetailDto>> update(@PathVariable Long id,
            @Valid @RequestBody SubjectUpdateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(subjectService.update(id, request), "Cập nhật môn học thành công"));
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
