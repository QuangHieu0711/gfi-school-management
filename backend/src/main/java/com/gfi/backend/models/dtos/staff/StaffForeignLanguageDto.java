package com.gfi.backend.models.dtos.staff;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffForeignLanguageDto {
    private Long id;
    private Long staffId;
    private String languageName;
    private String languageLevel;
    private LocalDate issueDate;
    private String score;
    private String note;
}
