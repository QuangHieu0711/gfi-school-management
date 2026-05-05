package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.dashboard.DashboardStatsDto;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller cho Dashboard thống kê tổng hợp.
 * Dữ liệu được lọc tự động theo phân quyền dữ liệu của người dùng.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard - Thống kê tổng hợp")
public class DashboardController extends ApiBaseController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Lấy số liệu thống kê tổng hợp cho Dashboard",
               description = "Trả về toàn bộ số liệu thống kê được lọc theo phân quyền dữ liệu. " +
                             "Có thể truyền unitId để lọc theo đơn vị cụ thể.")
    public ResponseEntity<ApiResult<DashboardStatsDto>> getStats(
            @RequestParam(required = false) Long unitId) {
        return executeApiResult(
                () -> ApiResult.success(dashboardService.getStats(unitId), "Lấy thống kê thành công"));
    }
}
