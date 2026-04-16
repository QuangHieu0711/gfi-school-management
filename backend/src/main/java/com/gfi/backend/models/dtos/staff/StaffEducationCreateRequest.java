package com.gfi.backend.models.dtos.staff;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StaffEducationCreateRequest {
    @NotNull(message = "Can bo khong duoc de trong")
    private Long staffId;

    @NotBlank(message = "Ten co so khong duoc de trong")
    private String schoolName;

    private String major;
    private String trainingForm;
    private String certificate;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String note;
}
