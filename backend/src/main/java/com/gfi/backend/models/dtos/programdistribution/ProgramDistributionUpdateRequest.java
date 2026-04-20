package com.gfi.backend.models.dtos.programdistribution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProgramDistributionUpdateRequest {
    @NotNull(message = "Tuần không được bỏ trống")
    private Integer weekNumber;

    private Integer orderNumber;
    private String periodPpct;

    @NotBlank(message = "Tên bài học không được bỏ trống")
    private String lessonName;

    private String note;
}
