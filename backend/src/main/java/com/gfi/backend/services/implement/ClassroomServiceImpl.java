package com.gfi.backend.services.implement;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.classroom.ClassroomCreateRequest;
import com.gfi.backend.models.dtos.classroom.ClassroomFilterDto;
import com.gfi.backend.models.dtos.classroom.ClassroomItemDto;
import com.gfi.backend.models.dtos.classroom.ClassroomUpdateRequest;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.entities.GradeLevel;
import com.gfi.backend.models.entities.SchoolYear;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.GradeLevelRepository;
import com.gfi.backend.repositories.SchoolYearRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.services.interfaces.ClassroomService;
import com.gfi.backend.utils.PageableUtils;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final UnitRepository unitRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolYearRepository schoolYearRepository;

    @Override
    public PageResponseDto<ClassroomItemDto, ClassroomFilterDto> search(PageRequestDto<ClassroomFilterDto> request) {
        ClassroomFilterDto filter = request.getFilter() == null ? new ClassroomFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<Classroom> page = classroomRepository.findAll(buildSpecification(filter), pageable);
        List<ClassroomItemDto> items = page.getContent().stream().map(this::toDto).toList();

        return PageResponseDto.<ClassroomItemDto, ClassroomFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    @Override
    public List<LookupItemDto> getOptions(Long unitId, Long gradeLevelId, Long schoolYearId) {
        Specification<Classroom> specification = buildSpecificationForOptions(unitId, gradeLevelId, schoolYearId);
        return classroomRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id")))
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    @Override
    public ClassroomItemDto getById(Long id) {
        return toDto(findClassroom(id));
    }

    @Override
    @Transactional
    public ClassroomItemDto create(ClassroomCreateRequest request) {
        Unit unit = findUnit(request.getUnitId());
        GradeLevel gradeLevel = findGradeLevel(request.getGradeLevelId());
        SchoolYear schoolYear = findSchoolYear(request.getSchoolYearId());
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        validateUnique(unit.getId(), gradeLevel.getId(), schoolYear.getId(), code, name, null);

        Classroom classroom = new Classroom();
        classroom.setCode(code);
        classroom.setName(name);
        classroom.setUnit(unit);
        classroom.setGradeLevel(gradeLevel);
        classroom.setSchoolYear(schoolYear);
        classroom.setStatus(request.getStatus());
        classroom.setDescription(normalizeNullable(request.getDescription()));
        classroom.setCreatedBy(getCurrentUsername());
        return toDto(classroomRepository.save(classroom));
    }

    @Override
    @Transactional
    public ClassroomItemDto update(Long id, ClassroomUpdateRequest request) {
        Classroom classroom = findClassroom(id);
        Unit unit = findUnit(request.getUnitId());
        GradeLevel gradeLevel = findGradeLevel(request.getGradeLevelId());
        SchoolYear schoolYear = findSchoolYear(request.getSchoolYearId());
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        validateUnique(unit.getId(), gradeLevel.getId(), schoolYear.getId(), code, name, id);

        classroom.setCode(code);
        classroom.setName(name);
        classroom.setUnit(unit);
        classroom.setGradeLevel(gradeLevel);
        classroom.setSchoolYear(schoolYear);
        classroom.setStatus(request.getStatus());
        classroom.setDescription(normalizeNullable(request.getDescription()));
        classroom.setUpdatedBy(getCurrentUsername());
        return toDto(classroomRepository.save(classroom));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        classroomRepository.delete(findClassroom(id));
    }

    private Classroom findClassroom(Long id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
    }

    private Unit findUnit(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));
    }

    private GradeLevel findGradeLevel(Long id) {
        return gradeLevelRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.GRADE_LEVEL_NOT_FOUND));
    }

    private SchoolYear findSchoolYear(Long id) {
        return schoolYearRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
    }

    private void validateUnique(Long unitId, Long gradeLevelId, Long schoolYearId, String code, String name, Long id) {
        classroomRepository.findByUnitIdAndGradeLevelIdAndSchoolYearIdAndCode(unitId, gradeLevelId, schoolYearId, code)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.CLASS_CODE_ALREADY_EXISTS);
                });
        classroomRepository.findByUnitIdAndGradeLevelIdAndSchoolYearIdAndName(unitId, gradeLevelId, schoolYearId, name)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.CLASS_NAME_ALREADY_EXISTS);
                });
    }

    private Specification<Classroom> buildSpecification(ClassroomFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> unitJoin = root.join("unit", JoinType.INNER);
            Join<Object, Object> gradeLevelJoin = root.join("gradeLevel", JoinType.INNER);
            Join<Object, Object> schoolYearJoin = root.join("schoolYear", JoinType.INNER);

            if (hasText(filter.getClassName())) {
                String keyword = "%" + filter.getClassName().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword),
                        cb.like(cb.lower(unitJoin.get("code")), keyword),
                        cb.like(cb.lower(unitJoin.get("name")), keyword),
                        cb.like(cb.lower(gradeLevelJoin.get("code")), keyword),
                        cb.like(cb.lower(gradeLevelJoin.get("name")), keyword),
                        cb.like(cb.lower(schoolYearJoin.get("code")), keyword),
                        cb.like(cb.lower(schoolYearJoin.get("name")), keyword)));
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
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Classroom> buildSpecificationForOptions(Long unitId, Long gradeLevelId, Long schoolYearId) {
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ClassroomItemDto toDto(Classroom classroom) {
        return ClassroomItemDto.builder()
                .id(classroom.getId())
                .code(classroom.getCode())
                .name(classroom.getName())
                .unitName(classroom.getUnit() == null ? null : classroom.getUnit().getName())
                .gradeLevelName(classroom.getGradeLevel() == null ? null : classroom.getGradeLevel().getName())
                .schoolYearName(classroom.getSchoolYear() == null ? null : classroom.getSchoolYear().getName())
                .status(classroom.getStatus())
                .description(classroom.getDescription())
                .build();
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }

    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow <= 0 ? 1 : pageNow;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
