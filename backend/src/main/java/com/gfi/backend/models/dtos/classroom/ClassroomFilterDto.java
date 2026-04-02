package com.gfi.backend.models.dtos.classroom;

import lombok.Data;

@Data
public class ClassroomFilterDto {
    private String className;
    private Long unitId;
    private Long gradeLevelId;
    private Long schoolYearId;
    private Integer status;
}
