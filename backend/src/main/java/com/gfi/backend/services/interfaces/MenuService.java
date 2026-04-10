package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.menu.MenuCreateRequest;
import com.gfi.backend.models.dtos.menu.MenuDetailDto;
import com.gfi.backend.models.dtos.menu.MenuFilterDto;
import com.gfi.backend.models.dtos.menu.MenuListItemDto;
import com.gfi.backend.models.dtos.menu.MenuUpdateRequest;

public interface MenuService {
    List<MenuListItemDto> search(MenuFilterDto filter);
    List<LookupItemDto> getOptions();
    MenuDetailDto getById(Long id);
    MenuDetailDto create(MenuCreateRequest request);
    MenuDetailDto update(Long id, MenuUpdateRequest request);
    void delete(Long id);
}
