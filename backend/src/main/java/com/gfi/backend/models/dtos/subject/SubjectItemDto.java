package com.gfi.backend.models.dtos.subject;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubjectItemDto {
    private Long id;
    private String code;
    private String name;
    private Integer type;
    private String description;
    private Integer status;
}
