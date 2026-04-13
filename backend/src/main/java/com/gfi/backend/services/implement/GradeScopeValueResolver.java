package com.gfi.backend.services.implement;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.gfi.backend.models.entities.User;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.services.interfaces.ScopeValueResolver;

/**
 * Resolve scope IDs theo Grade (Khối lớp)
 * 
 * Logic: tùy theo business logic
 * Có thể là:
 * - Giáo viên được phân công khối nào → lấy khối đó
 * - Hoặc query từ database các lớp mà user được phân công
 */
@Component
public class GradeScopeValueResolver implements ScopeValueResolver {

    @Override
    public ScopeType support() {
        return ScopeType.GRADE;
    }

    @Override
    public Set<Long> resolve(User user) {
        // TODO: implement logic để lấy danh sách grade ID mà user được phân công
        // Hiện tại trả rỗng - cần điền khi setup DB và chắc chắn có relation
        return Set.of();
    }
}
