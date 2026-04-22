package com.gfi.backend.controllers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

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
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.dtos.staff.StaffCreateRequest;
import com.gfi.backend.models.dtos.staff.StaffDetailDto;
import com.gfi.backend.models.dtos.staff.StaffFilterDto;
import com.gfi.backend.models.dtos.staff.StaffGradeItemDto;
import com.gfi.backend.models.dtos.staff.StaffImportResultDto;
import com.gfi.backend.models.dtos.staff.StaffItemDto;
import com.gfi.backend.models.dtos.staff.StaffUpdateRequest;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.StaffCodeGeneratorService;
import com.gfi.backend.services.interfaces.StaffService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staffs")
@RequiredArgsConstructor
@Tag(name = "Quản lý cán bộ giảng viên - Staff")
public class StaffController extends ApiBaseController {

        private final StaffService staffService;
        private final StaffCodeGeneratorService staffCodeGeneratorService;

        @PostMapping("/search")
        @DataScoped(feature = "STAFF_PROFILE", action = ActionType.VIEW)
        @Operation(summary = "Danh sách cán bộ")
        public ResponseEntity<ApiResult<PageResponseDto<StaffItemDto, StaffFilterDto>>> search(
                        @RequestBody(required = false) PageRequestDto<StaffFilterDto> request) {
                PageRequestDto<StaffFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
                return executeApiResult(
                                () -> ApiResult.success(staffService.search(safeRequest),
                                                "Hiển thị danh sách cán bộ thành công"));
        }

        @PostMapping("/export")
        @DataScoped(feature = "STAFF_PROFILE", action = ActionType.VIEW)
        @Operation(summary = "Xuất danh sách cán bộ")
        public ResponseEntity<byte[]> export(
                        @RequestBody(required = false) PageRequestDto<StaffFilterDto> request,
                        @RequestParam(required = false) Long unitId,
                        @RequestParam(defaultValue = "EXCEL") ExportType exportType) {
                PageRequestDto<StaffFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
                byte[] content = staffService.export(safeRequest, unitId, exportType);
                String extension = exportType == ExportType.PDF ? "pdf" : "xlsx";
                String contentType = exportType == ExportType.PDF
                                ? MediaType.APPLICATION_PDF_VALUE
                                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                                .filename("danh-sach-can-bo." + extension, StandardCharsets.UTF_8)
                                                .build()
                                                .toString())
                                .contentType(MediaType.parseMediaType(contentType))
                                .body(content);
        }

        @PostMapping(value = "/excel-template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        @DataScoped(feature = "STAFF_PROFILE", action = ActionType.ADD)
        @Operation(summary = "Tải mẫu import cán bộ")
        public ResponseEntity<byte[]> exportExcelTemplate(@RequestParam Long unitId) {
                byte[] content = staffService.exportExcelTemplate(unitId);
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                                .filename("mau-import-can-bo.xlsx", StandardCharsets.UTF_8)
                                                .build()
                                                .toString())
                                .contentType(MediaType.parseMediaType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .body(content);
        }

        @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @DataScoped(feature = "STAFF_PROFILE", action = ActionType.ADD)
        @Operation(summary = "Import excel cán bộ")
        public ResponseEntity<ApiResult<StaffImportResultDto>> importExcel(@RequestParam Long unitId,
                        @RequestParam MultipartFile file) {
                return executeApiResult(() -> {
                        StaffImportResultDto result = staffService.importExcel(unitId, file);
                        String message = result.getFailedCount() > 0
                                        ? String.format("Import hoàn tất: %d thành công, %d lỗi",
                                                        result.getSuccessCount(), result.getFailedCount())
                                        : String.format("Import cán bộ thành công: %d bản ghi",
                                                        result.getSuccessCount());
                        return ApiResult.success(result, message);
                });
        }

        @GetMapping("/import-error-file/{token}")
        @DataScoped(feature = "STAFF_PROFILE", action = ActionType.ADD)
        @Operation(summary = "Tải file lỗi import cán bộ")
        public ResponseEntity<byte[]> downloadImportErrorFile(@PathVariable String token) {
                TemporaryFileDto file = staffService.getImportErrorFile(token);
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                                .filename(file.getFileName(), StandardCharsets.UTF_8)
                                                .build()
                                                .toString())
                                .contentType(MediaType.parseMediaType(file.getContentType()))
                                .body(file.getContent());
        }

        @GetMapping("/generate-code")
        @DataScoped(feature = "STAFF_PROFILE", action = ActionType.ADD)
        @Operation(summary = "Sinh mã cán bộ")
        public ResponseEntity<ApiResult<String>> generateStaffCode(@RequestParam Long unitId) {
                return executeApiResult(() -> {
                        Integer year = LocalDate.now().getYear();
                        String staffCode = staffCodeGeneratorService.generateStaffCode(unitId, year);
                        return ApiResult.success(staffCode, "Sinh mã cán bộ thành công");
                });
        }

        @GetMapping("/{id}")
        @DataScoped(feature = "STAFF_PROFILE", action = ActionType.VIEW)
        @Operation(summary = "Chi tiết cán bộ")
        public ResponseEntity<ApiResult<StaffDetailDto>> getById(@PathVariable Long id) {
                return executeApiResult(
                                () -> ApiResult.success(staffService.getById(id),
                                                "Hiển thị chi tiết cán bộ thành công"));
        }

        @PostMapping
        @DataScoped(feature = "STAFF_PROFILE", action = ActionType.ADD)
        @Operation(summary = "Thêm cán bộ")
        public ResponseEntity<ApiResult<StaffDetailDto>> create(@Valid @RequestBody StaffCreateRequest request) {
                return executeApiResult(
                                () -> ApiResult.success(staffService.create(request), "Thêm cán bộ thành công"));
        }

        @PutMapping("/{id}")
        @DataScoped(feature = "STAFF_PROFILE", action = ActionType.EDIT)
        @Operation(summary = "Sửa cán bộ")
        public ResponseEntity<ApiResult<StaffDetailDto>> update(@PathVariable Long id,
                        @Valid @RequestBody StaffUpdateRequest request) {
                return executeApiResult(
                                () -> ApiResult.success(staffService.update(id, request),
                                                "Cập nhật cán bộ thành công"));
        }

        @DeleteMapping("/{id}")
        @DataScoped(feature = "STAFF_PROFILE", action = ActionType.DELETE)
        @Operation(summary = "Xóa cán bộ")
        public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
                return executeApiResult(() -> {
                        staffService.delete(id);
                        return ApiResult.success(null, "Xóa cán bộ thành công");
                });
        }

        @GetMapping("/grade/{gradeId}")
        @DataScoped(feature = "STAFF_PROFILE", action = ActionType.VIEW)
        @Operation(summary = "Danh sách cán bộ theo khối")
        public ResponseEntity<ApiResult<java.util.List<StaffGradeItemDto>>> getByGrade(@PathVariable Long gradeId,
                        @RequestParam(required = false) Long unitId) {
                return executeApiResult(
                                () -> ApiResult.success(staffService.getByGrade(gradeId, unitId),
                                                "Hiển thị danh sách cán bộ theo khối thành công"));
        }
}
