package com.gfi.backend.repositories.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.gfi.backend.models.dtos.schoolyear.SchoolYearFilterDto;
import com.gfi.backend.models.entities.SchoolYear;

import jakarta.persistence.criteria.Predicate;

/**
 * Specification builder cho SchoolYear query.
 * Tách biệt logic truy vấn từ service.
 */
@Component
public class SchoolYearSpecification {

    /**
     * Xây dựng Specification từ filter conditions.
     * Luôn loại bỏ năm học đã xóa (deletedFlag = 0).
     * 
     * @param filter điều kiện lọc
     * @return Specification<SchoolYear> dùng cho repository query
     */
    public Specification<SchoolYear> buildSpecification(SchoolYearFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Luôn loại bỏ các năm học đã xóa
            predicates.add(cb.equal(root.get("deletedFlag"), 0));

            // Tìm kiếm với từ khóa là code, name hoặc description
            if (StringUtils.hasText(filter.getSchoolYear())) {
                String keyword = "%" + filter.getSchoolYear().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)));
            }

            // Lọc theo trạng thái (status)
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            // Lọc theo năm học hiện tại (isCurrent)
            if (filter.getIsCurrent() != null) {
                predicates.add(cb.equal(root.get("isCurrent"), filter.getIsCurrent()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
