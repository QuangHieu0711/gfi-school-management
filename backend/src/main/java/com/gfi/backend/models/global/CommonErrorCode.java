package com.gfi.backend.models.global;

import lombok.Getter;

@Getter
public enum CommonErrorCode {
    INVALID_CREDENTIALS(1000, "Tên đăng nhập hoặc mật khẩu không chính xác"),
    USERNAME_ALREADY_EXISTS(1001, "ên đăng nhập đã tồn tại"),
    ROLE_NAME_ALREADY_EXISTS(1002, "ên vai trò đã tồn tại"),
    USER_NOT_FOUND(1003, "Không tìm thấy người dùng"),
    ROLE_NOT_FOUND(1004, "Không tìm thấy vai trò"),
    ROLE_IN_USE(1005, "Vai trò đang được gán cho người dùng, không thể xóa"),
    UNIT_CODE_ALREADY_EXISTS(1006, "Mã đơn vị đã tồn tại"),
    UNIT_NOT_FOUND(1007, "Không tìm thấy đơn vị"),
    UNIT_IN_USE(1008, "Đơn vị đang được gán cho người dùng, không thể xóa"),
    SCHOOL_YEAR_CODE_ALREADY_EXISTS(1009, "Mã năm học đã tồn tại"),
    SCHOOL_YEAR_NAME_ALREADY_EXISTS(1010, "Tên năm học đã tồn tại"),
    SCHOOL_YEAR_NOT_FOUND(1011, "Không tìm thấy năm học"),
    SCHOOL_YEAR_IN_USE(1012, "Năm học đang có học kỳ, không thể xóa"),
    SEMESTER_CODE_ALREADY_EXISTS(1013, "Mã học kỳ đã tồn tại trong năm học"),
    SEMESTER_NAME_ALREADY_EXISTS(1014, "Tên học kỳ đã tồn tại trong năm học"),
    SEMESTER_ORDER_ALREADY_EXISTS(1015, "Thứ tự học kỳ đã tồn tại trong năm học"),
    SEMESTER_NOT_FOUND(1016, "Không tìm thấy học kỳ"),
    INVALID_DATE_RANGE(1017, "Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc"),
    ACCESS_DENIED(1403, "Bạn không có quyền truy cập tài nguyên này"),
    BAD_REQUEST(1400, "Dữ liệu không hợp lệ"),
    INTERNAL_SERVER_ERROR(1500, " Lỗi hệ thống, vui lòng thử lại sau.");

    private final int code;
    private final String message;

    CommonErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
