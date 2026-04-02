package com.gfi.backend.models.dtos.semester;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SemesterUpdateRequest {
    @NotNull(message = "Nam hoc khong duoc de trong")
    private Long schoolYearId;

    @NotBlank(message = "Ma hoc ky khong duoc de trong")
    @Size(max = 50, message = "Ma hoc ky toi da 50 ky tu")
    private String code;

    @NotBlank(message = "Ten hoc ky khong duoc de trong")
    @Size(max = 255, message = "Ten hoc ky toi da 255 ky tu")
    private String name;

    @NotNull(message = "Thu tu hoc ky khong duoc de trong")
    @Min(value = 1, message = "Thu tu hoc ky phai lon hon 0")
    private Integer semesterOrder;

    @NotNull(message = "Ngay bat dau khong duoc de trong")
    private LocalDate startDate;

    @NotNull(message = "Ngay ket thuc khong duoc de trong")
    private LocalDate endDate;

    @NotNull(message = "Trang thai khong duoc de trong")
    private Integer status;

    private Boolean isCurrent;

    @Size(max = 500, message = "Ghi chu toi da 500 ky tu")
    private String description;
}
