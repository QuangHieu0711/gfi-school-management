package com.gfi.backend.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

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
     * @param menuCode Menu code to check
     * @throws AccessDeniedException if user has no access to this menu
     */
    public static void checkAccess(String menuCode) {
        UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new AccessDeniedException("Permission context is empty"));

        String normalizedMenuCode = normalize(menuCode);

        if (userScopes.getAllowedMenuCodes() == null
                || !userScopes.getAllowedMenuCodes().contains(normalizedMenuCode)) {
            throw new AccessDeniedException("User has no access to menu: " + menuCode);
        }

        logger.debug("checkAccess: menuCode={}, allowed=true", normalizedMenuCode);
    }

    /**
     * Get allowed scope IDs for a specific menu from current user's scopes
     * @param menuCode Menu code to get scopes for
     * @return List of allowed scope IDs (empty list = ALL, no restriction)
     * @throws AccessDeniedException if user has no access to this menu
     */
    public static List<Long> getRequiredScopes(String menuCode) {
        String normalizedMenuCode = normalize(menuCode);
        
        // Skip data scope check for system/configuration menus
        if (SKIP_DATA_SCOPE_CHECK.contains(normalizedMenuCode)) {
            logger.debug("getRequiredScopes: menuCode={} is in skip list, returning ALL scopes", normalizedMenuCode);
            return List.of();
        }
        
        UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new AccessDeniedException("User scopes not found in context"));

        // Get scopes map first to avoid NPE in logging
        Map<String, List<Long>> scopesByMenu = userScopes.getScopesByMenu();
        logger.debug("getRequiredScopes: menuCode={}, availableMenus={}",
                normalizedMenuCode,
                scopesByMenu != null ? scopesByMenu.keySet() : List.of());

        // If menu has no data permission, return ALL (empty list = no restriction)
        // This means: menu is accessible but no data scope restriction (admin can see all)
        if (scopesByMenu == null || !scopesByMenu.containsKey(normalizedMenuCode)) {
            logger.debug("getRequiredScopes: menuCode={} NOT in data_permissions, returning ALL scopes", normalizedMenuCode);
            return List.of();
        }

        List<Long> scopes = scopesByMenu.get(normalizedMenuCode);
        logger.debug("getRequiredScopes: menuCode={}, scopes={}", normalizedMenuCode, scopes);
        return scopes;
    }

    /**
     * Check if scopes are empty (= ALL, no restriction)
     */
    public static boolean isScopeUnrestricted(List<Long> scopes) {
        return scopes == null || scopes.isEmpty();
    }

    /**
     * Validate single record access
     * @param menuCode Menu code to check
     * @param recordScopeId The scope ID of the record (e.g., unitId, classId)
     * @throws AccessDeniedException if user cannot access this record
     */
    public static void validateAccess(String menuCode, Long recordScopeId) {
        String normalizedMenuCode = normalize(menuCode);
        
        // Skip data scope validation for system/configuration menus
        if (SKIP_DATA_SCOPE_CHECK.contains(normalizedMenuCode)) {
            logger.debug("validateAccess: menuCode={} is in skip list, allowing access", normalizedMenuCode);
            return;
        }
        
        List<Long> allowedScopes = getRequiredScopes(menuCode);

        // If empty scopes (ALL), no restriction
        if (isScopeUnrestricted(allowedScopes)) {
            return;
        }

        // Check if record's scope is in allowed scopes
        if (!allowedScopes.contains(recordScopeId)) {
            throw new AccessDeniedException(
                    "Access denied to " + menuCode + " with scope ID: " + recordScopeId);
        }

        logger.debug("validateAccess: menuCode={}, recordScopeId={}, allowed=true", menuCode, recordScopeId);
    }

    /**
     * Get scopes for query filtering
     * If unrestricted (ALL), returns empty list for "no filtering" behavior
     * Otherwise returns the list of allowed scope IDs for WHERE IN clause
     */
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
