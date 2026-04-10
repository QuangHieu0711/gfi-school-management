package com.gfi.backend.models.dtos.gradelevel;

import lombok.Builder;
import lombok.Data;

/**
 * DTO danh sách khối (minimal data).
 */
@Data
@Builder
public class GradeLevelListItemDto {
    private Long id;
    private String code;
    private String name;
    private Integer gradeNumber;
    private Integer status;
}
