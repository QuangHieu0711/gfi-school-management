package com.gfi.backend.models.dtos.staff;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TeacherAssignmentItemDto {
    private Long id;
    private Long staffId;
    private Long schoolYearId;
    private Long classId;
    private Long subjectId;
    private Boolean isHomeroom;
    private Long departmentId;
    private BigDecimal teachingLoad;
}
