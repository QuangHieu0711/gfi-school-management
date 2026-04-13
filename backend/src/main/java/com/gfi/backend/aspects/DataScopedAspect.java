package com.gfi.backend.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.gfi.backend.controllers.annotations.DataScoped;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.services.interfaces.DataScopeFilterService;

/**
 * AOP Aspect to enforce @DataScoped annotation
 * 
 * Hỗ trợ 2 chế độ:
 * 
 * 1. Chỉ check functional access (khi scopeExpression trống):
 *    @DataScoped(feature = "CLASS_MANAGEMENT", action = ActionType.VIEW)
 *    public void getList() { ... }
 * 
 * 2. Check functional + data scope access (khi có scopeExpression):
 *    Dùng SpEL để extract scope ID từ parameters hoặc body
 */
@Aspect
@Component
public class DataScopedAspect {
    
    private final DataScopeFilterService dataScopeFilterService;
    private final SpelExpressionParser spelParser = new SpelExpressionParser();

    public DataScopedAspect(DataScopeFilterService dataScopeFilterService) {
        this.dataScopeFilterService = dataScopeFilterService;
    }
    
    @Before("@annotation(dataScoped)")
    public void checkDataScope(JoinPoint joinPoint, DataScoped dataScoped) {
        String featureCode = dataScoped.feature();
        ActionType action = dataScoped.action();
        String scopeExpression = dataScoped.scopeExpression();
        
        // Bước 1: Check functional access (menu access + action permission)
        dataScopeFilterService.checkFunctionalAccess(featureCode, action);
        
        // Bước 2: Nếu không có scopeExpression → chỉ check functional, không check data scope
        if (scopeExpression == null || scopeExpression.isEmpty()) {
            return;
        }
        
        // Bước 3: Nếu có scopeExpression → extract scope ID và check data scope
        Long scopeId = extractScopeId(joinPoint, scopeExpression);
        
        if (scopeId == null) {
            throw new AccessDeniedException(
                    "Không thể extract scope ID từ expression: " + scopeExpression);
        }

        // Lấy scopeType từ annotation
        ScopeType scopeType = dataScoped.scopeType();
        
        // Bước 4: Check data scope access
        dataScopeFilterService.checkDataScopeAccess(featureCode, action, scopeType, scopeId);
    }

    /**
     * Ví dụ về cách sử dụng SpEL để extract scope ID:
     * 
     * Ví dụ:
     * "#id" → Tìm kiếm parameter có tên "id"
     * "#unitId" → Tìm kiếm parameter có tên "unitId"
     * "#request.unitId" → Tìm kiếm parameter "request" rồi lấy field "unitId"
     * "#filter.classId" → Tìm kiếm parameter "filter" rồi lấy field "classId"
     * 
     * @param joinPoint AOP join point
     * @param expression SpEL expression
     * @return scope ID (Long)
     */
    private Long extractScopeId(JoinPoint joinPoint, String expression) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            
            // Tạo SpEL context với tất cả parameters
            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            
            // Evaluate SpEL expression
            Object result = spelParser.parseExpression(expression).getValue(context);
            
            if (result == null) {
                return null;
            }
            
            // Convert result to Long
            if (result instanceof Long) {
                return (Long) result;
            } else if (result instanceof Integer) {
                return ((Integer) result).longValue();
            } else if (result instanceof String) {
                try {
                    return Long.parseLong((String) result);
                } catch (NumberFormatException e) {
                    throw new AccessDeniedException("Không thể chuyển đổi scope ID: " + result);
                }
            }
            
            return null;
        } catch (Exception e) {
            throw new AccessDeniedException("Lỗi khi đánh giá biểu thức scope: " + expression, e);
        }
    }
}
