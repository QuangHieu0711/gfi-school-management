package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.dashboard.DashboardStatsDto;

/**
 * Service interface cho Dashboard thống kê tổng hợp.
 */
public interface DashboardService {

    /**
     * Lấy số liệu thống kê cho dashboard.
     * Dữ liệu sẽ được lọc theo phạm vi quyền dữ liệu của người dùng hiện tại.
     *
     * @param unitId  (optional) Lọc theo đơn vị cụ thể; null = tất cả đơn vị trong phạm vi quyền
     * @return DTO chứa toàn bộ số liệu thống kê
     */
    DashboardStatsDto getStats(Long unitId);
}
