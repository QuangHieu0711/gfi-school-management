package com.gfi.backend.models.dtos.weekconfig;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeekConfigItemDto {
    private Long id;
    private Long schoolYearId;
    private String schoolYearName;
    private Long semesterId;
    private String semesterName;
    private Integer weekNumber;
    private LocalDate startDate;
    private LocalDate endDate;
}
