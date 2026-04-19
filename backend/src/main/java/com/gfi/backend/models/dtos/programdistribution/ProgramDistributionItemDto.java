package com.gfi.backend.models.dtos.programdistribution;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProgramDistributionItemDto {
    private Long id;
    private Long schoolYearId;
    private Long semesterId;
    private Long classroomId;
    private Long subjectId;
    private Integer orderNumber;
    private Integer weekNumber;
    private String periodPpct;
    private String lessonName;
    private String note;
}
