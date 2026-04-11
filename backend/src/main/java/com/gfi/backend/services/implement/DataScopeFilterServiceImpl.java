package com.gfi.backend.services.implement;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gfi.backend.models.security.UserScopes;
import com.gfi.backend.services.interfaces.DataScopeFilterService;
import com.gfi.backend.utils.SecurityContextUtils;

/**
 * Service to check and enforce data scope permissions for current user
 */
@Service
public class DataScopeFilterServiceImpl implements DataScopeFilterService {

    @Override
    public boolean checkAccess(String menuCode, Long scopeId) {
        UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new RuntimeException("User scopes not found in security context"));

        boolean hasAccess = userScopes.hasAccessToScope(menuCode, scopeId);
        
        if (!hasAccess) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User does not have access to scope: " + scopeId + " for menu: " + menuCode);
        }
        
        return true;
    }

    @Override
    public List<Long> getAllowedScopes(String menuCode) {
        return SecurityContextUtils.getCurrentUserScopes()
                .map(userScopes -> userScopes.getScopesForMenu(menuCode))
                .orElse(List.of());
    }

    @Override
    public boolean checkAccessToAll(String menuCode, List<Long> scopeIds) {
        return scopeIds.stream().allMatch(scopeId -> {
            try {
                checkAccess(menuCode, scopeId);
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }
}
