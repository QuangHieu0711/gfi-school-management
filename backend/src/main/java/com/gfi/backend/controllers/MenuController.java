package com.gfi.backend.controllers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.menu.MenuCreateRequest;
import com.gfi.backend.models.dtos.menu.MenuDetailDto;
import com.gfi.backend.models.dtos.menu.MenuFilterDto;
import com.gfi.backend.models.dtos.menu.MenuListItemDto;
import com.gfi.backend.models.dtos.menu.MenuUpdateRequest;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.MenuService;
import com.gfi.backend.controllers.annotations.DataScoped;
import com.gfi.backend.models.enums.ActionType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
@Tag(name = "Quản lý menu - Menu")
public class MenuController extends ApiBaseController {

    private final MenuService menuService;

    /**
     * Danh sách menu theo từ khóa tìm kiếm.
     *
     * @param request điều kiện lọc menu (tìm kiếm theo mã hoặc tên)
     * @return danh sách menu khớp với điều kiện
     */
    @PostMapping("/search")
    @Operation(summary = "Danh sách menu", description = "Lấy danh sách menu theo từ khóa, không phân trang.")
    public ResponseEntity<ApiResult<List<MenuListItemDto>>> search(@RequestBody(required = false) MenuFilterDto request) {
        MenuFilterDto safeRequest = request == null ? new MenuFilterDto() : request;
        return executeApiResult(() -> ApiResult.success(menuService.search(safeRequest), "Lấy danh sách menu thành công"));
    }

    /**
     * Danh sách menu cho dropdown/combobox.
     *
     * @return danh sách id và tên menu
     */
    @GetMapping("/options")
    @Operation(summary = "Danh sách menu cho combobox", description = "Lấy danh sách menu để sử dụng trong combobox.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions() {
        return executeApiResult(() -> ApiResult.success(menuService.getOptions(), "Lấy danh sách menu thành công"));
    }

    /**
     * Chi tiết menu theo ID.
     *
     * @param id ID của menu
     * @return thông tin chi tiết menu
     */
    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết menu", description = "Lấy chi tiết menu theo ID.")
    public ResponseEntity<ApiResult<MenuDetailDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(menuService.getById(id), "Lấy chi tiết menu thành công"));
    }

    /**
     * Tạo menu mới.
     *
     * @param request dữ liệu menu cần tạo
     * @return thông tin chi tiết menu vừa tạo
     */
    @PostMapping
    @Operation(summary = "Thêm menu", description = "Thêm menu mới.")
    public ResponseEntity<ApiResult<MenuDetailDto>> create(@Valid @RequestBody MenuCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(menuService.create(request), "Thêm menu thành công"));
    }

    /**
     * Cập nhật menu theo ID.
     *
     * @param id ID của menu
     * @param request dữ liệu cần cập nhật
     * @return thông tin chi tiết menu sau cập nhật
     */
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật menu", description = "Cập nhật thông tin menu.")
    public ResponseEntity<ApiResult<MenuDetailDto>> update(@PathVariable Long id, @Valid @RequestBody MenuUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(menuService.update(id, request), "Cập nhật menu thành công"));
    }

    /**
     * Xóa menu theo ID.
     *
     * @param id ID của menu
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa menu", description = "Xóa menu khỏi hệ thống.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            menuService.delete(id);
            return ApiResult.success(null, "Xóa menu thành công");
        });
    }

    @PostMapping("/export")
    @DataScoped(feature = "FUNCTION_MANAGEMENT", action = ActionType.DOWNLOAD)
    @Operation(summary = "Xuất danh sách menu", description = "Xuất danh sách menu theo điều kiện tìm kiếm, hỗ trợ EXCEL hoặc PDF.")
    public ResponseEntity<byte[]> export(
            @RequestBody(required = false) MenuFilterDto request,
            @RequestParam(defaultValue = "EXCEL") ExportType exportType) {
        MenuFilterDto safeRequest = request == null ? new MenuFilterDto() : request;
        byte[] content = menuService.export(safeRequest, exportType);
        String extension = exportType == ExportType.PDF ? "pdf" : "xlsx";
        String contentType = exportType == ExportType.PDF
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String fileName = "danh-sach-menu." + extension;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }
}
