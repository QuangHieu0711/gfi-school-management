package com.gfi.backend.models.dtos.student;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentTransferClassRequest {

    @NotEmpty(message = "Danh sách học sinh không được để trống")
    private List<Long> studentIds;

    @NotNull(message = "Năm học đích không được để trống")
    private Long targetSchoolYearId;

    @NotNull(message = "Lớp đích không được để trống")
    private Long targetClassId;

    private LocalDate enrolledAt;

    private Integer status;

    private Boolean isRepeater;
}
