package com.gfi.backend.models.dtos.staff;

import java.time.LocalDate;

import lombok.Data;

@Data
public class StaffJobHistoryFilterDto {
    private Long staffId;
    private Long unitId;
    private String departmentId;
    private String workingPositionId;
    private String titleId;
    private String employmentTypeId;
    private LocalDate fromDate;
    private LocalDate toDate;
}
