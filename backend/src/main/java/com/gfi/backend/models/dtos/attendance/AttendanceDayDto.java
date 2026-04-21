package com.gfi.backend.models.dtos.attendance;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceDayDto {
    private LocalDate attendanceDate;
    private Integer dayOfMonth;
    private String dayOfWeek;
    private boolean weekend;
}
