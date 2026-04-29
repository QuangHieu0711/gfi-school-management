package com.gfi.backend.models.dtos.staff;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeacherAssignmentStaffClassResponse {
    private Long classId;
    private String classCode;
    private String className;
    private Long schoolYearId;
    private String schoolYearName;
    private List<SubjectItem> subjects;

    @Data
    @Builder
    public static class SubjectItem {
        private Long assignmentId;
        private Long subjectId;
        private String subjectCode;
        private String subjectName;
        private Long semesterId;
        private String semesterName;
    }
}
