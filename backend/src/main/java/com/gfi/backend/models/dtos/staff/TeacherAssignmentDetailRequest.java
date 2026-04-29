package com.gfi.backend.models.dtos.staff;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeacherAssignmentDetailRequest {
    @NotNull(message = "Đơn vị không được để trống")
    private Long unitId;

    @NotNull(message = "Cán bộ không được để trống")
    private Long staffId;

    @NotNull(message = "Năm học không được để trống")
    private Long schoolYearId;

    private Long semesterId;

    private Long subjectId;
}
