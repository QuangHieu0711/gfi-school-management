package com.gfi.backend.models.dtos.staff;

import java.time.LocalDate;

import lombok.Data;

@Data
public class StaffJobHistoryFilterDto {
    private Long staffId;
    private Long unitId;
    private Long departmentId;
    private Long workingPositionId;
    private Long titleId;
    private Long employmentTypeId;
    private LocalDate fromDate;
    private LocalDate toDate;
}
