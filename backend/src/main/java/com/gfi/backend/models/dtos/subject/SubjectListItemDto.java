package com.gfi.backend.models.dtos.subject;

import lombok.Builder;
import lombok.Data;

/**
 * DTO danh sách môn học (minimal data)
 */
@Data
@Builder
public class SubjectListItemDto {
    private Long id;
    private String code;
    private String name;
    private Integer type;
    private Integer status;
}
