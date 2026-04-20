package com.gfi.backend.models.dtos.programdistribution;

import lombok.Data;

@Data
public class ProgramDistributionFilterDto {
    private Long schoolYearId;
    private Long unitId;
    private Long classroomId;
    private Long subjectId;
}
