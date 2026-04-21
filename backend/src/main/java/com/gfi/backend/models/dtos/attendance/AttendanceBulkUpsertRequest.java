package com.gfi.backend.models.dtos.attendance;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceBulkUpsertRequest {
    @NotNull
    private Long classroomId;

    @NotBlank
    private String sessionType;

    @Valid
    @NotEmpty
    private List<AttendanceBulkItemRequest> items;
}
