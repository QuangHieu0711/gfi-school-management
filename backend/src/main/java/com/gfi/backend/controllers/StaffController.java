package com.gfi.backend.controllers;

import java.time.LocalDate;

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

import com.gfi.backend.controllers.annotations.DataScoped;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.staff.StaffCreateRequest;
import com.gfi.backend.models.dtos.staff.StaffDetailDto;
import com.gfi.backend.models.dtos.staff.StaffFilterDto;
import com.gfi.backend.models.dtos.staff.StaffGradeItemDto;
import com.gfi.backend.models.dtos.staff.StaffItemDto;
import com.gfi.backend.models.dtos.staff.StaffUpdateRequest;
import com.gfi.backend.models.enums.ActionType;
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
@Tag(name = "Quản lý cán bộ/giảng viên - Staff")
public class StaffController extends ApiBaseController {

    private final StaffService staffService;
    private final StaffCodeGeneratorService staffCodeGeneratorService;

    @PostMapping("/search")
    @DataScoped(feature = "STAFF_PROFILE", action = ActionType.VIEW)
    @Operation(summary = "Danh sách cán bộ", description = "Lấy danh sách cán bộ có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<StaffItemDto, StaffFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<StaffFilterDto> request) {
        PageRequestDto<StaffFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(
                () -> ApiResult.success(staffService.search(safeRequest), "Hiển thị danh sách cán bộ thành công"));
    }

    @GetMapping("/generate-code")
    @DataScoped(feature = "STAFF_PROFILE", action = ActionType.ADD)
    @Operation(summary = "Sinh mã cán bộ", description = "Sinh mã cán bộ tự động theo format: CB-{UNIT_CODE}-{YEAR}-{STT}.")
    public ResponseEntity<ApiResult<String>> generateStaffCode(@RequestParam Long unitId) {
        return executeApiResult(() -> {
            Integer year = LocalDate.now().getYear();
            String staffCode = staffCodeGeneratorService.generateStaffCode(unitId, year);
            return ApiResult.success(staffCode, "Sinh mã cán bộ thành công");
        });
    }

    @GetMapping("/{id}")
    @DataScoped(feature = "STAFF_PROFILE", action = ActionType.VIEW)
    @Operation(summary = "Chi tiết cán bộ", description = "Lấy thông tin chi tiết cán bộ theo id.")
    public ResponseEntity<ApiResult<StaffDetailDto>> getById(@PathVariable Long id) {
        return executeApiResult(
                () -> ApiResult.success(staffService.getById(id), "Hiển thị chi tiết cán bộ thành công"));
    }

    @PostMapping
    @DataScoped(feature = "STAFF_PROFILE", action = ActionType.ADD)
    @Operation(summary = "Thêm cán bộ", description = "Tạo mới cán bộ.")
    public ResponseEntity<ApiResult<StaffDetailDto>> create(@Valid @RequestBody StaffCreateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(staffService.create(request), "Thêm cán bộ thành công"));
    }

    @PutMapping("/{id}")
    @DataScoped(feature = "STAFF_PROFILE", action = ActionType.EDIT)
    @Operation(summary = "Sửa cán bộ", description = "Cập nhật thông tin cán bộ.")
    public ResponseEntity<ApiResult<StaffDetailDto>> update(@PathVariable Long id,
            @Valid @RequestBody StaffUpdateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(staffService.update(id, request), "Cập nhật cán bộ thành công"));
    }

    @DeleteMapping("/{id}")
    @DataScoped(feature = "STAFF_PROFILE", action = ActionType.DELETE)
    @Operation(summary = "Xóa cán bộ", description = "Xóa cán bộ theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            staffService.delete(id);
            return ApiResult.success(null, "Xóa cán bộ thành công");
        });
    }

    @GetMapping("/grade/{gradeId}")
    @DataScoped(feature = "STAFF_PROFILE", action = ActionType.VIEW)
    @Operation(summary = "Danh sách cán bộ theo khối", description = "Lấy danh sách cán bộ thuộc khối (grade).")
    public ResponseEntity<ApiResult<java.util.List<StaffGradeItemDto>>> getByGrade(@PathVariable Long gradeId,
            @RequestParam(required = false) Long unitId) {
        return executeApiResult(
                () -> ApiResult.success(staffService.getByGrade(gradeId, unitId), "Hiển thị danh sách cán bộ theo khối thành công"));
    }
}
