package com.gfi.backend.repositories.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.gfi.backend.models.dtos.subject.SubjectFilterDto;
import com.gfi.backend.models.entities.Subject;

import jakarta.persistence.criteria.Predicate;

/**
 * Specification component để build query logic cho Subject.
 * Sử dụng Criteria API để xây dựng dynamic query với filter.
 * Luôn filter: deletedFlag = 0 (soft delete)
 */
@Component
public class SubjectSpecification {

    /**
     * Build Specification từ SubjectFilterDto.
     * Hỗ trợ filter theo:
     * - subject: keyword search across code, name, description
     * - type: direct reference
     * - status: direct reference
     */
    public Specification<Subject> buildSpecification(SubjectFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // KEYWORD SEARCH
            if (hasText(filter.getSubject())) {
                String keyword = "%" + filter.getSubject().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword)));
            }

            // Tìm kiếm theo loại môn học 0 - môn học chính khóa, 1 - môn học tự chọn
            if (filter.getType() != null) {
                predicates.add(cb.equal(root.get("type"), filter.getType()));
            }

            // Tìm kiếm theo trạng thái
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            // SOFT DELETE: Luôn exclude deletedFlag = 1
            predicates.add(cb.equal(root.get("deletedFlag"), 0));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Kiểm tra string có nội dung hay không.
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
