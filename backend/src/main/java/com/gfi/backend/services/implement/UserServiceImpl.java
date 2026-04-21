package com.gfi.backend.services.implement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.user.UserCreateRequest;
import com.gfi.backend.models.dtos.user.UserDetailDto;
import com.gfi.backend.models.dtos.user.UserFilterDto;
import com.gfi.backend.models.dtos.user.UserListItemDto;
import com.gfi.backend.models.dtos.user.UserUpdateRequest;
import com.gfi.backend.models.entities.Role;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.entities.User;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.models.mappers.UserMapper;
import com.gfi.backend.repositories.RoleRepository;
import com.gfi.backend.repositories.StaffRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.repositories.specifications.UserSpecification;
import com.gfi.backend.services.interfaces.UserService;
import com.gfi.backend.models.entities.Staff;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.PasswordUtils;
import com.gfi.backend.utils.SecurityUtils;
import com.gfi.backend.utils.SecurityContextUtils;
import com.gfi.backend.utils.ScopeFilterUtils;
import com.gfi.backend.models.security.UserScopes;
import com.gfi.backend.models.security.FeatureKey;
import com.gfi.backend.models.security.ResolvedScope;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.repositories.RoleAssignmentPermissionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý logic quản lý người dùng.
 * 
 * Trách nhiệm tách biệt:
 * - Logic query: UserSpecification
 * - Logic mapping: UserMapper
 * - Load quan hệ & validate: private helpers
 * - Security: SecurityUtils
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UnitRepository unitRepository;
    private final StaffRepository staffRepository;
    private final RoleAssignmentPermissionRepository roleAssignmentPermissionRepository;
    private final UserSpecification userSpecification;
    private final UserMapper userMapper;
    
    // Feature key cho phân quyền - match với DB (ACCOUNT_MANAGEMENT)
    private static final String FEATURE = FeatureKey.ACCOUNT_MANAGEMENT.getCode();
    private static final String STAFF_FEATURE = FeatureKey.STAFF_PROFILE.getCode();

    /**
     * Lấy danh sách unit options cho form tạo người dùng.
     * Chỉ trả về unit mà user hiện tại có quyền tạo.
     * - Nếu unrestricted (ALL scope): trả về tất cả units có status=1
     * - Nếu restricted: trả về units từ allowed unit IDs
     */
    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getUnitOptionsForCreateUser() {
        List<ResolvedScope> allowedScopes = resolveScopesForCreateUserOptions();
        
        // Check if user has unrestricted access
        boolean isUnrestricted = allowedScopes.stream().anyMatch(ResolvedScope::isUnrestricted);

        List<Unit> units;
        if (isUnrestricted) {
            // Unrestricted: get all active, non-deleted units
            units = unitRepository.findByStatusAndDeletedFlagOrderByName(1, 0);
        } else {
            // Restricted: get units from allowed IDs (also filter by status and deleted flag)
            List<Long> allowedUnitIds = allowedScopes.stream()
                    .flatMap(rs -> rs.getScopeIds().stream())
                    .toList();
            units = unitRepository.findByIdInAndStatusAndDeletedFlagOrderByName(allowedUnitIds, 1, 0);
        }

        // Map to LookupItemDto
        return units.stream()
                .map(unit -> LookupItemDto.builder()
                        .id(unit.getId())
                        .name(unit.getName())
                        .build())
                .toList();
    }

    private void ensureAccessForCreateUserOptions() {
        tryPrimaryThenFallback(
                () -> {
                    ScopeFilterUtils.checkAccess(FEATURE);
                    return null;
                },
                () -> {
                    ScopeFilterUtils.checkAccess(STAFF_FEATURE);
                    return null;
                });
    }

    private List<ResolvedScope> resolveScopesForCreateUserOptions() {
        return tryPrimaryThenFallback(
                () -> {
                    ScopeFilterUtils.checkAccess(FEATURE);
                    return ScopeFilterUtils.getScopesForQuery(FEATURE, ActionType.ADD);
                },
                () -> {
                    ScopeFilterUtils.checkAccess(STAFF_FEATURE);
                    return ScopeFilterUtils.getScopesForQuery(STAFF_FEATURE, ActionType.ADD);
                });
    }

    private <T> T tryPrimaryThenFallback(Supplier<T> primary, Supplier<T> fallback) {
        try {
            return primary.get();
        } catch (AccessDeniedException primaryException) {
            try {
                return fallback.get();
            } catch (AccessDeniedException fallbackException) {
                throw primaryException;
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getRoleOptionsForCreateUser() {
        ensureAccessForCreateUserOptions();

        UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ACCESS_DENIED));

        List<Role> roles = roleAssignmentPermissionRepository
                .findAssignableRolesForCreate(userScopes.getRoleId());

        return roles.stream()
                .map(role -> LookupItemDto.builder()
                        .id(role.getId())
                        .name(role.getRoleName())
                        .build())
                .toList();
    }

    // Danh sách & tìm kiếm có phân trang
    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<UserListItemDto, UserFilterDto> search(PageRequestDto<UserFilterDto> request) {
        UserFilterDto filter = request.getFilter() == null ? new UserFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        // Apply scope filtering: auto filter by allowed units
        List<ResolvedScope> allowedScopes = ScopeFilterUtils.getScopesForQuery(FEATURE, ActionType.VIEW);
        boolean isUnrestricted = allowedScopes.stream().anyMatch(ResolvedScope::isUnrestricted);

        Page<User> page;
        if (isUnrestricted) {
            // Unrestricted (ALL scope): use specification as is
            page = userRepository.findAll(userSpecification.buildSpecification(filter), pageable);
        } else {
            // Restricted: filter by allowed units + apply specification
            List<Long> allowedUnitIds = allowedScopes.stream()
                    .flatMap(rs -> rs.getScopeIds().stream())
                    .toList();
            page = userRepository.findByUnitIdIn(allowedUnitIds, pageable);
            // Apply filter spec on top of unit filter would require custom specification
            // For now, unit filter is the main restriction
        }

        List<UserListItemDto> items = page.getContent().stream()
                .map(userMapper::toListItemDto)
                .toList();

        return PageResponseDto.<UserListItemDto, UserFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    // Chi tiết người dùng theo ID
    @Override
    @Transactional(readOnly = true)
    public UserDetailDto getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.USER_NOT_FOUND));
        
        // Enforce scope: validate user's unit is within allowed scopes
        Long unitId = user.getUnitId();
        if (unitId != null) {
            ScopeFilterUtils.validateAccess(FEATURE, ActionType.VIEW, ScopeType.UNIT, unitId);
        }
        
        return userMapper.toDetailDto(user);
    }

    // Tạo mới người dùng
    @Override
    @Transactional
    public UserDetailDto create(UserCreateRequest request) {
        // Step 1: Check functional permission
        ScopeFilterUtils.checkAccess(FEATURE);

        // Step 2: Get current user scopes (already loaded by UserScopesLoadingFilter)
        UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ACCESS_DENIED));
        
        // Step 3: Get allowed unit scopes
        List<ResolvedScope> allowedScopes = ScopeFilterUtils.getScopesForQuery(FEATURE, ActionType.ADD);
        boolean isUnrestricted = allowedScopes.stream().anyMatch(ResolvedScope::isUnrestricted);

        // Step 4: Handle unit assignment
        Long unitIdToAssign = request.getUnitId();
        
        // If SCHOOL_ADMIN, auto-assign current user's unit if not specified
        if ("SCHOOL_ADMIN".equals(userScopes.getRoleCode())) {
            if (unitIdToAssign == null) {
                // No unit specified, use current user's unit from scope
                if (!isUnrestricted && !allowedScopes.isEmpty()) {
                    unitIdToAssign = allowedScopes.get(0).getScopeIds().stream().findFirst().orElse(null);
                }
            } else {
                // Unit specified, validate it's within SCHOOL_ADMIN's allowed scope
                List<Long> allowedUnitIds = allowedScopes.stream()
                        .flatMap(rs -> rs.getScopeIds().stream())
                        .toList();
                if (!isUnrestricted && !allowedUnitIds.contains(unitIdToAssign)) {
                    throw new UserMessageException(CommonErrorCode.ACCESS_DENIED.getCode(),
                            "SCHOOL_ADMIN chỉ được tạo tài khoản cho đơn vị của mình");
                }
            }
        } else if (unitIdToAssign != null) {
            // For non-SCHOOL_ADMIN roles, validate unitId is within allowed scopes
            if (!isUnrestricted) {
                List<Long> allowedUnitIds = allowedScopes.stream()
                        .flatMap(rs -> rs.getScopeIds().stream())
                        .toList();
                if (!allowedUnitIds.contains(unitIdToAssign)) {
                    throw new UserMessageException(CommonErrorCode.ACCESS_DENIED.getCode(),
                            "Không được tạo tài khoản cho đơn vị này");
                }
            }
        }

        // Step 5: Validate role-assignment permission: can current user assign requested role?
        boolean canAssignRole = roleAssignmentPermissionRepository
            .existsByCreatorRoleIdAndTargetRoleIdAndCanCreateAndStatusAndDeletedFlag(
                userScopes.getRoleId(),
                request.getRoleId(),
                1,
                1,
                0
            );

        if (!canAssignRole) {
            throw new UserMessageException(CommonErrorCode.ACCESS_DENIED.getCode(),
                "Bạn không được phép gán vai trò này");
        }

        // Step 6: Validate other duplicates
        String username = normalize(request.getUsername());
        validateUsernameDuplicate(username, null);

        // Step 7: Create user
        // ✅ NEW: User is now auth-only; profile data (fullName, email, phone, unit) 
        // will be managed via Staff entity after migration
        Role role = getRoleById(request.getRoleId());

        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        user.setStatus(request.getStatus());
        user.setPasswordHash(encodePasswordFromClient(request.getPassword().trim()));
        user.setCreatedBy(SecurityUtils.getCurrentUsername());
        user.setDeletedFlag(0);

        // Step 8: Link Staff if provided
        if (request.getStaffId() != null) {
            Staff staff = staffRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new UserMessageException(CommonErrorCode.STAFF_NOT_FOUND));
            user.setStaff(staff);
        }

        // NOTE: fullName, email, phone, unit are now profile data managed via Staff
        // This will be handled in Phase 2 with full API restructuring
        
        return userMapper.toDetailDto(userRepository.save(user));
    }

    // Cập nhật người dùng
    @Override
    @Transactional
    public UserDetailDto update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.USER_NOT_FOUND));

        // Enforce scope: validate user's unit is within allowed scopes before allowing update
        Long unitId = user.getUnitId();
        if (unitId != null) {
            ScopeFilterUtils.validateAccess(FEATURE, ActionType.EDIT, ScopeType.UNIT, unitId);
        }

        String username = normalize(request.getUsername());
        validateUsernameDuplicate(username, id);

        Role role = getRoleById(request.getRoleId());
        Unit unit = getUnitById(request.getUnitId());

        // If role is changing, validate current user can update to target role
        if (!user.getRole().getId().equals(request.getRoleId())) {
            UserScopes userScopes = SecurityContextUtils.getCurrentUserScopes()
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ACCESS_DENIED));

            boolean canUpdateRole = roleAssignmentPermissionRepository
                .existsByCreatorRoleIdAndTargetRoleIdAndCanUpdateAndStatusAndDeletedFlag(
                    userScopes.getRoleId(),
                    request.getRoleId(),
                    1,
                    1,
                    0
                );

            if (!canUpdateRole) {
            throw new UserMessageException(CommonErrorCode.ACCESS_DENIED.getCode(),
                "Bạn không được phép cập nhật sang vai trò này");
            }
        }

        applyCommonFields(user, request, role, unit);
        applyPasswordIfPresent(user, request.getPassword());
        user.setUpdatedBy(SecurityUtils.getCurrentUsername());

        return userMapper.toDetailDto(userRepository.save(user));
    }

    // Xóa người dùng
    @Override
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.USER_NOT_FOUND));

        // Enforce scope: validate user's unit is within allowed scopes before allowing delete
        Long unitId = user.getUnitId();
        if (unitId != null) {
            ScopeFilterUtils.validateAccess(FEATURE, ActionType.DELETE, ScopeType.UNIT, unitId);
        }

        // Xóa mềm: đánh dấu xóa 
        user.setDeletedFlag(1);
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(SecurityUtils.getCurrentUsername());
        userRepository.save(user);
    }

    /**
     * Lấy Role theo ID, throw exception nếu không tìm thấy.
     */
    private Role getRoleById(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));
    }

    /**
     * Lấy Unit theo ID, throw exception nếu không tìm thấy.
     */
    private Unit getUnitById(Long unitId) {
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));
    }

    /**
     * Validate username không trùng.
     * Khi update: excludeId cho phép user giữ nguyên username của chính nó.
     * 
     * @param username  tên đăng nhập cần check
     * @param excludeId ID user loại trừ (null khi create)
     */
    private void validateUsernameDuplicate(String username, Long excludeId) {
        boolean isDuplicate = excludeId == null
                ? userRepository.existsByUsername(username)
                : userRepository.existsByUsernameAndIdNot(username, excludeId);

        if (isDuplicate) {
            throw new UserMessageException(CommonErrorCode.USERNAME_ALREADY_EXISTS);
        }
    }

    /**
     * Áp dụng các trường chung từ request vào entity User.
     *  Profile fields (fullName, email, phone, unit) are now managed via Staff
     * User is authentication-only.
     */
    private void applyCommonFields(User user, UserUpdateRequest request, Role role, Unit unit) {
        user.setUsername(normalize(request.getUsername()));
        user.setRole(role);
        user.setStatus(request.getStatus());
        // Profile data (fullName, email, phone, unit) is no longer set on User
        // These are now managed via Staff entity
    }

    /**
     * Áp dụng thay đổi mật khẩu nếu có.
     * Xử lý convention: FE hash SHA256, BE hash SHA256 lần nữa.
     * 
     * @param user        entity User
     * @param rawPassword password từ request (có thể null)
     */
    private void applyPasswordIfPresent(User user, String rawPassword) {
        if (StringUtils.hasText(rawPassword)) {
            user.setPasswordHash(encodePasswordFromClient(rawPassword.trim()));
        }
    }

    /**
     * Mã hóa mật khẩu từ client.
     * Convention: FE gửi SHA256(password), BE apply SHA256 thêm lần nữa để bảo mật
     * thêm.
     * Note: Cách hash 2 lần này cần review lại trong tương lai.
     * 
     * @param clientPassword password từ client (đã SHA256 hash)
     * @return password hash 2 lần
     */
    private String encodePasswordFromClient(String clientPassword) {
        return PasswordUtils.sha256(clientPassword);
    }

    /**
     * Chuẩn hóa kích thước trang phân trang.
     */
    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }

    /**
     * Chuẩn hóa số trang hiện tại.
     */
    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow <= 0 ? 1 : pageNow;
    }

    /**
     * Chuẩn hóa string: trim.
     */
    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
