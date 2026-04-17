package com.gfi.backend.models.dtos.staff;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StaffJobHistoryUpdateRequest {
    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate fromDate;

    private LocalDate toDate;
    private Long unitId;
    private String departmentId;
    private String workingPositionId;
    private String titleId;
    private String employmentTypeId;
    private String decisionNo;
    private String note;
}
