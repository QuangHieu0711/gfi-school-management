package com.gfi.backend.models.dtos.attendance;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceBulkStudentRequest {
    @NotNull
    private Long studentId;

    private String studentName;
    private String status;
    private String note;
}
