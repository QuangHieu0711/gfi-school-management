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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.semester.SemesterFilterDto;
import com.gfi.backend.models.dtos.semester.SemesterCreateRequest;
import com.gfi.backend.models.dtos.semester.SemesterItemDto;
import com.gfi.backend.models.dtos.semester.SemesterUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.SemesterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/semesters")
@RequiredArgsConstructor
@Tag(name = "Quản lý học kỳ - Semester")
public class SemesterController extends ApiBaseController {

    private final SemesterService semesterService;

    /**
     * Danh sách học kỳ theo năm học.
     *
     * @param filter điều kiện lọc (chứa schoolYearId)
     * @return danh sách học kỳ phù hợp
     */
    @PostMapping("/search")
    @Operation(summary = "Danh sách học kỳ", description = "Lấy danh sách học kỳ theo mã năm học")
    public ResponseEntity<ApiResult<List<SemesterItemDto>>> search(
            @RequestBody(required = false) SemesterFilterDto filter) {
        SemesterFilterDto safeFilter = filter == null ? new SemesterFilterDto() : filter;
        return executeApiResult(() -> ApiResult.success(semesterService.search(safeFilter), "Hiển thị danh sách học kỳ thành công"));
    }

    /**
     * Danh sách học kỳ cho dropdown/combobox.
     *
     * @param schoolYearId ID của năm học (optional)
     * @return danh sách id và tên học kỳ
     */
    @GetMapping("/options")
    @Operation(summary = "Danh sách học kỳ cho combobox", description = "Lấy danh sách id và tên học kỳ.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions(@RequestParam(required = false) Long schoolYearId) {
        return executeApiResult(() -> ApiResult.success(semesterService.getOptions(schoolYearId), "Hiển thị danh sách học kỳ thành công"));
    }

    /**
     * Chi tiết học kỳ theo ID.
     *
     * @param id ID của học kỳ
     * @return thông tin chi tiết học kỳ
     */
    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết học kỳ", description = "Lấy thông tin học kỳ theo id.")
    public ResponseEntity<ApiResult<SemesterItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(semesterService.getById(id), "Hiển thị chi tiết học kỳ thành công"));
    }

    /**
     * Tạo học kỳ mới.
     *
     * @param request dữ liệu học kỳ cần tạo
     * @return thông tin chi tiết học kỳ vừa tạo
     */
    @PostMapping
    @Operation(summary = "Thêm học kỳ", description = "Tạo mới học kỳ.")
    public ResponseEntity<ApiResult<SemesterItemDto>> create(@Valid @RequestBody SemesterCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(semesterService.create(request), "Thêm học kỳ thành công"));
    }

    /**
     * Cập nhật học kỳ theo ID.
     *
     * @param id ID của học kỳ
     * @param request dữ liệu cần cập nhật
     * @return thông tin chi tiết học kỳ sau cập nhật
     */
    @PutMapping("/{id}")
    @Operation(summary = "Sửa học kỳ", description = "Cập nhật học kỳ theo id.")
    public ResponseEntity<ApiResult<SemesterItemDto>> update(@PathVariable Long id, @Valid @RequestBody SemesterUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(semesterService.update(id, request), "Cập nhật học kỳ thành công"));
    }

    /**
     * Xóa học kỳ theo ID.
     *
     * @param id ID của học kỳ
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa học kỳ", description = "Xóa học kỳ theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            semesterService.delete(id);
            return ApiResult.success(null, "Xóa học kỳ thành công");
        });
    }
}
