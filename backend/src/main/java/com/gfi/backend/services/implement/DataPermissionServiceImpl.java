package com.gfi.backend.services.implement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.datapermission.DataPermissionItemDto;
import com.gfi.backend.models.dtos.datapermission.DataPermissionSaveRequest;
import com.gfi.backend.models.dtos.datapermission.DataPermissionScopeItemDto;
import com.gfi.backend.models.dtos.datapermission.DataPermissionScopeSaveRequest;
import com.gfi.backend.models.dtos.datapermission.DataScopeContext;
import com.gfi.backend.models.entities.DataPermission;
import com.gfi.backend.models.entities.DataPermissionScope;
import com.gfi.backend.models.entities.Menu;
import com.gfi.backend.models.entities.Role;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.DataPermissionRepository;
import com.gfi.backend.repositories.MenuRepository;
import com.gfi.backend.repositories.RoleRepository;
import com.gfi.backend.services.interfaces.DataPermissionService;
import com.gfi.backend.utils.SecurityUtils;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Dịch vụ quản lý phân quyền dữ liệu cho các vai trò
 * Xử lý phân quyền cấp độ menu và scope cho từng role
 */
@Service
public class DataPermissionServiceImpl implements DataPermissionService {

    @Autowired
    private DataPermissionRepository dataPermissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MenuRepository menuRepository;

