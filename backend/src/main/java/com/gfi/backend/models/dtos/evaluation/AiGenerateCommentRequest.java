package com.gfi.backend.models.dtos.evaluation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiGenerateCommentRequest {
    @JsonProperty("grade_level")
    private String gradeLevel;

    @JsonProperty("subject_name")
    private String subjectName;

    @JsonProperty("term")
    private String term;

    @JsonProperty("week_no")
    private Integer weekNo;

    @JsonProperty("lesson_no")
    private Integer lessonNo;

    @JsonProperty("lesson_title")
    private String lessonTitle;

    @JsonProperty("learning_objective")
    private String learningObjective;

    @JsonProperty("evaluation")
    private String evaluation;

    @JsonProperty("attendance_status")
    private String attendanceStatus;

    @JsonProperty("attendance_code")
    private String attendanceCode;

    @JsonProperty("participation_level")
    private String participationLevel;

    @JsonProperty("behavior_tag")
    private String behaviorTag;

    @JsonProperty("textbook_series")
    private String textbookSeries;
}
