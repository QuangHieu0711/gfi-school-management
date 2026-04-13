package com.gfi.backend.repositories.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.gfi.backend.models.dtos.classroom.ClassroomFilterDto;
import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.models.security.ResolvedScope;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Component
public class ClassroomSpecification {

    public Specification<Classroom> buildSpecification(ClassroomFilterDto filter, List<ResolvedScope> resolvedScopes) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<Object, Object> unitJoin = root.join("unit", JoinType.INNER);
            Join<Object, Object> gradeLevelJoin = root.join("gradeLevel", JoinType.INNER);
            Join<Object, Object> schoolYearJoin = root.join("schoolYear", JoinType.INNER);

            if (hasText(filter.getClassName())) {
                String keyword = "%" + filter.getClassName().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword)));
            }

            if (filter.getUnitId() != null) {
                predicates.add(cb.equal(unitJoin.get("id"), filter.getUnitId()));
            }
            if (filter.getGradeLevelId() != null) {
                predicates.add(cb.equal(gradeLevelJoin.get("id"), filter.getGradeLevelId()));
            }
            if (filter.getSchoolYearId() != null) {
                predicates.add(cb.equal(schoolYearJoin.get("id"), filter.getSchoolYearId()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            predicates.add(cb.equal(root.get("deletedFlag"), 0));
            predicates.add(buildScopePredicate(root, cb, unitJoin, gradeLevelJoin, resolvedScopes));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public Specification<Classroom> buildSpecificationForOptions(Long unitId, Long gradeLevelId, Long schoolYearId,
            List<ResolvedScope> resolvedScopes) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> unitJoin = root.join("unit", JoinType.INNER);
            Join<Object, Object> gradeLevelJoin = root.join("gradeLevel", JoinType.INNER);

            if (unitId != null) {
                predicates.add(cb.equal(unitJoin.get("id"), unitId));
            }
            if (gradeLevelId != null) {
                predicates.add(cb.equal(gradeLevelJoin.get("id"), gradeLevelId));
            }
            if (schoolYearId != null) {
                predicates.add(cb.equal(root.join("schoolYear", JoinType.INNER).get("id"), schoolYearId));
            }

            predicates.add(cb.equal(root.get("deletedFlag"), 0));
            predicates.add(buildScopePredicate(root, cb, unitJoin, gradeLevelJoin, resolvedScopes));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate buildScopePredicate(jakarta.persistence.criteria.Root<Classroom> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Join<Object, Object> unitJoin,
            Join<Object, Object> gradeLevelJoin,
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

            switch (scope.getScopeType()) {
                case UNIT -> scopePredicates.add(unitJoin.get("id").in(scope.getScopeIds()));
                case CLASS -> scopePredicates.add(root.get("id").in(scope.getScopeIds()));
                case GRADE -> scopePredicates.add(gradeLevelJoin.get("id").in(scope.getScopeIds()));
                default -> {
                }
            }
        }

        return scopePredicates.isEmpty() ? cb.disjunction() : cb.or(scopePredicates.toArray(new Predicate[0]));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
