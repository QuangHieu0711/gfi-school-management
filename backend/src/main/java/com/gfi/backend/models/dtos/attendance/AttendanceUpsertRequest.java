package com.gfi.backend.models.dtos.attendance;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceUpsertRequest {
    @NotNull
    private Long classroomId;

    @NotNull
    private Long studentId;

    @NotNull
    private LocalDate attendanceDate;

    @NotBlank
    private String sessionType;

    private String attendanceStatus;
    private String note;
}