    /**
     * Lấy danh sách phân quyền theo role ID
     * Bao gồm cả quyền trên menu cha nếu có quyền menu con
     * 
     * @param roleId ID của role
     * @return Danh sách phân quyền (menu + scope)
     */
    @Override
    @Transactional(readOnly = true)
    public List<DataPermissionItemDto> getByRoleId(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));

        List<DataPermission> dataPermissions = dataPermissionRepository.findAllByRoleIdOrderByIdAsc(roleId);
        Map<Long, DataPermissionItemDto> resultMap = new HashMap<>();

        for (DataPermission dataPermission : dataPermissions) {
            DataPermissionItemDto dto = toDto(dataPermission);
            resultMap.put(dto.getMenuId(), dto);
            appendParentMenus(resultMap, role, dataPermission.getMenu());
        }

        return resultMap.values().stream()
                .sorted(Comparator
                        .comparing((DataPermissionItemDto item) -> item.getParentId() != null)
                        .thenComparing(item -> item.getParentId() == null ? 0L : item.getParentId())
                        .thenComparing(DataPermissionItemDto::getMenuId))
                .toList();
    }

    /**
     * Lưu/cập nhật phân quyền dữ liệu cho các role
     * Hỗ trợ batch save với validation unique (roleId + menuId)
     * 
     * @param requests Danh sách request phân quyền
     * @return Danh sách phân quyền sau khi lưu
     */
    @Override
    @Transactional
    public List<DataPermissionItemDto> savePermissions(List<DataPermissionSaveRequest> requests) {
        validateBatchRequest(requests);

        Set<String> uniquePairs = new HashSet<>();
        Set<Long> roleIds = new HashSet<>();
        String currentUsername = SecurityUtils.getCurrentUsername();

        for (DataPermissionSaveRequest request : requests) {
            ensureUniquePair(uniquePairs, request.getRoleId(), request.getMenuId());
            validateScopes(request.getScopes());
            roleIds.add(request.getRoleId());

            DataPermission dataPermission = dataPermissionRepository
                    .findByRoleIdAndMenuId(request.getRoleId(), request.getMenuId())
                    .orElse(null);

            if (!isEnabled(request.getStatus())) {
                if (dataPermission != null) {
                    dataPermissionRepository.delete(dataPermission);
                }
                continue;
            }

            boolean isNewDataPermission = dataPermission == null;
            if (isNewDataPermission) {
                dataPermission = new DataPermission();
            }

            applyRequest(dataPermission, request, currentUsername, isNewDataPermission);
            dataPermissionRepository.save(dataPermission);
        }

        if (roleIds.size() == 1) {
            return getByRoleId(roleIds.iterator().next());
        }

        List<DataPermissionItemDto> results = new ArrayList<>();
        for (Long roleId : roleIds) {
            results.addAll(getByRoleId(roleId));
        }
        return results;
    }

    /**
     * Xác định các scope type mà user có quyền thực hiện
     * Dùng để kiểm tra quyền dữ liệu ở mức độ chi tiết
     * 
     * @param roleId ID của role
     * @param menuId ID của menu
     * @param userId ID của user
     * @return Context scope với danh sách quyền
     */
    @Override
    @Transactional(readOnly = true)
    public DataScopeContext resolve(Long roleId, Long menuId, Long userId) {
        DataPermission dataPermission = dataPermissionRepository.findByRoleIdAndMenuId(roleId, menuId)
                .filter(item -> item.getStatus() != null && item.getStatus() == 1)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ACCESS_DENIED));

        List<String> scopeTypes = dataPermission.getScopes().stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == 1)
                .map(DataPermissionScope::getScopeType)
                .toList();

        return DataScopeContext.builder()
                .userId(userId)
                .scopeTypes(scopeTypes)
                .build();
    }

    /**
     * Thêm các menu cha vào kết quả nếu có menu con được phân quyền
     * 
     * @param resultMap Map chứa kết quả phân quyền
     * @param role      Role hiện tại
     * @param menu      Menu để tìm các menu cha
     */
    private void appendParentMenus(Map<Long, DataPermissionItemDto> resultMap, Role role, Menu menu) {
        Menu currentParent = menu.getParentMenu();
        while (currentParent != null) {
            resultMap.putIfAbsent(currentParent.getId(), toParentDto(role, currentParent));
            currentParent = currentParent.getParentMenu();
        }
    }

    /**
     * Chuyển đổi menu cha thành DTO (status = 0 vì không có phân quyền trực tiếp)
     * 
     * @param role Role
     * @param menu Menu cha
     * @return DTO với status = 0
     */
    private DataPermissionItemDto toParentDto(Role role, Menu menu) {
        return DataPermissionItemDto.builder()
                .id(null)
                .roleId(role.getId())
                .roleCode(role.getCode())
                .roleName(role.getRoleName())
                .menuId(menu.getId())
                .parentId(menu.getParentMenu() == null ? null : menu.getParentMenu().getId())
                .menuCode(menu.getCode())
                .menuName(menu.getName())
                .menuUrl(menu.getUrl())
                .icon(menu.getIcon())
                .ordinal(menu.getOrdinal())
                .status(0)
                .scopes(List.of())
                .build();
    }

    /**
     * Áp dụng dữ liệu từ request vào entity DataPermission
     * 
     * @param dataPermission  Entity DataPermission
     * @param request         Request chứa dữ liệu phân quyền
     * @param currentUsername User hiện tại
     * @param isNew           True nếu là tạo mới, False nếu là cập nhật
     */
    private void applyRequest(DataPermission dataPermission, DataPermissionSaveRequest request, String currentUsername,
            boolean isNew) {
        Menu menu = menuRepository.findById(request.getMenuId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.MENU_NOT_FOUND));
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.ROLE_NOT_FOUND));

        dataPermission.setMenu(menu);
        dataPermission.setRole(role);
        dataPermission.setStatus(request.getStatus());
        if (isNew) {
            dataPermission.setCreatedBy(currentUsername);
        } else {
            dataPermission.setUpdatedBy(currentUsername);
        }

        dataPermission.getScopes().clear();
        for (DataPermissionScopeSaveRequest scopeRequest : request.getScopes()) {
            if (!isEnabled(scopeRequest.getStatus())) {
                continue;
            }
            DataPermissionScope scope = new DataPermissionScope();
            scope.setDataPermission(dataPermission);
            scope.setScopeType(normalizeScopeType(scopeRequest.getScopeType()));
            scope.setStatus(scopeRequest.getStatus());
            scope.setCreatedBy(currentUsername);
            dataPermission.getScopes().add(scope);
        }
    }

    /**
     * Validate danh sách scope: không trống, không trùng lặp
     * 
     * @param scopes Danh sách scope request
     * @throws UserMessageException Nếu validation thất bại
     */
    private void validateScopes(List<DataPermissionScopeSaveRequest> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw new UserMessageException("Danh sach scope khong duoc de trong");
        }
        Set<String> uniqueScopes = new HashSet<>();
        for (DataPermissionScopeSaveRequest scope : scopes) {
            String normalized = normalizeScopeType(scope.getScopeType());
            if (!uniqueScopes.add(normalized)) {
                throw new UserMessageException("Danh sach scope bi trung");
            }
        }
    }

    /**
     * Chuẩn hóa scope type: trim + uppercase
     * 
     * @param scopeType Scope type cần chuẩn hóa
     * @return Scope type sau khi chuẩn hóa
     */
    private String normalizeScopeType(String scopeType) {
        if (!StringUtils.hasText(scopeType)) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
        return scopeType.trim().toUpperCase();
    }

    /**
     * Kiểm tra xem status có được enable (= 1) không
     * 
     * @param value Giá trị status
     * @return True nếu status = 1
     */
    private boolean isEnabled(Integer value) {
        return value != null && value == 1;
    }

    /**
     * Validate batch request: không null, không trống
     * 
     * @param requests Danh sách request
     * @throws UserMessageException Nếu validation thất bại
     */
    private void validateBatchRequest(List<?> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new UserMessageException("Danh sach phan quyen du lieu khong duoc de trong");
        }
    }

    /**
     * Đảm bảo cặp (roleId, menuId) không trùng lặp trong batch
     * 
     * @param uniquePairs Set chứa các cặp unique
     * @param roleId      Role ID
     * @param menuId      Menu ID
     * @throws UserMessageException Nếu cặp đã tồn tại
     */
    private void ensureUniquePair(Set<String> uniquePairs, Long roleId, Long menuId) {
        String pairKey = roleId + "-" + menuId;
        if (!uniquePairs.add(pairKey)) {
            throw new UserMessageException("Danh sach phan quyen du lieu bi trung roleId va menuId");
        }
    }

    /**
     * Chuyển đổi DataPermission entity thành DTO
     * 
     * @param dataPermission Entity DataPermission
     * @return DTO chứa thông tin phân quyền + scope
     */
    private DataPermissionItemDto toDto(DataPermission dataPermission) {
        return DataPermissionItemDto.builder()
                .id(dataPermission.getId())
                .roleId(dataPermission.getRole().getId())
                .roleCode(dataPermission.getRole().getCode())
                .roleName(dataPermission.getRole().getRoleName())
                .menuId(dataPermission.getMenu().getId())
                .parentId(dataPermission.getMenu().getParentMenu() == null ? null
                        : dataPermission.getMenu().getParentMenu().getId())
                .menuCode(dataPermission.getMenu().getCode())
                .menuName(dataPermission.getMenu().getName())
                .menuUrl(dataPermission.getMenu().getUrl())
                .icon(dataPermission.getMenu().getIcon())
                .ordinal(dataPermission.getMenu().getOrdinal())
                .status(dataPermission.getStatus())
                .scopes(dataPermission.getScopes().stream()
                        .map(scope -> DataPermissionScopeItemDto.builder()
                                .id(scope.getId())
                                .scopeType(scope.getScopeType())
                                .status(scope.getStatus())
                                .build())
                        .toList())
                .build();
    }
}

