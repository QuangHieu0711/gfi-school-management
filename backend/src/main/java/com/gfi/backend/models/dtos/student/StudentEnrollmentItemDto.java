package com.gfi.backend.models.dtos.student;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentEnrollmentItemDto {
    private Long id;
    private Long schoolYearId;
    private String schoolYearName;
    private Long classId;
    private String className;
    private LocalDate enrolledAt;
    private Integer status;
    private Boolean isRepeater;
    private String sessionsPerWeek;
    private String studyMode;
    private Boolean isBoarding;
    private Boolean isTwoSessionsPerDay;
}
