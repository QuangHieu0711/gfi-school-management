package com.gfi.backend.models.dtos.programdistribution;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProgramDistributionDetailDto {
    private Long id;
    private Long schoolYearId;
    private String schoolYearName;
    private Long unitId;
    private String unitName;
    private Long classroomId;
    private String classroomName;
    private Long subjectId;
    private String subjectName;
    private Integer orderNumber;
    private Integer weekNumber;
    private String weekName;
    private String periodPpct;
    private String lessonName;
    private String note;
}
