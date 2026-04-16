package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.staff.StaffJobHistoryCreateRequest;
import com.gfi.backend.models.dtos.staff.StaffJobHistoryDto;
import com.gfi.backend.models.dtos.staff.StaffJobHistoryFilterDto;
import com.gfi.backend.models.dtos.staff.StaffJobHistoryUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.StaffJobHistoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staff-job-histories")
@RequiredArgsConstructor
@Tag(name = "Quá trình công tác cán bộ - Staff Job History")
public class StaffJobHistoryController extends ApiBaseController {

    private final StaffJobHistoryService staffJobHistoryService;

    @PostMapping("/search")
    @Operation(summary = "Danh sách quá trình công tác", description = "Lấy danh sách quá trình công tác có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<StaffJobHistoryDto, StaffJobHistoryFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<StaffJobHistoryFilterDto> request) {
        PageRequestDto<StaffJobHistoryFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(staffJobHistoryService.search(safeRequest),
                "Hiển thị danh sách quá trình công tác thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết quá trình công tác", description = "Lấy chi tiết quá trình công tác theo id.")
    public ResponseEntity<ApiResult<StaffJobHistoryDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(staffJobHistoryService.getById(id),
                "Hiển thị chi tiết quá trình công tác thành công"));
    }

    @PostMapping
    @Operation(summary = "Thêm quá trình công tác", description = "Thêm mới quá trình công tác cho cán bộ.")
    public ResponseEntity<ApiResult<StaffJobHistoryDto>> create(
            @Valid @RequestBody StaffJobHistoryCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(staffJobHistoryService.create(request),
                "Thêm quá trình công tác thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật quá trình công tác", description = "Cập nhật quá trình công tác của cán bộ.")
    public ResponseEntity<ApiResult<StaffJobHistoryDto>> update(@PathVariable Long id,
            @Valid @RequestBody StaffJobHistoryUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(staffJobHistoryService.update(id, request),
                "Cập nhật quá trình công tác thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa quá trình công tác", description = "Xóa quá trình công tác theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            staffJobHistoryService.delete(id);
            return ApiResult.success(null, "Xóa quá trình công tác thành công");
        });
    }
}
