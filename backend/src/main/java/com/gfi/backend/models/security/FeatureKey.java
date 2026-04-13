package com.gfi.backend.models.security;

/**
 * Feature key - định danh tính năng trong hệ thống.
 * 
 * Sử dụng cho phân quyền security, không phụ thuộc vào:
 * - Menu UI (có thể đổi tên, thay đổi cấu trúc)
 * - URL (có thể đổi version, cấu trúc route)
 * 
 * Ổn định trong thời gian dài, chỉ thêm không bao giờ xả.
 */
public enum FeatureKey {
    USER_MANAGEMENT("Quản lý người dùng"),
    ACCOUNT_MANAGEMENT("Quản lý tài khoản"),
    ROLE_MANAGEMENT("Quản lý vai trò"),
    PERMISSION_MANAGEMENT("Quản lý quyền"),
    UNIT_MANAGEMENT("Quản lý đơn vị"),
    CLASS_MANAGEMENT("Quản lý lớp học"),
    STUDENT_PROFILE("Hồ sơ học sinh"),
    GRADE_CONFIG("Cấu hình khối"),
    GRADE_LEVEL_MANAGEMENT("Quản lý cấp học"),
    SUBJECT_MANAGEMENT("Quản lý môn học"),
    SCHOOL_YEAR_CONFIG("Cấu hình năm học"),
    SEMESTER_MANAGEMENT("Quản lý học kì"),
    ACADEMIC_MANAGEMENT("Quản lý học tập"),
    SYSTEM_CONFIG("Cấu hình hệ thống"),
    FUNCTION_MANAGEMENT("Quản lý chức năng"),
    SYSTEM_ADMIN("Quản trị hệ thống"),
    USER_ADMIN("Quản trị người dùng"),
    STUDENT("Học sinh");

    private final String description;

    FeatureKey(String description) {
        this.description = description;
    }

    /**
     * Lấy code của feature (với enum này chính là name())
     */
    public String getCode() {
        return name();
    }

    /**
     * Lấy mô tả để hiển thị
     */
    public String getDescription() {
        return description;
    }
}
