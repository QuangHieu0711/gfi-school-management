package com.gfi.backend.models.dtos.student;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentEnrollmentCreateRequest {

    @NotNull(message = "Năm học không được để trống")
    private Long schoolYearId;

    @NotNull(message = "Lớp không được để trống")
    private Long classId;

    private LocalDate enrolledAt;

    private Integer status;

    private Boolean isRepeater;

    private Integer sessionsPerWeek;

    private Integer studyMode;

    private Boolean isBoarding;

    private Boolean isTwoSessionsPerDay;
}
