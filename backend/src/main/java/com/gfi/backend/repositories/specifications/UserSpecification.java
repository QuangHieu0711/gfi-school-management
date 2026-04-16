package com.gfi.backend.repositories.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.gfi.backend.models.dtos.user.UserFilterDto;
import com.gfi.backend.models.entities.User;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * Builder tao Specification cho query User entity.
 */
@Component
public class UserSpecification {

    public Specification<User> buildSpecification(UserFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<Object, Object> roleJoin = root.join("role", JoinType.LEFT);
            Join<Object, Object> staffJoin = root.join("staff", JoinType.LEFT);
            Join<Object, Object> unitJoin = staffJoin.join("unit", JoinType.LEFT);

            if (hasText(filter.getFullName())) {
                String keyword = "%" + filter.getFullName().trim().toLowerCase() + "%";
                Expression<String> username = cb.lower(cb.coalesce(root.get("username"), ""));
                Expression<String> fullName = cb.lower(cb.coalesce(staffJoin.get("fullName"), ""));
                Expression<String> email = cb.lower(cb.coalesce(staffJoin.get("email"), ""));

                predicates.add(cb.or(
                        cb.like(username, keyword),
                        cb.like(fullName, keyword),
                        cb.like(email, keyword)));
            }

            if (filter.getRoleId() != null) {
                predicates.add(cb.equal(roleJoin.get("id"), filter.getRoleId()));
            }

            if (filter.getUnitId() != null && !filter.getUnitId().isEmpty()) {
                predicates.add(unitJoin.get("id").in(filter.getUnitId()));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            predicates.add(cb.equal(root.get("deletedFlag"), 0));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
