package com.gfi.backend.services.implement;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import com.gfi.backend.models.dtos.role.RoleCreateRequest;
import com.gfi.backend.models.dtos.role.RoleFilterDto;
import com.gfi.backend.models.dtos.role.RoleItemDto;
import com.gfi.backend.models.dtos.role.RoleUpdateRequest;
import com.gfi.backend.models.entities.Role;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.RoleRepository;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.services.interfaces.RoleService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public PageResponseDto<RoleItemDto, RoleFilterDto> search(PageRequestDto<RoleFilterDto> request) {
        RoleFilterDto filter = request.getFilter() == null ? new RoleFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageRequest.of(pageNow - 1, pageSize);

        Page<Role> page = roleRepository.findAll(buildSpecification(filter), pageable);
        List<RoleItemDto> items = page.getContent().stream()
                .map(this::toDto)
                .toList();

        return PageResponseDto.<RoleItemDto, RoleFilterDto>builder()
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
        return roleRepository.findAll(Sort.by(Sort.Direction.ASC, "roleName")).stream()
                .map(role -> LookupItemDto.builder()
                        .id(role.getId())
                        .name(role.getRoleName())
                        .build())
                .toList();
    }

    @Override
    public RoleItemDto getById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));
        return toDto(role);
    }

    @Override
    @Transactional
    public RoleItemDto create(RoleCreateRequest request) {
        String roleName = normalize(request.getRoleName());
        if (roleRepository.existsByRoleName(roleName)) {
            throw new UserMessageException(CommonErrorCode.ROLE_NAME_ALREADY_EXISTS);
        }

        Role role = new Role();
        role.setRoleName(roleName);
        role.setDescription(normalizeNullable(request.getDescription()));
        role.setStatus(request.getStatus());
        role.setCreatedBy(getCurrentUsername());
        return toDto(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleItemDto update(Long id, RoleUpdateRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));

        String roleName = normalize(request.getRoleName());
        roleRepository.findByRoleName(roleName)
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> {
                    throw new UserMessageException(CommonErrorCode.ROLE_NAME_ALREADY_EXISTS);
                });

        role.setRoleName(roleName);
        role.setDescription(normalizeNullable(request.getDescription()));
        role.setStatus(request.getStatus());
        role.setUpdatedBy(getCurrentUsername());
        return toDto(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));

        if (userRepository.countByRoleId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.ROLE_IN_USE);
        }

        roleRepository.delete(role);
    }

    private Specification<Role> buildSpecification(RoleFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(filter.getRoleName())) {
                String keyword = "%" + filter.getRoleName().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("roleName")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
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

    private RoleItemDto toDto(Role role) {
        return RoleItemDto.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .status(role.getStatus())
                .build();
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
