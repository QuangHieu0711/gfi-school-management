package com.gfi.backend.models.dtos.weekconfig;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WeekConfigBulkUpdateItemRequest {
    @NotNull(message = "Id cấu hình tuần không được để trống")
    private Long id;

    @NotNull(message = "Tuần học không được để trống")
    @Min(value = 1, message = "Số tuần phải lớn hơn 0")
    private Integer weekNumber;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;
}
