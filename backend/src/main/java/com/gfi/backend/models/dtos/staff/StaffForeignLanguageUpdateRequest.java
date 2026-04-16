package com.gfi.backend.models.dtos.staff;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StaffForeignLanguageUpdateRequest {
    @NotBlank(message = "Ngoai ngu khong duoc de trong")
    private String languageName;

    private String languageLevel;
    private LocalDate issueDate;
    private String score;
    private String note;
}
