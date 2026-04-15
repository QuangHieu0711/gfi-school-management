package com.gfi.backend.services.implement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.gfi.backend.repositories.specifications.SchoolYearSpecification;
import com.gfi.backend.services.interfaces.SchoolYearService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý logic quản lý năm học.
 * 
 * Trách nhiệm tách biệt:
 * - Logic query: SchoolYearSpecification
 * - Validate & load relations: private helpers
 * - Security: SecurityUtils
 */
@Service
@RequiredArgsConstructor
public class SchoolYearServiceImpl implements SchoolYearService {

    private final SchoolYearRepository schoolYearRepository;
    private final SemesterRepository semesterRepository;
    private final ClassroomRepository classroomRepository;
    private final SchoolYearSpecification schoolYearSpecification;

    // Tìm kiếm và phân trang năm học với filter
    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<SchoolYearItemDto, SchoolYearFilterDto> search(PageRequestDto<SchoolYearFilterDto> request) {
        SchoolYearFilterDto filter = request.getFilter() == null ? new SchoolYearFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<SchoolYear> page = schoolYearRepository.findAll(schoolYearSpecification.buildSpecification(filter), pageable);
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

    // Danh sách năm học cho dropdown/combobox
    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getOptions() {
        return schoolYearRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate").and(Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    // Chi tiết năm học theo ID
    @Override
    @Transactional(readOnly = true)
    public SchoolYearItemDto getById(Long id) {
        return toDto(findSchoolYear(id));
    }

    // Lấy năm học hiện tại
    @Override
    @Transactional(readOnly = true)
    public LookupItemDto getCurrentSchoolYear() {
        return schoolYearRepository.findByIsCurrentTrueAndDeletedFlagEquals(0)
                .map(sy -> LookupItemDto.builder()
                        .id(sy.getId())
                        .name(sy.getName())
                        .build())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
    }

    // Thêm mới năm học
    @Override
    @Transactional
    public SchoolYearItemDto create(SchoolYearCreateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        validateNoOverlappingSchoolYear(request.getStartDate(), request.getEndDate(), null);
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

    // Cập nhật năm học
    @Override
    @Transactional
    public SchoolYearItemDto update(Long id, SchoolYearUpdateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        validateNoOverlappingSchoolYear(request.getStartDate(), request.getEndDate(), id);
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

    // Xóa năm học (soft delete). Kiểm tra không được xóa nếu còn học kỳ hoặc lớp học.
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

        // Xóa mềm: đánh dấu xóa thay vì hard delete
        schoolYear.setDeletedFlag(1);
        schoolYear.setDeletedAt(LocalDateTime.now());
        schoolYear.setDeletedBy(SecurityUtils.getCurrentUsername());
        schoolYearRepository.save(schoolYear);
    }

    // Kiểm tra mã năm học phải duy nhất
    private void ensureCodeUnique(String code, Long id) {
        schoolYearRepository.findByCode(code)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SCHOOL_YEAR_CODE_ALREADY_EXISTS);
                });
    }

    // Kiểm tra tên năm học phải duy nhất
    private void ensureNameUnique(String name, Long id) {
        schoolYearRepository.findByName(name)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NAME_ALREADY_EXISTS);
                });
    }

    // Đánh dấu năm học là "hiện tại" và bỏ đánh dấu các năm học khác
    private void applyCurrentFlag(SchoolYear schoolYear) {
        if (Boolean.TRUE.equals(schoolYear.getIsCurrent())) {
            schoolYearRepository.clearCurrentExcept(schoolYear.getId());
        }
    }

    private SchoolYear findSchoolYear(Long id) {
        return schoolYearRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
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

    /**
     * Kiểm tra năm học không được overlapping với năm học khác.
     */
    private void validateNoOverlappingSchoolYear(LocalDate startDate, LocalDate endDate, Long excludeId) {
        List<SchoolYear> existingSchoolYears = schoolYearRepository.findAll();
        for (SchoolYear existing : existingSchoolYears) {
            // Bỏ qua năm học hiện tại (khi update)
            if (excludeId != null && existing.getId().equals(excludeId)) {
                continue;
            }
            // Bỏ qua năm học đã xóa
            if (existing.getDeletedFlag() == 1) {
                continue;
            }
            // Kiểm tra overlap: startDate < existing.endDate AND endDate > existing.startDate
            if (startDate != null && endDate != null &&
                    startDate.isBefore(existing.getEndDate()) && endDate.isAfter(existing.getStartDate())) {
                throw new UserMessageException(CommonErrorCode.SCHOOL_YEAR_DATE_OVERLAP);
            }
        }
    }

    /**
     * Kiểm tra string có nội dung hay không.
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Chuẩn hóa string: trim.
     */
    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Chuẩn hóa string nullable: return null nếu rỗng hoặc whitespace.
     */
    private String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * Lấy username người dùng hiện tại từ security context.
     */
    private String getCurrentUsername() {
        return SecurityUtils.getCurrentUsername();
    }

    /**
     * Chuẩn hóa kích thước trang phân trang.
     */
    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }

    /**
     * Chuẩn hóa số trang hiện tại.
     */
    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow <= 0 ? 1 : pageNow;
    }
}
