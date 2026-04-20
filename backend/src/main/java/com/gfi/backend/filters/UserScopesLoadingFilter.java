package com.gfi.backend.filters;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.services.interfaces.AuthPermissionService;
import com.gfi.backend.utils.SecurityContextUtils;

public class UserScopesLoadingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(UserScopesLoadingFilter.class);

    private final AuthPermissionService authPermissionService;
    private final UserRepository userRepository;

    public UserScopesLoadingFilter(AuthPermissionService authPermissionService, UserRepository userRepository) {
        this.authPermissionService = authPermissionService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            logger.info("UserScopesLoadingFilter request={}", request.getRequestURI());

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            logger.info("authenticationClass={}", authentication != null ? authentication.getClass().getName() : "null");

            if (authentication != null
                    && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken)) {

                String username = resolveUsername(authentication);
                logger.info("resolvedUsername={}", username);

                if (username != null && !username.isBlank() && !"anonymousUser".equals(username)) {
                    var user = userRepository.findByUsernameWithStaffAndRole(username).orElse(null);

                    if (user != null && user.getRole() != null) {
                        logger.info("loading scopes for userId={}, roleId={}", user.getId(), user.getRole().getId());

                        authPermissionService.loadUserScopesIntoContext(user, user.getRole().getId());

                        logger.info("loaded scopes successfully for username={}", username);
                    } else {
                        logger.warn("user not found or role missing for username={}", username);
                    }
                } else {
                    logger.warn("could not resolve username from authentication");
                }
            } else {
                logger.info("no authenticated user in SecurityContext");
            }

            filterChain.doFilter(request, response);
        } finally {
            logger.info("clearing user scopes for request={}", request.getRequestURI());
            SecurityContextUtils.clearUserScopes();
        }
    }

    private String resolveUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        logger.info("principalClass={}", principal != null ? principal.getClass().getName() : "null");

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal instanceof String principalStr) {
            return principalStr;
        }

        return authentication.getName();
    }
}