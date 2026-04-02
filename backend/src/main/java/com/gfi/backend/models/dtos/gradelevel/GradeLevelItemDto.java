package com.gfi.backend.models.dtos.gradelevel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GradeLevelItemDto {
    private Long id;
    private String code;
    private String name;
    private Integer gradeNumber;
    private Integer status;
    private String description;
}
