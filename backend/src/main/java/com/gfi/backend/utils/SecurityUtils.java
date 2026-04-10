package com.gfi.backend.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for security and authentication operations.
 */
public class SecurityUtils {

    private static final String ANONYMOUS_USER = "anonymousUser";
    private static final String SYSTEM_USER = "SYSTEM";

    /**
     * Gets the current authenticated username.
     * Returns "SYSTEM" if no user is authenticated or if the user is anonymous.
     *
     * @return the current username or "SYSTEM"
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || ANONYMOUS_USER.equals(authentication.getName())) {
            return SYSTEM_USER;
        }
        return authentication.getName();
    }
}
