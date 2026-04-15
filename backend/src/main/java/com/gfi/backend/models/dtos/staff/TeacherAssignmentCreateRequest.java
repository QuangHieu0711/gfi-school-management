package com.gfi.backend.models.dtos.staff;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class TeacherAssignmentCreateRequest {
    @NotNull(message = "Cán bộ không được để trống")
    private Long staffId;

    @NotNull(message = "Năm học không được để trống")
    private Long schoolYearId;

    private Long classId;
    private Long subjectId;
    private Boolean isHomeroom;
    private Long departmentId;
    private BigDecimal teachingLoad;
    private String note;
}
