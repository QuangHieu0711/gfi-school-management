package com.gfi.backend.models.dtos.evaluation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluationStudentUpsertItemDto {
    @NotNull
    private Long studentId;

    private String midtermLevel;
    private String midtermRemark;
    private String finalLevel;
    private String finalRemark;
}
