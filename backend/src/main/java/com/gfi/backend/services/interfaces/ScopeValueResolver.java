package com.gfi.backend.services.interfaces;

import java.util.Set;

import com.gfi.backend.models.entities.User;
import com.gfi.backend.models.enums.ScopeType;

/**
 * Resolver để convert từ ScopeType thành danh sách ID thực tế
 * 
 * Ví dụ:
 * - UnitScopeValueResolver: độc lập, trả unit IDs
 * - ClassScopeValueResolver: độc lập, trả class IDs
 * - SelfScopeValueResolver: trả set chứa user ID của chính người dùng
 * 
 * Mục đích: tách biệt logic resolve cho từng loại scope
 * Sau này thêm scope type mới chỉ cần thêm resolver mới, không cần sửa nhiều file
 */
public interface ScopeValueResolver {
    
    /**
     * Kiểu scope mà resolver này hỗ trợ
     */
    ScopeType support();

    /**
     * Resolve scope IDs cho user
     * 
     * Ví dụ:
     * - UnitScopeValueResolver: trả [9] (user thuộc unit 9)
     * - SelfScopeValueResolver: trả [user.getId()]
     * 
     * @param user User cần resolve
     * @return set của scope IDs
     */
    Set<Long> resolve(User user);
}
