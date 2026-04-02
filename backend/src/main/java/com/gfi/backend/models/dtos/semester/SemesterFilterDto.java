package com.gfi.backend.models.dtos.semester;

import lombok.Data;

@Data
public class SemesterFilterDto {
    private Long schoolYearId;
    private Integer status;
    private Boolean isCurrent;
}
