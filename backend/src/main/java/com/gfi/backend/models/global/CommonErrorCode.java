package com.gfi.backend.models.global;

import lombok.Getter;

@Getter
public enum CommonErrorCode {
    INVALID_CREDENTIALS(1000, "Ten dang nhap hoac mat khau khong dung"),
    USERNAME_ALREADY_EXISTS(1001, "Ten dang nhap da ton tai"),
    ROLE_CODE_ALREADY_EXISTS(1002, "Ma vai tro da ton tai"),
    ROLE_NAME_ALREADY_EXISTS(1003, "Ten vai tro da ton tai"),
    USER_NOT_FOUND(1004, "Khong tim thay nguoi dung"),
    ROLE_NOT_FOUND(1005, "Khong tim thay vai tro"),
    ROLE_IN_USE(1006, "Vai tro dang duoc gan, khong the xoa"),
    UNIT_CODE_ALREADY_EXISTS(1007, "Ma don vi da ton tai"),
    UNIT_NOT_FOUND(1008, "Khong tim thay don vi"),
    UNIT_IN_USE(1009, "Don vi dang duoc su dung, khong the xoa"),
    SCHOOL_YEAR_CODE_ALREADY_EXISTS(1010, "Ma nam hoc da ton tai"),
    SCHOOL_YEAR_NAME_ALREADY_EXISTS(1011, "Ten nam hoc da ton tai"),
    SCHOOL_YEAR_NOT_FOUND(1012, "Khong tim thay nam hoc"),
    SCHOOL_YEAR_IN_USE(1013, "Nam hoc dang duoc su dung, khong the xoa"),
    SEMESTER_CODE_ALREADY_EXISTS(1014, "Ma hoc ky da ton tai trong nam hoc"),
    SEMESTER_NAME_ALREADY_EXISTS(1015, "Ten hoc ky da ton tai trong nam hoc"),
    SEMESTER_ORDER_ALREADY_EXISTS(1016, "Thu tu hoc ky da ton tai trong nam hoc"),
    SEMESTER_NOT_FOUND(1017, "Khong tim thay hoc ky"),
    INVALID_DATE_RANGE(1018, "Ngay bat dau phai nho hon hoac bang ngay ket thuc"),
    GRADE_LEVEL_CODE_ALREADY_EXISTS(1019, "Ma khoi da ton tai"),
    GRADE_LEVEL_NAME_ALREADY_EXISTS(1020, "Ten khoi da ton tai"),
    GRADE_LEVEL_NUMBER_ALREADY_EXISTS(1021, "So khoi da ton tai"),
    GRADE_LEVEL_NOT_FOUND(1022, "Khong tim thay khoi"),
    GRADE_LEVEL_IN_USE(1023, "Khoi dang duoc su dung, khong the xoa"),
    CLASS_CODE_ALREADY_EXISTS(1024, "Ma lop da ton tai trong don vi, khoi va nam hoc"),
    CLASS_NAME_ALREADY_EXISTS(1025, "Ten lop da ton tai trong don vi, khoi va nam hoc"),
    CLASS_NOT_FOUND(1026, "Khong tim thay lop"),
    SUBJECT_CODE_ALREADY_EXISTS(1027, "Ma mon hoc da ton tai"),
    SUBJECT_NAME_ALREADY_EXISTS(1028, "Ten mon hoc da ton tai"),
    SUBJECT_NOT_FOUND(1029, "Khong tim thay mon hoc"),
    SUBJECT_IN_USE(1030, "Mon hoc dang duoc cau hinh, khong the xoa"),
    STUDENT_CODE_ALREADY_EXISTS(1031, "Ma hoc sinh da ton tai"),
    STUDENT_NOT_FOUND(1032, "Khong tim thay hoc sinh"),
    STUDENT_ENROLLMENT_SCHOOL_YEAR_MISMATCH(1033, "Lop khong thuoc nam hoc da chon"),
    STUDENT_ENROLLMENT_UNIT_MISMATCH(1034, "Lop khong thuoc don vi cua hoc sinh"),
    STUDENT_ADDRESS_TYPE_DUPLICATED(1035, "Loai dia chi bi trung"),
    STUDENT_GUARDIAN_TYPE_DUPLICATED(1036, "Loai nguoi giam ho bi trung"),
    TEACHER_CODE_ALREADY_EXISTS(1037, "Ma giao vien da ton tai"),
    TEACHER_NOT_FOUND(1038, "Khong tim thay giao vien/can bo"),
    MENU_CODE_ALREADY_EXISTS(1039, "Ma menu da ton tai"),
    MENU_NOT_FOUND(1040, "Khong tim thay menu"),
    MENU_IN_USE(1041, "Menu dang duoc gan permission, khong the xoa"),
    MENU_HAS_CHILDREN(1042, "Menu dang co menu con, khong the xoa"),
    PERMISSION_NOT_FOUND(1043, "Khong tim thay permission"),
    PERMISSION_ALREADY_EXISTS(1044, "Permission da ton tai cho role va menu nay"),
    ACCESS_DENIED(1403, "Ban khong co quyen truy cap tai nguyen nay"),
    BAD_REQUEST(1400, "Du lieu khong hop le"),
    INTERNAL_SERVER_ERROR(1500, "Loi he thong, vui long thu lai sau.");

    private final int code;
    private final String message;

    CommonErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
