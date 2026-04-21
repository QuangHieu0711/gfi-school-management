package com.gfi.backend.models.dtos.staff;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeacherAssignmentItemDto {
    private Long id;
    private Long staffId;
    private Long schoolYearId;
    private Long semesterId;
    private Long classId;
    private Long subjectId;
}
