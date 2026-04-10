package com.gfi.backend.models.dtos.classroom;

import lombok.Builder;
import lombok.Data;

/**
 * DTO chi tiết lớp học (full data)
 */
@Data
@Builder
public class ClassroomDetailDto {
    private Long id;
    private String code;
    private String name;
    private Long unitId;
    private Long gradeLevelId;
    private Long schoolYearId;
    private Integer status;
    private String description;
}
