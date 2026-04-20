package com.gfi.backend.controllers;

import java.nio.charset.StandardCharsets;

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
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionDetailDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionFilterDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionImportResultDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionItemDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionUpdateRequest;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionCreateRequest;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.ProgramDistributionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/program-distributions")
@RequiredArgsConstructor
@Tag(name = "Phân phối chương trình - Program Distribution")
public class ProgramDistributionController extends ApiBaseController {

    private final ProgramDistributionService programDistributionService;

    @PostMapping("/search")
    @DataScoped(feature = "CURRICULUM_DISTRIBUTION", action = ActionType.VIEW)
    @Operation(
            summary = "Danh sách phân phối chương trình",
            description = "Lấy danh sách phân phối chương trình với phân trang")
    public ResponseEntity<ApiResult<PageResponseDto<ProgramDistributionItemDto, ProgramDistributionFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<ProgramDistributionFilterDto> request) {
        PageRequestDto<ProgramDistributionFilterDto> safeRequest = request == null 
            ? new PageRequestDto<>() 
            : request;
        return executeApiResult(() -> ApiResult.success(
                programDistributionService.search(safeRequest),
                "Lấy danh sách phân phối chương trình thành công"));
    }

    @PostMapping
    @DataScoped(feature = "CURRICULUM_DISTRIBUTION", action = ActionType.ADD)
    @Operation(
            summary = "Thêm mới phân phối chương trình",
            description = "Tạo một phân phối chương trình mới")
    public ResponseEntity<ApiResult<ProgramDistributionDetailDto>> create(
            @Valid @RequestBody ProgramDistributionCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(
                programDistributionService.create(request),
                "Thêm mới phân phối chương trình thành công"));
    }

    @PostMapping(value = "/excel-template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @DataScoped(feature = "CURRICULUM_DISTRIBUTION", action = ActionType.DOWNLOAD)
    @Operation(
            summary = "Tạo mẫu Excel phân phối chương trình",
            description = "Tạo file Excel mẫu theo năm học, đơn vị, lớp và môn học để giáo viên tải về và điền thông tin phân phối chương trình")
    public ResponseEntity<byte[]> exportExcelTemplate(
            @RequestParam Long schoolYearId,
            @RequestParam Long unitId,
            @RequestParam Long classroomId,
            @RequestParam Long subjectId) {
        byte[] content = programDistributionService.exportExcelTemplate(schoolYearId, unitId, classroomId, subjectId);
        String fileName = "phan-phoi-chuong-trinh-" + classroomId + "-" + subjectId + ".xlsx";

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
    @DataScoped(feature = "CURRICULUM_DISTRIBUTION", action = ActionType.ADD)
    @Operation(
            summary = "Import Excel phân phối chương trình",
            description = "Đọc file Excel và lưu danh sách phân phối chương trình")
    public ResponseEntity<ApiResult<ProgramDistributionImportResultDto>> importExcel(
            @RequestParam Long schoolYearId,
            @RequestParam Long unitId,
            @RequestParam Long classroomId,
            @RequestParam Long subjectId,
            @RequestParam MultipartFile file) {
        return executeApiResult(() -> ApiResult.success(
                programDistributionService.importExcel(schoolYearId, unitId, classroomId, subjectId, file),
                "Import phân phối chương trình thành công"));
    }

    @GetMapping("/{id}")
    @DataScoped(feature = "CURRICULUM_DISTRIBUTION", action = ActionType.VIEW)
    @Operation(
            summary = "Chi tiết phân phối chương trình",
            description = "Lấy thông tin chi tiết phân phối chương trình theo id")
    public ResponseEntity<ApiResult<ProgramDistributionDetailDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(
                programDistributionService.getById(id),
                "Lấy chi tiết phân phối chương trình thành công"));
    }

    @PutMapping("/{id}")
    @DataScoped(feature = "CURRICULUM_DISTRIBUTION", action = ActionType.EDIT)
    @Operation(
            summary = "Cập nhật phân phối chương trình",
            description = "Cập nhật thông tin phân phối chương trình")
    public ResponseEntity<ApiResult<ProgramDistributionDetailDto>> update(@PathVariable Long id,
            @Valid @RequestBody ProgramDistributionUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(
                programDistributionService.update(id, request),
                "Cập nhật phân phối chương trình thành công"));
    }

    @DeleteMapping("/{id}")
    @DataScoped(feature = "CURRICULUM_DISTRIBUTION", action = ActionType.DELETE)
    @Operation(
            summary = "Xóa phân phối chương trình",
            description = "Xóa phân phối chương trình theo id")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            programDistributionService.delete(id);
            return ApiResult.success(null, "Xóa phân phối chương trình thành công");
        });
    }
}
