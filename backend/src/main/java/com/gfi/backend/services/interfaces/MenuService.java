package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.menu.MenuCreateRequest;
import com.gfi.backend.models.dtos.menu.MenuFilterDto;
import com.gfi.backend.models.dtos.menu.MenuItemDto;
import com.gfi.backend.models.dtos.menu.MenuUpdateRequest;

public interface MenuService {
    List<MenuItemDto> search(MenuFilterDto filter);
    List<LookupItemDto> getOptions();
    MenuItemDto getById(Long id);
    MenuItemDto create(MenuCreateRequest request);
    MenuItemDto update(Long id, MenuUpdateRequest request);
    void delete(Long id);
}
