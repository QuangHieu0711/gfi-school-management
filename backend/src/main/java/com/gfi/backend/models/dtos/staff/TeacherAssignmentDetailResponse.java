package com.gfi.backend.models.dtos.staff;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TeacherAssignmentDetailResponse {
    private Long unitId;
    private Long staffId;
    private Long schoolYearId;
    private Long semesterId;
    private String semesterName;
    private Long subjectId;
    private List<Long> classIds;
    private List<TeacherAssignmentAssignmentDto> assignments;
}
