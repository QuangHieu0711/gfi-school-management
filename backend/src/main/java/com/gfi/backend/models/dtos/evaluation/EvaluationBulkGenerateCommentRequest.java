package com.gfi.backend.models.dtos.evaluation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EvaluationBulkGenerateCommentRequest {
    @NotNull(message = "Lớp học không được để trống")
    private Long classroomId;
    
    @NotNull(message = "Môn học không được để trống")
    private Long subjectId;
    
    @NotBlank(message = "Nửa học kỳ/Học kỳ không được để trống")
    private String term; // VD: Giữa HK1 (GK1), Cuối HK1 (CK1)
    
    @NotEmpty(message = "Danh sách học sinh không được để trống")
    @Valid
    private List<EvaluationBulkGenerateCommentItemDto> items;
}
