package com.gfi.backend.utils;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.gfi.backend.models.security.UserScopes;

/**
 * Utility class to access current user's security information and scopes
 * Uses ThreadLocal to store UserScopes for the current request thread
 * 
 * This is a static utility - no @Component needed
 */
public class SecurityContextUtils {

    private static final ThreadLocal<UserScopes> userScopesThreadLocal = new ThreadLocal<>();

    /**
     * Get current username from security context
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            }
        }
        return null;
    }

    /**
     * Get current user's scopes from ThreadLocal
     */
    public static Optional<UserScopes> getCurrentUserScopes() {
        UserScopes scopes = userScopesThreadLocal.get();
        return Optional.ofNullable(scopes);
    }

    /**
     * Set user scopes to ThreadLocal
     */
    public static void setUserScopes(UserScopes userScopes) {
        userScopesThreadLocal.set(userScopes);
    }

    /**
     * Clear user scopes from ThreadLocal (important for cleanup)
     * Should be called when user logs out or request completes
     */
    public static void clearUserScopes() {
        userScopesThreadLocal.remove();
    }
}
