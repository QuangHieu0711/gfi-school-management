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

    @NotNull(message = "Há»c ká»³ khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng")
    private Long semesterId;

    @NotNull(message = "Môn học không được để trống")
    private Long subjectId;
}
