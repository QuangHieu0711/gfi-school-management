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
import com.gfi.backend.models.dtos.subject.SubjectCreateRequest;
import com.gfi.backend.models.dtos.subject.SubjectDetailDto;
import com.gfi.backend.models.dtos.subject.SubjectFilterDto;
import com.gfi.backend.models.dtos.subject.SubjectListItemDto;
import com.gfi.backend.models.dtos.subject.SubjectUpdateRequest;
import com.gfi.backend.models.entities.Subject;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.ClassroomSubjectRepository;
import com.gfi.backend.repositories.GradeLevelSubjectRepository;
import com.gfi.backend.repositories.SubjectRepository;
import com.gfi.backend.repositories.specifications.SubjectSpecification;
import com.gfi.backend.services.interfaces.SubjectService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý logic quản lý môn học.
 * 
 * Trách nhiệm tách biệt:
 * - Logic query: SubjectSpecification
 * - Validate & load relations: private helpers
 * - Security: SecurityUtils
 */
@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final GradeLevelSubjectRepository gradeLevelSubjectRepository;
    private final ClassroomSubjectRepository classroomSubjectRepository;
    private final SubjectSpecification subjectSpecification;

    // Tìm kiếm và phân trang môn học với filter
    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<SubjectListItemDto, SubjectFilterDto> search(PageRequestDto<SubjectFilterDto> request) {
        SubjectFilterDto filter = request.getFilter() == null ? new SubjectFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<Subject> page = subjectRepository.findAll(subjectSpecification.buildSpecification(filter), pageable);
        List<SubjectListItemDto> items = page.getContent().stream().map(this::toListItemDto).toList();

        return PageResponseDto.<SubjectListItemDto, SubjectFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    // Danh sách môn học cho dropdown/combobox
    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getOptions() {
        return subjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id")))
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    // Chi tiết môn học theo ID
    @Override
    @Transactional(readOnly = true)
    public SubjectDetailDto getById(Long id) {
        return toDetailDto(findSubject(id));
    }

    // Thêm mới môn học
    @Override
    @Transactional
    public SubjectDetailDto create(SubjectCreateRequest request) {
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        ensureCodeUnique(code, null);
        ensureNameUnique(name, null);

        Subject subject = new Subject();
        subject.setCode(code);
        subject.setName(name);
        subject.setType(request.getType());
        subject.setDescription(normalizeNullable(request.getDescription()));
        subject.setStatus(request.getStatus());
        subject.setCreatedBy(SecurityUtils.getCurrentUsername());
        return toDetailDto(subjectRepository.save(subject));
    }

    // Cập nhật môn học
    @Override
    @Transactional
    public SubjectDetailDto update(Long id, SubjectUpdateRequest request) {
        Subject subject = findSubject(id);
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        ensureCodeUnique(code, id);
        ensureNameUnique(name, id);

        subject.setCode(code);
        subject.setName(name);
        subject.setType(request.getType());
        subject.setDescription(normalizeNullable(request.getDescription()));
        subject.setStatus(request.getStatus());
        subject.setUpdatedBy(SecurityUtils.getCurrentUsername());
        return toDetailDto(subjectRepository.save(subject));
    }

    // Xóa môn học (soft delete). Kiểm tra không được xóa nếu còn cấu hình khối lớp hoặc lớp học.
    @Override
    @Transactional
    public void delete(Long id) {
        Subject subject = findSubject(id);
        if (gradeLevelSubjectRepository.countBySubjectId(id) > 0 || classroomSubjectRepository.countBySubjectId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.SUBJECT_IN_USE);
        }

        // Xóa mềm: đánh dấu xóa thay vì hard delete
        subject.setDeletedFlag(1);
        subject.setDeletedAt(LocalDateTime.now());
        subject.setDeletedBy(SecurityUtils.getCurrentUsername());
        subjectRepository.save(subject);
    }

    private Subject findSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SUBJECT_NOT_FOUND));
    }

    // Kiểm tra mã môn học phải duy nhất
    private void ensureCodeUnique(String code, Long id) {
        subjectRepository.findByCode(code)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SUBJECT_CODE_ALREADY_EXISTS);
                });
    }

    // Kiểm tra tên môn học phải duy nhất
    private void ensureNameUnique(String name, Long id) {
        subjectRepository.findByName(name)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SUBJECT_NAME_ALREADY_EXISTS);
                });
    }

    private SubjectDetailDto toDetailDto(Subject subject) {
        return SubjectDetailDto.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .type(subject.getType())
                .description(subject.getDescription())
                .status(subject.getStatus())
                .build();
    }

    private SubjectListItemDto toListItemDto(Subject subject) {
        return SubjectListItemDto.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .type(subject.getType())
                .status(subject.getStatus())
                .build();
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
}
