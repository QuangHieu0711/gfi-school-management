package com.gfi.backend.repositories.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.gfi.backend.models.dtos.unit.UnitFilterDto;
import com.gfi.backend.models.entities.Unit;
import jakarta.persistence.criteria.Predicate;

/**
 * Builder tạo Specification cho query Unit entity.
 * Đóng gói logic query cho tìm kiếm/filter.
 */
@Component
public class UnitSpecification {

    /**
     * Tạo Specification<Unit> dựa trên filter cho trước.
     * 
     * @param filter tiêu chí filter
     * @return Specification<Unit> cho JPA query
     */
    public Specification<Unit> buildSpecification(UnitFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter theo unitName (code hoặc name)
            if (hasText(filter.getUnitName())) {
                String keyword = "%" + filter.getUnitName().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword)));
            }
            
            // Filter theo status
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
