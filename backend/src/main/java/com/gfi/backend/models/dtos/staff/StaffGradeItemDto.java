package com.gfi.backend.models.dtos.staff;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffGradeItemDto {
    private Long id;
    private String staffCode;
    private String fullName;
    private Long unitId;
    private Long gradeId;
}
