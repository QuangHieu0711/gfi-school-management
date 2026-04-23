package com.gfi.backend.models.dtos.attendance;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceMonthlyTableStudentDto {
    private Long studentId;
    private String studentCode;
    private String studentName;
    private Map<String, String> attendance;
}
