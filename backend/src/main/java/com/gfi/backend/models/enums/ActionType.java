package com.gfi.backend.models.enums;

/**
 * Loại hành động (quyền chức năng)
 * Quyết định được phép làm gì với dữ liệu
 */
public enum ActionType {
    VIEW("Xem"),
    ADD("Thêm"),
    EDIT("Sửa"),
    DELETE("Xóa"),
    DOWNLOAD("Tải xuống"),
    CONFIGURE("Cấu hình");

    private final String displayName;

    ActionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
