package com.gfi.backend.services.implement;

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
import com.gfi.backend.models.dtos.subject.SubjectCreateRequest;
import com.gfi.backend.models.dtos.subject.SubjectFilterDto;
import com.gfi.backend.models.dtos.subject.SubjectItemDto;
import com.gfi.backend.models.dtos.subject.SubjectUpdateRequest;
import com.gfi.backend.models.entities.Subject;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.GradeLevelSubjectRepository;
import com.gfi.backend.repositories.SubjectRepository;
import com.gfi.backend.services.interfaces.SubjectService;
import com.gfi.backend.utils.PageableUtils;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final GradeLevelSubjectRepository gradeLevelSubjectRepository;

    @Override
    public PageResponseDto<SubjectItemDto, SubjectFilterDto> search(PageRequestDto<SubjectFilterDto> request) {
        SubjectFilterDto filter = request.getFilter() == null ? new SubjectFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<Subject> page = subjectRepository.findAll(buildSpecification(filter), pageable);
        List<SubjectItemDto> items = page.getContent().stream().map(this::toDto).toList();

        return PageResponseDto.<SubjectItemDto, SubjectFilterDto>builder()
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
        return subjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id")))
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    @Override
    public SubjectItemDto getById(Long id) {
        return toDto(findSubject(id));
    }

    @Override
    @Transactional
    public SubjectItemDto create(SubjectCreateRequest request) {
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
        subject.setCreatedBy(getCurrentUsername());
        return toDto(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public SubjectItemDto update(Long id, SubjectUpdateRequest request) {
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
        subject.setUpdatedBy(getCurrentUsername());
        return toDto(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Subject subject = findSubject(id);
        if (gradeLevelSubjectRepository.countBySubjectId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.SUBJECT_IN_USE);
        }
        subjectRepository.delete(subject);
    }

    private Subject findSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SUBJECT_NOT_FOUND));
    }

    private void ensureCodeUnique(String code, Long id) {
        subjectRepository.findByCode(code)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SUBJECT_CODE_ALREADY_EXISTS);
                });
    }

    private void ensureNameUnique(String name, Long id) {
        subjectRepository.findByName(name)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SUBJECT_NAME_ALREADY_EXISTS);
                });
    }

    private Specification<Subject> buildSpecification(SubjectFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(filter.getSubject())) {
                String keyword = "%" + filter.getSubject().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)));
            }
            if (filter.getType() != null) {
                predicates.add(cb.equal(root.get("type"), filter.getType()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private SubjectItemDto toDto(Subject subject) {
        return SubjectItemDto.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .type(subject.getType())
                .description(subject.getDescription())
                .status(subject.getStatus())
                .build();
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
