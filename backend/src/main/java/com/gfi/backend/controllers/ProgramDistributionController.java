package com.gfi.backend.controllers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionImportResultDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionItemDto;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.ProgramDistributionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/program-distributions")
@RequiredArgsConstructor
@Tag(name = "Phân phối chương trình - Program Distribution")
public class ProgramDistributionController extends ApiBaseController {

        private final ProgramDistributionService programDistributionService;

        @GetMapping("/list")
        @Operation(summary = "Danh sách phân phối chương trình", description = "Lấy danh sách phân phối chương trình theo năm học, học kỳ, lớp và môn học")
        public ResponseEntity<ApiResult<List<ProgramDistributionItemDto>>> getList(
                        @RequestParam Long schoolYearId,
                        @RequestParam Long semesterId,
                        @RequestParam Long classroomId,
                        @RequestParam Long subjectId) {
                return executeApiResult(() -> ApiResult.success(
                                programDistributionService.findList(schoolYearId, semesterId, classroomId, subjectId),
                                "Lấy danh sách phân phối chương trình thành công"));
        }

        @PostMapping(value = "/excel-template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        @Operation(summary = "Tạo mẫu Excel phân phối chương trình", description = "Tạo file Excel mẫu theo năm học, học kỳ, lớp và môn học")
        public ResponseEntity<byte[]> exportExcelTemplate(
                        @RequestParam Long schoolYearId,
                        @RequestParam Long semesterId,
                        @RequestParam Long classroomId,
                        @RequestParam Long subjectId) {
                byte[] content = programDistributionService.exportExcelTemplate(schoolYearId, semesterId, classroomId,
                                subjectId);
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
        @Operation(summary = "Import Excel phân phối chương trình", description = "Đọc file Excel và lưu danh sách phân phối chương trình")
        public ResponseEntity<ApiResult<ProgramDistributionImportResultDto>> importExcel(
                        @RequestParam Long schoolYearId,
                        @RequestParam Long classroomId,
                        @RequestParam Long subjectId,
                        @RequestParam MultipartFile file) {
                return executeApiResult(() -> ApiResult.success(
                                programDistributionService.importExcel(schoolYearId, classroomId, subjectId,
                                                file),
                                "Import phân phối chương trình thành công"));
        }
}
