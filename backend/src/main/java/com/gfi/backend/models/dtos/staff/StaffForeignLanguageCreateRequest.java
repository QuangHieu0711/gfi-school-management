package com.gfi.backend.models.dtos.staff;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StaffForeignLanguageCreateRequest {
    @NotNull(message = "Cán bộ không được để trống")
    private Long staffId;

    @NotBlank(message = "Ngoại ngữ không được để trống")
    private String languageName;

    private String languageLevel;
    private LocalDate issueDate;
    private String score;
    private String note;
}
