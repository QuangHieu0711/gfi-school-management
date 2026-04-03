package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.models.dtos.common.FileUploadDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.student.StudentCreateRequest;
import com.gfi.backend.models.dtos.student.StudentFilterDto;
import com.gfi.backend.models.dtos.student.StudentItemDto;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.FileStorageService;
import com.gfi.backend.services.interfaces.StudentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Quan ly hoc sinh - Student")
public class StudentController extends ApiBaseController {

    private final StudentService studentService;
    private final FileStorageService fileStorageService;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách học sinh", description = "Lấy danh sách học sinh có phân trang và filter cho màn hình lưới.")
    public ResponseEntity<ApiResult<PageResponseDto<StudentItemDto, StudentFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<StudentFilterDto> request) {
        PageRequestDto<StudentFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(
                () -> ApiResult.success(studentService.search(safeRequest), "Hiển thị danh sách học sinh thành công"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm học sinh", description = "Tạo mới học sinh kèm thông tin nhập học, địa chỉ, người giám hộ và hồ sơ mở rộng.")
    public ResponseEntity<ApiResult<StudentItemDto>> create(@Valid @RequestBody StudentCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(studentService.create(request), "Thêm học sinh thành công"));
    }

    @PostMapping("/upload-avatar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tai anh hoc sinh", description = "Tai anh dai dien hoc sinh len server va tra ve avatarUrl.")
    public ResponseEntity<ApiResult<FileUploadDto>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return executeApiResult(() -> ApiResult.success(fileStorageService.storeStudentAvatar(file), "Tai anh thanh cong"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chi tiết học sinh", description = "Lấy thông tin học sinh theo id.")
    public ResponseEntity<ApiResult<StudentItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(
                () -> ApiResult.success(studentService.getById(id), "Hiển thị chi tiết học sinh thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật học sinh", description = "Cập nhật học sinh theo id.")
    public ResponseEntity<ApiResult<StudentItemDto>> update(@PathVariable Long id,
            @Valid @RequestBody StudentCreateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(studentService.update(id, request), "Cập nhật học sinh thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa học sinh", description = "Xóa học sinh theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            studentService.delete(id);
            return ApiResult.success(null, "Xóa học sinh thành công");
        });
    }
}
