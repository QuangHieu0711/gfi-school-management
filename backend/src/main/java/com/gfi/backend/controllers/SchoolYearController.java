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
import com.gfi.backend.models.dtos.schoolyear.SchoolYearCreateRequest;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearFilterDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearImportResultDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearItemDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearUpdateRequest;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.SchoolYearService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/school-years")
@RequiredArgsConstructor
@Tag(name = "Quản lý năm học - School Year")
public class SchoolYearController extends ApiBaseController {

    private final SchoolYearService schoolYearService;

    @PostMapping("/search")
    @Operation(summary = "Danh sách năm học", description = "Lấy danh sách năm học có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<SchoolYearItemDto, SchoolYearFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<SchoolYearFilterDto> request) {
        PageRequestDto<SchoolYearFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(
                () -> ApiResult.success(schoolYearService.search(safeRequest), "Hiển thị danh sách năm học thành công"));
    }

    @PostMapping("/export")
    @Operation(summary = "Xuất danh sách năm học", description = "Xuất danh sách cấu hình năm học, hỗ trợ EXCEL hoặc PDF.")
    public ResponseEntity<byte[]> export(
            @RequestBody(required = false) PageRequestDto<SchoolYearFilterDto> request,
            @RequestParam(defaultValue = "EXCEL") ExportType exportType) {
        PageRequestDto<SchoolYearFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        byte[] content = schoolYearService.export(safeRequest, exportType);
        String extension = exportType == ExportType.PDF ? "pdf" : "xlsx";
        String contentType = exportType == ExportType.PDF
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("cau-hinh-nam-hoc." + extension, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }

    @PostMapping(value = "/excel-template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Tải mẫu Excel năm học", description = "Tải file mẫu Excel để import cấu hình năm học và học kỳ.")
    public ResponseEntity<byte[]> exportExcelTemplate() {
        byte[] content = schoolYearService.exportExcelTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("mau-import-cau-hinh-nam-hoc.xlsx", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import Excel năm học", description = "Đọc file Excel và tạo hoặc cập nhật năm học, học kỳ.")
    public ResponseEntity<ApiResult<SchoolYearImportResultDto>> importExcel(@RequestParam MultipartFile file) {
        return executeApiResult(() -> {
            SchoolYearImportResultDto result = schoolYearService.importExcel(file);
            String message = result.getFailedCount() > 0
                    ? String.format("Import hoàn tất: %d thành công, %d lỗi", result.getSuccessCount(), result.getFailedCount())
                    : String.format("Import cấu hình năm học thành công: %d bản ghi", result.getSuccessCount());
            return ApiResult.success(result, message);
        });
    }

    @GetMapping("/import-error-file/{token}")
    @Operation(summary = "Tải file lỗi import năm học", description = "Tải file Excel phản hồi sau khi import cấu hình năm học có lỗi.")
    public ResponseEntity<byte[]> downloadImportErrorFile(@PathVariable String token) {
        TemporaryFileDto file = schoolYearService.getImportErrorFile(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(file.getContent());
    }

    @GetMapping("/options")
    @Operation(summary = "Danh sách năm học cho combobox", description = "Lấy danh sách id và tên năm học.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions() {
        return executeApiResult(
                () -> ApiResult.success(schoolYearService.getOptions(), "Hiển thị danh sách năm học thành công"));
    }

    @GetMapping("/current")
    @Operation(summary = "Năm học hiện hành", description = "Lấy thông tin năm học đang diễn ra.")
    public ResponseEntity<ApiResult<LookupItemDto>> getCurrentSchoolYear() {
        return executeApiResult(
                () -> ApiResult.success(schoolYearService.getCurrentSchoolYear(), "Hiển thị năm học hiện hành thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết năm học", description = "Lấy thông tin năm học theo id.")
    public ResponseEntity<ApiResult<SchoolYearItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(
                () -> ApiResult.success(schoolYearService.getById(id), "Hiển thị chi tiết năm học thành công"));
    }

    @PostMapping
    @Operation(summary = "Thêm năm học", description = "Tạo mới năm học.")
    public ResponseEntity<ApiResult<SchoolYearItemDto>> create(@Valid @RequestBody SchoolYearCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(schoolYearService.create(request), "Thêm năm học thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa năm học", description = "Cập nhật năm học theo id.")
    public ResponseEntity<ApiResult<SchoolYearItemDto>> update(@PathVariable Long id,
            @Valid @RequestBody SchoolYearUpdateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(schoolYearService.update(id, request), "Cập nhật năm học thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa năm học", description = "Xóa năm học theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            schoolYearService.delete(id);
            return ApiResult.success(null, "Xóa năm học thành công");
        });
    }
}
