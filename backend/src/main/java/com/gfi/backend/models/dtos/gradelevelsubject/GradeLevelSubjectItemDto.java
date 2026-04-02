package com.gfi.backend.models.dtos.gradelevelsubject;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GradeLevelSubjectItemDto {
    private Long id;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer subjectType;
}
