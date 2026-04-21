package com.gfi.backend.models.dtos.attendance;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceDailySheetDto {
    private AttendanceClassroomDto classroom;
    private LocalDate attendanceDate;
    private String sessionType;
    private List<AttendanceDailyStudentDto> students;
}
