package com.gfi.backend.models.dtos.staff;

import lombok.Data;

@Data
public class TeacherAssignmentSearchRequest {
    private Integer pageSize = 10;
    private Integer pageNow = 1;
    private TeacherAssignmentFilterDto filter;
}
