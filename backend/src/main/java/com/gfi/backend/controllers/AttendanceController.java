package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.attendance.AttendanceBulkUpsertRequest;
import com.gfi.backend.models.dtos.attendance.AttendanceMonthlyTableDto;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.AttendanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
@Tag(name = "Điểm danh - Attendance")
public class AttendanceController extends ApiBaseController {

    private final AttendanceService attendanceService;

    @GetMapping("/monthly-table")
    @Operation(summary = "Danh sách điểm danh theo tháng", description = "Lấy dữ liệu điểm danh của một lớp học theo tháng.")
    public ResponseEntity<ApiResult<AttendanceMonthlyTableDto>> getMonthlyTable(
            @RequestParam Long classroomId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam String sessionType) {
        return executeApiResult(() -> ApiResult.success(
                attendanceService.getMonthlyTable(classroomId, year, month, sessionType),
                "Lấy dữ liệu điểm danh theo tháng thành công"));
    }

    @PutMapping("/bulk")
    @Operation(summary = "Điểm danh học sinh", description = "Cập nhật điểm danh cho học sinh trong một lớp học.")
    public ResponseEntity<ApiResult<String>> bulkUpsert(@Valid @RequestBody AttendanceBulkUpsertRequest request) {
        return executeApiResult(() -> {
            attendanceService.bulkUpsert(request);
            return ApiResult.success(null, "Cập nhật điểm danh thành công");
        });
    }
}
