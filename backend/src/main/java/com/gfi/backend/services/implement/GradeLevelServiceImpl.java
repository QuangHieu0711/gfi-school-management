package com.gfi.backend.services.implement;

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
import com.gfi.backend.models.dtos.gradelevel.GradeLevelCreateRequest;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelDetailDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelFilterDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelListItemDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelUpdateRequest;
import com.gfi.backend.models.entities.GradeLevel;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.GradeLevelRepository;
import com.gfi.backend.repositories.GradeLevelSubjectRepository;
import com.gfi.backend.repositories.specifications.GradeLevelSpecification;
import com.gfi.backend.services.interfaces.GradeLevelService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý logic quản lý khối lớp.
 * 
 * Trách nhiệm tách biệt:
 * - Logic query: GradeLevelSpecification
 * - Validate & load relations: private helpers
 * - Security: SecurityUtils
 */
@Service
@RequiredArgsConstructor
public class GradeLevelServiceImpl implements GradeLevelService {

    private final GradeLevelRepository gradeLevelRepository;
    private final ClassroomRepository classroomRepository;
    private final GradeLevelSubjectRepository gradeLevelSubjectRepository;
    private final GradeLevelSpecification gradeLevelSpecification;

    // Tìm kiếm và phân trang khối lớp với filter
    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<GradeLevelListItemDto, GradeLevelFilterDto> search(
            PageRequestDto<GradeLevelFilterDto> request) {
        GradeLevelFilterDto filter = request.getFilter() == null ? new GradeLevelFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<GradeLevel> page = gradeLevelRepository.findAll(gradeLevelSpecification.buildSpecification(filter),
                pageable);
        List<GradeLevelListItemDto> items = page.getContent().stream().map(this::toListItemDto).toList();

        return PageResponseDto.<GradeLevelListItemDto, GradeLevelFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    // Danh sách khối lớp cho dropdown/combobox
    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getOptions() {
        return gradeLevelRepository
                .findAll(Sort.by(Sort.Direction.ASC, "gradeNumber").and(Sort.by(Sort.Direction.ASC, "id")))
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    // Chi tiết khối lớp theo ID
    @Override
    @Transactional(readOnly = true)
    public GradeLevelDetailDto getById(Long id) {
        return toDetailDto(findGradeLevel(id));
    }

    // Thêm mới khối lớp
    @Override
    @Transactional
    public GradeLevelDetailDto create(GradeLevelCreateRequest request) {
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
        gradeLevel.setCreatedBy(SecurityUtils.getCurrentUsername());
        return toDetailDto(gradeLevelRepository.save(gradeLevel));
    }

    // Cập nhật khối lớp
    @Override
    @Transactional
    public GradeLevelDetailDto update(Long id, GradeLevelUpdateRequest request) {
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
        gradeLevel.setUpdatedBy(SecurityUtils.getCurrentUsername());
        return toDetailDto(gradeLevelRepository.save(gradeLevel));
    }

    // Xóa khối lớp (soft delete). Kiểm tra không được xóa nếu còn lớp học hoặc cấu
    // hình môn học.
    @Override
    @Transactional
    public void delete(Long id) {
        GradeLevel gradeLevel = findGradeLevel(id);
        if (classroomRepository.countByGradeLevelId(id) > 0
                || gradeLevelSubjectRepository.countByGradeLevelId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.GRADE_LEVEL_IN_USE);
        }

        // Xóa mềm: đánh dấu xóa thay vì hard delete
        gradeLevel.setDeletedFlag(1);
        gradeLevel.setDeletedAt(LocalDateTime.now());
        gradeLevel.setDeletedBy(SecurityUtils.getCurrentUsername());
        gradeLevelRepository.save(gradeLevel);
    }

    private GradeLevel findGradeLevel(Long id) {
        return gradeLevelRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.GRADE_LEVEL_NOT_FOUND));
    }

    // Kiểm tra mã khối lớp phải duy nhất
    private void ensureCodeUnique(String code, Long id) {
        gradeLevelRepository.findByCode(code)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.GRADE_LEVEL_CODE_ALREADY_EXISTS);
                });
    }

    // Kiểm tra tên khối lớp phải duy nhất
    private void ensureNameUnique(String name, Long id) {
        gradeLevelRepository.findByName(name)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.GRADE_LEVEL_NAME_ALREADY_EXISTS);
                });
    }

    // Kiểm tra thứ tự khối lớp phải duy nhất
    private void ensureGradeNumberUnique(Integer gradeNumber, Long id) {
        gradeLevelRepository.findByGradeNumber(gradeNumber)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.GRADE_LEVEL_NUMBER_ALREADY_EXISTS);
                });
    }

    private GradeLevelDetailDto toDetailDto(GradeLevel gradeLevel) {
        return GradeLevelDetailDto.builder()
                .id(gradeLevel.getId())
                .code(gradeLevel.getCode())
                .name(gradeLevel.getName())
                .gradeNumber(gradeLevel.getGradeNumber())
                .status(gradeLevel.getStatus())
                .description(gradeLevel.getDescription())
                .build();
    }

    private GradeLevelListItemDto toListItemDto(GradeLevel gradeLevel) {
        return GradeLevelListItemDto.builder()
                .id(gradeLevel.getId())
                .code(gradeLevel.getCode())
                .name(gradeLevel.getName())
                .gradeNumber(gradeLevel.getGradeNumber())
                .status(gradeLevel.getStatus())
                .build();
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
     * Chuẩn hóa kích thước trang phân trang.
     */
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 20; // Default page size
        }
        return Math.min(pageSize, 100); // Max 100 items per page
    }

    /**
     * Chuẩn hóa số trang hiện tại.
     */
    private int normalizePageNow(Integer pageNow) {
        if (pageNow == null || pageNow < 0) {
            return 0; // Default first page
        }
        return pageNow;
    }
}
