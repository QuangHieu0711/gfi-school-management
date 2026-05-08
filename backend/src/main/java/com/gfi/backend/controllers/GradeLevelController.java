package com.gfi.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelCreateRequest;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelFilterDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelDetailDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelListItemDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.GradeLevelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller quản lý khối lớp.
 * Xử lý HTTP requests cho các phép CRUD khối lớp.
 */
@RestController
@RequestMapping("/api/grade-levels")
@RequiredArgsConstructor
@Tag(name = "Quản lý khối - GradeLevel")
public class GradeLevelController extends ApiBaseController {

    private final GradeLevelService gradeLevelService;

    /**
     * Tìm kiếm và phân trang khối lớp với filter.
     * Trả về list view chứa thông tin tối thiểu.
     */
    @PostMapping("/search")
    @Operation(summary = "Danh sách khối", description = "Lấy danh sách khối có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<GradeLevelListItemDto, GradeLevelFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<GradeLevelFilterDto> request) {
        PageRequestDto<GradeLevelFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(
                () -> ApiResult.success(gradeLevelService.search(safeRequest), "Hiển thị danh sách khối thành công"));
    }

    /**
     * Lấy danh sách khối lớp cho combobox.
     * Trả về id và tên khối lớp để hiển thị trong dropdown.
     */
    @GetMapping("/options")
    @Operation(summary = "Danh sách khối cho combobox", description = "Lấy danh sách id và tên khối.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions() {
        return executeApiResult(
                () -> ApiResult.success(gradeLevelService.getOptions(), "Hiển thị danh sách khối thành công"));
    }

    /**
     * Lấy chi tiết khối lớp theo ID.
     * Trả về đầy đủ thông tin khối lớp bao gồm quan hệ.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết khối", description = "Lấy thông tin khối theo id.")
    public ResponseEntity<ApiResult<GradeLevelDetailDto>> getById(@PathVariable Long id) {
        return executeApiResult(
                () -> ApiResult.success(gradeLevelService.getById(id), "Hiển thị chi tiết khối thành công"));
    }

    /**
     * Tạo khối lớp mới.
     * 
     * @param request DTO chứa thông tin cần thiết để tạo khối lớp.
     * @return
     */
    @PostMapping
    @Operation(summary = "Thêm khối", description = "Tạo mới khối.")
    public ResponseEntity<ApiResult<GradeLevelDetailDto>> create(@Valid @RequestBody GradeLevelCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(gradeLevelService.create(request), "Thêm khối thành công"));
    }

    /**
     * Cập nhật khối lớp hiện có.
     * 
     * @param id      ID của khối lớp cần cập nhật.
     * @param request DTO chứa thông tin cần thiết để cập nhật khối lớp.
     * @return
     */
    @PutMapping("/{id}")
    @Operation(summary = "Sửa khối", description = "Cập nhật khối theo id.")
    public ResponseEntity<ApiResult<GradeLevelDetailDto>> update(@PathVariable Long id,
            @Valid @RequestBody GradeLevelUpdateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(gradeLevelService.update(id, request), "Cập nhật khối thành công"));
    }

    /**
     * Xóa (xóa mềm) khối lớp theo ID.
     * 
     * @param id ID của khối lớp cần xóa.
     * @return
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa khối", description = "Xóa khối theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            gradeLevelService.delete(id);
            return ApiResult.success(null, "Xóa khối thành công");
        });
    }
    @PostMapping("/export")
    @com.gfi.backend.controllers.annotations.DataScoped(feature = "GRADE_CONFIG", action = com.gfi.backend.models.enums.ActionType.DOWNLOAD)
    @Operation(summary = "Xuất danh sách khối", description = "Xuất danh sách khối theo điều kiện tìm kiếm, hỗ trợ EXCEL hoặc PDF.")
    public ResponseEntity<byte[]> export(
            @RequestBody(required = false) PageRequestDto<GradeLevelFilterDto> request,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "EXCEL") com.gfi.backend.models.enums.ExportType exportType) {
        PageRequestDto<GradeLevelFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        byte[] content = gradeLevelService.export(safeRequest, exportType);
        String extension = exportType == com.gfi.backend.models.enums.ExportType.PDF ? "pdf" : "xlsx";
        String contentType = exportType == com.gfi.backend.models.enums.ExportType.PDF
                ? org.springframework.http.MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String fileName = "danh-sach-khoi." + extension;

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, org.springframework.http.ContentDisposition.attachment()
                        .filename(fileName, java.nio.charset.StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(content);
    }
}
