package com.gfi.backend.models.dtos.attendance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceSummaryDto {
    private long presentCount;
    private long excusedCount;
    private long unexcusedCount;
    private long lateCount;
    private long totalAbsentCount;
}
