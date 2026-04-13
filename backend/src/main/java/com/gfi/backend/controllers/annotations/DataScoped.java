package com.gfi.backend.controllers.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ScopeType;

/**
 * Annotation to enforce data scope filtering on controller methods
 * 
 * Hỗ trợ 2 chế độ:
 * 
 * 1. Chỉ check menu access (không check scope cụ thể):
 *    @DataScoped(feature = "CLASS_MANAGEMENT", action = ActionType.VIEW)
 * 
 * 2. Check menu + scope (dùng SpEL để extract scope ID):
 *    @DataScoped(
 *        feature = "CLASS_MANAGEMENT",
 *        action = ActionType.VIEW,
 *        scopeExpression = "#unitId"
 *    )
 *    public ResponseEntity<?> getClasses(@PathVariable Long unitId) { ... }
 *
 *    @DataScoped(
 *        feature = "STUDENT_MANAGEMENT",
 *        action = ActionType.EDIT,
 *        scopeExpression = "#request.classId"
 *    )
 *    public ResponseEntity<?> update(@RequestBody StudentUpdateRequest request) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScoped {
    
    /**
     * Feature/section code (tên menu code)
     * Ví dụ: "CLASS_MANAGEMENT", "STUDENT_MANAGEMENT", "ACCOUNT_MANAGEMENT"
     */
    String feature();
    
    /**
     * Hành động/action type
     * Ví dụ: VIEW, ADD, EDIT, DELETE
     */
    ActionType action() default ActionType.VIEW;
    
    /**
     * ⚠️ NEW: Loại scope để check
     * Default là UNIT, nhưng có thể là CLASS, GRADE, USER, SELF, v.v
     * 
     * Ví dụ:
     * - scopeType = ScopeType.UNIT: check unitId
     * - scopeType = ScopeType.CLASS: check classId
     * - scopeType = ScopeType.SELF: check userId
     */
    ScopeType scopeType() default ScopeType.UNIT;
    
    /**
     * SpEL expression để extract scope ID từ request
     * 
     * Ví dụ:
     * - "#id" → extract từ @PathVariable Long id
     * - "#unitId" → extract từ @RequestParam Long unitId
     * - "#request.unitId" → extract từ @RequestBody request.unitId
     * - "#filter.classId" → extract từ @RequestBody filter.classId
     * 
     * Để trống nếu chỉ check menu access, không check scope
     */
    String scopeExpression() default "";
}
