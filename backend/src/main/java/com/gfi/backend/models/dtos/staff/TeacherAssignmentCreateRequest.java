package com.gfi.backend.models.dtos.staff;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class TeacherAssignmentCreateRequest {
    @NotNull(message = "Đơn vị không được để trống")
    private Long unitId;

    @NotNull(message = "Cán bộ không được để trống")
    private Long staffId;

    @NotNull(message = "Năm học không được để trống")
    private Long schoolYearId;

    @NotNull(message = "Há»c ká»³ khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng")
    private Long semesterId;

    @Valid
    @NotEmpty(message = "Danh sách phân công không được để trống")
    private List<SubjectAssignmentRequest> assignments;

    @Data
    public static class SubjectAssignmentRequest {
        @NotNull(message = "Môn học không được để trống")
        private Long subjectId;

        @NotEmpty(message = "Danh sách lớp không được để trống")
        private List<@NotNull(message = "Lớp học không hợp lệ") Long> classIds;
    }
}
