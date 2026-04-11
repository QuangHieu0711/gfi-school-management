package com.gfi.backend.models.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Store user's data scopes for access control
 * Maps menuCode -> List of scope IDs
 * 
 * Example:
 * {
 *   "CLASS_MANAGEMENT": [9],       // Can access classes only in unit 9
 *   "ACCOUNT_MANAGEMENT": [9],     // Can access users only in unit 9
 *   "STUDENT_MANAGEMENT": []       // ALL - no restriction
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserScopes {
    private Long userId;
    private Long roleId;
    private String roleCode;
    
    /**
     * Functional permissions: menu codes user can access
     * Example: {"ACCOUNT_MANAGEMENT", "CLASS_MANAGEMENT", "STUDENT_MANAGEMENT"}
     */
    private Set<String> allowedMenuCodes;
    
    /**
     * Map of menuCode -> list of scope IDs
     * Empty list means ALL (no restriction)
     */
    private Map<String, List<Long>> scopesByMenu;

    /**
     * Check if user has access to specific scope
     * @param menuCode Menu code to check
     * @param scopeId Scope ID to verify (e.g., unit ID, class ID)
     * @return true if user can access this scope for this menu, false otherwise
     */
    public boolean hasAccessToScope(String menuCode, Long scopeId) {
        if (scopesByMenu == null) {
            return false;
        }
        
        String normalizedMenuCode = normalize(menuCode);
        List<Long> allowedScopes = scopesByMenu.get(normalizedMenuCode);
        if (allowedScopes == null) {
            return false;
        }
        
        // Empty list means ALL (no restriction)
        if (allowedScopes.isEmpty()) {
            return true;
        }
        
        // Check if scopeId is in allowed scopes
        return allowedScopes.contains(scopeId);
    }

    /**
     * Get scope filter for a specific menu
     * @param menuCode Menu code
     * @return List of allowed scope IDs, empty list if ALL
     */
    public List<Long> getScopesForMenu(String menuCode) {
        if (scopesByMenu == null) {
            return List.of();
        }
        String normalizedMenuCode = normalize(menuCode);
        return scopesByMenu.getOrDefault(normalizedMenuCode, List.of());
    }

    /**
     * Check if user has functional permission for a menu
     */
    public boolean hasMenuAccess(String menuCode) {
        if (allowedMenuCodes == null) {
            return false;
        }
        String normalizedMenuCode = normalize(menuCode);
        return allowedMenuCodes.contains(normalizedMenuCode);
    }

    /**
     * Normalize menu code: trim and uppercase to avoid case/whitespace differences
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
