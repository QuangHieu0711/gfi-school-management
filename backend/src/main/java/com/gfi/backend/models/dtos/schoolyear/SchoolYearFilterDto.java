package com.gfi.backend.models.dtos.schoolyear;

import com.gfi.backend.models.enums.AcademicPeriodStatus;

import lombok.Data;

@Data
public class SchoolYearFilterDto {
    private String schoolYear;
    private AcademicPeriodStatus status;
    private Boolean isCurrent;
}
