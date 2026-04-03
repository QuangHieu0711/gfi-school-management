package com.gfi.backend.models.dtos.student;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentEnrollmentCreateRequest {

    @NotNull(message = "Nam hoc khong duoc de trong")
    private Long schoolYearId;

    @NotNull(message = "Lop khong duoc de trong")
    private Long classId;

    private LocalDate enrolledAt;

    private Integer status;

    private Boolean isRepeater;

    @Size(max = 50, message = "So buoi hoc toi da 50 ky tu")
    private String sessionsPerWeek;

    @Size(max = 50, message = "Hinh thuc hoc toi da 50 ky tu")
    private String studyMode;

    private Boolean isBoarding;

    private Boolean isTwoSessionsPerDay;
}
