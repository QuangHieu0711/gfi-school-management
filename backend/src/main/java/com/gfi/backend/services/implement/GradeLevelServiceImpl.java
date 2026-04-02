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
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelCreateRequest;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelFilterDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelItemDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelUpdateRequest;
import com.gfi.backend.models.entities.GradeLevel;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.GradeLevelRepository;
import com.gfi.backend.repositories.GradeLevelSubjectRepository;
import com.gfi.backend.services.interfaces.GradeLevelService;
import com.gfi.backend.utils.PageableUtils;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GradeLevelServiceImpl implements GradeLevelService {

    private final GradeLevelRepository gradeLevelRepository;
    private final ClassroomRepository classroomRepository;
    private final GradeLevelSubjectRepository gradeLevelSubjectRepository;

    @Override
    public PageResponseDto<GradeLevelItemDto, GradeLevelFilterDto> search(PageRequestDto<GradeLevelFilterDto> request) {
        GradeLevelFilterDto filter = request.getFilter() == null ? new GradeLevelFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<GradeLevel> page = gradeLevelRepository.findAll(buildSpecification(filter), pageable);
        List<GradeLevelItemDto> items = page.getContent().stream().map(this::toDto).toList();

        return PageResponseDto.<GradeLevelItemDto, GradeLevelFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    @Override
    public List<LookupItemDto> getOptions() {
        return gradeLevelRepository.findAll(Sort.by(Sort.Direction.ASC, "gradeNumber").and(Sort.by(Sort.Direction.ASC, "id")))
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    @Override
    public GradeLevelItemDto getById(Long id) {
        return toDto(findGradeLevel(id));
    }

    @Override
    @Transactional
    public GradeLevelItemDto create(GradeLevelCreateRequest request) {
        String code = normalize(request.getCode());
        String name = normalize(request.getName());
        Integer gradeNumber = request.getGradeNumber();

        ensureCodeUnique(code, null);
        ensureNameUnique(name, null);
        ensureGradeNumberUnique(gradeNumber, null);

        GradeLevel gradeLevel = new GradeLevel();
        gradeLevel.setCode(code);
        gradeLevel.setName(name);
        gradeLevel.setGradeNumber(gradeNumber);
        gradeLevel.setStatus(request.getStatus());
        gradeLevel.setDescription(normalizeNullable(request.getDescription()));
        gradeLevel.setCreatedBy(getCurrentUsername());
        return toDto(gradeLevelRepository.save(gradeLevel));
    }

    @Override
    @Transactional
    public GradeLevelItemDto update(Long id, GradeLevelUpdateRequest request) {
        GradeLevel gradeLevel = findGradeLevel(id);
        String code = normalize(request.getCode());
        String name = normalize(request.getName());
        Integer gradeNumber = request.getGradeNumber();

        ensureCodeUnique(code, id);
        ensureNameUnique(name, id);
        ensureGradeNumberUnique(gradeNumber, id);

        gradeLevel.setCode(code);
        gradeLevel.setName(name);
        gradeLevel.setGradeNumber(gradeNumber);
        gradeLevel.setStatus(request.getStatus());
        gradeLevel.setDescription(normalizeNullable(request.getDescription()));
        gradeLevel.setUpdatedBy(getCurrentUsername());
        return toDto(gradeLevelRepository.save(gradeLevel));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        GradeLevel gradeLevel = findGradeLevel(id);
        if (classroomRepository.countByGradeLevelId(id) > 0
                || gradeLevelSubjectRepository.countByGradeLevelId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.GRADE_LEVEL_IN_USE);
        }
        gradeLevelRepository.delete(gradeLevel);
    }

    private GradeLevel findGradeLevel(Long id) {
        return gradeLevelRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.GRADE_LEVEL_NOT_FOUND));
    }

    private void ensureCodeUnique(String code, Long id) {
        gradeLevelRepository.findByCode(code)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.GRADE_LEVEL_CODE_ALREADY_EXISTS);
                });
    }

    private void ensureNameUnique(String name, Long id) {
        gradeLevelRepository.findByName(name)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.GRADE_LEVEL_NAME_ALREADY_EXISTS);
                });
    }

    private void ensureGradeNumberUnique(Integer gradeNumber, Long id) {
        gradeLevelRepository.findByGradeNumber(gradeNumber)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.GRADE_LEVEL_NUMBER_ALREADY_EXISTS);
                });
    }

    private Specification<GradeLevel> buildSpecification(GradeLevelFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(filter.getGradeLevel())) {
                String keyword = "%" + filter.getGradeLevel().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private GradeLevelItemDto toDto(GradeLevel gradeLevel) {
        return GradeLevelItemDto.builder()
                .id(gradeLevel.getId())
                .code(gradeLevel.getCode())
                .name(gradeLevel.getName())
                .gradeNumber(gradeLevel.getGradeNumber())
                .status(gradeLevel.getStatus())
                .description(gradeLevel.getDescription())
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
