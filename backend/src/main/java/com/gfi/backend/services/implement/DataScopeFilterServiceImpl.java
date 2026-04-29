package com.gfi.backend.services.implement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.models.security.ResolvedScope;
import com.gfi.backend.models.security.UserScopes;
import com.gfi.backend.repositories.TeacherAssignmentRepository;
import com.gfi.backend.services.interfaces.DataScopeFilterService;
import com.gfi.backend.utils.SecurityContextUtils;

/**
 * Service to check and enforce data scope permissions for current user
 * 
 * Tách biệt:
 * - checkFunctionalAccess: kiểm tra user có permission action đó không
 * - checkDataScopeAccess: kiểm tra user có access record cụ thể không
 * - getResolvedScopes: trả list scope để filter query
 * 
 * Xử lý CLASS scope:
 * - Khi scopeType là CLASS, query TeacherAssignment để lấy danh sách teacherId dạy các lớp đó
 * - Chuyển đổi CLASS scope thành STAFF scope với danh sách teacherId
 */
@Service
public class DataScopeFilterServiceImpl implements DataScopeFilterService {

    private final TeacherAssignmentRepository teacherAssignmentRepository;

    public DataScopeFilterServiceImpl(TeacherAssignmentRepository teacherAssignmentRepository) {
        this.teacherAssignmentRepository = teacherAssignmentRepository;
    }

    @Override
    public void checkFunctionalAccess(String featureCode, ActionType action) {
        UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new AccessDeniedException("User scopes not found in context"));

        // ⚠️ BLOCKER FIX: Check functional permission (action capability)
        // Không check data scope, chỉ check: user có permission action này không?
        if (!userScopes.hasActionAccess(featureCode, action)) {
            throw new AccessDeniedException(
                    "User không có quyền " + action + " trên feature: " + featureCode);
        }
    }

    @Override
    public boolean checkDataScopeAccess(String featureCode, ActionType action, ScopeType scopeType, Long scopeId) {
        UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new AccessDeniedException("User scopes not found in context"));

        boolean hasAccess = userScopes.hasAccess(featureCode, action, scopeType, scopeId);

        // Nếu là CLASS scope, resolve thành teacherId và check
        if (!hasAccess && scopeType == ScopeType.CLASS && scopeId != null) {
            Set<Long> teacherIds = resolveClassToTeacherIds(Set.of(scopeId));
            if (!teacherIds.isEmpty()) {
                // Check xem có teacher nào được phép trong scope không
                hasAccess = teacherIds.stream()
                    .anyMatch(teacherId -> userScopes.hasAccess(featureCode, action, ScopeType.STAFF, teacherId));
            }
        }

        if (!hasAccess) {
            throw new AccessDeniedException(
                    String.format("User không có quyền %s %s (scope: %s=%d)", 
                            action, featureCode, scopeType, scopeId));
        }

        return true;
    }

    @Override
    public List<ResolvedScope> getResolvedScopes(String featureCode, ActionType action) {
        List<ResolvedScope> baseScopes = SecurityContextUtils.getCurrentUserScopes()
                .map(userScopes -> userScopes.getScopes(featureCode, action))
                .orElse(List.of());

        // Xử lý CLASS scope: chuyển đổi thành STAFF scope với danh sách teacherId
        List<ResolvedScope> resolvedScopes = new ArrayList<>();
        for (ResolvedScope scope : baseScopes) {
            if (scope.getScopeType() == ScopeType.CLASS && !scope.isUnrestricted() && scope.getScopeIds() != null) {
                // Query TeacherAssignment để lấy danh sách teacherId dạy các lớp này
                Set<Long> teacherIds = resolveClassToTeacherIds(scope.getScopeIds());
                if (!teacherIds.isEmpty()) {
                    // Thêm STAFF scope với danh sách teacherId
                    resolvedScopes.add(ResolvedScope.of(ScopeType.STAFF, teacherIds));
                }
            } else {
                // Giữ nguyên các scope khác
                resolvedScopes.add(scope);
            }
        }

        return resolvedScopes;
    }

    /**
     * Lấy danh sách teacherId của giáo viên dạy các lớp được chỉ định
     * 
     * @param classIds Danh sách ID lớp
     * @return Set<Long> Danh sách teacherId (staffId)
     */
    private Set<Long> resolveClassToTeacherIds(Set<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return Set.of();
        }

        return teacherAssignmentRepository.findAll((root, query, cb) -> {
            return cb.in(root.get("classroom").get("id")).value(classIds);
        })
        .stream()
        .map(assignment -> assignment.getStaff().getId())
        .collect(Collectors.toSet());
    }

    @Override
    public void checkMenuAccess(String featureCode) {
        UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new AccessDeniedException("User scopes not found in context"));

        boolean hasAccess = userScopes.hasMenuAccess(featureCode);
        
        if (!hasAccess) {
            throw new AccessDeniedException(
                    "User không có quyền truy cập vào menu: " + featureCode);
        }
    }
}
