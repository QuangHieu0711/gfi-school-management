package com.gfi.backend.services.implement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.semester.SemesterCreateRequest;
import com.gfi.backend.models.dtos.semester.SemesterFilterDto;
import com.gfi.backend.models.dtos.semester.SemesterItemDto;
import com.gfi.backend.models.dtos.semester.SemesterUpdateRequest;
import com.gfi.backend.models.entities.SchoolYear;
import com.gfi.backend.models.entities.Semester;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.SchoolYearRepository;
import com.gfi.backend.repositories.SemesterRepository;
import com.gfi.backend.repositories.specifications.SemesterSpecification;
import com.gfi.backend.services.interfaces.SemesterService;
import com.gfi.backend.utils.SecurityUtils;

import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;

/**
 * Service xử lý logic quản lý học kỳ.
 * 
 * Trách nhiệm tách biệt:
 * - Logic query: SemesterSpecification
 * - Validate & load relations: private helpers
 * - Security: SecurityUtils
 */
@Service
@RequiredArgsConstructor
public class SemesterServiceImpl implements SemesterService {

    private final SemesterRepository semesterRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final SemesterSpecification semesterSpecification;

    // Tìm kiếm và phân trang học kỳ với filter
    @Override
    @Transactional(readOnly = true)
    public List<SemesterItemDto> search(SemesterFilterDto filter) {
        SemesterFilterDto safeFilter = filter == null ? new SemesterFilterDto() : filter;
        return semesterRepository
                .findAll(semesterSpecification.buildSpecification(safeFilter), Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    // Danh sách học kỳ cho dropdown/combobox
    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getOptions(Long schoolYearId) {
        Specification<Semester> specification = (root, query, cb) -> schoolYearId == null
                ? cb.conjunction()
                : cb.equal(root.join("schoolYear", JoinType.INNER).get("id"), schoolYearId);

        return semesterRepository
                .findAll(specification,
                        Sort.by(Sort.Direction.ASC, "semesterOrder").and(Sort.by(Sort.Direction.ASC, "id")))
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    // Chi tiết học kỳ theo ID
    @Override
    @Transactional(readOnly = true)
    public SemesterItemDto getById(Long id) {
        return toDto(findSemester(id));
    }

    // Thêm mới học kỳ
    @Override
    @Transactional
    public SemesterItemDto create(SemesterCreateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        SchoolYear schoolYear = findSchoolYear(request.getSchoolYearId());
        validateSemesterWithinSchoolYearDates(request.getStartDate(), request.getEndDate(), schoolYear);
        validateSemesterUnique(request.getSchoolYearId(), normalize(request.getCode()), normalize(request.getName()),
                request.getSemesterOrder(), null);
        validateNoOverlappingSemester(request.getSchoolYearId(), request.getStartDate(), request.getEndDate(), null);

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

    // Cập nhật học kỳ
    @Override
    @Transactional
    public SemesterItemDto update(Long id, SemesterUpdateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        Semester semester = findSemester(id);
        SchoolYear schoolYear = findSchoolYear(request.getSchoolYearId());
        validateSemesterWithinSchoolYearDates(request.getStartDate(), request.getEndDate(), schoolYear);
        validateSemesterUnique(request.getSchoolYearId(), normalize(request.getCode()), normalize(request.getName()),
                request.getSemesterOrder(), id);
        validateNoOverlappingSemester(request.getSchoolYearId(), request.getStartDate(), request.getEndDate(), id);

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

    // Xóa học kỳ (soft delete)
    @Override
    @Transactional
    public void delete(Long id) {
        Semester semester = findSemester(id);

        // Xóa mềm: đánh dấu xóa thay vì hard delete
        semester.setDeletedFlag(1);
        semester.setDeletedAt(LocalDateTime.now());
        semester.setDeletedBy(SecurityUtils.getCurrentUsername());
        semesterRepository.save(semester);
    }

    // Kiểm tra tính duy nhất của mã, tên và thứ tự học kỳ trong năm học
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

    // Đánh dấu học kỳ là "hiện tại" và bỏ đánh dấu các học kỳ khác
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

    /**
     * Kiểm tra thời gian học kỳ phải nằm trong khoảng thời gian của năm học.
     */
    private void validateSemesterWithinSchoolYearDates(LocalDate semesterStart, LocalDate semesterEnd,
            SchoolYear schoolYear) {
        if (semesterStart != null && schoolYear.getStartDate() != null
                && semesterStart.isBefore(schoolYear.getStartDate())) {
            throw new UserMessageException(CommonErrorCode.SEMESTER_START_DATE_INVALID);
        }
        if (semesterEnd != null && schoolYear.getEndDate() != null && semesterEnd.isAfter(schoolYear.getEndDate())) {
            throw new UserMessageException(CommonErrorCode.SEMESTER_END_DATE_INVALID);
        }
    }

    /**
     * Kiểm tra học kỳ không được overlapping với học kỳ khác trong cùng năm học.
     */
    private void validateNoOverlappingSemester(Long schoolYearId, LocalDate startDate, LocalDate endDate,
            Long excludeId) {
        List<Semester> existingSemesters = semesterRepository.findBySchoolYearId(schoolYearId);
        for (Semester existing : existingSemesters) {
            // Bỏ qua semester hiện tại (khi update)
            if (excludeId != null && existing.getId().equals(excludeId)) {
                continue;
            }
            // Bỏ qua semester đã xóa
            if (existing.getDeletedFlag() == 1) {
                continue;
            }
            // Kiểm tra overlap: startDate < existing.endDate AND endDate >
            // existing.startDate
            if (startDate != null && endDate != null &&
                    startDate.isBefore(existing.getEndDate()) && endDate.isAfter(existing.getStartDate())) {
                throw new UserMessageException(CommonErrorCode.SEMESTER_DATE_OVERLAP);
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
}
