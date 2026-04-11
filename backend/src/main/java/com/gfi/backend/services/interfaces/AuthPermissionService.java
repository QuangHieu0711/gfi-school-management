package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.auth.PermissionsResponse;
import com.gfi.backend.models.entities.User;

public interface AuthPermissionService {
    /**
     * Get permissions and menus for a user based on their role
     * Used for login response - does NOT set ThreadLocal
     * @param roleId User's role ID
     * @param userId User's ID
     * @return PermissionsResponse with actions and scopes
     */
    PermissionsResponse getPermissionsByRoleId(Long roleId, Long userId);

    /**
     * Load user scopes into ThreadLocal for current request
     * Called by UserScopesLoadingFilter for EVERY authenticated request
     * @param user The authenticated User object (already loaded)
     * @param roleId User's role ID
     */
    void loadUserScopesIntoContext(User user, Long roleId);
}
