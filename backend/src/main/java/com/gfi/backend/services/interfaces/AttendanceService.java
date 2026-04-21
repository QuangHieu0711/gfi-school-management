package com.gfi.backend.services.interfaces;

import java.time.LocalDate;

import com.gfi.backend.models.dtos.attendance.AttendanceBulkUpsertRequest;
import com.gfi.backend.models.dtos.attendance.AttendanceDailySheetDto;
import com.gfi.backend.models.dtos.attendance.AttendanceMonthlySheetDto;
import com.gfi.backend.models.dtos.attendance.AttendanceRecordDto;
import com.gfi.backend.models.dtos.attendance.AttendanceUpsertRequest;

public interface AttendanceService {
    AttendanceMonthlySheetDto getMonthlySheet(Long classroomId, String month, String sessionType);
    AttendanceDailySheetDto getDailySheet(Long classroomId, LocalDate attendanceDate, String sessionType);
    AttendanceRecordDto upsert(AttendanceUpsertRequest request);
    void bulkUpsert(AttendanceBulkUpsertRequest request);
}
