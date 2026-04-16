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
import com.gfi.backend.models.dtos.staff.StaffEducationCreateRequest;
import com.gfi.backend.models.dtos.staff.StaffEducationDto;
import com.gfi.backend.models.dtos.staff.StaffEducationFilterDto;
import com.gfi.backend.models.dtos.staff.StaffEducationUpdateRequest;
import com.gfi.backend.models.entities.Staff;
import com.gfi.backend.models.entities.StaffEducation;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.StaffEducationRepository;
import com.gfi.backend.repositories.StaffRepository;
import com.gfi.backend.services.interfaces.StaffEducationService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaffEducationServiceImpl implements StaffEducationService {

    private static final String EDUCATION_TYPE_TRAINING = "TRAINING";

    private final StaffEducationRepository staffEducationRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<StaffEducationDto, StaffEducationFilterDto> search(PageRequestDto<StaffEducationFilterDto> request) {
        StaffEducationFilterDto filter = request.getFilter() == null ? new StaffEducationFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());

        if (filter.getStaffId() != null) {
            ensureStaffExists(filter.getStaffId());
        }

        Pageable pageable = PageRequest.of(pageNow - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "fromDate").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<StaffEducation> page = staffEducationRepository.findAll(buildSpecification(filter), pageable);
        List<StaffEducationDto> items = page.getContent().stream().map(this::toDto).toList();

        return PageResponseDto.<StaffEducationDto, StaffEducationFilterDto>builder()
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
    public StaffEducationDto getById(Long id) {
        return toDto(findEducation(id));
    }

    @Override
    @Transactional
    public StaffEducationDto create(StaffEducationCreateRequest request) {
        validateDateRange(request.getFromDate(), request.getToDate());

        StaffEducation education = new StaffEducation();
        education.setStaff(ensureStaffExists(request.getStaffId()));
        education.setEducationType(EDUCATION_TYPE_TRAINING);
        applyFields(education, request.getSchoolName(), request.getMajor(), request.getTrainingForm(),
                request.getCertificate(), request.getFromDate(), request.getToDate(), request.getNote());

        return toDto(staffEducationRepository.save(education));
    }

    @Override
    @Transactional
    public StaffEducationDto update(Long id, StaffEducationUpdateRequest request) {
        validateDateRange(request.getFromDate(), request.getToDate());

        StaffEducation education = findEducation(id);
        applyFields(education, request.getSchoolName(), request.getMajor(), request.getTrainingForm(),
                request.getCertificate(), request.getFromDate(), request.getToDate(), request.getNote());

        return toDto(staffEducationRepository.save(education));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        staffEducationRepository.delete(findEducation(id));
    }

    private Specification<StaffEducation> buildSpecification(StaffEducationFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            predicates.add(cb.equal(root.get("educationType"), EDUCATION_TYPE_TRAINING));

            if (filter.getStaffId() != null) {
                predicates.add(cb.equal(root.get("staff").get("id"), filter.getStaffId()));
            }
            if (hasText(filter.getSchoolName())) {
                predicates.add(cb.like(cb.lower(root.get("schoolName")), likeValue(filter.getSchoolName())));
            }
            if (hasText(filter.getMajor())) {
                predicates.add(cb.like(cb.lower(root.get("major")), likeValue(filter.getMajor())));
            }
            if (hasText(filter.getTrainingForm())) {
                predicates.add(cb.like(cb.lower(root.get("trainingForm")), likeValue(filter.getTrainingForm())));
            }
            if (hasText(filter.getCertificate())) {
                predicates.add(cb.like(cb.lower(root.get("certificate")), likeValue(filter.getCertificate())));
            }
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fromDate"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("toDate"), filter.getToDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Staff ensureStaffExists(Long staffId) {
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.STAFF_NOT_FOUND));
    }

    private StaffEducation findEducation(Long id) {
        return staffEducationRepository.findByIdAndEducationType(id, EDUCATION_TYPE_TRAINING)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.BAD_REQUEST.getCode(),
                        "Khong tim thay thong tin dao tao"));
    }

    private void applyFields(StaffEducation education, String schoolName, String major, String trainingForm,
            String certificate, LocalDate fromDate, LocalDate toDate, String note) {
        education.setSchoolName(normalize(schoolName));
        education.setMajor(normalizeNullable(major));
        education.setTrainingForm(normalizeNullable(trainingForm));
        education.setCertificate(normalizeNullable(certificate));
        education.setFromDate(fromDate);
        education.setToDate(toDate);
        education.setNote(normalizeNullable(note));
    }

    private StaffEducationDto toDto(StaffEducation education) {
        return StaffEducationDto.builder()
                .id(education.getId())
                .staffId(education.getStaff() == null ? null : education.getStaff().getId())
                .schoolName(education.getSchoolName())
                .major(education.getMajor())
                .trainingForm(education.getTrainingForm())
                .certificate(education.getCertificate())
                .fromDate(education.getFromDate())
                .toDate(education.getToDate())
                .note(education.getNote())
                .build();
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new UserMessageException(CommonErrorCode.INVALID_DATE_RANGE);
        }
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
