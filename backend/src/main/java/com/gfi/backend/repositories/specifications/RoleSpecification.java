package com.gfi.backend.repositories.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.gfi.backend.models.dtos.role.RoleFilterDto;
import com.gfi.backend.models.entities.Role;

import jakarta.persistence.criteria.Predicate;

/**
 * Specification builder cho Role query.
 * Tách biệt logic truy vấn từ service.
 */
@Component
public class RoleSpecification {

    /**
     * Xây dựng Specification từ filter conditions.
     * 
     * @param filter điều kiện lọc
     * @return Specification<Role> dùng cho repository query
     */
    public Specification<Role> buildSpecification(RoleFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Lọc theo code
            if (StringUtils.hasText(filter.getCode())) {
                String keyword = "%" + filter.getCode().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("code")), keyword));
            }

            // Lọc theo roleName hoặc description
            if (StringUtils.hasText(filter.getRoleName())) {
                String keyword = "%" + filter.getRoleName().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("roleName")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)));
            }

            // Lọc theo status
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            // Luôn loại bỏ các role đã xóa (xóa mềm)
            predicates.add(cb.equal(root.get("deletedFlag"), 0));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
