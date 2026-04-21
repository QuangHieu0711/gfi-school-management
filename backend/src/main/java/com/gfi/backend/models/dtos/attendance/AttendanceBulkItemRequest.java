package com.gfi.backend.models.dtos.attendance;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceBulkItemRequest {
    @NotNull
    private Long studentId;

    @NotNull
    private LocalDate attendanceDate;

    private String attendanceStatus;
    private String note;
}
