package com.gfi.backend.services.implement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.roleassignment.RoleAssignmentPermissionItemDto;
import com.gfi.backend.models.dtos.roleassignment.RoleAssignmentPermissionItemRequest;
import com.gfi.backend.models.dtos.roleassignment.RoleAssignmentPermissionResponse;
import com.gfi.backend.models.dtos.roleassignment.RoleAssignmentPermissionSaveRequest;
import com.gfi.backend.models.entities.Role;
import com.gfi.backend.models.entities.RoleAssignmentPermission;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.RoleAssignmentPermissionRepository;
import com.gfi.backend.repositories.RoleRepository;
import com.gfi.backend.services.interfaces.RoleAssignmentPermissionService;
import com.gfi.backend.utils.SecurityUtils;
import com.gfi.backend.utils.ScopeFilterUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleAssignmentPermissionServiceImpl implements RoleAssignmentPermissionService {

    private static final String FEATURE = "ROLE_MANAGEMENT";

    private final RoleRepository roleRepository;
    private final RoleAssignmentPermissionRepository roleAssignmentPermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public RoleAssignmentPermissionResponse getByCreatorRoleId(Long creatorRoleId) {
        ScopeFilterUtils.checkAccess(FEATURE);

        Role creatorRole = roleRepository.findById(creatorRoleId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));

        List<Role> allTargetRoles = roleRepository.findAll(Sort.by(Sort.Direction.ASC, "roleName"));

        Map<Long, RoleAssignmentPermission> existingMap = roleAssignmentPermissionRepository
                .findAllByCreatorRoleId(creatorRoleId)
                .stream()
                .collect(Collectors.toMap(
                        rap -> rap.getTargetRole().getId(),
                        Function.identity()
                ));

        List<RoleAssignmentPermissionItemDto> items = allTargetRoles.stream()
                .map(targetRole -> {
                    RoleAssignmentPermission existing = existingMap.get(targetRole.getId());

                    return RoleAssignmentPermissionItemDto.builder()
                            .targetRoleId(targetRole.getId())
                            .targetRoleCode(targetRole.getCode())
                            .targetRoleName(targetRole.getRoleName())
                            .canCreate(existing != null ? nvl(existing.getCanCreate()) : 0)
                            .canUpdate(existing != null ? nvl(existing.getCanUpdate()) : 0)
                            .build();
                })
                .toList();

        return RoleAssignmentPermissionResponse.builder()
                .creatorRoleId(creatorRole.getId())
                .creatorRoleCode(creatorRole.getCode())
                .creatorRoleName(creatorRole.getRoleName())
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public RoleAssignmentPermissionResponse save(RoleAssignmentPermissionSaveRequest request) {
        ScopeFilterUtils.checkAccess(FEATURE);

        Role creatorRole = roleRepository.findById(request.getCreatorRoleId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));

        List<RoleAssignmentPermission> existingList = roleAssignmentPermissionRepository
                .findAllByCreatorRoleId(request.getCreatorRoleId());

        Map<Long, RoleAssignmentPermission> existingMap = existingList.stream()
                .collect(Collectors.toMap(
                        rap -> rap.getTargetRole().getId(),
                        Function.identity()
                ));

        Map<Long, RoleAssignmentPermissionItemRequest> requestMap = new HashMap<>();
        for (RoleAssignmentPermissionItemRequest item : request.getItems()) {
            requestMap.put(item.getTargetRoleId(), item);
        }

        // 1) Update/insert theo request
        for (RoleAssignmentPermissionItemRequest item : request.getItems()) {
            Role targetRole = roleRepository.findById(item.getTargetRoleId())
                    .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));

            RoleAssignmentPermission permission = existingMap.get(targetRole.getId());
            if (permission == null) {
                permission = new RoleAssignmentPermission();
                permission.setCreatorRole(creatorRole);
                permission.setTargetRole(targetRole);
                permission.setCreatedBy(SecurityUtils.getCurrentUsername());
                permission.setDeletedFlag(0);
                permission.setStatus(1);
            } else {
                permission.setUpdatedBy(SecurityUtils.getCurrentUsername());
            }

            permission.setCanCreate(normalizeFlag(item.getCanCreate()));
            permission.setCanUpdate(normalizeFlag(item.getCanUpdate()));

            roleAssignmentPermissionRepository.save(permission);
        }

        // 2) Các role cũ không còn gửi lên thì set về 0
        for (RoleAssignmentPermission existing : existingList) {
            Long targetRoleId = existing.getTargetRole().getId();
            if (!requestMap.containsKey(targetRoleId)) {
                existing.setCanCreate(0);
                existing.setCanUpdate(0);
                existing.setUpdatedBy(SecurityUtils.getCurrentUsername());
                roleAssignmentPermissionRepository.save(existing);
            }
        }

        return getByCreatorRoleId(request.getCreatorRoleId());
    }

    private Integer normalizeFlag(Integer value) {
        return value != null && value == 1 ? 1 : 0;
    }

    private Integer nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
