package com.gfi.backend.models.dtos.semester;

import com.gfi.backend.models.enums.AcademicPeriodStatus;

import lombok.Data;

@Data
public class SemesterFilterDto {
    private String semester;
    private Long schoolYearId;
    private AcademicPeriodStatus status;
    private Boolean isCurrent;
}
