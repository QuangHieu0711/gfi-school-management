package com.gfi.backend.models.dtos.staff;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeacherAssignmentSearchStaffItemDto {
    private Long unitId;
    private Long schoolYearId;
    private Long semesterId;
    private String semesterName;
    private Long staffId;
    private String staffCode;
    private String staffName;
    private List<TeacherAssignmentAssignmentDto> assignments;
}
