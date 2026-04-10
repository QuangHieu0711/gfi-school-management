package com.gfi.backend.models.dtos.classroom;

import lombok.Builder;
import lombok.Data;

/**
 * DTO danh sách lớp học
 */
@Data
@Builder
public class ClassroomListItemDto {
    private Long id;
    private String code;
    private String name;
    private String unitName;
    private String gradeLevelName;
    private String schoolYearName;
    private Integer status;
}
