package com.gfi.backend.models.dtos.classroom;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GradeLevelClassroomGroupDto {
    private Long gradeLevelId;
    private String gradeLevelName;
    private Integer gradeNumber;
    private List<ClassroomGroupItemDto> classes;
}
