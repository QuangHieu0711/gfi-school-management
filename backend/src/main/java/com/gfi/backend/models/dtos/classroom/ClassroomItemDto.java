package com.gfi.backend.models.dtos.classroom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomItemDto {
    private Long id;
    private String code;
    private String name;
    private String unitName;
    private String gradeLevelName;
    private String schoolYearName;
    private Integer status;
    private String description;
}
