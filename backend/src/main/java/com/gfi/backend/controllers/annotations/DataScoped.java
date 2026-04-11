package com.gfi.backend.controllers.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enforce data scope filtering on controller methods
 * 
 * Usage:
 * @GetMapping("/classrooms")
 * @DataScoped(menuCode = "CLASS_MANAGEMENT", scopeParamName = "unitId")
 * public ResponseEntity<?> getClassrooms(@RequestParam Long unitId) { ... }
 * 
 * The framework will automatically check if user has access to the specified unitId
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScoped {
    
    /**
     * Menu code to check permissions for
     */
    String menuCode();
    
    /**
     * Name of request parameter containing the scope ID to check
     * Can be path variable or request parameter
     */
    String scopeParamName() default "";
    
    /**
     * Whether to filter list results or just check single scope access
     * If true, the method return value will be filtered by user's allowed scopes
     */
    boolean autoFilter() default false;
}
