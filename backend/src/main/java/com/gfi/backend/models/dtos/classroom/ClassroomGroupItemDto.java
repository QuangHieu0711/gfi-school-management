package com.gfi.backend.models.dtos.classroom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomGroupItemDto {
    private Long id;
    private String name;
}
