package com.gfi.backend.models.dtos.semester;

import java.time.LocalDate;

import com.gfi.backend.models.enums.AcademicPeriodStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SemesterItemDto {
    private Long id;
    private Long schoolYearId;
    private String schoolYearCode;
    private String schoolYearName;
    private String code;
    private String name;
    private Integer semesterOrder;
    private LocalDate startDate;
    private LocalDate endDate;
    private AcademicPeriodStatus status;
    private Boolean isCurrent;
    private String description;
}
