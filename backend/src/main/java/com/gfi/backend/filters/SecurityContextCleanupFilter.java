package com.gfi.backend.filters;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.springframework.stereotype.Component;

import com.gfi.backend.utils.SecurityContextUtils;

/**
 * Filter to cleanup ThreadLocal UserScopes after each request
 * Prevents memory leaks from ThreadLocal not being cleaned up
 */
@Component
public class SecurityContextCleanupFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            // Always cleanup ThreadLocal at the end of request
            SecurityContextUtils.clearUserScopes();
        }
    }
}
