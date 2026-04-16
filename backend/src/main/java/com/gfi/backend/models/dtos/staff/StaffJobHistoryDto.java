package com.gfi.backend.models.dtos.staff;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffJobHistoryDto {
    private Long id;
    private Long staffId;
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
