package com.gfi.backend.models.dtos.attendance;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceBulkItemRequest {
    @NotNull
    private LocalDate attendanceDate;

    @Valid
    @NotEmpty
    private List<AttendanceBulkStudentRequest> students;
}
