package com.gfi.backend.services.implement;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.staff.StaffForeignLanguageCreateRequest;
import com.gfi.backend.models.dtos.staff.StaffForeignLanguageDto;
import com.gfi.backend.models.dtos.staff.StaffForeignLanguageFilterDto;
import com.gfi.backend.models.dtos.staff.StaffForeignLanguageUpdateRequest;
import com.gfi.backend.models.entities.Staff;
import com.gfi.backend.models.entities.StaffEducation;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.StaffEducationRepository;
import com.gfi.backend.repositories.StaffRepository;
import com.gfi.backend.services.interfaces.StaffForeignLanguageService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaffForeignLanguageServiceImpl implements StaffForeignLanguageService {

    private static final String EDUCATION_TYPE_FOREIGN_LANGUAGE = "FOREIGN_LANGUAGE";

    private final StaffEducationRepository staffEducationRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<StaffForeignLanguageDto, StaffForeignLanguageFilterDto> search(
            PageRequestDto<StaffForeignLanguageFilterDto> request) {
        StaffForeignLanguageFilterDto filter = request.getFilter() == null
                ? new StaffForeignLanguageFilterDto()
                : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());

        if (filter.getStaffId() != null) {
            ensureStaffExists(filter.getStaffId());
        }

        Pageable pageable = PageRequest.of(pageNow - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "fromDate").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<StaffEducation> page = staffEducationRepository.findAll(buildSpecification(filter), pageable);
        List<StaffForeignLanguageDto> items = page.getContent().stream().map(this::toDto).toList();

        return PageResponseDto.<StaffForeignLanguageDto, StaffForeignLanguageFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffForeignLanguageDto getById(Long id) {
        return toDto(findForeignLanguage(id));
    }

    @Override
    @Transactional
    public StaffForeignLanguageDto create(StaffForeignLanguageCreateRequest request) {
        StaffEducation education = new StaffEducation();
        education.setStaff(ensureStaffExists(request.getStaffId()));
        education.setEducationType(EDUCATION_TYPE_FOREIGN_LANGUAGE);
        applyFields(education, request.getLanguageName(), request.getLanguageLevel(), request.getIssueDate(),
                request.getScore(), request.getNote());
        return toDto(staffEducationRepository.save(education));
    }

    @Override
    @Transactional
    public StaffForeignLanguageDto update(Long id, StaffForeignLanguageUpdateRequest request) {
        StaffEducation education = findForeignLanguage(id);
        applyFields(education, request.getLanguageName(), request.getLanguageLevel(), request.getIssueDate(),
                request.getScore(), request.getNote());
        return toDto(staffEducationRepository.save(education));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        staffEducationRepository.delete(findForeignLanguage(id));
    }

    private Specification<StaffEducation> buildSpecification(StaffForeignLanguageFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            predicates.add(cb.equal(root.get("educationType"), EDUCATION_TYPE_FOREIGN_LANGUAGE));

            if (filter.getStaffId() != null) {
                predicates.add(cb.equal(root.get("staff").get("id"), filter.getStaffId()));
            }
            if (hasText(filter.getLanguageName())) {
                predicates.add(cb.like(cb.lower(root.get("schoolName")), likeValue(filter.getLanguageName())));
            }
            if (hasText(filter.getLanguageLevel())) {
                predicates.add(cb.like(cb.lower(root.get("frameworkLevel")), likeValue(filter.getLanguageLevel())));
            }
            if (filter.getIssueDate() != null) {
                predicates.add(cb.equal(root.get("fromDate"), filter.getIssueDate()));
            }
            if (hasText(filter.getScore())) {
                predicates.add(cb.like(cb.lower(root.get("score")), likeValue(filter.getScore())));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Staff ensureStaffExists(Long staffId) {
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.STAFF_NOT_FOUND));
    }

    private StaffEducation findForeignLanguage(Long id) {
        return staffEducationRepository.findByIdAndEducationType(id, EDUCATION_TYPE_FOREIGN_LANGUAGE)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.BAD_REQUEST.getCode(),
                        "Khong tim thay thong tin ngoai ngu"));
    }

    private void applyFields(StaffEducation education, String languageName, String languageLevel, LocalDate issueDate,
            String score, String note) {
        education.setSchoolName(normalize(languageName));
        education.setFrameworkLevel(normalizeNullable(languageLevel));
        education.setFromDate(issueDate);
        education.setScore(normalizeNullable(score));
        education.setNote(normalizeNullable(note));
        education.setMajor(null);
        education.setTrainingForm(null);
        education.setCertificate(null);
        education.setToDate(null);
    }

    private StaffForeignLanguageDto toDto(StaffEducation education) {
        return StaffForeignLanguageDto.builder()
                .id(education.getId())
                .staffId(education.getStaff() == null ? null : education.getStaff().getId())
                .languageName(education.getSchoolName())
                .languageLevel(education.getFrameworkLevel())
                .issueDate(education.getFromDate())
                .score(education.getScore())
                .note(education.getNote())
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String likeValue(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 20 : pageSize;
    }

    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow <= 0 ? 1 : pageNow;
    }
}
