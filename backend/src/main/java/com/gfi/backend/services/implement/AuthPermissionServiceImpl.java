package com.gfi.backend.services.implement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.models.dtos.auth.PermissionsResponse;
import com.gfi.backend.models.dtos.auth.PermissionsResponse.ActionDto;
import com.gfi.backend.models.dtos.auth.PermissionsResponse.DataScopeDto;
import com.gfi.backend.models.dtos.auth.PermissionsResponse.MenuPermissionDto;
import com.gfi.backend.models.entities.DataPermission;
import com.gfi.backend.models.entities.Permission;
import com.gfi.backend.models.entities.User;
import com.gfi.backend.models.security.UserScopes;
import com.gfi.backend.repositories.DataPermissionRepository;
import com.gfi.backend.repositories.PermissionRepository;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.services.interfaces.AuthPermissionService;
import com.gfi.backend.utils.SecurityContextUtils;

@Service
public class AuthPermissionServiceImpl implements AuthPermissionService {

    private static final Logger logger = LoggerFactory.getLogger(AuthPermissionServiceImpl.class);

    private final PermissionRepository permissionRepository;
    private final DataPermissionRepository dataPermissionRepository;
    private final UserRepository userRepository;

    public AuthPermissionServiceImpl(PermissionRepository permissionRepository,
            DataPermissionRepository dataPermissionRepository,
            UserRepository userRepository) {
        this.permissionRepository = permissionRepository;
        this.dataPermissionRepository = dataPermissionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public PermissionsResponse getPermissionsByRoleId(Long roleId, Long userId) {
        // Get user to resolve scope values
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Build response with permissions and data scopes
        return buildPermissionsResponse(user, roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public void loadUserScopesIntoContext(User user, Long roleId) {
        // No need to re-query user - already passed in
        logger.info("loadUserScopesIntoContext: userId={}, roleId={}", user.getId(), roleId);

        // 1. Load menu functional permissions (isView)
        List<Permission> permissions = permissionRepository.findAllByRoleIdOrderByIdAsc(roleId);
        logger.info("loadUserScopesIntoContext: permissions.size={}", permissions.size());

        Set<String> allowedMenuCodes = permissions.stream()
                .filter(permission -> permission.getIsView() != null && permission.getIsView() == 1)
                .map(permission -> normalize(permission.getMenu().getCode()))
                .collect(Collectors.toSet());

        logger.info("loadUserScopesIntoContext: allowedMenuCodes={}", allowedMenuCodes);

        // 2. Load data scopes (with scopes eagerly fetched to avoid LazyInitializationException)
        Map<String, List<Long>> scopesByMenu = buildScopesMap(user, roleId);
        logger.info("loadUserScopesIntoContext: scopesByMenu.keys={}", scopesByMenu.keySet());

        // 3. Build and set user scopes in ThreadLocal
        UserScopes userScopes = UserScopes.builder()
                .userId(user.getId())
                .roleId(roleId)
                .roleCode(user.getRole() != null ? user.getRole().getCode() : null)
                .allowedMenuCodes(allowedMenuCodes)
                .scopesByMenu(scopesByMenu)
                .build();

        logger.info("loadUserScopesIntoContext: FINAL - allowedMenuCodes={}, scopesByMenuKeys={}", 
                userScopes.getAllowedMenuCodes(), userScopes.getScopesByMenu().keySet());

        // Set into ThreadLocal for this request
        SecurityContextUtils.setUserScopes(userScopes);
    }

    /**
     * Build scopes map for a user based on their role
     * Returns: Map of normalized menuCode -> List of scope IDs
     * Uses findAllByRoleIdWithScopesOrderByIdAsc() to eagerly fetch scopes (avoid LazyInitializationException)
     */
    private Map<String, List<Long>> buildScopesMap(User user, Long roleId) {
        Map<String, List<Long>> scopesByMenu = new HashMap<>();

        // Fetch data permissions with scopes eagerly loaded via fetch join
        List<DataPermission> dataPermissions = dataPermissionRepository.findAllByRoleIdWithScopesOrderByIdAsc(roleId);
        logger.debug("buildScopesMap: userId={}, roleId={}, dataPermissions.size={}", user.getId(), roleId, dataPermissions.size());

        for (DataPermission dataPermission : dataPermissions) {
            List<Long> mergedScopeValues = new ArrayList<>();

            for (var scope : dataPermission.getScopes()) {
                List<Long> resolvedValues = resolveScopeValues(scope.getScopeType(), user);
                mergedScopeValues.addAll(resolvedValues);
            }

            String menuCode = normalize(dataPermission.getMenu().getCode());
            scopesByMenu.put(menuCode, mergedScopeValues);
            logger.debug("buildScopesMap: menuCode={}, scopeValues={}", menuCode, mergedScopeValues);
        }

        logger.debug("buildScopesMap result: scopesByMenu.keySet={}", scopesByMenu.keySet());
        return scopesByMenu;
    }

    /**
     * Build complete permissions response for login
     */
    private PermissionsResponse buildPermissionsResponse(User user, Long roleId) {
        // Get menu permissions
        List<Permission> permissions = permissionRepository.findAllByRoleIdOrderByIdAsc(roleId);
        
        // Get data permissions with scopes eagerly fetched (avoid LazyInitializationException)
        List<DataPermission> dataPermissions = dataPermissionRepository.findAllByRoleIdWithScopesOrderByIdAsc(roleId);

        // Build menu permissions DTOs with parentMenuId
        Map<Long, MenuPermissionDto> menuDtoMap = new HashMap<>();
        List<MenuPermissionDto> menuPermissions = new ArrayList<>();

        for (Permission permission : permissions) {
            MenuPermissionDto menuDto = MenuPermissionDto.builder()
                    .menuCode(permission.getMenu().getCode())
                    .menuName(permission.getMenu().getName())
                    .path(permission.getMenu().getUrl())
                    .icon(permission.getMenu().getIcon())
                    .level(permission.getMenu().getOrdinal())
                    .parentMenuId(permission.getMenu().getParentMenu() != null ? permission.getMenu().getParentMenu().getId() : null)
                    .actions(ActionDto.builder()
                            .isView(permission.getIsView())
                            .isAdd(permission.getIsAdd())
                            .isEdit(permission.getIsEdit())
                            .isDelete(permission.getIsDelete())
                            .isDownload(permission.getIsDownload())
                            .isConfig(permission.getIsConfig())
                            .build())
                    .dataScopes(new ArrayList<>())
                    .children(new ArrayList<>())
                    .build();
            
            menuDtoMap.put(permission.getMenu().getId(), menuDto);
            menuPermissions.add(menuDto);
        }

        // Merge data permissions with resolved scope values
        for (DataPermission dataPermission : dataPermissions) {
            MenuPermissionDto existingMenu = menuDtoMap.values().stream()
                    .filter(m -> m.getMenuCode().equals(dataPermission.getMenu().getCode()))
                    .findFirst()
                    .orElse(null);

            // Resolve scope values dynamically based on scopeType and user
            List<DataScopeDto> scopes = new ArrayList<>();

            for (var scope : dataPermission.getScopes()) {
                List<Long> resolvedValues = resolveScopeValues(scope.getScopeType(), user);
                scopes.add(DataScopeDto.builder()
                        .scopeType(scope.getScopeType())
                        .scopeValues(resolvedValues)
                        .build());
            }

            if (existingMenu != null) {
                existingMenu.setDataScopes(scopes);
            } else {
                // If no regular permission exists, create a data-only menu entry
                MenuPermissionDto menuDto = MenuPermissionDto.builder()
                        .menuCode(dataPermission.getMenu().getCode())
                        .path(dataPermission.getMenu().getUrl())
                        .icon(dataPermission.getMenu().getIcon())
                        .level(dataPermission.getMenu().getOrdinal())
                        .parentMenuId(dataPermission.getMenu().getParentMenu() != null ? dataPermission.getMenu().getParentMenu().getId() : null)
                        .actions(ActionDto.builder()
                                .isView(0)
                                .isAdd(0)
                                .isEdit(0)
                                .isDelete(0)
                                .isDownload(0)
                                .isConfig(0)
                                .build())
                        .dataScopes(scopes)
                        .children(new ArrayList<>())
                        .build();
                menuDtoMap.put(dataPermission.getMenu().getId(), menuDto);
                menuPermissions.add(menuDto);
            }
        }

        // Build hierarchy: link children to parents
        List<MenuPermissionDto> rootMenus = new ArrayList<>();
        for (MenuPermissionDto menuDto : menuPermissions) {
            if (menuDto.getParentMenuId() == null) {
                // Root level menu
                rootMenus.add(menuDto);
            } else {
                // Child menu - add to parent's children
                MenuPermissionDto parentDto = menuDtoMap.get(menuDto.getParentMenuId());
                if (parentDto != null) {
                    if (parentDto.getChildren() == null) {
                        parentDto.setChildren(new ArrayList<>());
                    }
                    parentDto.getChildren().add(menuDto);
                }
            }
        }

        return PermissionsResponse.builder()
                .menus(rootMenus)
                .build();
    }

    /**
     * Resolve scope values dynamically based on scopeType
     * @param scopeType UNIT, ALL, CLASS, GRADE, SELF, etc
     * @param user Current user
     * @return List of resolved scope IDs
     */
    private List<Long> resolveScopeValues(String scopeType, User user) {
        List<Long> result = new ArrayList<>();

        switch (scopeType != null ? scopeType.toUpperCase() : "") {
            case "UNIT":
                // For UNIT scope, return user's unit ID
                if (user.getUnit() != null) {
                    result.add(user.getUnit().getId());
                }
                break;

            case "ALL":
                // For ALL scope, return empty list (meaning no restriction)
                // Client can interpret empty list as "all data"
                break;

            case "SELF":
                // For SELF scope, return user's ID
                result.add(user.getId());
                break;

            // Additional scope types can be added here later:
            // case "CLASS": - get classes assigned to user
            // case "GRADE": - get grades assigned to user
            // case "SCHOOL_YEAR": - get school years assigned to user
            // etc
        }

        return result;
    }

    /**
     * Normalize menu code: trim and uppercase to avoid case/whitespace differences
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}

