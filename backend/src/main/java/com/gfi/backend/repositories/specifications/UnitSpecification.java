package com.gfi.backend.repositories.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.gfi.backend.models.dtos.unit.UnitFilterDto;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.models.security.ResolvedScope;

import jakarta.persistence.criteria.Predicate;

@Component
public class UnitSpecification {

    public Specification<Unit> buildSpecification(UnitFilterDto filter, List<ResolvedScope> resolvedScopes) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(filter.getUnitName())) {
                String keyword = "%" + filter.getUnitName().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword)));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            predicates.add(cb.equal(root.get("deletedFlag"), 0));
            predicates.add(buildScopePredicate(root, cb, resolvedScopes));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate buildScopePredicate(jakarta.persistence.criteria.Root<Unit> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            List<ResolvedScope> resolvedScopes) {
        if (resolvedScopes == null || resolvedScopes.isEmpty()) {
            return cb.disjunction();
        }

        List<Predicate> scopePredicates = new ArrayList<>();
        for (ResolvedScope scope : resolvedScopes) {
            if (scope == null) {
                continue;
            }
            if (scope.isUnrestricted() || scope.getScopeType() == ScopeType.ALL) {
                return cb.conjunction();
            }
            if (scope.getScopeIds() == null || scope.getScopeIds().isEmpty()) {
                continue;
            }
            if (scope.getScopeType() == ScopeType.UNIT) {
                scopePredicates.add(root.get("id").in(scope.getScopeIds()));
            }
        }

        return scopePredicates.isEmpty() ? cb.disjunction() : cb.or(scopePredicates.toArray(new Predicate[0]));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
