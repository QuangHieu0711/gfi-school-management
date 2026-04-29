package com.gfi.backend.models.enums;

/**
 * Loại scope - quyết định data scope theo chiều nào
 * 
 * ALL: không hạn chế dữ liệu
 * UNIT: hạn chế theo đơn vị học
 * GRADE: hạn chế theo khối lớp
 * CLASS: hạn chế theo lớp học
 * STAFF: hạn chế theo giáo viên/cán bộ
 * USER: hạn chế theo người dùng tạo ra
 * SELF: chỉ dữ liệu của chính người dùng đó
 */
public enum ScopeType {
    ALL("Toàn bộ"),
    UNIT("Đơn vị"),
    GRADE("Khối lớp"),
    CLASS("Lớp học"),
    STAFF("Giáo viên"),
    USER("Người dùng"),
    SELF("Chính mình");

    private final String displayName;

    ScopeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
