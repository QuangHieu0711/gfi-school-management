package com.gfi.backend.models.dtos.weekconfig;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WeekConfigGenerateRequest {
    @NotNull(message = "Năm học không được để trống")
    private Long schoolYearId;

    @NotNull(message = "Học kỳ không được để trống")
    private Long semesterId;

    private Boolean forceRegenerate = false;
}
