package com.gfi.backend.services.implement;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.staff.StaffJobHistoryCreateRequest;
import com.gfi.backend.models.dtos.staff.StaffJobHistoryDto;
import com.gfi.backend.models.dtos.staff.StaffJobHistoryFilterDto;
import com.gfi.backend.models.dtos.staff.StaffJobHistoryUpdateRequest;
import com.gfi.backend.models.entities.Staff;
import com.gfi.backend.models.entities.StaffJobHistory;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.StaffJobHistoryRepository;
import com.gfi.backend.repositories.StaffRepository;
import com.gfi.backend.services.interfaces.StaffJobHistoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaffJobHistoryServiceImpl implements StaffJobHistoryService {

    private final StaffRepository staffRepository;
    private final StaffJobHistoryRepository staffJobHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<StaffJobHistoryDto, StaffJobHistoryFilterDto> search(
            PageRequestDto<StaffJobHistoryFilterDto> request) {
        StaffJobHistoryFilterDto filter = request.getFilter() == null ? new StaffJobHistoryFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());

        if (filter.getStaffId() != null) {
            ensureStaffExists(filter.getStaffId());
        }

        Pageable pageable = PageRequest.of(pageNow - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "fromDate").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<StaffJobHistory> page = staffJobHistoryRepository.findAll(buildSpecification(filter), pageable);
        List<StaffJobHistoryDto> items = page.getContent().stream()
                .map(this::toDto)
                .toList();

        return PageResponseDto.<StaffJobHistoryDto, StaffJobHistoryFilterDto>builder()
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
    public StaffJobHistoryDto getById(Long id) {
        return toDto(findJobHistory(id));
    }

    @Override
    @Transactional
    public StaffJobHistoryDto create(StaffJobHistoryCreateRequest request) {
        validateDateRange(request.getFromDate(), request.getToDate());

        StaffJobHistory jobHistory = new StaffJobHistory();
        jobHistory.setStaff(ensureStaffExists(request.getStaffId()));
        applyFields(jobHistory, request.getFromDate(), request.getToDate(), request.getUnitId(),
                request.getDepartmentId(), request.getWorkingPositionId(), request.getTitleId(),
                request.getEmploymentTypeId(), request.getDecisionNo(), request.getNote());

        return toDto(staffJobHistoryRepository.save(jobHistory));
    }

    @Override
    @Transactional
    public StaffJobHistoryDto update(Long id, StaffJobHistoryUpdateRequest request) {
        validateDateRange(request.getFromDate(), request.getToDate());

        StaffJobHistory jobHistory = findJobHistory(id);
        applyFields(jobHistory, request.getFromDate(), request.getToDate(), request.getUnitId(),
                request.getDepartmentId(), request.getWorkingPositionId(), request.getTitleId(),
                request.getEmploymentTypeId(), request.getDecisionNo(), request.getNote());

        return toDto(staffJobHistoryRepository.save(jobHistory));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        StaffJobHistory jobHistory = findJobHistory(id);
        staffJobHistoryRepository.delete(jobHistory);
    }

    private Staff ensureStaffExists(Long staffId) {
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.STAFF_NOT_FOUND));
    }

    private StaffJobHistory findJobHistory(Long id) {
        return staffJobHistoryRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.BAD_REQUEST.getCode(),
                        "Khong tim thay qua trinh cong tac"));
    }

    private Specification<StaffJobHistory> buildSpecification(StaffJobHistoryFilterDto filter) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (filter.getStaffId() != null) {
                predicates.add(cb.equal(root.get("staff").get("id"), filter.getStaffId()));
            }
            if (filter.getUnitId() != null) {
                predicates.add(cb.equal(root.get("unitId"), filter.getUnitId()));
            }
            if (filter.getDepartmentId() != null) {
                predicates.add(cb.equal(root.get("departmentId"), filter.getDepartmentId()));
            }
            if (filter.getWorkingPositionId() != null) {
                predicates.add(cb.equal(root.get("workingPositionId"), filter.getWorkingPositionId()));
            }
            if (filter.getTitleId() != null) {
                predicates.add(cb.equal(root.get("titleId"), filter.getTitleId()));
            }
            if (filter.getEmploymentTypeId() != null) {
                predicates.add(cb.equal(root.get("employmentTypeId"), filter.getEmploymentTypeId()));
            }
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fromDate"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("toDate"), filter.getToDate()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private void validateDateRange(java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new UserMessageException(CommonErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void applyFields(StaffJobHistory jobHistory, java.time.LocalDate fromDate, java.time.LocalDate toDate,
            Long unitId, String departmentId, String workingPositionId, String titleId, String employmentTypeId,
            String decisionNo, String note) {
        jobHistory.setFromDate(fromDate);
        jobHistory.setToDate(toDate);
        jobHistory.setUnitId(unitId);
        jobHistory.setDepartmentId(departmentId);
        jobHistory.setWorkingPositionId(workingPositionId);
        jobHistory.setTitleId(titleId);
        jobHistory.setEmploymentTypeId(employmentTypeId);
        jobHistory.setDecisionNo(normalizeNullable(decisionNo));
        jobHistory.setNote(normalizeNullable(note));
    }

    private StaffJobHistoryDto toDto(StaffJobHistory jobHistory) {
        return StaffJobHistoryDto.builder()
                .id(jobHistory.getId())
                .staffId(jobHistory.getStaff() == null ? null : jobHistory.getStaff().getId())
                .fromDate(jobHistory.getFromDate())
                .toDate(jobHistory.getToDate())
                .unitId(jobHistory.getUnitId())
                .departmentId(jobHistory.getDepartmentId())
                .workingPositionId(jobHistory.getWorkingPositionId())
                .titleId(jobHistory.getTitleId())
                .employmentTypeId(jobHistory.getEmploymentTypeId())
                .decisionNo(jobHistory.getDecisionNo())
                .note(jobHistory.getNote())
                .build();
    }

    private String normalizeNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 20 : pageSize;
    }

    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow <= 0 ? 1 : pageNow;
    }
}
