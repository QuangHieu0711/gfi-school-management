package com.gfi.backend.services.implement;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.gfi.backend.models.entities.User;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.services.interfaces.ScopeValueResolver;

/**
 * Resolve scope IDs theo SELF
 * 
 * Logic: trả về user ID của chính user đó
 * 
 * Dùng cho các trường hợp data chỉ thuộc về chính userCreator
 * Ví dụ: tài khoản người dùng chỉ có thể sửa tài khoản của chính mình
 */
@Component
public class SelfScopeValueResolver implements ScopeValueResolver {

    @Override
    public ScopeType support() {
        return ScopeType.SELF;
    }

    @Override
    public Set<Long> resolve(User user) {
        if (user == null || user.getId() == null) {
            return Set.of();
        }
        return Set.of(user.getId());
    }
}
