package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.attendance.AttendanceBulkUpsertRequest;
import com.gfi.backend.models.dtos.attendance.AttendanceMonthlyTableDto;

public interface AttendanceService {
    AttendanceMonthlyTableDto getMonthlyTable(Long classroomId, Integer year, Integer month, String sessionType);
    void bulkUpsert(AttendanceBulkUpsertRequest request);
}
