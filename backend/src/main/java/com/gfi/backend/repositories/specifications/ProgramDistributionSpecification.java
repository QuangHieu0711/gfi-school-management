package com.gfi.backend.repositories.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.gfi.backend.models.entities.ProgramDistribution;

import jakarta.persistence.criteria.Predicate;

@Component
public class ProgramDistributionSpecification {

    public Specification<ProgramDistribution> buildSpecification(Long schoolYearId, Long unitId, Long classroomId, Long subjectId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (schoolYearId != null) {
                predicates.add(cb.equal(root.get("schoolYear").get("id"), schoolYearId));
            }
            if (unitId != null) {
                predicates.add(cb.equal(root.get("unit").get("id"), unitId));
            }
            if (classroomId != null) {
                predicates.add(cb.equal(root.get("classroom").get("id"), classroomId));
            }
            if (subjectId != null) {
                predicates.add(cb.equal(root.get("subject").get("id"), subjectId));
            }

            // Only include non-deleted records
            predicates.add(cb.equal(root.get("deletedFlag"), 0));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
