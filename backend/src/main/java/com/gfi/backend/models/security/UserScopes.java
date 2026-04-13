package com.gfi.backend.models.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ScopeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Store user's data scopes for access control
 * 
 * Cấu trúc mới:
 * feature → action → list of ResolvedScope
 * 
 * Giải quyết các vấn đề:
 * - Rõ ràng action (VIEW/ADD/EDIT/DELETE) có thể có scope khác nhau
 * - ResolvedScope phân biệt rõ: unrestricted=true vs configured scopes
 * - Hỗ trợ multiple scope types cùng lúc cho 1 action
 * 
 * Ví dụ:
 * {
 *   "CLASS_MANAGEMENT" -> {
 *     VIEW: [
 *       { scopeType: UNIT, unrestricted: false, scopeIds: [9] }
 *     ],
 *     EDIT: [
 *       { scopeType: CLASS, unrestricted: false, scopeIds: [1, 2] }
 *     ]
 *   }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserScopes {
    private Long userId;
    private Long roleId;
    private String roleCode;
    
    /**
     * Functional permissions: menu codes user can access
     * Example: {"ACCOUNT_MANAGEMENT", "CLASS_MANAGEMENT", "STUDENT_MANAGEMENT"}
     * ⚠️ Deprecated - dùng allowedActionsByFeature thay vì cái này
     */
    @Deprecated
    private Set<String> allowedMenuCodes;
    
    /**
     * Functional permissions per action: feature -> set of allowed actions
     * 
     * Build từ Permission table:
     * - isView=1 → ActionType.VIEW
     * - isAdd=1 → ActionType.ADD
     * - isEdit=1 → ActionType.EDIT
     * - isDelete=1 → ActionType.DELETE
     * - isDownload=1 → ActionType.DOWNLOAD
     * 
     * Ví dụ:
     * {
     *   "CLASS_MANAGEMENT": {VIEW, EDIT},
     *   "STUDENT_MANAGEMENT": {VIEW, ADD, EDIT},
     *   "ACCOUNT_MANAGEMENT": {VIEW}
     * }
     */
    private Map<String, Set<ActionType>> allowedActionsByFeature;
    
    /**
     * Map: featureCode → actionType → list of ResolvedScope
     * 
     * Cấu trúc chi tiết hơn Map<String, List<Long>>:
     * - Giữ scopeType (UNIT, GRADE, CLASS, etc)
     * - Phân biệt unrestricted từ "có cấu hình" vs "không cấu hình"
     * - Hỗ trợ action-level (VIEW/ADD/EDIT/DELETE khác nhau)
     * - Hỗ trợ multiple scope rules cho 1 action (OR logic)
     */
    private Map<String, Map<ActionType, List<ResolvedScope>>> scopesByFeatureAndAction;

    /**
     * Check if user has functional permission (action capability)
     * 
     * @param featureCode Feature code
     * @param action Action type to check
     * @return true nếu user có quyền làm action này
     */
    public boolean hasActionAccess(String featureCode, ActionType action) {
        if (allowedActionsByFeature == null) {
            return false;
        }
        String normalizedCode = normalize(featureCode);
        Set<ActionType> actions = allowedActionsByFeature.get(normalizedCode);
        return actions != null && actions.contains(action);
    }

    /**
     * Check if user has functional permission for a menu
     * Dùng allowedActionsByFeature thay vì allowedMenuCodes (deprecated)
     */
    public boolean hasMenuAccess(String featureCode) {
        if (allowedActionsByFeature == null) {
            return false;
        }
        String normalizedCode = normalize(featureCode);
        Set<ActionType> actions = allowedActionsByFeature.get(normalizedCode);
        return actions != null && !actions.isEmpty();
    }

    /**
     * Get list of scopes cho một feature + action
     * Nếu không có cấu hình hoặc action không hỗ trợ → trả empty list
     */
    public List<ResolvedScope> getScopes(String featureCode, ActionType action) {
        if (scopesByFeatureAndAction == null) {
            return List.of();
        }
        String normalizedCode = normalize(featureCode);
        Map<ActionType, List<ResolvedScope>> byAction = scopesByFeatureAndAction.get(normalizedCode);
        if (byAction == null) {
            return List.of();
        }
        return byAction.getOrDefault(action, List.of());
    }

    /**
     * Check if user has access to specific scope
     * 
     * @param featureCode Feature code
     * @param action Action type
     * @param scopeType Scope type cần check (UNIT, GRADE, CLASS, USER, SELF)
     * @param scopeId ID cụ thể (ví dụ: unitId=9)
     * @return true nếu user có quyền
     */
    public boolean hasAccess(String featureCode, ActionType action, ScopeType scopeType, Long scopeId) {
        List<ResolvedScope> scopes = getScopes(featureCode, action);

        // Không có cấu hình action cho feature → deny
        if (scopes.isEmpty()) {
            return false;
        }

        // Kiểm tra OR logic: user có access nếu match 1 trong các scope rules
        for (ResolvedScope scope : scopes) {
            // Nếu rule là ALL hoặc unrestricted → allow
            if (scope.isUnrestricted() || scope.getScopeType() == ScopeType.ALL) {
                return true;
            }
            // Nếu rule match scope type này và ID có trong list → allow
            if (scope.getScopeType() == scopeType && scope.getScopeIds() != null && scope.getScopeIds().contains(scopeId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normalize feature code: trim và uppercase
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
