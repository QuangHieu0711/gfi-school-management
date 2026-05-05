package com.gfi.backend.models.dtos.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO thống kê điểm danh theo tháng.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceMonthlyStatsDto {
    /** Nhãn tháng, ví dụ "01/2026" */
    private String month;
    /** Số lượt điểm danh có mặt */
    private long presentCount;
    /** Số lượt vắng */
    private long absentCount;
}
