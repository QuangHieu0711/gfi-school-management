package com.gfi.backend.controllers;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.attendance.AttendanceBulkUpsertRequest;
import com.gfi.backend.models.dtos.attendance.AttendanceDailySheetDto;
import com.gfi.backend.models.dtos.attendance.AttendanceMonthlySheetDto;
import com.gfi.backend.models.dtos.attendance.AttendanceRecordDto;
import com.gfi.backend.models.dtos.attendance.AttendanceUpsertRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.AttendanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Điểm danh - Attendance")
public class AttendanceController extends ApiBaseController {

    private final AttendanceService attendanceService;

    @GetMapping("/monthly-sheet")
    @Operation(summary = "Bảng điểm danh theo tháng", description = "Lấy dữ liệu điểm danh theo tháng để render grid điểm danh.")
    public ResponseEntity<ApiResult<AttendanceMonthlySheetDto>> getMonthlySheet(
            @RequestParam Long classroomId,
            @RequestParam String month,
            @RequestParam String sessionType) {
        return executeApiResult(() -> ApiResult.success(
                attendanceService.getMonthlySheet(classroomId, month, sessionType),
                "Lấy bảng điểm danh tháng thành công"));
    }

    @GetMapping("/daily-sheet")
    @Operation(summary = "Bảng điểm danh theo ngày", description = "Lấy dữ liệu điểm danh theo ngày cho một lớp.")
    public ResponseEntity<ApiResult<AttendanceDailySheetDto>> getDailySheet(
            @RequestParam Long classroomId,
            @RequestParam LocalDate attendanceDate,
            @RequestParam String sessionType) {
        return executeApiResult(() -> ApiResult.success(
                attendanceService.getDailySheet(classroomId, attendanceDate, sessionType),
                "Lấy bảng điểm danh ngày thành công"));
    }

    @PutMapping
    @Operation(summary = "Lưu một ô điểm danh", description = "Thêm mới hoặc cập nhật một bản ghi điểm danh.")
    public ResponseEntity<ApiResult<AttendanceRecordDto>> upsert(@Valid @RequestBody AttendanceUpsertRequest request) {
        return executeApiResult(() -> ApiResult.success(
                attendanceService.upsert(request),
                "Lưu điểm danh thành công"));
    }

    @PutMapping("/bulk")
    @Operation(summary = "Lưu điểm danh hàng loạt", description = "Thêm mới hoặc cập nhật nhiều bản ghi điểm danh trong một lần gửi.")
    public ResponseEntity<ApiResult<String>> bulkUpsert(@Valid @RequestBody AttendanceBulkUpsertRequest request) {
        return executeApiResult(() -> {
            attendanceService.bulkUpsert(request);
            return ApiResult.success(null, "Lưu điểm danh hàng loạt thành công");
        });
    }
}
