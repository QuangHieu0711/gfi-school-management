package com.gfi.backend.services.implement;

import java.time.LocalDate;
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
import com.gfi.backend.models.dtos.schoolyear.SchoolYearCreateRequest;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearFilterDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearItemDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearUpdateRequest;
import com.gfi.backend.models.entities.SchoolYear;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.SchoolYearRepository;
import com.gfi.backend.repositories.SemesterRepository;
import com.gfi.backend.services.interfaces.SchoolYearService;
import com.gfi.backend.utils.PageableUtils;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchoolYearServiceImpl implements SchoolYearService {

    private final SchoolYearRepository schoolYearRepository;
    private final SemesterRepository semesterRepository;
    private final ClassroomRepository classroomRepository;

    @Override
    public PageResponseDto<SchoolYearItemDto, SchoolYearFilterDto> search(PageRequestDto<SchoolYearFilterDto> request) {
        SchoolYearFilterDto filter = request.getFilter() == null ? new SchoolYearFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<SchoolYear> page = schoolYearRepository.findAll(buildSpecification(filter), pageable);
        List<SchoolYearItemDto> items = page.getContent().stream().map(this::toDto).toList();

        return PageResponseDto.<SchoolYearItemDto, SchoolYearFilterDto>builder()
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
        return schoolYearRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate").and(Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    @Override
    public SchoolYearItemDto getById(Long id) {
        return toDto(findSchoolYear(id));
    }

    @Override
    @Transactional
    public SchoolYearItemDto create(SchoolYearCreateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        String code = normalize(request.getCode());
        String name = normalize(request.getName());
        ensureCodeUnique(code, null);
        ensureNameUnique(name, null);

        SchoolYear schoolYear = new SchoolYear();
        schoolYear.setCode(code);
        schoolYear.setName(name);
        schoolYear.setStartDate(request.getStartDate());
        schoolYear.setEndDate(request.getEndDate());
        schoolYear.setStatus(request.getStatus());
        schoolYear.setIsCurrent(Boolean.TRUE.equals(request.getIsCurrent()));
        schoolYear.setDescription(normalizeNullable(request.getDescription()));
        schoolYear.setCreatedBy(getCurrentUsername());

        SchoolYear saved = schoolYearRepository.save(schoolYear);
        applyCurrentFlag(saved);
        return toDto(saved);
    }

    @Override
    @Transactional
    public SchoolYearItemDto update(Long id, SchoolYearUpdateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        SchoolYear schoolYear = findSchoolYear(id);
        String code = normalize(request.getCode());
        String name = normalize(request.getName());
        ensureCodeUnique(code, id);
        ensureNameUnique(name, id);

        schoolYear.setCode(code);
        schoolYear.setName(name);
        schoolYear.setStartDate(request.getStartDate());
        schoolYear.setEndDate(request.getEndDate());
        schoolYear.setStatus(request.getStatus());
        schoolYear.setIsCurrent(Boolean.TRUE.equals(request.getIsCurrent()));
        schoolYear.setDescription(normalizeNullable(request.getDescription()));
        schoolYear.setUpdatedBy(getCurrentUsername());

        SchoolYear saved = schoolYearRepository.save(schoolYear);
        applyCurrentFlag(saved);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SchoolYear schoolYear = findSchoolYear(id);
        if (semesterRepository.countBySchoolYearId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.SCHOOL_YEAR_IN_USE);
        }
        if (classroomRepository.countBySchoolYearId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.SCHOOL_YEAR_IN_USE);
        }
        schoolYearRepository.delete(schoolYear);
    }

    private void ensureCodeUnique(String code, Long id) {
        schoolYearRepository.findByCode(code)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SCHOOL_YEAR_CODE_ALREADY_EXISTS);
                });
    }

    private void ensureNameUnique(String name, Long id) {
        schoolYearRepository.findByName(name)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NAME_ALREADY_EXISTS);
                });
    }

    private void applyCurrentFlag(SchoolYear schoolYear) {
        if (Boolean.TRUE.equals(schoolYear.getIsCurrent())) {
            schoolYearRepository.clearCurrentExcept(schoolYear.getId());
        }
    }

    private SchoolYear findSchoolYear(Long id) {
        return schoolYearRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
    }

    private Specification<SchoolYear> buildSpecification(SchoolYearFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(filter.getSchoolYear())) {
                String keyword = "%" + filter.getSchoolYear().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getIsCurrent() != null) {
                predicates.add(cb.equal(root.get("isCurrent"), filter.getIsCurrent()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private SchoolYearItemDto toDto(SchoolYear schoolYear) {
        return SchoolYearItemDto.builder()
                .id(schoolYear.getId())
                .code(schoolYear.getCode())
                .name(schoolYear.getName())
                .startDate(schoolYear.getStartDate())
                .endDate(schoolYear.getEndDate())
                .status(schoolYear.getStatus())
                .isCurrent(schoolYear.getIsCurrent())
                .description(schoolYear.getDescription())
                .build();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new UserMessageException(CommonErrorCode.INVALID_DATE_RANGE);
        }
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
