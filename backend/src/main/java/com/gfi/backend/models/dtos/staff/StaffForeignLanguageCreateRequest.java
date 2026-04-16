package com.gfi.backend.models.dtos.staff;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StaffForeignLanguageCreateRequest {
    @NotNull(message = "Can bo khong duoc de trong")
    private Long staffId;

    @NotBlank(message = "Ngoai ngu khong duoc de trong")
    private String languageName;

    private String languageLevel;
    private LocalDate issueDate;
    private String score;
    private String note;
}
