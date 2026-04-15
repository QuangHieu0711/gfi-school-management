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
import com.gfi.backend.models.dtos.schoolyear.SchoolYearCreateRequest;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearFilterDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearItemDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearUpdateRequest;
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

    /**
     * Danh sách năm học với phân trang và filter.
     *
     * @param request yêu cầu tìm kiếm chứa điều kiện lọc và phân trang
     * @return trang danh sách năm học
     */
    @PostMapping("/search")
    @Operation(summary = "Danh sách năm học", description = "Lấy danh sách năm học có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<SchoolYearItemDto, SchoolYearFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<SchoolYearFilterDto> request) {
        PageRequestDto<SchoolYearFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(schoolYearService.search(safeRequest), "Hiển thị danh sách năm học thành công"));
    }

    /**
     * Danh sách năm học cho dropdown/combobox.
     *
     * @return danh sách id và tên năm học
     */
    @GetMapping("/options")
    @Operation(summary = "Danh sách năm học cho combobox", description = "Lấy danh sách id và tên năm học.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions() {
        return executeApiResult(() -> ApiResult.success(schoolYearService.getOptions(), "Hiển thị danh sách năm học thành công"));
    }

    /**
     * Lấy năm học hiện hành.
     *
     * @return thông tin id và name năm học hiện tại
     */
    @GetMapping("/current")
    @Operation(summary = "Năm học hiện hành", description = "Lấy thông tin năm học đang diễn ra.")
    public ResponseEntity<ApiResult<LookupItemDto>> getCurrentSchoolYear() {
        return executeApiResult(() -> ApiResult.success(schoolYearService.getCurrentSchoolYear(), "Hiển thị năm học hiện hành thành công"));
    }

    /**
     * Chi tiết năm học theo ID.
     *
     * @param id ID của năm học
     * @return thông tin chi tiết năm học
     */
    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết năm học", description = "Lấy thông tin năm học theo id.")
    public ResponseEntity<ApiResult<SchoolYearItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(schoolYearService.getById(id), "Hiển thị chi tiết năm học thành công"));
    }

    /**
     * Tạo năm học mới.
     *
     * @param request dữ liệu năm học cần tạo
     * @return thông tin chi tiết năm học vừa tạo
     */
    @PostMapping
    @Operation(summary = "Thêm năm học", description = "Tạo mới năm học.")
    public ResponseEntity<ApiResult<SchoolYearItemDto>> create(@Valid @RequestBody SchoolYearCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(schoolYearService.create(request), "Thêm năm học thành công"));
    }

    /**
     * Cập nhật năm học theo ID.
     *
     * @param id ID của năm học
     * @param request dữ liệu cần cập nhật
     * @return thông tin chi tiết năm học sau cập nhật
     */
    @PutMapping("/{id}")
    @Operation(summary = "Sửa năm học", description = "Cập nhật năm học theo id.")
    public ResponseEntity<ApiResult<SchoolYearItemDto>> update(@PathVariable Long id, @Valid @RequestBody SchoolYearUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(schoolYearService.update(id, request), "Cập nhật năm học thành công"));
    }

    /**
     * Xóa năm học theo ID.
     *
     * @param id ID của năm học
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa năm học", description = "Xóa năm học theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            schoolYearService.delete(id);
            return ApiResult.success(null, "Xóa năm học thành công");
        });
    }
}
