package com.gfi.backend.services.implement;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.menu.MenuCreateRequest;
import com.gfi.backend.models.dtos.menu.MenuFilterDto;
import com.gfi.backend.models.dtos.menu.MenuItemDto;
import com.gfi.backend.models.dtos.menu.MenuUpdateRequest;
import com.gfi.backend.models.entities.Menu;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.MenuRepository;
import com.gfi.backend.repositories.PermissionRepository;
import com.gfi.backend.services.interfaces.MenuService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public List<MenuItemDto> search(MenuFilterDto filter) {
        MenuFilterDto safeFilter = filter == null ? new MenuFilterDto() : filter;
        return menuRepository.findAll(buildSpecification(safeFilter), Sort.by(Sort.Direction.ASC, "ordinal", "name")).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<LookupItemDto> getOptions() {
        return menuRepository.findAll(Sort.by(Sort.Direction.ASC, "ordinal", "name")).stream()
                .map(menu -> LookupItemDto.builder()
                        .id(menu.getId())
                        .name(menu.getName())
                        .build())
                .toList();
    }

    @Override
    public MenuItemDto getById(Long id) {
        return toDto(findMenu(id));
    }

    @Override
    @Transactional
    public MenuItemDto create(MenuCreateRequest request) {
        String code = normalize(request.getCode());
        if (menuRepository.existsByCode(code)) {
            throw new UserMessageException(CommonErrorCode.MENU_CODE_ALREADY_EXISTS);
        }

        Menu menu = new Menu();
        applyRequest(menu, request.getParentId(), code, request.getName(), request.getUrl(), request.getIcon(), request.getOrdinal());
        menu.setCreatedBy(getCurrentUsername());
        return toDto(menuRepository.save(menu));
    }

    @Override
    @Transactional
    public MenuItemDto update(Long id, MenuUpdateRequest request) {
        Menu menu = findMenu(id);
        String code = normalize(request.getCode());
        menuRepository.findByCode(code)
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> {
                    throw new UserMessageException(CommonErrorCode.MENU_CODE_ALREADY_EXISTS);
                });

        applyRequest(menu, request.getParentId(), code, request.getName(), request.getUrl(), request.getIcon(), request.getOrdinal());
        menu.setUpdatedBy(getCurrentUsername());
        return toDto(menuRepository.save(menu));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Menu menu = findMenu(id);
        if (permissionRepository.countByMenuId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.MENU_IN_USE);
        }
        if (menuRepository.countByParentMenuId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.MENU_HAS_CHILDREN);
        }
        menuRepository.delete(menu);
    }

    private void applyRequest(Menu menu, Long parentId, String code, String name, String url, String icon, Integer ordinal) {
        if (parentId != null) {
            if (menu.getId() != null && menu.getId().equals(parentId)) {
                throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
            }
            menu.setParentMenu(findMenu(parentId));
        } else {
            menu.setParentMenu(null);
        }

        menu.setCode(code);
        menu.setName(normalize(name));
        menu.setUrl(normalizeNullable(url));
        menu.setIcon(normalizeNullable(icon));
        menu.setOrdinal(ordinal == null ? 0 : ordinal);
    }

    private Specification<Menu> buildSpecification(MenuFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(filter.getMenu())) {
                String keyword = "%" + filter.getMenu().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Menu findMenu(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.MENU_NOT_FOUND));
    }

    private MenuItemDto toDto(Menu menu) {
        return MenuItemDto.builder()
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
