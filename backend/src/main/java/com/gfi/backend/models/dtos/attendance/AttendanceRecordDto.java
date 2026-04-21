package com.gfi.backend.models.dtos.attendance;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceRecordDto {
    private Long id;
    private Long classroomId;
    private Long studentId;
    private LocalDate attendanceDate;
    private String sessionType;
    private String attendanceStatus;
    private String note;
}
