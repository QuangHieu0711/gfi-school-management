package com.gfi.backend.repositories.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.gfi.backend.models.dtos.semester.SemesterFilterDto;
import com.gfi.backend.models.entities.Semester;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * Specification builder cho Semester query.
 * Tách biệt logic truy vấn từ service.
 */
@Component
public class SemesterSpecification {

    /**
     * Xây dựng Specification từ filter conditions.
     * Luôn loại bỏ semester đã xóa (deletedFlag = 0).
     * 
     * @param filter điều kiện lọc
     * @return Specification<Semester> dùng cho repository query
     */
    public Specification<Semester> buildSpecification(SemesterFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> schoolYearJoin = root.join("schoolYear", JoinType.INNER);

            // Luôn loại bỏ các semester đã xóa
            predicates.add(cb.equal(root.get("deletedFlag"), 0));

            // Lọc theo từ khóa mã năm học
            if (filter.getSchoolYearId() != null) {
                predicates.add(cb.equal(schoolYearJoin.get("id"), filter.getSchoolYearId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
