package com.gfi.backend.models.dtos.staff;

import java.time.LocalDate;

import lombok.Data;

@Data
public class StaffForeignLanguageFilterDto {
    private Long staffId;
    private String languageName;
    private String languageLevel;
    private LocalDate issueDate;
    private String score;
}
