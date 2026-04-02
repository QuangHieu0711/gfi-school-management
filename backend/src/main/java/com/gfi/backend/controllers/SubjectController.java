package com.gfi.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.gfi.backend.models.dtos.subject.SubjectCreateRequest;
import com.gfi.backend.models.dtos.subject.SubjectFilterDto;
import com.gfi.backend.models.dtos.subject.SubjectItemDto;
import com.gfi.backend.models.dtos.subject.SubjectUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.SubjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@Tag(name = "Subject")
public class SubjectController extends ApiBaseController {

    private final SubjectService subjectService;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sach mon hoc", description = "Lay danh sach mon hoc co phan trang va filter.")
    public ResponseEntity<ApiResult<PageResponseDto<SubjectItemDto, SubjectFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<SubjectFilterDto> request) {
        PageRequestDto<SubjectFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(subjectService.search(safeRequest), "Hien thi danh sach mon hoc thanh cong"));
    }

    @GetMapping("/options")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sach mon hoc cho combobox", description = "Lay danh sach id va ten mon hoc.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions() {
        return executeApiResult(() -> ApiResult.success(subjectService.getOptions(), "Hien thi danh sach mon hoc thanh cong"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chi tiet mon hoc", description = "Lay thong tin mon hoc theo id.")
    public ResponseEntity<ApiResult<SubjectItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(subjectService.getById(id), "Hien thi chi tiet mon hoc thanh cong"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Them mon hoc", description = "Tao moi mon hoc.")
    public ResponseEntity<ApiResult<SubjectItemDto>> create(@Valid @RequestBody SubjectCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(subjectService.create(request), "Them mon hoc thanh cong"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sua mon hoc", description = "Cap nhat mon hoc theo id.")
    public ResponseEntity<ApiResult<SubjectItemDto>> update(@PathVariable Long id, @Valid @RequestBody SubjectUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(subjectService.update(id, request), "Cap nhat mon hoc thanh cong"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xoa mon hoc", description = "Xoa mon hoc theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            subjectService.delete(id);
            return ApiResult.success(null, "Xoa mon hoc thanh cong");
        });
    }
}
