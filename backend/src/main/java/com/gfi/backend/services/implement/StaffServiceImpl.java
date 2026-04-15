package com.gfi.backend.services.implement;

import com.gfi.backend.models.dtos.staff.*;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.entities.Staff;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.repositories.StaffRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.services.interfaces.StaffService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final UnitRepository unitRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<StaffItemDto, StaffFilterDto> search(PageRequestDto<StaffFilterDto> request) {
        StaffFilterDto filter = request.getFilter() == null ? new StaffFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<Staff> page = staffRepository.findAll(buildSpecification(filter), pageable);
        List<StaffItemDto> items = page.getContent().stream()
                .map(this::toItemDto)
                .toList();

        return PageResponseDto.<StaffItemDto, StaffFilterDto>builder()
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
    public StaffDetailDto getById(Long id) {
        Staff staff = findStaff(id);
        return toDetailDto(staff);
    }

    @Override
    @Transactional
    public StaffDetailDto create(StaffCreateRequest request) {
        // Validate staff code
        String staffCode = normalize(request.getStaffCode());
        staffRepository.findByStaffCode(staffCode)
                .ifPresent(s -> {
                    throw new UserMessageException(CommonErrorCode.STAFF_CODE_ALREADY_EXISTS);
                });

        // Validate unit exists
        Unit unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));

        Staff staff = new Staff();
        applyStaffFields(staff, request);
        staff.setStaffCode(staffCode);
        staff.setUnit(unit);
        staff.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        staff.setCreatedBy(SecurityUtils.getCurrentUsername());
        staff.setDeletedFlag(0);

        Staff saved = staffRepository.save(staff);
        return toDetailDto(saved);
    }

    @Override
    @Transactional
    public StaffDetailDto update(Long id, StaffUpdateRequest request) {
        Staff staff = findStaff(id);
        applyStaffFields(staff, request);
        staff.setUpdatedBy(SecurityUtils.getCurrentUsername());

        Staff saved = staffRepository.save(staff);
        return toDetailDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Staff staff = findStaff(id);
        staff.setDeletedFlag(1);
        staff.setDeletedBy(SecurityUtils.getCurrentUsername());
        staffRepository.save(staff);
    }

    private Specification<Staff> buildSpecification(StaffFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            if (hasText(filter.getStaffCode())) {
                predicates.add(cb.like(cb.lower(root.get("staffCode")), "%" + filter.getStaffCode().toLowerCase() + "%"));
            }
            if (hasText(filter.getFullName())) {
                predicates.add(cb.like(cb.lower(root.get("fullName")), "%" + filter.getFullName().toLowerCase() + "%"));
            }
            if (filter.getUnitId() != null) {
                predicates.add(cb.equal(root.get("unit").get("id"), filter.getUnitId()));
            }
            if (hasText(filter.getStatus())) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (hasText(filter.getGender())) {
                predicates.add(cb.equal(root.get("gender"), filter.getGender()));
            }
            if (hasText(filter.getPhone())) {
                predicates.add(cb.like(root.get("phone"), "%" + filter.getPhone() + "%"));
            }
            if (hasText(filter.getEmail())) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + filter.getEmail().toLowerCase() + "%"));
            }

            // Always filter deleted flag
            predicates.add(cb.equal(root.get("deletedFlag"), 0));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void applyStaffFields(Staff staff, StaffCreateRequest request) {
        staff.setFullName(normalize(request.getFullName()));
        staff.setAliasName(normalizeNullable(request.getAliasName()));
        staff.setIdentityCode(normalizeNullable(request.getIdentityCode()));
        staff.setGender(request.getGender());
        staff.setDateOfBirth(request.getDateOfBirth());
        staff.setEthnicityId(request.getEthnicityId());
        staff.setReligionId(request.getReligionId());
        staff.setNationalityId(request.getNationalityId());
        staff.setCccdNo(normalizeNullable(request.getCccdNo()));
        staff.setCccdIssueDate(request.getCccdIssueDate());
        staff.setCccdIssuePlace(normalizeNullable(request.getCccdIssuePlace()));
        staff.setPhone(normalizeNullable(request.getPhone()));
        staff.setEmail(normalizeNullable(request.getEmail()));
        staff.setHealthStatus(normalizeNullable(request.getHealthStatus()));
        staff.setSocialInsuranceNo(normalizeNullable(request.getSocialInsuranceNo()));
        staff.setNote(normalizeNullable(request.getNote()));
    }

    private void applyStaffFields(Staff staff, StaffUpdateRequest request) {
        staff.setFullName(normalize(request.getFullName()));
        staff.setAliasName(normalizeNullable(request.getAliasName()));
        staff.setIdentityCode(normalizeNullable(request.getIdentityCode()));
        staff.setGender(request.getGender());
        staff.setDateOfBirth(request.getDateOfBirth());
        staff.setEthnicityId(request.getEthnicityId());
        staff.setReligionId(request.getReligionId());
        staff.setNationalityId(request.getNationalityId());
        staff.setCccdNo(normalizeNullable(request.getCccdNo()));
        staff.setCccdIssueDate(request.getCccdIssueDate());
        staff.setCccdIssuePlace(normalizeNullable(request.getCccdIssuePlace()));
        staff.setPhone(normalizeNullable(request.getPhone()));
        staff.setEmail(normalizeNullable(request.getEmail()));
        staff.setHealthStatus(normalizeNullable(request.getHealthStatus()));
        staff.setSocialInsuranceNo(normalizeNullable(request.getSocialInsuranceNo()));
        staff.setStatus(request.getStatus() != null ? request.getStatus() : staff.getStatus());
        staff.setNote(normalizeNullable(request.getNote()));
    }

    private StaffItemDto toItemDto(Staff staff) {
        return StaffItemDto.builder()
                .id(staff.getId())
                .staffCode(staff.getStaffCode())
                .fullName(staff.getFullName())
                .aliasName(staff.getAliasName())
                .unitId(staff.getUnit().getId())
                .gender(staff.getGender())
                .dateOfBirth(staff.getDateOfBirth())
                .phone(staff.getPhone())
                .email(staff.getEmail())
                .status(staff.getStatus())
                .cccdNo(staff.getCccdNo())
                .build();
    }

    private StaffDetailDto toDetailDto(Staff staff) {
        return StaffDetailDto.builder()
                .id(staff.getId())
                .userId(staff.getUser() != null ? staff.getUser().getId() : null)
                .unitId(staff.getUnit().getId())
                .staffCode(staff.getStaffCode())
                .identityCode(staff.getIdentityCode())
                .fullName(staff.getFullName())
                .aliasName(staff.getAliasName())
                .gender(staff.getGender())
                .dateOfBirth(staff.getDateOfBirth())
                .ethnicityId(staff.getEthnicityId())
                .religionId(staff.getReligionId())
                .nationalityId(staff.getNationalityId())
                .cccdNo(staff.getCccdNo())
                .cccdIssueDate(staff.getCccdIssueDate())
                .cccdIssuePlace(staff.getCccdIssuePlace())
                .phone(staff.getPhone())
                .email(staff.getEmail())
                .healthStatus(staff.getHealthStatus())
                .socialInsuranceNo(staff.getSocialInsuranceNo())
                .avatarFileId(staff.getAvatarFileId())
                .signatureFileId(staff.getSignatureFileId())
                .status(staff.getStatus())
                .note(staff.getNote())
                .build();
    }

    private Staff findStaff(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.STAFF_NOT_FOUND));
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

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 20 : pageSize;
    }

    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow < 0 ? 0 : pageNow;
    }
}
