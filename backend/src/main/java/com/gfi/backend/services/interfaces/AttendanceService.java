package com.gfi.backend.services.interfaces;

import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.models.dtos.attendance.AttendanceBulkUpsertRequest;
import com.gfi.backend.models.dtos.attendance.AttendanceImportResultDto;
import com.gfi.backend.models.dtos.attendance.AttendanceMonthlyTableDto;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.enums.ExportType;

public interface AttendanceService {
    AttendanceMonthlyTableDto getMonthlyTable(Long classroomId, Integer year, Integer month, String sessionType);
    byte[] export(Long classroomId, Integer year, Integer month, String sessionType, ExportType exportType);
    byte[] exportExcelTemplate(Long classroomId, Integer year, Integer month, String sessionType);
    AttendanceImportResultDto importExcel(Long classroomId, Integer year, Integer month, String sessionType, MultipartFile file);
    TemporaryFileDto getImportErrorFile(String token);
    void bulkUpsert(AttendanceBulkUpsertRequest request);
}
