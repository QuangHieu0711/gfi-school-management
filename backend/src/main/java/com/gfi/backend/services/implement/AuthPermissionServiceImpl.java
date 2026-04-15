package com.gfi.backend.services.implement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
import com.gfi.backend.models.entities.Menu;
import com.gfi.backend.models.entities.Permission;
import com.gfi.backend.models.entities.User;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.models.security.ResolvedScope;
import com.gfi.backend.models.security.UserScopes;
import com.gfi.backend.repositories.DataPermissionRepository;
import com.gfi.backend.repositories.PermissionRepository;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.services.interfaces.AuthPermissionService;
import com.gfi.backend.services.interfaces.ScopeValueResolver;
import com.gfi.backend.utils.SecurityContextUtils;

/**
 * Service để load user scopes từ database
 * 
 * Giai đoạn 1: Chưa sửa DB action-level, nên:
 * - Load data permission per menu
 * - Áp dụng tất cả action (VIEW, ADD, EDIT, DELETE) dùng chung 1 data
 * permission rule
 * - Giai đoạn sau: khi DB có action-level, cấu trúc không thay đổi
 */
@Service
public class AuthPermissionServiceImpl implements AuthPermissionService {

    private static final Logger logger = LoggerFactory.getLogger(AuthPermissionServiceImpl.class);

    private final PermissionRepository permissionRepository;
    private final DataPermissionRepository dataPermissionRepository;
    private final UserRepository userRepository;
    private final Map<ScopeType, ScopeValueResolver> resolverMap;

    public AuthPermissionServiceImpl(PermissionRepository permissionRepository,
            DataPermissionRepository dataPermissionRepository,
            UserRepository userRepository,
            List<ScopeValueResolver> scopeResolvers) {
        this.permissionRepository = permissionRepository;
        this.dataPermissionRepository = dataPermissionRepository;
        this.userRepository = userRepository;

        // ⚠️ Nối ScopeValueResolver: build map scopeType -> resolver
        // Sau này chỉ cần thêm resolver bean mới, không cần sửa logic
        this.resolverMap = scopeResolvers.stream()
                .collect(Collectors.toMap(ScopeValueResolver::support, Function.identity()));
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

        // 1.5 Build allowedActionsByFeature từ Permission
        // ⚠️ BLOCKER FIX: Phải separate functional permission khỏi data scope
        Map<String, Set<ActionType>> allowedActionsByFeature = new HashMap<>();

        for (Permission permission : permissions) {
            String featureCode = normalize(permission.getMenu().getCode());
            Set<ActionType> actions = new HashSet<>();

            if (Integer.valueOf(1).equals(permission.getIsView())) {
                actions.add(ActionType.VIEW);
            }
            if (Integer.valueOf(1).equals(permission.getIsAdd())) {
                actions.add(ActionType.ADD);
            }
            if (Integer.valueOf(1).equals(permission.getIsEdit())) {
                actions.add(ActionType.EDIT);
            }
            if (Integer.valueOf(1).equals(permission.getIsDelete())) {
                actions.add(ActionType.DELETE);
            }
            if (Integer.valueOf(1).equals(permission.getIsDownload())) {
                actions.add(ActionType.DOWNLOAD);
            }
            if (Integer.valueOf(1).equals(permission.getIsConfig())) {
                actions.add(ActionType.CONFIGURE);
            }

            if (!actions.isEmpty()) {
                allowedActionsByFeature.put(featureCode, actions);
            }

            logger.debug("loadUserScopesIntoContext: featureCode={}, actions={}", featureCode, actions);
        }

        logger.info("loadUserScopesIntoContext: allowedActionsByFeature.keys={}",
                allowedActionsByFeature.keySet());

        // 2. Load data scopes (với cấu trúc mới: feature -> action -> ResolvedScope)
        Map<String, Map<ActionType, List<ResolvedScope>>> scopesByFeatureAndAction = buildScopesByFeatureAndAction(user,
                roleId);
        logger.info("loadUserScopesIntoContext: scopesByFeatureAndAction.keys={}",
                scopesByFeatureAndAction.keySet());

        // 3. Build and set user scopes in ThreadLocal
        UserScopes userScopes = UserScopes.builder()
                .userId(user.getId())
                .roleId(roleId)
                .roleCode(user.getRole() != null ? user.getRole().getCode() : null)
                .allowedActionsByFeature(allowedActionsByFeature)
                .scopesByFeatureAndAction(scopesByFeatureAndAction)
                .build();

        logger.info(
                "loadUserScopesIntoContext: FINAL - allowedMenuCodes={}, allowedActionsByFeature.keys={}, scopesByFeatureAndAction.keys={}",
                userScopes.getAllowedActionsByFeature().keySet(),
                userScopes.getScopesByFeatureAndAction().keySet());

        // Set into ThreadLocal for this request
        SecurityContextUtils.setUserScopes(userScopes);
    }

