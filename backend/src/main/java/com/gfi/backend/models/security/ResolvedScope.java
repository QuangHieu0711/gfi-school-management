package com.gfi.backend.models.security;

import java.util.Set;

import com.gfi.backend.models.enums.ScopeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thể hiện một quy tắc scope đã được resolve
 * 
 * Ví dụ:
 * 1. ALL: scopeType=ALL, unrestricted=true, scopeIds=[]
 * 2. UNIT: scopeType=UNIT, unrestricted=false, scopeIds=[9]
 * 3. GRADE: scopeType=GRADE, unrestricted=false, scopeIds=[1,2,3]
 * 
 * Khác với map trước đây, kiểu này rõ ràng hơn:
 * - unrestricted=true → không giới hạn
 * - unrestricted=false + scopeIds khác rỗng → giới hạn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedScope {
    /**
     * Loại scope: ALL, UNIT, GRADE, CLASS, USER, SELF
     */
    private ScopeType scopeType;

    /**
     * true nếu không hạn chế (scopeType = ALL hoặc admin)
     * false nếu có hạn chế
     */
    private boolean unrestricted;

    /**
     * ID của scope (unit IDs, class IDs, grade IDs, user IDs, etc)
     * Rỗng nếu unrestricted=true
     */
    private Set<Long> scopeIds;

    /**
     * Build một ResolvedScope ALL (không hạn chế)
     */
    public static ResolvedScope all() {
        return ResolvedScope.builder()
                .scopeType(ScopeType.ALL)
                .unrestricted(true)
                .scopeIds(Set.of())
                .build();
    }

    /**
     * Build một ResolvedScope với giới hạn
     */
    public static ResolvedScope of(ScopeType scopeType, Set<Long> scopeIds) {
        return ResolvedScope.builder()
                .scopeType(scopeType)
                .unrestricted(false)
                .scopeIds(scopeIds != null ? scopeIds : Set.of())
                .build();
    }

    /**
     * Kiểm tra scope ID có nằm trong rule này không
     */
    public boolean contains(Long scopeId) {
        if (unrestricted || scopeType == ScopeType.ALL) {
            return true;
        }
        return scopeIds != null && scopeIds.contains(scopeId);
    }
}
