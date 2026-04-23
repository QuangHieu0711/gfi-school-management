package com.gfi.backend.models.dtos.attendance;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceMonthlyTableDto {
    private Long classroomId;
    private String classroomName;
    private String sessionType;
    private Integer year;
    private Integer month;
    private List<AttendanceMonthlyTableStudentDto> students;
}
