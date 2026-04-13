package com.gfi.backend.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.models.security.ResolvedScope;
import com.gfi.backend.models.security.UserScopes;

/**
 * Utility to apply permission and data scope filter from ThreadLocal to queries
 * Ensures backend enforces permissions and scope restrictions on all data access
 * 
 * Two-layer security:
 * 1. checkAccess(menuCode) - verify functional permission to access menu
 * 2. validateAccess(menuCode, scopeId) - verify data scope allows access to specific record
 * 
 * Menus not requiring data scope (system config, configuration menus):
 * - MENU: system menu configuration
 * - PERMISSION: system permission configuration
 * - ROLE: system role configuration
 */
public class ScopeFilterUtils {
    private static final Logger logger = LoggerFactory.getLogger(ScopeFilterUtils.class);
    
    /**
     * Menus that don't require data scope check (system/configuration menus)
     * Anyone with menu access can view all data
     */
    private static final Set<String> SKIP_DATA_SCOPE_CHECK = Set.of(
            "MENU",
            "PERMISSION",
            "ROLE",
            "GRADE_LEVEL",
            "SUBJECT",
            "SEMESTER",
            "SCHOOL_YEAR",
            "GRADE_LEVEL_SUBJECT"
    );

    /**
     * Check if user has functional permission to access menu
     * ⚠️ UPDATED: Dùng API mới hasMenuAccess() thay vì allowedMenuCodes
     * @param menuCode Menu code to check
     * @throws AccessDeniedException if user has no access to this menu
     */
    public static void checkAccess(String menuCode) {
        UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new AccessDeniedException("Permission context is empty"));

        String normalizedMenuCode = normalize(menuCode);

        // ⚠️ UPDATED: Dùng hasMenuAccess() từ UserScopes
        // Nó sẽ check allowedActionsByFeature thay vì allowedMenuCodes
        if (!userScopes.hasMenuAccess(normalizedMenuCode)) {
            throw new AccessDeniedException("User has no access to menu: " + menuCode);
        }

        logger.debug("checkAccess: menuCode={}, allowed=true", normalizedMenuCode);
    }

    /**
     * Get allowed scopes for a specific menu and action from current user's scopes
     * ⚠️ UPDATED: Trả ResolvedScope thay vì List<Long>, support action-level
     * @param menuCode Menu code to get scopes for
     * @param action Action type (VIEW, ADD, EDIT, DELETE)
     * @return List of ResolvedScope objects (empty list = deny, not allow all)
     * @throws AccessDeniedException if user has no access to this menu
     */
    public static List<ResolvedScope> getRequiredScopes(String menuCode, ActionType action) {
        String normalizedMenuCode = normalize(menuCode);
        
        // Skip data scope check for system/configuration menus
        if (SKIP_DATA_SCOPE_CHECK.contains(normalizedMenuCode)) {
            logger.debug("getRequiredScopes: menuCode={} is in skip list, returning ALL scopes", normalizedMenuCode);
            // Return unrestricted scope for system menus
            return List.of(ResolvedScope.all());
        }
        
        UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new AccessDeniedException("User scopes not found in context"));

        // ⚠️ UPDATED: Dùng getScopes() API baru, trả ResolvedScope thay vì Long
        List<ResolvedScope> scopes = userScopes.getScopes(normalizedMenuCode, action);
        logger.debug("getRequiredScopes: menuCode={}, action={}, resolvedScopes.size={}",
                normalizedMenuCode, action, scopes != null ? scopes.size() : 0);

        return scopes;
    }
    
    /**
     * ⚠️ DEPRECATED: Use getRequiredScopes(menuCode, action) instead
     * This overload kept for backward compatibility only
     */
    @Deprecated
    public static List<Long> getRequiredScopes(String menuCode) {
        logger.warn("getRequiredScopes(menuCode) is deprecated, use getRequiredScopes(menuCode, action) instead");
        // Default to VIEW action for backward compat
        List<ResolvedScope> resolvedScopes = getRequiredScopes(menuCode, ActionType.VIEW);
        // Flatten to Long list (loses scope type info)
        return resolvedScopes.stream()
                .flatMap(rs -> rs.getScopeIds().stream())
                .toList();
    }

    /**
     * Check if ResolvedScope is unrestricted (allows all data)
     */
    public static boolean isScopeUnrestricted(ResolvedScope scope) {
        return scope != null && scope.isUnrestricted();
    }
    
    /**
     * ⚠️ DEPRECATED: Use isScopeUnrestricted(ResolvedScope) instead
     */
    @Deprecated
    public static boolean isScopeUnrestricted(List<Long> scopes) {
        return scopes == null || scopes.isEmpty();
    }

    /**
     * Validate single record access
     * ⚠️ UPDATED: Work with ResolvedScope and support multiple scope types
     * @param menuCode Menu code to check
     * @param action Action type (VIEW, ADD, EDIT, DELETE)
     * @param scopeType Scope type (UNIT, CLASS, GRADE, etc)
     * @param recordScopeId The scope ID of the record (e.g., unitId, classId)
     * @throws AccessDeniedException if user cannot access this record
     */
    public static void validateAccess(String menuCode, ActionType action, ScopeType scopeType, Long recordScopeId) {
        String normalizedMenuCode = normalize(menuCode);
        
        // Skip data scope validation for system/configuration menus
        if (SKIP_DATA_SCOPE_CHECK.contains(normalizedMenuCode)) {
            logger.debug("validateAccess: menuCode={} is in skip list, allowing access", normalizedMenuCode);
            return;
        }
        
        UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new AccessDeniedException("User scopes not found in context"));

        // ⚠️ UPDATED: Dùng hasAccess() method từ UserScopes
        // Nó sẽ check tất cả ResolvedScope rules với OR logic
        if (!userScopes.hasAccess(normalizedMenuCode, action, scopeType, recordScopeId)) {
            throw new AccessDeniedException(
                    String.format("Access denied to %s action=%s scope=%s(id=%d)", 
                            menuCode, action, scopeType, recordScopeId));
        }

        logger.debug("validateAccess: menuCode={}, action={}, scopeType={}, recordScopeId={}, allowed=true", 
                menuCode, action, scopeType, recordScopeId);
    }
    
    /**
     * ⚠️ DEPRECATED: Use validateAccess(menuCode, action, scopeType, recordScopeId) instead
     */
    @Deprecated
    public static void validateAccess(String menuCode, Long recordScopeId) {
        logger.warn("validateAccess(menuCode, recordScopeId) is deprecated, use validateAccess(menuCode, action, scopeType, recordScopeId) instead");
        // Default to VIEW action and UNIT scope for backward compat
        validateAccess(menuCode, ActionType.VIEW, ScopeType.UNIT, recordScopeId);
    }

    /**
     * Get scopes for query filtering
     * ⚠️ UPDATED: Trả ResolvedScope để service layer có thể build proper WHERE clause
     * @param menuCode Menu code
     * @param action Action type (VIEW, ADD, EDIT, DELETE)
     * @return List of ResolvedScope to use for query building
     */
    public static List<ResolvedScope> getScopesForQuery(String menuCode, ActionType action) {
        return getRequiredScopes(menuCode, action);
    }
    
    /**
     * ⚠️ DEPRECATED: Use getScopesForQuery(menuCode, action) instead
     */
    @Deprecated
    public static List<Long> getScopesForQuery(String menuCode) {
        return getRequiredScopes(menuCode);
    }

    /**
     * Normalize menu code: trim and uppercase to avoid case/whitespace differences
     */
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
