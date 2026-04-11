package com.gfi.backend.services.interfaces;

import java.util.List;

/**
 * Service to check and enforce data scope permissions
 */
public interface DataScopeFilterService {
    
    /**
     * Check if current user has access to a specific scope
     * @param menuCode Menu code 
     * @param scopeId Scope ID to check (e.g., unit ID, class ID)
     * @return true if user can access, false otherwise
     * @throws AccessDeniedException if user has no access
     */
    boolean checkAccess(String menuCode, Long scopeId);

    /**
     * Get list of allowed scope IDs for current user on a menu
     * @param menuCode Menu code
     * @return List of allowed scope IDs, empty list means ALL (no restriction)
     */
    List<Long> getAllowedScopes(String menuCode);

    /**
     * Check if user can access multiple scopes
     * @param menuCode Menu code
     * @param scopeIds List of scope IDs to check
     * @return true if user can access ALL scopes, false if any scope is denied
     */
    boolean checkAccessToAll(String menuCode, List<Long> scopeIds);
}
