package com.gfi.backend.services.implement;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.gfi.backend.models.entities.User;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.services.interfaces.ScopeValueResolver;

/**
 * Resolve scope IDs theo User (Người dùng)
 * 
 * Logic: kiểm tra user được phép manage những user ID nào
 * 
 * Trường hợp dùng:
 * - Quản lý viên có thể manage tài khoản trong phòng ban mình
 * - Admin có thể manage toàn bộ tài khoản
 */
@Component
public class UserScopeValueResolver implements ScopeValueResolver {

    @Override
    public ScopeType support() {
        return ScopeType.USER;
    }

    @Override
    public Set<Long> resolve(User user) {
        // TODO: implement logic để lấy danh sách user ID mà user được phép quản lý
        // Hiện tại trả rỗng - cần điền khi setup DB và có role/permission mapping
        return Set.of();
    }
}
