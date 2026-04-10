package com.gfi.backend.services.implement;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.menu.MenuCreateRequest;
import com.gfi.backend.models.dtos.menu.MenuDetailDto;
import com.gfi.backend.models.dtos.menu.MenuFilterDto;
import com.gfi.backend.models.dtos.menu.MenuListItemDto;
import com.gfi.backend.models.dtos.menu.MenuUpdateRequest;
import com.gfi.backend.models.entities.Menu;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.DataPermissionRepository;
import com.gfi.backend.repositories.MenuRepository;
import com.gfi.backend.repositories.PermissionRepository;
import com.gfi.backend.repositories.specifications.MenuSpecification;
import com.gfi.backend.services.interfaces.MenuService;
import com.gfi.backend.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * Triển khai service cho quản lý menu.
 * Xử lý các thao tác CRUD và nghiệp vụ cho thực thể menu.
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final PermissionRepository permissionRepository;
    private final DataPermissionRepository dataPermissionRepository;
    private final MenuSpecification menuSpecification;

    @Override
    public List<MenuListItemDto> search(MenuFilterDto filter) {
        MenuFilterDto safeFilter = filter == null ? new MenuFilterDto() : filter;
        return menuRepository.findAll(menuSpecification.buildSpecification(safeFilter), Sort.by(Sort.Direction.ASC, "ordinal", "name"))
                .stream()
                .map(this::toListDto)
                .toList();
    }

    @Override
    public List<LookupItemDto> getOptions() {
        return menuRepository.findAll(menuSpecification.buildActiveOnlySpecification(), Sort.by(Sort.Direction.ASC, "ordinal", "name"))
                .stream()
                .map(menu -> LookupItemDto.builder()
                        .id(menu.getId())
                        .name(menu.getName())
                        .build())
                .toList();
    }

    @Override
    public MenuDetailDto getById(Long id) {
        return toDetailDto(findMenu(id));
    }

    @Override
    @Transactional
    public MenuDetailDto create(MenuCreateRequest request) {
        // Normalize and validate code uniqueness
        String code = normalize(request.getCode());
        validateUniqueCode(code, null);

        // Create new menu entity and map request data
        Menu menu = new Menu();
        mapCreateRequestToEntity(request, menu);
        menu.setCreatedBy(SecurityUtils.getCurrentUsername());

        return toDetailDto(menuRepository.save(menu));
    }

    @Override
    @Transactional
    public MenuDetailDto update(Long id, MenuUpdateRequest request) {
        Menu menu = findMenu(id);

        // Kiểm tra mã mới có trùng với menu khác không
        String code = normalize(request.getCode());
        validateUniqueCode(code, id);

        // Cập nhật thông tin menu từ request
        mapUpdateRequestToEntity(request, menu);
        menu.setUpdatedBy(SecurityUtils.getCurrentUsername());

        return toDetailDto(menuRepository.save(menu));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Menu menu = findMenu(id);

        // Kiểm tra nếu menu đang được sử dụng trong permissions
        if (permissionRepository.countByMenuId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.MENU_IN_USE);
        }
        if (dataPermissionRepository.countByMenuId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.MENU_IN_USE);
        }

        // Kiểm tra nếu menu có menu con (chỉ lấy những menu chưa xóa)
        if (menuRepository.countActiveByParentMenuId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.MENU_HAS_CHILDREN);
        }

        // Xóa mềm: đánh dấu xóa thay vì hard delete
        menu.setDeletedFlag(1);
        menu.setDeletedAt(LocalDateTime.now());
        menu.setDeletedBy(SecurityUtils.getCurrentUsername());
        menuRepository.save(menu);
    }

    /**
     * Ánh xa dữ liệu từ MenuCreateRequest sang Menu entity.
     * Xử lý validate parent menu và normalize các trường dữ liệu.
     */
    private void mapCreateRequestToEntity(MenuCreateRequest request, Menu menu) {
        validateParent(null, request.getParentId());

        if (request.getParentId() != null) {
            menu.setParentMenu(findMenu(request.getParentId()));
        }

        menu.setCode(normalize(request.getCode()));
        menu.setName(normalize(request.getName()));
        menu.setUrl(normalizeNullable(request.getUrl()));
        menu.setIcon(normalizeNullable(request.getIcon()));
        menu.setOrdinal(request.getOrdinal() == null ? 0 : request.getOrdinal());
    }

    /**
     * Ánh xa dữ liệu từ MenuUpdateRequest sang Menu entity.
     * Xử lý validate parent menu và normalize các trường dữ liệu.
     */
    private void mapUpdateRequestToEntity(MenuUpdateRequest request, Menu menu) {
        validateParent(menu.getId(), request.getParentId());

        if (request.getParentId() != null) {
            menu.setParentMenu(findMenu(request.getParentId()));
        } else {
            menu.setParentMenu(null);
        }

        menu.setCode(normalize(request.getCode()));
        menu.setName(normalize(request.getName()));
        menu.setUrl(normalizeNullable(request.getUrl()));
        menu.setIcon(normalizeNullable(request.getIcon()));
        menu.setOrdinal(request.getOrdinal() == null ? 0 : request.getOrdinal());
    }

    /**
     * Kiểm tra tính duy nhất của mã menu.
     * Chỉ kiểm tra các menu chưa xóa (deletedFlag = 0).
     *
     * @param code      Mã cần kiểm tra
     * @param excludeId ID menu cần loại trừ khỏi kiểm tra
     */
    private void validateUniqueCode(String code, Long excludeId) {
        menuRepository.findByCodeAndDeletedFlagZero(code)
                .filter(found -> excludeId == null || !found.getId().equals(excludeId))
                .ifPresent(found -> {
                    throw new UserMessageException(CommonErrorCode.MENU_CODE_ALREADY_EXISTS);
                });
    }

    /**
     * Kiểm tra tính hợp lệ của menu cha.
     *
     * @param menuId   ID của menu hiện tại
     * @param parentId ID của menu cha được đề xuất
     */
    private void validateParent(Long menuId, Long parentId) {
        if (menuId != null && parentId != null && menuId.equals(parentId)) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
    }

    /**
     * Tìm menu theo ID, chỉ lấy menu chưa xóa (deletedFlag = 0).
     * Nếu không tìm thấy sẽ ném UserMessageException với lỗi MENU_NOT_FOUND.
     */
    private Menu findMenu(Long id) {
        return menuRepository.findById(id)
                .filter(menu -> menu.getDeletedFlag() == 0)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.MENU_NOT_FOUND));
    }

    /**
     * Chuyển đổi Menu entity sang MenuListItemDto (cho danh sách).
     * Chỉ chứa thông tin cơ bản cần thiết cho hiển thị trong danh sách
     */
    private MenuListItemDto toListDto(Menu menu) {
        return MenuListItemDto.builder()
                .id(menu.getId())
                .code(menu.getCode())
                .name(menu.getName())
                .parentCode(menu.getParentMenu() == null ? null : menu.getParentMenu().getCode())
                .ordinal(menu.getOrdinal())
                .icon(menu.getIcon())
                .url(menu.getUrl())
                .build();
    }

    /**
     * Chuyển đổi Menu entity sang MenuDetailDto (cho chi tiết).
     * Chỉ chứa thông tin chi tiết cần thiết cho hiển thị trong phần chi tiết menu
     */
    private MenuDetailDto toDetailDto(Menu menu) {
        return MenuDetailDto.builder()
                .id(menu.getId())
                .parentId(menu.getParentMenu() == null ? null : menu.getParentMenu().getId())
                .parentCode(menu.getParentMenu() == null ? null : menu.getParentMenu().getCode())
                .code(menu.getCode())
                .name(menu.getName())
                .url(menu.getUrl())
                .icon(menu.getIcon())
                .ordinal(menu.getOrdinal())
                .build();
    }

    /**
     * Kiểm tra xem chuỗi có chứa nội dung hay không
     * (khác null và không rỗng sau khi loại bỏ khoảng trắng).
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Chuẩn hóa chuỗi bằng cách loại bỏ khoảng trắng ở đầu và cuối.
     * Trả về null nếu giá trị đầu vào là null.
     */
    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Chuẩn hóa chuỗi có thể null bằng cách loại bỏ khoảng trắng ở đầu và cuối.
     * Trả về null nếu giá trị đầu vào null hoặc rỗng sau khi trim.
     */
    private String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
