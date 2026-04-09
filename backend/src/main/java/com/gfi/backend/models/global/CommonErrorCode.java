package com.gfi.backend.models.global;

import lombok.Getter;

@Getter
public enum CommonErrorCode {
    INVALID_CREDENTIALS(1000, "Tên đăng nhập hoặc mật khẩu không đúng"),
    USERNAME_ALREADY_EXISTS(1001, "Tên đăng nhập đã tồn tại"),
    ROLE_CODE_ALREADY_EXISTS(1002, "Mã vai trò đã tồn tại"),
    ROLE_NAME_ALREADY_EXISTS(1003, "Tên vai trò đã tồn tại"),
    USER_NOT_FOUND(1004, "Không tìm thấy người dùng"),
    ROLE_NOT_FOUND(1005, "Không tìm thấy vai trò"),
    ROLE_IN_USE(1006, "Vai trò đang được gán cho người dùng, không thể xóa"),
    UNIT_CODE_ALREADY_EXISTS(1007, "Mã đơn vị đã tồn tại"),
    UNIT_NOT_FOUND(1008, "Không tìm thấy đơn vị"),
    UNIT_IN_USE(1009, "Đơn vị đang được sử dụng, không thể xóa"),
    SCHOOL_YEAR_CODE_ALREADY_EXISTS(1010, "Mã năm học đã tồn tại"),
    SCHOOL_YEAR_NAME_ALREADY_EXISTS(1011, "Tên năm học đã tồn tại"),
    SCHOOL_YEAR_NOT_FOUND(1012, "Không tìm thấy năm học"),
    SCHOOL_YEAR_IN_USE(1013, "Năm học đang được sử dụng, không thể xóa"),
    SEMESTER_CODE_ALREADY_EXISTS(1014, "Mã học kỳ đã tồn tại trong năm học"),
    SEMESTER_NAME_ALREADY_EXISTS(1015, "Tên học kỳ đã tồn tại trong năm học"),
    SEMESTER_ORDER_ALREADY_EXISTS(1016, "Thứ tự học kỳ đã tồn tại trong năm học"),
    SEMESTER_NOT_FOUND(1017, "Không tìm thấy học kỳ"),
    INVALID_DATE_RANGE(1018, "Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc"),
    GRADE_LEVEL_CODE_ALREADY_EXISTS(1019, "Mã khối đã tồn tại"),
    GRADE_LEVEL_NAME_ALREADY_EXISTS(1020, "Tên khối đã tồn tại"),
    GRADE_LEVEL_NUMBER_ALREADY_EXISTS(1021, "Số khối đã tồn tại"),
    GRADE_LEVEL_NOT_FOUND(1022, "Không tìm thấy khối"),
    GRADE_LEVEL_IN_USE(1023, "Khối đang được sử dụng, không thể xóa"),
    CLASS_CODE_ALREADY_EXISTS(1024, "Mã lớp đã tồn tại trong đơn vị, khối và năm học"),
    CLASS_NAME_ALREADY_EXISTS(1025, "Tên lớp đã tồn tại trong đơn vị, khối và năm học"),
    CLASS_NOT_FOUND(1026, "Không tìm thấy lớp"),
    SUBJECT_CODE_ALREADY_EXISTS(1027, "Mã môn học đã tồn tại"),
    SUBJECT_NAME_ALREADY_EXISTS(1028, "Tên môn học đã tồn tại"),
    SUBJECT_NOT_FOUND(1029, "Không tìm thấy môn học"),
    SUBJECT_IN_USE(1030, "Môn học đang được cấu hình cho khối, không thể xóa"),
    STUDENT_CODE_ALREADY_EXISTS(1031, "Mã học sinh đã tồn tại"),
    STUDENT_NOT_FOUND(1032, "Không tìm thấy học sinh"),
    STUDENT_ENROLLMENT_SCHOOL_YEAR_MISMATCH(1033, "Lớp không thuộc năm học đã chọn"),
    STUDENT_ENROLLMENT_UNIT_MISMATCH(1034, "Lớp không thuộc đơn vị của học sinh"),
    STUDENT_ADDRESS_TYPE_DUPLICATED(1035, "Loại địa chỉ bị trùng"),
    STUDENT_GUARDIAN_TYPE_DUPLICATED(1036, "Loại người giám hộ bị trùng"),
    TEACHER_CODE_ALREADY_EXISTS(1037, "Mã giáo viên đã tồn tại"),
    TEACHER_NOT_FOUND(1038, "Không tìm thấy giáo viên/cán bộ"),
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
