package com.gfi.backend.models.dtos.staff;

import lombok.Data;

@Data
public class TeacherAssignmentFilterDto {
    private Long unitId;
    private String staffCode;
    private Long staffId;
    private Long schoolYearId;
    private Long semesterId;
    private Long classId;
    private Long subjectId;
}
