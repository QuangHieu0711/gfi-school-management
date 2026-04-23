package com.gfi.backend.models.dtos.evaluation;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationSheetDto {
    private Long classroomId;
    private String classroomName;
    private Long subjectId;
    private String subjectName;
    private Long semesterId;
    private String semesterName;
    private List<EvaluationSheetStudentDto> students;
}
