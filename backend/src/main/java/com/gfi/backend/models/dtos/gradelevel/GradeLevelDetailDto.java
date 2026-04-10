package com.gfi.backend.models.dtos.gradelevel;

import lombok.Builder;
import lombok.Data;

/**
 * DTO chi tiết khối (full data).
 */
@Data
@Builder
public class GradeLevelDetailDto {
    private Long id;
    private String code;
    private String name;
    private Integer gradeNumber;
    private Integer status;
    private String description;
}
