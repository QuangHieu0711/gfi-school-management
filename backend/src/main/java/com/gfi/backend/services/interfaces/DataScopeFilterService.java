package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.models.security.ResolvedScope;

/**
 * Service to check and enforce data scope permissions
 * 
 * Tách biệt thành 2 lớp:
 * 1. Functional permission: có được action đó không
 * 2. Data scope permission: thiệt tế được access record nào
 */
public interface DataScopeFilterService {
    
    /**
     * Check if user has functional permission to perform an action on a feature
     * 
     * @param featureCode Feature/menu code (e.g., "CLASS_MANAGEMENT", "STUDENT_MANAGEMENT")
     * @param action Action type (VIEW, ADD, EDIT, DELETE, DOWNLOAD)
     * @throws AccessDeniedException nếu user không có permission
     */
    void checkFunctionalAccess(String featureCode, ActionType action);

    /**
     * Check if user has data scope access to a specific record
     * 
     * @param featureCode Feature code
     * @param action Action type
     * @param scopeType Scope type (UNIT, GRADE, CLASS, USER, SELF)
     * @param scopeId ID của scope (unitId, gradeId, classId, userId, etc.)
     * @return true nếu có access
     * @throws AccessDeniedException nếu không có access
     */
    boolean checkDataScopeAccess(String featureCode, ActionType action, ScopeType scopeType, Long scopeId);

    /**
     * Get list of resolved scopes cho một feature + action
     * Dùng khi cần filter query results
     * 
     * @param featureCode Feature code
     * @param action Action type
     * @return List của ResolvedScope - empty list nếu không cấu hình hoặc deny
     */
    List<ResolvedScope> getResolvedScopes(String featureCode, ActionType action);

    /**
     * Kiểm tra xem người dùng hiện tại có quyền truy cập vào menu hay không
     * (chỉ check functional access, không check data scope)
     * 
     * @param featureCode Feature code 
     * @throws AccessDeniedException nếu người dùng không có quyền truy cập vào menu
     */
    void checkMenuAccess(String featureCode);
}