package com.gfi.backend.models.dtos.evaluation;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluationEditWindowRequest {
    @NotNull
    private Long semesterId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
