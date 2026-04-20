package com.gfi.backend.models.dtos.programdistribution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProgramDistributionCreateRequest {
    @NotNull(message = "Năm học không được để trống")
    private Long schoolYearId;

    @NotNull(message = "Đơn vị không được để trống")
    private Long unitId;

    @NotNull(message = "Lớp không được để trống")
    private Long classroomId;

    @NotNull(message = "Môn học không được để trống")
    private Long subjectId;

    @NotNull(message = "Tuần không được để trống")
    private Integer weekNumber;

    private Integer orderNumber;

    private String periodPpct;

    @NotBlank(message = "Tên bài học không được để trống")
    private String lessonName;

    private String note;
}
