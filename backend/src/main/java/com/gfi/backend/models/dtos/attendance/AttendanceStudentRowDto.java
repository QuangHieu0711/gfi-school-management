package com.gfi.backend.models.dtos.attendance;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceStudentRowDto {
    private Long studentId;
    private String studentCode;
    private String fullName;
    private Map<String, String> attendanceByDay;
    private AttendanceSummaryDto summary;
}
