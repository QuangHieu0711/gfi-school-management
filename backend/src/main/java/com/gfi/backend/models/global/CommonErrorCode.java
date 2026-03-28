package com.gfi.backend.models.global;

import lombok.Getter;

@Getter
public enum CommonErrorCode {
    INVALID_CREDENTIALS(1000, "Tên đăng nhập hoặc mật khẩu không chính xác"),
    USERNAME_ALREADY_EXISTS(1001, "Tên đăng nhập đã tồn tại"),
    ROLE_NAME_ALREADY_EXISTS(1002, "Tên vai trò đã tồn tại"),
    USER_NOT_FOUND(1003, "Không tìm thấy user"),
    ROLE_NOT_FOUND(1004, "Không tìm thấy role"),
    ROLE_IN_USE(1005, "Vai trò đang được gán cho người dùng, không thể xóa"),
    UNIT_CODE_ALREADY_EXISTS(1006, "Mã đơn vị đã tồn tại"),
    UNIT_NOT_FOUND(1007, "Không tìm thấy đơn vị"),
    UNIT_IN_USE(1008, "Đơn vị đang được gán cho người dùng, không thể xóa"),
    ACCESS_DENIED(1403, "Bạn không có quyền truy cập tài nguyên này"),
    BAD_REQUEST(1400, "Dữ liệu không hợp lệ"),
    INTERNAL_SERVER_ERROR(1500, "Lỗi hệ thống, vui lòng thử lại sau.");

    private final int code;
    private final String message;

    CommonErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
