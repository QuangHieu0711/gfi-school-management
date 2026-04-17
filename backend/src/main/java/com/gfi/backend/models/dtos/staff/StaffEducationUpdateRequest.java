package com.gfi.backend.models.dtos.staff;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StaffEducationUpdateRequest {
    @NotBlank(message = "Tên cơ sở không được để trống")
    private String schoolName;

    private String major;
    private String trainingForm;
    private String certificate;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String note;
}
