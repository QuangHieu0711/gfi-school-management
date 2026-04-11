package com.gfi.backend.aspects;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.gfi.backend.controllers.annotations.DataScoped;
import com.gfi.backend.services.interfaces.DataScopeFilterService;

/**
 * AOP Aspect to enforce @DataScoped annotation
 * Automatically validates user has access to requested scope before method execution
 */
@Aspect
@Component
public class DataScopedAspect {
    
    private final DataScopeFilterService dataScopeFilterService;
    
    public DataScopedAspect(DataScopeFilterService dataScopeFilterService) {
        this.dataScopeFilterService = dataScopeFilterService;
    }
    
    @Before("@annotation(dataScoped)")
    public void checkDataScope(JoinPoint joinPoint, DataScoped dataScoped) {
        String menuCode = dataScoped.menuCode();
        String scopeParamName = dataScoped.scopeParamName();
        
        if (scopeParamName.isEmpty()) {
            // No scope parameter to check
            return;
        }
        
        // Get method signature and parameters
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();
        
        // Find the parameter value by name
        Long scopeId = null;
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(scopeParamName)) {
                Object paramValue = args[i];
                if (paramValue instanceof Long) {
                    scopeId = (Long) paramValue;
                } else if (paramValue instanceof Integer) {
                    scopeId = ((Integer) paramValue).longValue();
                } else if (paramValue instanceof String) {
                    try {
                        scopeId = Long.parseLong((String) paramValue);
                    } catch (NumberFormatException e) {
                        throw new AccessDeniedException("Invalid scope ID format: " + paramValue);
                    }
                }
                break;
            }
        }
        
        if (scopeId == null) {
            throw new AccessDeniedException("Scope parameter '" + scopeParamName + "' not found");
        }
        
        // Check if user has access to this scope
        dataScopeFilterService.checkAccess(menuCode, scopeId);
    }
}
