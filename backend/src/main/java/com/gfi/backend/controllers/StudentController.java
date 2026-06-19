package com.gfi.backend.controllers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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

import com.gfi.backend.controllers.annotations.DataScoped;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.dtos.student.StudentCreateRequest;
import com.gfi.backend.models.dtos.student.StudentFilterDto;
import com.gfi.backend.models.dtos.student.StudentImportResultDto;
import com.gfi.backend.models.dtos.student.StudentItemDto;
import com.gfi.backend.models.dtos.student.StudentReportCardExportRequest;
import com.gfi.backend.models.dtos.student.StudentTransferClassRequest;
import com.gfi.backend.models.dtos.student.StudentTransferClassResultDto;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.StudentCodeGeneratorService;
import com.gfi.backend.services.interfaces.StudentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Quản lý học sinh - Student")
public class StudentController extends ApiBaseController {

    private final StudentService studentService;
    private final StudentCodeGeneratorService studentCodeGeneratorService;

    @PostMapping("/search")
    @DataScoped(feature = "STUDENT_PROFILE", action = ActionType.VIEW)
    @Operation(summary = "Danh sách học sinh")
    public ResponseEntity<ApiResult<PageResponseDto<StudentItemDto, StudentFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<StudentFilterDto> request) {
        PageRequestDto<StudentFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(
                () -> ApiResult.success(studentService.search(safeRequest), "Hiển thị danh sách học sinh thành công"));
    }

    @PostMapping("/export")
    @DataScoped(feature = "STUDENT_PROFILE", action = ActionType.VIEW)
    @Operation(summary = "Xuất danh sách học sinh")
    public ResponseEntity<byte[]> export(
            @RequestBody(required = false) PageRequestDto<StudentFilterDto> request,
            @RequestParam(required = false) Long unitId,
            @RequestParam(defaultValue = "EXCEL") ExportType exportType) {
        PageRequestDto<StudentFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        byte[] content = studentService.export(safeRequest, unitId, exportType);
        String extension = exportType == ExportType.PDF ? "pdf" : "xlsx";
        String contentType = exportType == ExportType.PDF
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("danh-sach-hoc-sinh." + extension, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }

    @PostMapping(value = "/excel-template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @DataScoped(feature = "STUDENT_PROFILE", action = ActionType.ADD)
    @Operation(summary = "Tải mẫu import học sinh")
    public ResponseEntity<byte[]> exportExcelTemplate(@RequestParam Long unitId) {
        byte[] content = studentService.exportExcelTemplate(unitId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("mau-import-hoc-sinh.xlsx", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @DataScoped(feature = "STUDENT_PROFILE", action = ActionType.ADD)
    @Operation(summary = "Import excel học sinh")
    public ResponseEntity<ApiResult<StudentImportResultDto>> importExcel(@RequestParam Long unitId,
            @RequestParam MultipartFile file) {
        return executeApiResult(() -> {
            StudentImportResultDto result = studentService.importExcel(unitId, file);
            String message = result.getFailedCount() > 0
                    ? String.format("Import hoàn tất: %d thành công, %d lỗi", result.getSuccessCount(),
                            result.getFailedCount())
                    : String.format("Import học sinh thành công: %d bản ghi", result.getSuccessCount());
            return ApiResult.success(result, message);
        });
    }

    @GetMapping("/import-error-file/{token}")
    @DataScoped(feature = "STUDENT_PROFILE", action = ActionType.ADD)
    @Operation(summary = "Tải file lỗi import học sinh")
    public ResponseEntity<byte[]> downloadImportErrorFile(@PathVariable String token) {
        TemporaryFileDto file = studentService.getImportErrorFile(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(file.getContent());
    }

    @GetMapping("/generate-code")
    @DataScoped(feature = "STUDENT_PROFILE", action = ActionType.ADD)
    @Operation(summary = "Sinh mã học sinh")
    public ResponseEntity<ApiResult<String>> generateStudentCode(@RequestParam Long unitId) {
        return executeApiResult(() -> {
            Integer year = LocalDate.now().getYear();
            String studentCode = studentCodeGeneratorService.generateStudentCode(unitId, year);
            return ApiResult.success(studentCode, "Sinh mã học sinh thành công");
        });
    }

    @GetMapping("/by-classroom")
    @DataScoped(feature = "STUDENT_PROFILE", action = ActionType.VIEW)
    @Operation(summary = "Danh sách học sinh thuộc lớp")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getStudentsByClassroom(@RequestParam Long classroomId) {
        return executeApiResult(() -> ApiResult.success(
                studentService.getStudentsByClassroom(classroomId),
                "Hiển thị danh sách học sinh theo lớp thành công"));
    }

    @PostMapping
    @DataScoped(feature = "STUDENT_PROFILE", action = ActionType.ADD)
    @Operation(summary = "Thêm học sinh")
    public ResponseEntity<ApiResult<StudentItemDto>> create(@Valid @RequestBody StudentCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(studentService.create(request), "Thêm học sinh thành công"));
    }

    @GetMapping("/{id}")
    @DataScoped(feature = "STUDENT_PROFILE", action = ActionType.VIEW)
    @Operation(summary = "Chi tiết học sinh")
    public ResponseEntity<ApiResult<StudentItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(
                () -> ApiResult.success(studentService.getById(id), "Hiển thị chi tiết học sinh thành công"));
    }

    @PutMapping("/{id}")
    @DataScoped(feature = "STUDENT_PROFILE", action = ActionType.EDIT)
    @Operation(summary = "Cập nhật học sinh")
    public ResponseEntity<ApiResult<StudentItemDto>> update(@PathVariable Long id,
            @Valid @RequestBody StudentCreateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(studentService.update(id, request), "Cập nhật học sinh thành công"));
    }

    @PostMapping("/transfer-class")
    @DataScoped(feature = "STUDENT_PROFILE", action = ActionType.EDIT)
    @Operation(summary = "Chuyển lớp học sinh")
    public ResponseEntity<ApiResult<StudentTransferClassResultDto>> transferClass(
            @Valid @RequestBody StudentTransferClassRequest request) {
        return executeApiResult(() -> ApiResult.success(
                studentService.transferClass(request),
                "Chuyển lớp học sinh thành công"));
    }

    @PostMapping("/export-report-cards")
    @DataScoped(feature = "STUDENT_PROFILE", action = ActionType.DOWNLOAD)
    @Operation(summary = "Xuat hoc ba hoc sinh")
    public ResponseEntity<byte[]> exportReportCards(
            @Valid @RequestBody StudentReportCardExportRequest request,
            @RequestParam(defaultValue = "PDF") ExportType exportType) {
        byte[] content = studentService.exportReportCards(request, exportType);
        String extension = exportType == ExportType.PDF ? "pdf" : "xlsx";
        String contentType = exportType == ExportType.PDF
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("hoc-ba-hoc-sinh." + extension, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }

    @DeleteMapping("/{id}")
    @DataScoped(feature = "STUDENT_PROFILE", action = ActionType.DELETE)
    @Operation(summary = "Xóa học sinh")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            studentService.delete(id);
            return ApiResult.success(null, "Xóa học sinh thành công");
        });
    }
}
