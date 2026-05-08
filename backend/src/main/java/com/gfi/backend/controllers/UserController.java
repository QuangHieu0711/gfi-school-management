package com.gfi.backend.controllers;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.user.UserCreateRequest;
import com.gfi.backend.models.dtos.user.UserDetailDto;
import com.gfi.backend.models.dtos.user.UserFilterDto;
import com.gfi.backend.models.dtos.user.UserListItemDto;
import com.gfi.backend.models.dtos.user.UserUpdateRequest;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller quản lý người dùng.
 * Xử lý HTTP requests cho các phép CRUD user.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Quản lý người dùng - User")
public class UserController extends ApiBaseController {

    private final UserService userService;

    @GetMapping("/role-options")
    @Operation(summary = "Danh sách vai trò cho thêm mới người dùng")
    public ResponseEntity<ApiResult<java.util.List<LookupItemDto>>> getRoleOptionsForCreateUser() {
        return executeApiResult(() ->
                ApiResult.success(userService.getRoleOptionsForCreateUser(), "Lấy danh sách vai trò thành công"));
    }

    @GetMapping("/staff-options")
    @Operation(summary = "Danh sách cán bộ chưa có tài khoản cho thêm mới người dùng")
    public ResponseEntity<ApiResult<java.util.List<com.gfi.backend.models.dtos.user.StaffOptionDto>>> getStaffOptionsForCreateUser() {
        return executeApiResult(() ->
                ApiResult.success(userService.getStaffOptionsForCreateUser(), "Lấy danh sách cán bộ thành công"));
    }

    /**
     * Tìm kiếm và phân trang users với filter.
     * Trả về list view chứa thông tin tối thiểu.
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResult<PageResponseDto<UserListItemDto, UserFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<UserFilterDto> request) {
        PageRequestDto<UserFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(
                () -> ApiResult.success(userService.search(safeRequest), "Hiển thị danh sách người dùng thành công"));
    }

        @PostMapping(value = "/export")
        @Operation(summary = "Xuất danh sách người dùng", description = "Xuất toàn bộ danh sách người dùng theo điều kiện tìm kiếm, hỗ trợ EXCEL hoặc PDF trong cùng một API.")
        public ResponseEntity<byte[]> export(
            @RequestBody(required = false) PageRequestDto<UserFilterDto> request,
            @RequestParam(defaultValue = "EXCEL") ExportType exportType) {
        PageRequestDto<UserFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        byte[] content = userService.export(safeRequest, exportType);

        String extension = exportType == ExportType.PDF ? "pdf" : "xlsx";
        String contentType = exportType == ExportType.PDF
            ? MediaType.APPLICATION_PDF_VALUE
            : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String fileName = "danh-sach-nguoi-dung." + extension;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString())
            .contentType(MediaType.parseMediaType(contentType))
            .body(content);
        }

    /**
     * Lấy chi tiết user theo ID.
     * Trả về đầy đủ thông tin user bao gồm quan hệ.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết người dùng", description = "Lấy thông tin người dùng theo id.")
    public ResponseEntity<ApiResult<UserDetailDto>> getById(@PathVariable Long id) {
        return executeApiResult(
                () -> ApiResult.success(userService.getById(id), "Hiển thị chi tiết người dùng thành công"));
    }

    /**
     * Tạo user mới.
     */
    @PostMapping
    @Operation(summary = "Thêm người dùng", description = "Tạo mới người dùng.")
    public ResponseEntity<ApiResult<UserDetailDto>> create(@Valid @RequestBody UserCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(userService.create(request), "Thêm người dùng thành công"));
    }

    /**
     * Cập nhật user hiện có.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Sửa người dùng", description = "Cập nhật người dùng theo id.")
    public ResponseEntity<ApiResult<UserDetailDto>> update(@PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return executeApiResult(
                () -> ApiResult.success(userService.update(id, request), "Cập nhật người dùng thành công"));
    }

    /**
     * Xóa (xóa mềm) user theo ID.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa người dùng", description = "Xóa người dùng theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            userService.delete(id);
            return ApiResult.success(null, "Xóa người dùng thành công");
        });
    }

    /**
     * Reset mật khẩu người dùng.
     * Tạo mật khẩu tạm thời và gửi về email.
     */
    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset mật khẩu", description = "Tạo mật khẩu tạm thời và gửi về email của người dùng.")
    public ResponseEntity<ApiResult<String>> resetPassword(@PathVariable Long id) {
        return executeApiResult(() -> {
            userService.resetPassword(id);
            return ApiResult.success(null, "Đã gửi mật khẩu mới về email của người dùng");
        });
    }
}
