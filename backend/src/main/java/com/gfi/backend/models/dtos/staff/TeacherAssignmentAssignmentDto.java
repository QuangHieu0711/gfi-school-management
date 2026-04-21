package com.gfi.backend.models.dtos.staff;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeacherAssignmentAssignmentDto {
    private Long subjectId;
    private String subjectName;
    private List<Long> classIds;
    private List<String> classNames;
}
