package com.gfi.backend.models.dtos.attendance;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceMonthlySheetDto {
    private AttendanceClassroomDto classroom;
    private String month;
    private String sessionType;
    private List<AttendanceDayDto> days;
    private List<AttendanceStudentRowDto> students;
}
