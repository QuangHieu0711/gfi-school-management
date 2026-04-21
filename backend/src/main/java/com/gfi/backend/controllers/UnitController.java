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

import com.gfi.backend.controllers.annotations.DataScoped;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.unit.UnitCreateRequest;
import com.gfi.backend.models.dtos.unit.UnitDetailDto;
import com.gfi.backend.models.dtos.unit.UnitFilterDto;
import com.gfi.backend.models.dtos.unit.UnitImportResultDto;
import com.gfi.backend.models.dtos.unit.UnitListItemDto;
import com.gfi.backend.models.dtos.unit.UnitUpdateRequest;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.UnitService;
import com.gfi.backend.services.interfaces.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
@Tag(name = "Quản lý đơn vị - Unit")
public class UnitController extends ApiBaseController {

    private final UnitService unitService;
    private final UserService userService;

    @PostMapping("/search")
    @DataScoped(feature = "UNIT_MANAGEMENT", action = ActionType.VIEW)
    @Operation(summary = "Danh sách đơn vị", description = "Lấy danh sách đơn vị có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<UnitListItemDto, UnitFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<UnitFilterDto> request) {
        PageRequestDto<UnitFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(
                () -> ApiResult.success(unitService.search(safeRequest), "Hiển thị danh sách đơn vị thành công"));
    }

    @PostMapping("/export")
    @DataScoped(feature = "UNIT_MANAGEMENT", action = ActionType.DOWNLOAD)
    @Operation(summary = "Xuất danh sách đơn vị", description = "Xuất danh sách đơn vị theo điều kiện tìm kiếm, hỗ trợ EXCEL hoặc PDF.")
    public ResponseEntity<byte[]> export(
            @RequestBody(required = false) PageRequestDto<UnitFilterDto> request,
            @RequestParam(defaultValue = "EXCEL") ExportType exportType) {
        PageRequestDto<UnitFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        byte[] content = unitService.export(safeRequest, exportType);
        String extension = exportType == ExportType.PDF ? "pdf" : "xlsx";
        String contentType = exportType == ExportType.PDF
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String fileName = "danh-sach-don-vi." + extension;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }

    @GetMapping("/options")
    @DataScoped(feature = "UNIT_MANAGEMENT", action = ActionType.VIEW)
    @Operation(summary = "Danh sách đơn vị cho combobox", description = "Lấy danh sách id và tên đơn vị.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions() {
        return executeApiResult(
                () -> ApiResult.success(unitService.getOptions(), "Hiển thị danh sách đơn vị thành công"));
    }

    @GetMapping("/user-creation-options")
    @Operation(summary = "Danh sách đơn vị phân quyền dữ liệu", description = "Lấy danh sách unit user được phép gán khi tạo tài khoản mới.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getUnitOptionsForCreateUser() {
        return executeApiResult(
                () -> ApiResult.success(userService.getUnitOptionsForCreateUser(), "Lấy danh sách đơn vị thành công"));
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @DataScoped(feature = "UNIT_MANAGEMENT", action = ActionType.ADD)
    @Operation(summary = "Import Excel đơn vị", description = "Đọc file Excel và tạo/cập nhật danh sách đơn vị.")
    public ResponseEntity<ApiResult<UnitImportResultDto>> importExcel(@RequestParam MultipartFile file) {
        return executeApiResult(() -> ApiResult.success(unitService.importExcel(file), "Import đơn vị thành công"));
    }

    @GetMapping("/{id}")
    @DataScoped(feature = "UNIT_MANAGEMENT", action = ActionType.VIEW, scopeExpression = "#id")
    @Operation(summary = "Chi tiết đơn vị", description = "Lấy thông tin đơn vị theo id.")
    public ResponseEntity<ApiResult<UnitDetailDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(unitService.getById(id), "Hiển thị chi tiết đơn vị thành công"));
    }

    @PostMapping
    @DataScoped(feature = "UNIT_MANAGEMENT", action = ActionType.ADD)
    @Operation(summary = "Thêm đơn vị", description = "Tạo mới đơn vị.")
    public ResponseEntity<ApiResult<UnitDetailDto>> create(@Valid @RequestBody UnitCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(unitService.create(request), "Thêm đơn vị thành công"));
    }

    @PutMapping("/{id}")
    @DataScoped(feature = "UNIT_MANAGEMENT", action = ActionType.EDIT, scopeExpression = "#id")
    @Operation(summary = "Sửa đơn vị", description = "Cập nhật đơn vị theo id.")
    public ResponseEntity<ApiResult<UnitDetailDto>> update(@PathVariable Long id,
            @Valid @RequestBody UnitUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(unitService.update(id, request), "Cập nhật đơn vị thành công"));
    }

    @DeleteMapping("/{id}")
    @DataScoped(feature = "UNIT_MANAGEMENT", action = ActionType.DELETE, scopeExpression = "#id")
    @Operation(summary = "Xóa đơn vị", description = "Xóa đơn vị theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            unitService.delete(id);
            return ApiResult.success(null, "Xóa đơn vị thành công");
        });
    }
}
