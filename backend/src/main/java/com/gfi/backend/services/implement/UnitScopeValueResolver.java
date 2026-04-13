package com.gfi.backend.services.implement;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.gfi.backend.models.entities.User;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.services.interfaces.ScopeValueResolver;

/**
 * Resolve scope IDs theo Unit
 * 
 * Logic: nếu user có gắn unit, thì lấy unit ID đó
 * 
 * Hiện tại hệ thống có user.unit, nên resolver này đơn giản chỉ cần:
 * - Nếu user có unit → trả [unit.id]
 * - Không có unit → trả Set.of() (có thể là admin, hoặc chưa gán unit)
 */
@Component
public class UnitScopeValueResolver implements ScopeValueResolver {

    @Override
    public ScopeType support() {
        return ScopeType.UNIT;
    }

    @Override
    public Set<Long> resolve(User user) {
        // Giả sử user có getUnitId() hoặc getUnit()
        // Nếu không, cần adjust theo actual User entity
        if (user == null || user.getUnit() == null) {
            return Set.of();
        }
        return Set.of(user.getUnit().getId());
    }
}
