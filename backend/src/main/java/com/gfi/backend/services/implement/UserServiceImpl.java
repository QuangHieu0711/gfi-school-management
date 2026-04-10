package com.gfi.backend.services.implement;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
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
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.repositories.specifications.UserSpecification;
import com.gfi.backend.services.interfaces.UserService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.PasswordUtils;
import com.gfi.backend.utils.SecurityUtils;

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
    private final UserSpecification userSpecification;
    private final UserMapper userMapper;

    // Danh sách & tìm kiếm có phân trang
    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<UserListItemDto, UserFilterDto> search(PageRequestDto<UserFilterDto> request) {
        UserFilterDto filter = request.getFilter() == null ? new UserFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<User> page = userRepository.findAll(userSpecification.buildSpecification(filter), pageable);
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
        return userMapper.toDetailDto(user);
    }

    // Tạo mới người dùng
    @Override
    @Transactional
    public UserDetailDto create(UserCreateRequest request) {
        String username = normalize(request.getUsername());
        validateUsernameDuplicate(username, null);
        validateEmailDuplicate(normalizeNullable(request.getEmail()), null);
        validatePhoneDuplicate(normalizeNullable(request.getPhone()), null);

        Role role = getRoleById(request.getRoleId());
        Unit unit = getUnitById(request.getUnitId());

        User user = new User();
        user.setUsername(username);
        user.setFullName(normalize(request.getFullName()));
        user.setEmail(normalizeNullable(request.getEmail()));
        user.setPhone(normalizeNullable(request.getPhone()));
        user.setRole(role);
        user.setUnit(unit);
        user.setStatus(request.getStatus());
        user.setPassword(encodePasswordFromClient(request.getPassword().trim()));
        user.setCreatedBy(SecurityUtils.getCurrentUsername());
        user.setDeletedFlag(0);

        return userMapper.toDetailDto(userRepository.save(user));
    }

    // Cập nhật người dùng
    @Override
    @Transactional
    public UserDetailDto update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.USER_NOT_FOUND));

        String username = normalize(request.getUsername());
        validateUsernameDuplicate(username, id);
        validateEmailDuplicate(normalizeNullable(request.getEmail()), id);
        validatePhoneDuplicate(normalizeNullable(request.getPhone()), id);

        Role role = getRoleById(request.getRoleId());
        Unit unit = getUnitById(request.getUnitId());

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
     * Validate email không trùng.
     * Khi update: excludeId cho phép user giữ nguyên email của chính nó.
     * 
     * @param email     email cần check
     * @param excludeId ID user loại trừ (null khi create)
     */
    private void validateEmailDuplicate(String email, Long excludeId) {
        // Bỏ qua nếu email rỗng hoặc null
        if (!StringUtils.hasText(email)) {
            return;
        }

        boolean isDuplicate = excludeId == null
                ? userRepository.existsByEmailAndDeletedFlagEquals(email, 0)
                : userRepository.existsByEmailAndIdNot(email, excludeId);

        if (isDuplicate) {
            throw new UserMessageException(CommonErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    /**
     * Validate số điện thoại không trùng.
     * Khi update: excludeId cho phép user giữ nguyên số điện thoại của chính nó.
     * 
     * @param phone     số điện thoại cần check
     * @param excludeId ID user loại trừ (null khi create)
     */
    private void validatePhoneDuplicate(String phone, Long excludeId) {
        // Bỏ qua nếu số điện thoại rỗng hoặc null
        if (!StringUtils.hasText(phone)) {
            return;
        }

        boolean isDuplicate = excludeId == null
                ? userRepository.existsByPhoneAndDeletedFlagEquals(phone, 0)
                : userRepository.existsByPhoneAndIdNot(phone, excludeId);

        if (isDuplicate) {
            throw new UserMessageException(CommonErrorCode.PHONE_ALREADY_EXISTS);
        }
    }

    /**
     * Áp dụng các trường chung từ request vào entity User.
     * Dùng chung cho cả create và update.
     */
    private void applyCommonFields(User user, UserUpdateRequest request, Role role, Unit unit) {
        user.setUsername(normalize(request.getUsername()));
        user.setFullName(normalize(request.getFullName()));
        user.setEmail(normalizeNullable(request.getEmail()));
        user.setPhone(normalizeNullable(request.getPhone()));
        user.setRole(role);
        user.setUnit(unit);
        user.setStatus(request.getStatus());
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
            user.setPassword(encodePasswordFromClient(rawPassword.trim()));
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

    /**
     * Chuẩn hóa string nullable: return null nếu rỗng hoặc whitespace.
     */
    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
