package com.gfi.backend.controllers;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.models.dtos.attendance.AttendanceBulkUpsertRequest;
import com.gfi.backend.models.dtos.attendance.AttendanceImportResultDto;
import com.gfi.backend.models.dtos.attendance.AttendanceMonthlyTableDto;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.AttendanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
@Tag(name = "Điểm danh - Attendance")
public class AttendanceController extends ApiBaseController {

    private final AttendanceService attendanceService;

    @GetMapping("/monthly-table")
    @Operation(summary = "Danh sách điểm danh theo tháng", description = "Lấy dữ liệu điểm danh của một lớp học trong một tháng, theo loại buổi học (sáng/chiều).")
    public ResponseEntity<ApiResult<AttendanceMonthlyTableDto>> getMonthlyTable(
            @RequestParam Long classroomId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam String sessionType) {
        return executeApiResult(() -> ApiResult.success(
                attendanceService.getMonthlyTable(classroomId, year, month, sessionType),
                "Lấy dữ liệu điểm danh theo tháng thành công"));
    }

    @GetMapping("/export")
    @Operation(summary = "Xuất điểm danh tháng", description = "Xuất file Excel hoặc PDF bảng điểm danh theo tháng.")
    public ResponseEntity<byte[]> export(
            @RequestParam Long classroomId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam String sessionType,
            @RequestParam(defaultValue = "EXCEL") ExportType exportType) {
        byte[] content = attendanceService.export(classroomId, year, month, sessionType, exportType);
        String extension = exportType == ExportType.PDF ? "pdf" : "xlsx";
        String contentType = exportType == ExportType.PDF
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String fileName = "bang-diem-danh-" + classroomId + "-" + year + "-" + String.format("%02d", month) + "."
                + extension;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }

    @org.springframework.web.bind.annotation.PostMapping(value = "/excel-template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Tải mẫu import điểm danh", description = "ải file Excel mẫu import điểm danh theo tháng.")
    public ResponseEntity<byte[]> exportExcelTemplate(
            @RequestParam Long classroomId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam String sessionType) {
        byte[] content = attendanceService.exportExcelTemplate(classroomId, year, month, sessionType);
        String fileName = "mau-import-diem-danh-" + classroomId + "-" + year + "-" + String.format("%02d", month) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @org.springframework.web.bind.annotation.PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import điểm danh tháng", description = "Import file Excel điểm danh theo tháng.")
    public ResponseEntity<ApiResult<AttendanceImportResultDto>> importExcel(
            @RequestParam Long classroomId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam String sessionType,
            @RequestParam MultipartFile file) {
        return executeApiResult(() -> {
            AttendanceImportResultDto result = attendanceService.importExcel(classroomId, year, month, sessionType, file);
            String message = result.getFailedCount() > 0
                    ? String.format("Import hoàn tất: %d thành công, %d lỗi", result.getSuccessCount(), result.getFailedCount())
                    : String.format("Import điểm danh thành công: %d bản ghi", result.getSuccessCount());
            return ApiResult.success(result, message);
        });
    }

    @GetMapping("/import-error-file/{token}")
    @Operation(summary = "Tải file lỗi import điểm danh", description = "Tải file lỗi sau khi import điểm danh thất bại.")
    public ResponseEntity<byte[]> downloadImportErrorFile(@PathVariable String token) {
        TemporaryFileDto file = attendanceService.getImportErrorFile(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(file.getContent());
    }

    @PutMapping("/bulk")
    @Operation(summary = "Điểm danh học sinh", description = "ập nhật điểm danh cho học sinh trong một lớp học.")
    public ResponseEntity<ApiResult<String>> bulkUpsert(@Valid @RequestBody AttendanceBulkUpsertRequest request) {
        return executeApiResult(() -> {
            attendanceService.bulkUpsert(request);
            return ApiResult.success(null, "Cập nhật điểm danh thành công");
        });
    }
}
