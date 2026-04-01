package com.gfi.backend.models.dtos.schoolyear;

import java.time.LocalDate;

import com.gfi.backend.models.enums.AcademicPeriodStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SchoolYearCreateRequest {
    @NotBlank(message = "Ma nam hoc khong duoc de trong")
    @Size(max = 50, message = "Ma nam hoc toi da 50 ky tu")
    private String code;

    @NotBlank(message = "Ten nam hoc khong duoc de trong")
    @Size(max = 255, message = "Ten nam hoc toi da 255 ky tu")
    private String name;

    @NotNull(message = "Ngay bat dau khong duoc de trong")
    private LocalDate startDate;

    @NotNull(message = "Ngay ket thuc khong duoc de trong")
    private LocalDate endDate;

    @NotNull(message = "Trang thai khong duoc de trong")
    private AcademicPeriodStatus status;

    private Boolean isCurrent;

    @Size(max = 500, message = "Ghi chu toi da 500 ky tu")
    private String description;
}
