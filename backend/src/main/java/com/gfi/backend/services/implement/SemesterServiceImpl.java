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
import com.gfi.backend.models.dtos.semester.SemesterCreateRequest;
import com.gfi.backend.models.dtos.semester.SemesterFilterDto;
import com.gfi.backend.models.dtos.semester.SemesterItemDto;
import com.gfi.backend.models.dtos.semester.SemesterUpdateRequest;
import com.gfi.backend.models.entities.SchoolYear;
import com.gfi.backend.models.entities.Semester;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.SchoolYearRepository;
import com.gfi.backend.repositories.SemesterRepository;
import com.gfi.backend.services.interfaces.SemesterService;
import com.gfi.backend.utils.PageableUtils;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SemesterServiceImpl implements SemesterService {

    private final SemesterRepository semesterRepository;
    private final SchoolYearRepository schoolYearRepository;

    @Override
    public PageResponseDto<SemesterItemDto, SemesterFilterDto> search(PageRequestDto<SemesterFilterDto> request) {
        SemesterFilterDto filter = request.getFilter() == null ? new SemesterFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<Semester> page = semesterRepository.findAll(buildSpecification(filter), pageable);
        List<SemesterItemDto> items = page.getContent().stream().map(this::toDto).toList();

        return PageResponseDto.<SemesterItemDto, SemesterFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    @Override
    public List<LookupItemDto> getOptions(Long schoolYearId) {
        Specification<Semester> specification = (root, query, cb) -> schoolYearId == null
                ? cb.conjunction()
                : cb.equal(root.join("schoolYear", JoinType.INNER).get("id"), schoolYearId);

        return semesterRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "semesterOrder").and(Sort.by(Sort.Direction.ASC, "id")))
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    @Override
    public SemesterItemDto getById(Long id) {
        return toDto(findSemester(id));
    }

    @Override
    @Transactional
    public SemesterItemDto create(SemesterCreateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        SchoolYear schoolYear = findSchoolYear(request.getSchoolYearId());
        validateSemesterUnique(request.getSchoolYearId(), normalize(request.getCode()), normalize(request.getName()), request.getSemesterOrder(), null);

        Semester semester = new Semester();
        semester.setSchoolYear(schoolYear);
        semester.setCode(normalize(request.getCode()));
        semester.setName(normalize(request.getName()));
        semester.setSemesterOrder(request.getSemesterOrder());
        semester.setStartDate(request.getStartDate());
        semester.setEndDate(request.getEndDate());
        semester.setStatus(request.getStatus());
        semester.setIsCurrent(Boolean.TRUE.equals(request.getIsCurrent()));
        semester.setDescription(normalizeNullable(request.getDescription()));
        semester.setCreatedBy(getCurrentUsername());

        Semester saved = semesterRepository.save(semester);
        applyCurrentFlag(saved);
        return toDto(saved);
    }

    @Override
    @Transactional
    public SemesterItemDto update(Long id, SemesterUpdateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        Semester semester = findSemester(id);
        SchoolYear schoolYear = findSchoolYear(request.getSchoolYearId());
        validateSemesterUnique(request.getSchoolYearId(), normalize(request.getCode()), normalize(request.getName()), request.getSemesterOrder(), id);

        semester.setSchoolYear(schoolYear);
        semester.setCode(normalize(request.getCode()));
        semester.setName(normalize(request.getName()));
        semester.setSemesterOrder(request.getSemesterOrder());
        semester.setStartDate(request.getStartDate());
        semester.setEndDate(request.getEndDate());
        semester.setStatus(request.getStatus());
        semester.setIsCurrent(Boolean.TRUE.equals(request.getIsCurrent()));
        semester.setDescription(normalizeNullable(request.getDescription()));
        semester.setUpdatedBy(getCurrentUsername());

        Semester saved = semesterRepository.save(semester);
        applyCurrentFlag(saved);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        semesterRepository.delete(findSemester(id));
    }

    private void validateSemesterUnique(Long schoolYearId, String code, String name, Integer semesterOrder, Long id) {
        semesterRepository.findBySchoolYearIdAndCode(schoolYearId, code)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SEMESTER_CODE_ALREADY_EXISTS);
                });
        semesterRepository.findBySchoolYearIdAndName(schoolYearId, name)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SEMESTER_NAME_ALREADY_EXISTS);
                });
        semesterRepository.findBySchoolYearIdAndSemesterOrder(schoolYearId, semesterOrder)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SEMESTER_ORDER_ALREADY_EXISTS);
                });
    }

    private void applyCurrentFlag(Semester semester) {
        if (Boolean.TRUE.equals(semester.getIsCurrent())) {
            semesterRepository.clearCurrentExcept(semester.getId());
        }
    }

    private SchoolYear findSchoolYear(Long id) {
        return schoolYearRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
    }

    private Semester findSemester(Long id) {
        return semesterRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SEMESTER_NOT_FOUND));
    }

    private Specification<Semester> buildSpecification(SemesterFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> schoolYearJoin = root.join("schoolYear", JoinType.INNER);

            if (hasText(filter.getSemester())) {
                String keyword = "%" + filter.getSemester().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword),
                        cb.like(cb.lower(schoolYearJoin.get("code")), keyword),
                        cb.like(cb.lower(schoolYearJoin.get("name")), keyword)));
            }
            if (filter.getSchoolYearId() != null) {
                predicates.add(cb.equal(schoolYearJoin.get("id"), filter.getSchoolYearId()));
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

    private SemesterItemDto toDto(Semester semester) {
        return SemesterItemDto.builder()
                .id(semester.getId())
                .schoolYearId(semester.getSchoolYear() == null ? null : semester.getSchoolYear().getId())
                .schoolYearCode(semester.getSchoolYear() == null ? null : semester.getSchoolYear().getCode())
                .schoolYearName(semester.getSchoolYear() == null ? null : semester.getSchoolYear().getName())
                .code(semester.getCode())
                .name(semester.getName())
                .semesterOrder(semester.getSemesterOrder())
                .startDate(semester.getStartDate())
                .endDate(semester.getEndDate())
                .status(semester.getStatus())
                .isCurrent(semester.getIsCurrent())
                .description(semester.getDescription())
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
