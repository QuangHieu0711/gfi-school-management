package com.gfi.backend.models.dtos.evaluation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluationGenerateCommentRequest {
    @NotNull(message = "Lớp học không được để trống")
    private Long classroomId;
    
    @NotNull(message = "Môn học không được để trống")
    private Long subjectId;
    
    @NotNull(message = "Học sinh không được để trống")
    private Long studentId;
    
    @NotBlank(message = "Nửa học kỳ/Học kỳ không được để trống")
    private String term; // VD: Giữa HK1 (GK1), Cuối HK1 (CK1)
    
    @NotBlank(message = "Mức đánh giá không được để trống")
    private String evaluation; // Tốt/Hoàn thành/Cần cố gắng (hoặc T/H/C)
    
    @NotBlank(message = "Mức độ tham gia không được để trống")
    private String participationLevel;
    
    @NotBlank(message = "Thẻ hành vi không được để trống")
    private String behaviorTag;
}
