package com.gfi.backend.models.dtos.staff;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeacherAssignmentSearchResponse {
    private Integer pageNow;
    private Integer pageSize;
    private Long totalItems;
    private Integer totalPages;
    private List<TeacherAssignmentSearchStaffItemDto> items;
}
