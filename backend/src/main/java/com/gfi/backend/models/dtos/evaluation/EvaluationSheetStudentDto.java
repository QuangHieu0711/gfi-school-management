package com.gfi.backend.models.dtos.evaluation;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationSheetStudentDto {
    private Long studentId;
    private String studentCode;
    private String studentName;
    private String midtermLevel;
    private Double midtermScore;
    private String midtermRemark;
    private String finalLevel;
    private Double finalScore;
    private String finalRemark;
}
