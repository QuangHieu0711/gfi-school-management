package com.gfi.backend.models.dtos.schoolyear;

import lombok.Data;

@Data
public class SchoolYearFilterDto {
    private String schoolYear;
    private Integer status;
    private Boolean isCurrent;
}
