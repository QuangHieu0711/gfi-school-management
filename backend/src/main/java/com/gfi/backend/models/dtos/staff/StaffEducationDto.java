package com.gfi.backend.models.dtos.staff;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffEducationDto {
    private Long id;
    private Long staffId;
    private String schoolName;
    private String major;
    private String trainingForm;
    private String certificate;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String note;
}
