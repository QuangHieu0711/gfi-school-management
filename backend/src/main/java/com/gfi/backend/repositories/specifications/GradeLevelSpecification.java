package com.gfi.backend.repositories.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.gfi.backend.models.dtos.gradelevel.GradeLevelFilterDto;
import com.gfi.backend.models.entities.GradeLevel;

import jakarta.persistence.criteria.Predicate;

/**
 * Specification builder cho GradeLevel query.
 * Tách biệt logic truy vấn từ service.
 */
@Component
public class GradeLevelSpecification {

    /**
     * Xây dựng Specification từ filter conditions.
     * Luôn loại bỏ khối lớp đã xóa (deletedFlag = 0).
     * 
     * @param filter điều kiện lọc
     * @return Specification<GradeLevel> dùng cho repository query
     */
    public Specification<GradeLevel> buildSpecification(GradeLevelFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Luôn loại bỏ các khối lớp đã xóa
            predicates.add(cb.equal(root.get("deletedFlag"), 0));

            // Tìm kiếm với từ khóa là code, name hoặc description
            if (StringUtils.hasText(filter.getGradeLevel())) {
                String keyword = "%" + filter.getGradeLevel().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)));
            }

            // Lọc theo trạng thái (status)
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
