package com.gfi.backend.models.security;

/**
 * Action key - định danh hành động người dùng trong hệ thống.
 * 
 * Kết hợp với FeatureKey để tạo thành phân quyền đầy đủ.
 * Ví dụ: USER_MANAGEMENT + VIEW = quyền xem danh sách người dùng
 */
public enum ActionKey {
    VIEW("Xem"),
    ADD("Thêm mới"),
    EDIT("Chỉnh sửa"),
    DELETE("Xóa"),
    DOWNLOAD("Tải xuống"),
    CONFIG("Cấu hình");

    private final String description;

    ActionKey(String description) {
        this.description = description;
    }

    /**
     * Lấy code của action (với enum này chính là name())
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
