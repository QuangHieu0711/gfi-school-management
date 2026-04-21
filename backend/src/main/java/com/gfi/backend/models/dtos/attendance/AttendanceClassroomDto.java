package com.gfi.backend.models.dtos.attendance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceClassroomDto {
    private Long id;
    private String code;
    private String name;
}
