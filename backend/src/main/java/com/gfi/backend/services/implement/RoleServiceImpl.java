package com.gfi.backend.services.implement;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.role.RoleCreateRequest;
import com.gfi.backend.models.dtos.role.RoleDetailDto;
import com.gfi.backend.models.dtos.role.RoleFilterDto;
import com.gfi.backend.models.dtos.role.RoleListItemDto;
import com.gfi.backend.models.dtos.role.RoleUpdateRequest;
import com.gfi.backend.models.entities.Role;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.models.mappers.RoleMapper;
import com.gfi.backend.repositories.DataPermissionRepository;
import com.gfi.backend.repositories.PermissionRepository;
import com.gfi.backend.repositories.RoleRepository;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.repositories.specifications.RoleSpecification;
import com.gfi.backend.services.interfaces.RoleService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý logic quản lý vai trò.
 * 
 * Trách nhiệm tách biệt:
 * - Logic query: RoleSpecification
 * - Logic mapping: RoleMapper
 * - Validate & load relations: private helpers
 * - Security: SecurityUtils
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final DataPermissionRepository dataPermissionRepository;
    private final RoleSpecification roleSpecification;
    private final RoleMapper roleMapper;

    // Tìm kiếm và phân trang roles với filter
    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<RoleListItemDto, RoleFilterDto> search(PageRequestDto<RoleFilterDto> request) {
        RoleFilterDto filter = request.getFilter() == null ? new RoleFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<Role> page = roleRepository.findAll(roleSpecification.buildSpecification(filter), pageable);
        List<RoleListItemDto> items = page.getContent().stream()
                .map(roleMapper::toListItemDto)
                .toList();

        return PageResponseDto.<RoleListItemDto, RoleFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    // Danh sách vai trò cho dropdown/combobox
    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getOptions() {
        return roleRepository.findAll(Sort.by(Sort.Direction.ASC, "roleName")).stream()
                .map(role -> LookupItemDto.builder()
                        .id(role.getId())
                        .name(role.getRoleName())
                        .build())
                .toList();
    }

    // Lấy chi tiết vai trò theo id
    @Override
    @Transactional(readOnly = true)
    public RoleDetailDto getById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));
        return roleMapper.toDetailDto(role);
    }

    // Thêm mới vai trò
    @Override
    @Transactional
    public RoleDetailDto create(RoleCreateRequest request) {
        String code = normalize(request.getCode());
        String roleName = normalize(request.getRoleName());
        
        validateCodeDuplicate(code, null);
        validateRoleNameDuplicate(roleName, null);

        Role role = new Role();
        role.setCode(code);
        role.setRoleName(roleName);
        role.setDescription(normalizeNullable(request.getDescription()));
        role.setStatus(request.getStatus());
        role.setCreatedBy(SecurityUtils.getCurrentUsername());

        return roleMapper.toDetailDto(roleRepository.save(role));
    }

    // Cập nhật vai trò
    @Override
    @Transactional
    public RoleDetailDto update(Long id, RoleUpdateRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));

        String code = normalize(request.getCode());
        String roleName = normalize(request.getRoleName());
        
        validateCodeDuplicate(code, id);
        validateRoleNameDuplicate(roleName, id);

        role.setCode(code);
        role.setRoleName(roleName);
        role.setDescription(normalizeNullable(request.getDescription()));
        role.setStatus(request.getStatus());
        role.setUpdatedBy(SecurityUtils.getCurrentUsername());

        return roleMapper.toDetailDto(roleRepository.save(role));
    }

    // ==================== XÓA ====================
    @Override
    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));

        // Kiểm tra role có được sử dụng không
        if (userRepository.countByRoleId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.ROLE_IN_USE);
        }
        if (permissionRepository.countByRoleId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.ROLE_IN_USE);
        }
        if (dataPermissionRepository.countByRoleId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.ROLE_IN_USE);
        }

        // Xóa mềm: đánh dấu xóa
        role.setDeletedFlag(1);
        role.setDeletedAt(LocalDateTime.now());
        role.setDeletedBy(SecurityUtils.getCurrentUsername());
        roleRepository.save(role);
    }


    /**
     * Validate code không trùng.
     * Khi update: excludeId cho phép role giữ nguyên code của chính nó.
     * 
     * @param code mã role cần check
     * @param excludeId ID role loại trừ (null khi create)
     */
    private void validateCodeDuplicate(String code, Long excludeId) {
        boolean isDuplicate = excludeId == null
                ? roleRepository.existsByCode(code)
                : roleRepository.existsByCodeAndIdNot(code, excludeId);
        
        if (isDuplicate) {
            throw new UserMessageException(CommonErrorCode.ROLE_CODE_ALREADY_EXISTS);
        }
    }

    /**
     * Validate tên vai trò không trùng.
     * Khi update: excludeId cho phép role giữ nguyên tên của chính nó.
     * 
     * @param roleName tên role cần check
     * @param excludeId ID role loại trừ (null khi create)
     */
    private void validateRoleNameDuplicate(String roleName, Long excludeId) {
        boolean isDuplicate = excludeId == null
                ? roleRepository.existsByRoleName(roleName)
                : roleRepository.existsByRoleNameAndIdNot(roleName, excludeId);
        
        if (isDuplicate) {
            throw new UserMessageException(CommonErrorCode.ROLE_NAME_ALREADY_EXISTS);
        }
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
