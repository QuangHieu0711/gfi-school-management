package com.gfi.backend.models.dtos.attendance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceDailyStudentDto {
    private Long studentId;
    private String studentCode;
    private String fullName;
    private String attendanceStatus;
    private String note;
}
