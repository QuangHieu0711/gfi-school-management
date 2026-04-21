package com.gfi.backend.services.implement;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.gfi.backend.models.entities.Staff;
import com.gfi.backend.models.entities.User;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.services.interfaces.ScopeValueResolver;

/**
 * Resolve scope IDs theo Grade (Khối lớp).
 *
 * Với vai trò như tổ khối, hệ thống đang lưu khối phụ trách ngay trên hồ sơ staff
 * (`staff.gradeLevel`). Vì vậy GRADE scope sẽ resolve trực tiếp từ user -> staff -> gradeLevel.
 */
@Component
public class GradeScopeValueResolver implements ScopeValueResolver {

    @Override
    public ScopeType support() {
        return ScopeType.GRADE;
    }

    @Override
    public Set<Long> resolve(User user) {
        if (user == null) {
            return Set.of();
        }

        Staff staff = user.getStaff();
        if (staff == null || staff.getGradeLevel() == null || staff.getGradeLevel().getId() == null) {
            return Set.of();
        }

        return Set.of(staff.getGradeLevel().getId());
    }
}