    /**
     * Build scopes map với cấu trúc mới: feature -> action -> ResolvedScope[]
     * 
     * Vì hiện tại DB chưa có action-level, nên:
     * - Load data permission per menu
     * - Resolve scope values dựa trên scopeType
     * - Copy vào tất cả action (VIEW, ADD, EDIT, DELETE, DOWNLOAD)
     * 
     * Sau khi nâng DB: chỉ cần thay load query và mapping, cấu trúc giữ nguyên
     */
    private Map<String, Map<ActionType, List<ResolvedScope>>> buildScopesByFeatureAndAction(
            User user, Long roleId) {

        Map<String, Map<ActionType, List<ResolvedScope>>> result = new HashMap<>();

        // Fetch data permissions với scopes eagerly loaded via fetch join
        List<DataPermission> dataPermissions = dataPermissionRepository
                .findAllByRoleIdWithScopesOrderByIdAsc(roleId);
        logger.debug("buildScopesByFeatureAndAction: userId={}, roleId={}, dataPermissions.size={}",
                user.getId(), roleId, dataPermissions.size());

        for (DataPermission dataPermission : dataPermissions) {
            String featureCode = normalize(dataPermission.getMenu().getCode());

            // Build list of ResolvedScope cho menu này
            List<ResolvedScope> resolvedScopes = buildResolvedScopes(dataPermission, user);

            logger.debug("buildScopesByFeatureAndAction: featureCode={}, resolvedScopes.size={}",
                    featureCode, resolvedScopes.size());

            // Tạo map action -> scopes
            Map<ActionType, List<ResolvedScope>> actionMap = new HashMap<>();

            // Giai đoạn 1: copy scopes cho tất cả action vì DB chưa có action-level
            for (ActionType action : ActionType.values()) {
                actionMap.put(action, resolvedScopes);
            }

            result.put(featureCode, actionMap);
        }

        logger.debug("buildScopesByFeatureAndAction result: featureKeys={}", result.keySet());
        return result;
    }

    /**
     * Build list của ResolvedScope từ một DataPermission
     * Không flatten IDs nữa, mà giữ scopeType + unrestricted info
     */
    private List<ResolvedScope> buildResolvedScopes(DataPermission dataPermission, User user) {
        List<ResolvedScope> resolvedScopes = new ArrayList<>();

        for (var scope : dataPermission.getScopes()) {
            String scopeTypeStr = scope.getScopeType();

            try {
                ScopeType scopeType = ScopeType.valueOf(scopeTypeStr.toUpperCase());

                // Resolve scope values dựa trên type
                Set<Long> scopeIds = resolveScopeIds(scopeType, user);

                ResolvedScope resolved;
                // Chỉ ScopeType.ALL mới là unrestricted
                // Nếu scopeIds rỗng nhưng scopeType != ALL → đây là "configured but empty"
                // Mình hiểu là deny, không phải allow all
                if (scopeType == ScopeType.ALL) {
                    // ALL scope → unrestricted
                    resolved = ResolvedScope.all();
                } else {
                    // Bất kỳ scope type khác: build ResolvedScope với setting
                    // Nếu scopeIds rỗng → unrestricted=false, scopeIds=empty
                    // => Đây là empty restricted (deny, chờ resolver thêm dữ liệu)
                    resolved = ResolvedScope.of(scopeType, scopeIds);
                }

                resolvedScopes.add(resolved);
                logger.debug("buildResolvedScopes: scopeType={}, unrestricted={}, scopeIds={}",
                        scopeType, resolved.isUnrestricted(), resolved.getScopeIds());

            } catch (IllegalArgumentException e) {
                logger.warn("Unknown scope type: {}", scopeTypeStr);
            }
        }

        // Deny by default, không allow by default
        // Nếu resolvedScopes empty → báo lỗi hoặc trả empty list
        // Service layer sẽ quyết định deny hoặc xử lý riêng
        return resolvedScopes;
    }

