package com.gfi.backend.models.dtos.subject;

import lombok.Data;

@Data
public class SubjectFilterDto {
    private String subject;
    private Integer type;
    private Integer status;
}
