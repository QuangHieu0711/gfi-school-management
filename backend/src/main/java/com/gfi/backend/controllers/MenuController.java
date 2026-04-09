package com.gfi.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.menu.MenuCreateRequest;
import com.gfi.backend.models.dtos.menu.MenuFilterDto;
import com.gfi.backend.models.dtos.menu.MenuItemDto;
import com.gfi.backend.models.dtos.menu.MenuUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.MenuService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
@Tag(name = "Quan ly menu")
public class MenuController extends ApiBaseController {

    private final MenuService menuService;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sach menu", description = "Lay danh sach menu theo tu khoa, khong phan trang.")
    public ResponseEntity<ApiResult<List<MenuItemDto>>> search(@RequestBody(required = false) MenuFilterDto request) {
        MenuFilterDto safeRequest = request == null ? new MenuFilterDto() : request;
        return executeApiResult(() -> ApiResult.success(menuService.search(safeRequest), "Lay danh sach menu thanh cong"));
    }

    @GetMapping("/options")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sach menu cho combobox", description = "Lay danh sach menu de su dung trong combobox.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions() {
        return executeApiResult(() -> ApiResult.success(menuService.getOptions(), "Lay danh sach menu thanh cong"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chi tiet menu", description = "Lay chi tiet menu theo ID.")
    public ResponseEntity<ApiResult<MenuItemDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(menuService.getById(id), "Lay chi tiet menu thanh cong"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Them menu", description = "Them menu moi.")
    public ResponseEntity<ApiResult<MenuItemDto>> create(@Valid @RequestBody MenuCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(menuService.create(request), "Them menu thanh cong"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cap nhat menu", description = "Cap nhat thong tin menu.")
    public ResponseEntity<ApiResult<MenuItemDto>> update(@PathVariable Long id, @Valid @RequestBody MenuUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(menuService.update(id, request), "Cap nhat menu thanh cong"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xoa menu", description = "Xoa menu khoi he thong.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            menuService.delete(id);
            return ApiResult.success(null, "Xoa menu thanh cong");
        });
    }
}
