package com.gfi.backend.models.dtos.evaluation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluationBulkGenerateCommentItemDto {
    @NotNull(message = "Học sinh không được để trống")
    private Long studentId;

    @NotBlank(message = "Mức đánh giá không được để trống")
    private String evaluation; // Tốt/Hoàn thành/Cần cố gắng (hoặc T/H/C)

    private String participationLevel;

    private String behaviorTag;
}
