package com.gfi.backend.models.dtos.student;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentTransferClassResultDto {
    private int transferredCount;
    private Long targetSchoolYearId;
    private String targetSchoolYearName;
    private Long targetClassId;
    private String targetClassName;
    private Boolean isRepeater;
    private List<Long> studentIds;
}
