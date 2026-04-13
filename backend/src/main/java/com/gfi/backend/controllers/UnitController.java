package com.gfi.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.controllers.annotations.DataScoped;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.unit.UnitCreateRequest;
import com.gfi.backend.models.dtos.unit.UnitDetailDto;
import com.gfi.backend.models.dtos.unit.UnitFilterDto;
import com.gfi.backend.models.dtos.unit.UnitListItemDto;
import com.gfi.backend.models.dtos.unit.UnitUpdateRequest;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.UnitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
@Tag(name = "Quản lý đơn vị - Unit")
public class UnitController extends ApiBaseController {

    private final UnitService unitService;

    /**
     * Danh sách đơn vị với phân trang và filter.
     *
     * @param request yêu cầu tìm kiếm chứa điều kiện lọc và phân trang
     * @return trang danh sách đơn vị cơ bản (id, code, name, status)
     */
    @PostMapping("/search")
    @DataScoped(feature = "UNIT_MANAGEMENT", action = ActionType.VIEW)
    @Operation(summary = "Danh sách đơn vị", description = "Lấy danh sách đơn vị có phân trang và filter.")
    public ResponseEntity<ApiResult<PageResponseDto<UnitListItemDto, UnitFilterDto>>> search(
            @RequestBody(required = false) PageRequestDto<UnitFilterDto> request) {
        PageRequestDto<UnitFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        return executeApiResult(() -> ApiResult.success(unitService.search(safeRequest), "Hiển thị danh sách đơn vị thành công"));
    }

    /**
     * Danh sách đơn vị cho dropdown/combobox.
     *
     * @return danh sách id và tên đơn vị
     */
    @GetMapping("/options")
    @DataScoped(feature = "UNIT_MANAGEMENT", action = ActionType.VIEW)
    @Operation(summary = "Danh sách đơn vị cho combobox", description = "Lấy danh sách id và tên đơn vị.")
    public ResponseEntity<ApiResult<List<LookupItemDto>>> getOptions() {
        return executeApiResult(() -> ApiResult.success(unitService.getOptions(), "Hiển thị danh sách đơn vị thành công"));
    }

    /**
     * Chi tiết đơn vị theo id.
     *
     * @param id ID của đơn vị
     * @return thông tin chi tiết đơn vị (tất cả trường)
     */
    @GetMapping("/{id}")
    @DataScoped(feature = "UNIT_MANAGEMENT", action = ActionType.VIEW, scopeExpression = "#id")
    @Operation(summary = "Chi tiết đơn vị", description = "Lấy thông tin đơn vị theo id.")
    public ResponseEntity<ApiResult<UnitDetailDto>> getById(@PathVariable Long id) {
        return executeApiResult(() -> ApiResult.success(unitService.getById(id), "Hiển thị chi tiết đơn vị thành công"));
    }

    /**
     * Tạo mới đơn vị.
     *
     * @param request dữ liệu đơn vị cần tạo
     * @return thông tin chi tiết đơn vị vừa tạo
     */
    @PostMapping
    @DataScoped(feature = "UNIT_MANAGEMENT", action = ActionType.ADD)
    @Operation(summary = "Thêm đơn vị", description = "Tạo mới đơn vị.")
    public ResponseEntity<ApiResult<UnitDetailDto>> create(@Valid @RequestBody UnitCreateRequest request) {
        return executeApiResult(() -> ApiResult.success(unitService.create(request), "Thêm đơn vị thành công"));
    }

    /**
     * Cập nhật đơn vị theo id.
     *
     * @param id ID của đơn vị
     * @param request dữ liệu đơn vị cần cập nhật
     * @return thông tin chi tiết đơn vị sau cập nhật
     */
    @PutMapping("/{id}")
    @DataScoped(feature = "UNIT_MANAGEMENT", action = ActionType.EDIT, scopeExpression = "#id")
    @Operation(summary = "Sửa đơn vị", description = "Cập nhật đơn vị theo id.")
    public ResponseEntity<ApiResult<UnitDetailDto>> update(@PathVariable Long id, @Valid @RequestBody UnitUpdateRequest request) {
        return executeApiResult(() -> ApiResult.success(unitService.update(id, request), "Cập nhật đơn vị thành công"));
    }

    /**
     * Xóa đơn vị theo id.
     *
     * @param id ID của đơn vị
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/{id}")
    @DataScoped(feature = "UNIT_MANAGEMENT", action = ActionType.DELETE, scopeExpression = "#id")
    @Operation(summary = "Xóa đơn vị", description = "Xóa đơn vị theo id.")
    public ResponseEntity<ApiResult<String>> delete(@PathVariable Long id) {
        return executeApiResult(() -> {
            unitService.delete(id);
            return ApiResult.success(null, "Xóa đơn vị thành công");
        });
    }
}
