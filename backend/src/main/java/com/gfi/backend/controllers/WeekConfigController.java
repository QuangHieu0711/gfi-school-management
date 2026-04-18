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

import com.gfi.backend.models.dtos.weekconfig.WeekConfigBulkUpdateRequest;
import com.gfi.backend.models.dtos.weekconfig.WeekConfigGenerateRequest;
import com.gfi.backend.models.dtos.weekconfig.WeekConfigItemDto;
import com.gfi.backend.models.dtos.weekconfig.WeekConfigUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.WeekConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/week-configs")
@RequiredArgsConstructor
@Tag(name = "Quản lý cấu hình tuần học - Week Config")
public class WeekConfigController extends ApiBaseController {

    private final WeekConfigService weekConfigService;

    @GetMapping
    @Operation(summary = "Danh sách cấu hình tuần", description = "Lấy danh sách cấu hình tuần học theo năm học và học kỳ")
    public ResponseEntity<ApiResult<List<WeekConfigItemDto>>> getWeekConfigs(
            @RequestParam Long schoolYearId,
            @RequestParam(required = false) Long semesterId) {
        return executeApiResult(() -> ApiResult.success(
                weekConfigService.getWeekConfigs(schoolYearId, semesterId),
                "Hiển thị danh sách cấu hình tuần thành công"));
    }

    @PostMapping("/generate")
    @Operation(summary = "Sinh tự động cấu hình tuần", description = "Sinh danh sách tuần từ ngày bắt đầu đến ngày kết thúc học kỳ")
    public ResponseEntity<ApiResult<List<WeekConfigItemDto>>> generate(@Valid @RequestBody WeekConfigGenerateRequest request) {
        return executeApiResult(() -> ApiResult.success(
                weekConfigService.generate(request),
                "Sinh cau hinh tuan thanh cong"));
    }

    @PutMapping("/{id}")
        @Operation(summary = "Sửa cấu hình tuần", description = "Cập nhật thông tin tuần học")
    public ResponseEntity<ApiResult<WeekConfigItemDto>> update(@PathVariable Long id,
            @Valid @RequestBody WeekConfigUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(
                weekConfigService.update(id, request),
                "Cập nhật cấu hình tuần thành công"));
    }

    @PostMapping("/bulk-update")
    @Operation(summary = "Lưu danh sách cấu hình tuần", description = "Cập nhật nhiều dòng cấu hình tuần cùng lúc")
    public ResponseEntity<ApiResult<List<WeekConfigItemDto>>> bulkUpdate(
            @Valid @RequestBody WeekConfigBulkUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(
                weekConfigService.bulkUpdate(request),
                "Cập nhật danh sách cấu hình tuần thành công"));
    }

    @DeleteMapping("/by-semester/{semesterId}")
    @Operation(summary = "Xóa cấu hình tuần theo học kỳ", description = "Xóa cấu hình cụ thể theo học kỳ để sinh lại")
    public ResponseEntity<ApiResult<String>> deleteBySemester(@PathVariable Long semesterId) {
        return executeApiResult(() -> {
            weekConfigService.deleteBySemester(semesterId);
            return ApiResult.success(null, "Xóa cấu hình tuần theo học kỳ thành công");
        });
    }
}
