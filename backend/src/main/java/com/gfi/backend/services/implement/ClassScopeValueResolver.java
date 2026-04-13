package com.gfi.backend.services.implement;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.gfi.backend.models.entities.User;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.services.interfaces.ScopeValueResolver;

/**
 * Resolve scope IDs theo Class (Lớp học)
 * 
 * Logic: tùy theo business logic
 * Có thể là:
 * - Giáo viên được phân công những lớp nào → lấy các lớp đó
 * - Hoặc query từ database các lớp mà user được phân công
 */
@Component
public class ClassScopeValueResolver implements ScopeValueResolver {

    @Override
    public ScopeType support() {
        return ScopeType.CLASS;
    }

    @Override
    public Set<Long> resolve(User user) {
        // TODO: implement logic để lấy danh sách class ID mà user được phân công
        // Hiện tại trả rỗng - cần điền khi setup DB và chắc chắn có relation
        return Set.of();
    }
}
