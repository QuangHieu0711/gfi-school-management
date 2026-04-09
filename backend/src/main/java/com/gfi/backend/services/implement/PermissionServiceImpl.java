package com.gfi.backend.services.implement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.permission.PermissionItemDto;
import com.gfi.backend.models.dtos.permission.PermissionSaveRequest;
import com.gfi.backend.models.entities.Menu;
import com.gfi.backend.models.entities.Permission;
import com.gfi.backend.models.entities.Role;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.MenuRepository;
import com.gfi.backend.repositories.PermissionRepository;
import com.gfi.backend.repositories.RoleRepository;
import com.gfi.backend.services.interfaces.PermissionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;

    @Override
    public List<PermissionItemDto> getByRoleId(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));

        List<Permission> permissions = permissionRepository.findAllByRoleIdOrderByIdAsc(roleId);
        Map<Long, PermissionItemDto> resultMap = new HashMap<>();

        for (Permission permission : permissions) {
            PermissionItemDto dto = toDto(permission);
            resultMap.put(dto.getMenuId(), dto);
            appendParentMenus(resultMap, role, permission.getMenu());
        }

        return resultMap.values().stream()
                .sorted(Comparator
                        .comparing((PermissionItemDto item) -> item.getParentId() != null)
                        .thenComparing(item -> item.getParentId() == null ? 0L : item.getParentId())
                        .thenComparing(PermissionItemDto::getMenuId))
                .toList();
    }

    @Override
    @Transactional
    public List<PermissionItemDto> savePermissions(List<PermissionSaveRequest> requests) {
        validateBatchRequest(requests);

        Set<String> uniquePairs = new HashSet<>();
        Set<Long> roleIds = new HashSet<>();
        String currentUsername = getCurrentUsername();

        for (PermissionSaveRequest request : requests) {
            ensureUniquePair(uniquePairs, request.getRoleId(), request.getMenuId());
            roleIds.add(request.getRoleId());

            Permission permission = permissionRepository.findByRoleIdAndMenuId(request.getRoleId(), request.getMenuId())
                    .orElse(null);

            if (!hasAnyPermission(request)) {
                if (permission != null) {
                    permissionRepository.delete(permission);
                }
                continue;
            }

            boolean isNewPermission = permission == null;
            if (isNewPermission) {
                permission = new Permission();
            }

            applyRequest(permission, request.getMenuId(), request.getRoleId(), request.getIsAdd(),
                    request.getIsDelete(), request.getIsDownload(), request.getIsEdit(), request.getIsView());

            if (isNewPermission) {
                permission.setCreatedBy(currentUsername);
            } else {
                permission.setUpdatedBy(currentUsername);
            }

            permissionRepository.save(permission);
        }

        if (roleIds.size() == 1) {
            return getByRoleId(roleIds.iterator().next());
        }

        List<PermissionItemDto> results = new ArrayList<>();
        for (Long roleId : roleIds) {
            results.addAll(getByRoleId(roleId));
        }
        return results;
    }

    private void appendParentMenus(Map<Long, PermissionItemDto> resultMap, Role role, Menu menu) {
        Menu currentParent = menu.getParentMenu();
        while (currentParent != null) {
            resultMap.putIfAbsent(currentParent.getId(), toParentDto(role, currentParent));
            currentParent = currentParent.getParentMenu();
        }
    }

    private PermissionItemDto toParentDto(Role role, Menu menu) {
        return PermissionItemDto.builder()
                .id(null)
                .roleId(role.getId())
                .roleCode(role.getCode())
                .roleName(role.getRoleName())
                .menuId(menu.getId())
                .parentId(menu.getParentMenu() == null ? null : menu.getParentMenu().getId())
                .menuCode(menu.getCode())
                .menuName(menu.getName())
                .menuUrl(menu.getUrl())
                .icon(menu.getIcon())
                .ordinal(menu.getOrdinal())
                .isView(0)
                .isAdd(0)
                .isEdit(0)
                .isDelete(0)
                .isDownload(0)
                .build();
    }

    private void applyRequest(Permission permission, Long menuId, Long roleId, Integer isAdd,
            Integer isDelete, Integer isDownload, Integer isEdit, Integer isView) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.MENU_NOT_FOUND));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));

        permission.setMenu(menu);
        permission.setRole(role);
        permission.setIsAdd(isAdd);
        permission.setIsDelete(isDelete);
        permission.setIsDownload(isDownload);
        permission.setIsEdit(isEdit);
        permission.setIsView(isView);
    }

    private boolean hasAnyPermission(PermissionSaveRequest request) {
        return isEnabled(request.getIsAdd())
                || isEnabled(request.getIsDelete())
                || isEnabled(request.getIsDownload())
                || isEnabled(request.getIsEdit())
                || isEnabled(request.getIsView());
    }

    private boolean isEnabled(Integer value) {
        return value != null && value == 1;
    }

    private void validateBatchRequest(List<?> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new UserMessageException("Danh sach quyen khong duoc de trong");
        }
    }

    private void ensureUniquePair(Set<String> uniquePairs, Long roleId, Long menuId) {
        String pairKey = roleId + "-" + menuId;
        if (!uniquePairs.add(pairKey)) {
            throw new UserMessageException("Danh sach quyen bi trung roleId va menuId");
        }
    }

    private PermissionItemDto toDto(Permission permission) {
        return PermissionItemDto.builder()
                .id(permission.getId())
                .roleId(permission.getRole().getId())
                .roleCode(permission.getRole().getCode())
                .roleName(permission.getRole().getRoleName())
                .menuId(permission.getMenu().getId())
                .parentId(permission.getMenu().getParentMenu() == null ? null : permission.getMenu().getParentMenu().getId())
                .menuCode(permission.getMenu().getCode())
                .menuName(permission.getMenu().getName())
                .menuUrl(permission.getMenu().getUrl())
                .icon(permission.getMenu().getIcon())
                .ordinal(permission.getMenu().getOrdinal())
                .isView(permission.getIsView())
                .isAdd(permission.getIsAdd())
                .isEdit(permission.getIsEdit())
                .isDelete(permission.getIsDelete())
                .isDownload(permission.getIsDownload())
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
