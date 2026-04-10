package com.gfi.backend.repositories.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.gfi.backend.models.dtos.classroom.ClassroomFilterDto;
import com.gfi.backend.models.entities.Classroom;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * Specification component để build query logic cho Classroom.
 * Sử dụng Criteria API để xây dựng dynamic query với filter.
 * Luôn filter: deletedFlag = 0 (soft delete)
 */
@Component
public class ClassroomSpecification {

    /**
     * Build Specification từ ClassroomFilterDto.
     * Hỗ trợ filter theo:
     * - className: keyword search across code, name, description, unit, gradeLevel, schoolYear
     * - unitId: direct reference
     * - gradeLevelId: direct reference
     * - schoolYearId: direct reference
     * - status: direct reference
     */
    public Specification<Classroom> buildSpecification(ClassroomFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // JOINS (sẽ được sử dụng trong search nhưng không bắt buộc có dữ liệu)
            Join<Object, Object> unitJoin = root.join("unit", JoinType.INNER);
            Join<Object, Object> gradeLevelJoin = root.join("gradeLevel", JoinType.INNER);
            Join<Object, Object> schoolYearJoin = root.join("schoolYear", JoinType.INNER);

            // Tìm kiếm theo tên hoặc mã lớp (code hoặc name)
            if (hasText(filter.getClassName())) {
                String keyword = "%" + filter.getClassName().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword)
                ));
            }

            // Tìm kiếm theo đơn vị
            if (filter.getUnitId() != null) {
                predicates.add(cb.equal(unitJoin.get("id"), filter.getUnitId()));
            }

            // Tìm kiếm theo khối
            if (filter.getGradeLevelId() != null) {
                predicates.add(cb.equal(gradeLevelJoin.get("id"), filter.getGradeLevelId()));
            }
            // Tìm kiếm theo năm học
            if (filter.getSchoolYearId() != null) {
                predicates.add(cb.equal(schoolYearJoin.get("id"), filter.getSchoolYearId()));
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
     * Build Specification cho getOptions() dựa trên unitId, gradeLevelId,
     * schoolYearId.
     */
    public Specification<Classroom> buildSpecificationForOptions(Long unitId, Long gradeLevelId, Long schoolYearId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (unitId != null) {
                predicates.add(cb.equal(root.join("unit", JoinType.INNER).get("id"), unitId));
            }
            if (gradeLevelId != null) {
                predicates.add(cb.equal(root.join("gradeLevel", JoinType.INNER).get("id"), gradeLevelId));
            }
            if (schoolYearId != null) {
                predicates.add(cb.equal(root.join("schoolYear", JoinType.INNER).get("id"), schoolYearId));
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
