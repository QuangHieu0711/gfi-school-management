package com.gfi.backend.models.dtos.staff;

import lombok.Data;

@Data
public class TeacherAssignmentFilterDto {
    private Long staffId;
    private Long schoolYearId;
    private Long classId;
    private Long subjectId;
}