    /**
     * Build complete permissions response for login
     * Giữ nguyên cấu trúc response (có dataScopes per menu)
     * Vì FE chỉ cần biết: menu nào, action nào, scope type + values
     */
    private PermissionsResponse buildPermissionsResponse(User user, Long roleId) {
        // Get menu permissions
        List<Permission> permissions = permissionRepository.findAllByRoleIdOrderByIdAsc(roleId);

        // Get data permissions with scopes eagerly fetched (avoid
        // LazyInitializationException)
        List<DataPermission> dataPermissions = dataPermissionRepository
                .findAllByRoleIdWithScopesOrderByIdAsc(roleId);

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
                    .parentMenuId(
                            permission.getMenu().getParentMenu() != null ? permission.getMenu().getParentMenu().getId()
                                    : null)
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
            appendParentMenus(menuDtoMap, menuPermissions, permission.getMenu());
        }

        // Merge data permissions với resolved scope values
        for (DataPermission dataPermission : dataPermissions) {
            MenuPermissionDto existingMenu = menuDtoMap.values().stream()
                    .filter(m -> m.getMenuCode().equals(dataPermission.getMenu().getCode()))
                    .findFirst()
                    .orElse(null);

            // Resolve scope values dynamically based on scopeType and user
            List<DataScopeDto> scopes = new ArrayList<>();

            for (var scope : dataPermission.getScopes()) {
                Set<Long> resolvedValues = resolveScopeIds(
                        ScopeType.valueOf(scope.getScopeType().toUpperCase()),
                        user);
                scopes.add(DataScopeDto.builder()
                        .scopeType(scope.getScopeType())
                        .scopeValues(new ArrayList<>(resolvedValues))
                        .build());
            }

            if (existingMenu != null) {
                existingMenu.setDataScopes(scopes);
            } else {
                // If no regular permission exists, create a data-only menu entry
                MenuPermissionDto menuDto = MenuPermissionDto.builder()
                        .menuCode(dataPermission.getMenu().getCode())
                        .menuName(dataPermission.getMenu().getName())
                        .path(dataPermission.getMenu().getUrl())
                        .icon(dataPermission.getMenu().getIcon())
                        .level(dataPermission.getMenu().getOrdinal())
                        .parentMenuId(dataPermission.getMenu().getParentMenu() != null
                                ? dataPermission.getMenu().getParentMenu().getId()
                                : null)
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
                appendParentMenus(menuDtoMap, menuPermissions, dataPermission.getMenu());
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
     * Ensure all ancestor menus exist in login permissions tree.
     * Without this, a newly granted child menu is dropped if its parent has no explicit permission row.
     */
    private void appendParentMenus(Map<Long, MenuPermissionDto> menuDtoMap,
            List<MenuPermissionDto> menuPermissions,
            Menu menu) {
        Menu currentParent = menu.getParentMenu();
        while (currentParent != null) {
            if (!menuDtoMap.containsKey(currentParent.getId())) {
                MenuPermissionDto parentDto = MenuPermissionDto.builder()
                        .menuCode(currentParent.getCode())
                        .menuName(currentParent.getName())
                        .path(currentParent.getUrl())
                        .icon(currentParent.getIcon())
                        .level(currentParent.getOrdinal())
                        .parentMenuId(currentParent.getParentMenu() != null ? currentParent.getParentMenu().getId() : null)
                        .actions(ActionDto.builder()
                                .isView(0)
                                .isAdd(0)
                                .isEdit(0)
                                .isDelete(0)
                                .isDownload(0)
                                .isConfig(0)
                                .build())
                        .dataScopes(new ArrayList<>())
                        .children(new ArrayList<>())
                        .build();
                menuDtoMap.put(currentParent.getId(), parentDto);
                menuPermissions.add(parentDto);
            }
            currentParent = currentParent.getParentMenu();
        }
    }

    /**
     * Resolve scope IDs dynamically based on scopeType
     * Dùng các ScopeValueResolver để tính toán
     * 
     * @param scopeType UNIT, ALL, CLASS, GRADE, SELF, USER, etc
     * @param user      Current user
     * @return Set of resolved scope IDs
     */
    private Set<Long> resolveScopeIds(ScopeType scopeType, User user) {
        if (scopeType == null) {
            return Set.of();
        }

        // ⚠️ BLOCKER FIX: Bỏ switch-case cứng, dùng resolver pattern
        if (scopeType == ScopeType.ALL) {
            // Special case: ALL always returns empty set
            return Set.of();
        }

        ScopeValueResolver resolver = resolverMap.get(scopeType);
        if (resolver == null) {
            logger.warn("No resolver found for scopeType={}", scopeType);
            return Set.of();
        }

        return resolver.resolve(user);
    }

    /**
     * Normalize feature code: trim and uppercase
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
