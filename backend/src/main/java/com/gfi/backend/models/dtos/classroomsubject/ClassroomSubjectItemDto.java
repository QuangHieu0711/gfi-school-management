package com.gfi.backend.models.dtos.classroomsubject;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomSubjectItemDto {
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer subjectType;
    private Boolean selected;
}
