package com.gfi.backend.services.implement;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.permission.PermissionCreateRequest;
import com.gfi.backend.models.dtos.permission.PermissionFilterDto;
import com.gfi.backend.models.dtos.permission.PermissionItemDto;
import com.gfi.backend.models.dtos.permission.PermissionUpdateRequest;
import com.gfi.backend.models.entities.Menu;
import com.gfi.backend.models.entities.Permission;
import com.gfi.backend.models.entities.Role;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.MenuRepository;
import com.gfi.backend.repositories.PermissionRepository;
import com.gfi.backend.repositories.RoleRepository;
import com.gfi.backend.services.interfaces.PermissionService;
import com.gfi.backend.utils.PageableUtils;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;

    @Override
    public PageResponseDto<PermissionItemDto, PermissionFilterDto> search(PageRequestDto<PermissionFilterDto> request) {
        PermissionFilterDto filter = request.getFilter() == null ? new PermissionFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());

        Page<Permission> page = permissionRepository.findAll(buildSpecification(filter), PageableUtils.newestFirst(pageNow, pageSize));
        List<PermissionItemDto> items = page.getContent().stream()
                .map(this::toDto)
                .toList();

        return PageResponseDto.<PermissionItemDto, PermissionFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    @Override
    public PermissionItemDto getById(Long id) {
        return toDto(findPermission(id));
    }

    @Override
    @Transactional
    public PermissionItemDto create(PermissionCreateRequest request) {
        if (permissionRepository.existsByRoleIdAndMenuId(request.getRoleId(), request.getMenuId())) {
            throw new UserMessageException(CommonErrorCode.PERMISSION_ALREADY_EXISTS);
        }

        Permission permission = new Permission();
        applyRequest(permission, request.getMenuId(), request.getRoleId(), request.getIsAdd(), request.getIsApprove(),
                request.getIsDelete(), request.getIsDownload(), request.getIsEdit(), request.getIsView());
        permission.setCreatedBy(getCurrentUsername());
        return toDto(permissionRepository.save(permission));
    }

    @Override
    @Transactional
    public PermissionItemDto update(Long id, PermissionUpdateRequest request) {
        Permission permission = findPermission(id);
        permissionRepository.findByRoleIdAndMenuId(request.getRoleId(), request.getMenuId())
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> {
                    throw new UserMessageException(CommonErrorCode.PERMISSION_ALREADY_EXISTS);
                });

        applyRequest(permission, request.getMenuId(), request.getRoleId(), request.getIsAdd(), request.getIsApprove(),
                request.getIsDelete(), request.getIsDownload(), request.getIsEdit(), request.getIsView());
        permission.setUpdatedBy(getCurrentUsername());
        return toDto(permissionRepository.save(permission));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        permissionRepository.delete(findPermission(id));
    }

    private void applyRequest(Permission permission, Long menuId, Long roleId, Integer isAdd, Integer isApprove,
            Integer isDelete, Integer isDownload, Integer isEdit, Integer isView) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.MENU_NOT_FOUND));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));

        permission.setMenu(menu);
        permission.setRole(role);
        permission.setIsAdd(isAdd);
        permission.setIsApprove(isApprove);
        permission.setIsDelete(isDelete);
        permission.setIsDownload(isDownload);
        permission.setIsEdit(isEdit);
        permission.setIsView(isView);
    }

    private Specification<Permission> buildSpecification(PermissionFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getMenuId() != null) {
                predicates.add(cb.equal(root.get("menu").get("id"), filter.getMenuId()));
            }
            if (filter.getRoleId() != null) {
                predicates.add(cb.equal(root.get("role").get("id"), filter.getRoleId()));
            }
            if (hasText(filter.getMenuKeyword())) {
                String keyword = "%" + filter.getMenuKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("menu").get("code")), keyword),
                        cb.like(cb.lower(root.get("menu").get("name")), keyword)));
            }
            if (hasText(filter.getRoleKeyword())) {
                String keyword = "%" + filter.getRoleKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("role").get("code")), keyword),
                        cb.like(cb.lower(root.get("role").get("roleName")), keyword)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Permission findPermission(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.PERMISSION_NOT_FOUND));
    }

    private PermissionItemDto toDto(Permission permission) {
        return PermissionItemDto.builder()
                .id(permission.getId())
                .menuId(permission.getMenu().getId())
                .menuCode(permission.getMenu().getCode())
                .menuName(permission.getMenu().getName())
                .roleId(permission.getRole().getId())
                .roleCode(permission.getRole().getCode())
                .roleName(permission.getRole().getRoleName())
                .isAdd(permission.getIsAdd())
                .isApprove(permission.getIsApprove())
                .isDelete(permission.getIsDelete())
                .isDownload(permission.getIsDownload())
                .isEdit(permission.getIsEdit())
                .isView(permission.getIsView())
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

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
