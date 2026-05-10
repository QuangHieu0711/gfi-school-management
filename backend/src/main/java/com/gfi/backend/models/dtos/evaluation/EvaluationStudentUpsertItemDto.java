package com.gfi.backend.models.dtos.evaluation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationStudentUpsertItemDto {
    @NotNull
    private Long studentId;

    private String midtermLevel;
    private Double midtermScore;
    private String midtermRemark;
    private String finalLevel;
    private Double finalScore;
    private String finalRemark;
}
