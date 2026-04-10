package com.gfi.backend.models.dtos.subject;

import lombok.Builder;
import lombok.Data;

/**
 * DTO chi tiết môn học (full data)
 */
@Data
@Builder
public class SubjectDetailDto {
    private Long id;
    private String code;
    private String name;
    private Integer type;
    private String description;
    private Integer status;
}
