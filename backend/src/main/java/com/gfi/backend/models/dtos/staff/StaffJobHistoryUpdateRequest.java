package com.gfi.backend.models.dtos.staff;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StaffJobHistoryUpdateRequest {
    @NotNull(message = "Ngay bat dau khong duoc de trong")
    private LocalDate fromDate;

    private LocalDate toDate;
    private Long unitId;
    private Long departmentId;
    private Long workingPositionId;
    private Long titleId;
    private Long employmentTypeId;
    private String decisionNo;
    private String note;
}
