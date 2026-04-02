package com.gfi.backend.models.dtos.classroomsubject;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomSubjectConfigDto {
    private Long classroomId;
    private String classroomName;
    private Long gradeLevelId;
    private String gradeLevelName;
    private List<Long> subjectIds;
    private List<ClassroomSubjectItemDto> subjects;
}
