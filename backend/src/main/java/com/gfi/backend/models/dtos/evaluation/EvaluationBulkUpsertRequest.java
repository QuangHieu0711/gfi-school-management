package com.gfi.backend.models.dtos.evaluation;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluationBulkUpsertRequest {
    @NotNull
    private Long classroomId;

    @NotNull
    private Long subjectId;

    @NotNull
    private Long semesterId;

    @Valid
    @NotEmpty
    private List<EvaluationStudentUpsertItemDto> items;
}
