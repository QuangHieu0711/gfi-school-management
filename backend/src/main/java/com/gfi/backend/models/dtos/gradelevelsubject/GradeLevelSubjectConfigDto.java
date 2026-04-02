package com.gfi.backend.models.dtos.gradelevelsubject;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GradeLevelSubjectConfigDto {
    private Long gradeLevelId;
    private String gradeLevelName;
    private List<Long> subjectIds;
    private List<GradeLevelSubjectItemDto> subjects;
}
