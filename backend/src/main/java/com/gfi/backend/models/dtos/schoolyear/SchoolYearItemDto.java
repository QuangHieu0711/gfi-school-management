package com.gfi.backend.models.dtos.schoolyear;

import java.time.LocalDate;

import com.gfi.backend.models.enums.AcademicPeriodStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SchoolYearItemDto {
    private Long id;
    private String code;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private AcademicPeriodStatus status;
    private Boolean isCurrent;
    private String description;
}
