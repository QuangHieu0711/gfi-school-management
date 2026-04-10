package com.gfi.backend.repositories.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.gfi.backend.models.dtos.user.UserFilterDto;
import com.gfi.backend.models.entities.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * Builder tạo Specification cho query User entity.
 * Đóng gói logic query cho tìm kiếm/filter.
 * Giúp UserService tập trung vào business logic.
 */
@Component
public class UserSpecification {

    /**
     * Tạo Specification<User> dựa trên filter cho trước.
     * Join với role và unit để filter.
     * 
     * @param filter tiêu chí filter
     * @return Specification<User> cho JPA query
     */
    public Specification<User> buildSpecification(UserFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> roleJoin = root.join("role", JoinType.LEFT);
            Join<Object, Object> unitJoin = root.join("unit", JoinType.LEFT);

            // Tìm kiếm theo họ tên hoặc tên tài khoản
            if (hasText(filter.getFullName())) {
                String keyword = "%" + filter.getFullName().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), keyword),
                        cb.like(cb.lower(root.get("fullName")), keyword),
                        cb.like(cb.lower(roleJoin.get("email")), keyword)
                ));
            }
            
            // Tìm kiếm theo vai trò (roleId)
            if (filter.getRoleId() != null) {
                predicates.add(cb.equal(roleJoin.get("id"), filter.getRoleId()));
            }
            
            // Tìm kiếm theo đơn vị (unitId)
            if (filter.getUnitId() != null && !filter.getUnitId().isEmpty()) {
                predicates.add(unitJoin.get("id").in(filter.getUnitId()));
            }
            
            // Tìm kiếm theo trạng thái (status)
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            
            // Luôn tìm kiếm bỏ users đã xóa (xóa mềm)
            predicates.add(cb.equal(root.get("deletedFlag"), 0));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
