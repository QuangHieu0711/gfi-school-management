package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.role.RoleCreateRequest;
import com.gfi.backend.models.dtos.role.RoleDetailDto;
import com.gfi.backend.models.dtos.role.RoleFilterDto;
import com.gfi.backend.models.dtos.role.RoleListItemDto;
import com.gfi.backend.models.dtos.role.RoleUpdateRequest;

/**
 * Service interface quản lý vai trò (Role).
 */
public interface RoleService {
    
    /**
     * Tìm kiếm danh sách vai trò với phân trang và lọc.
     * 
     * @param request yêu cầu chứa filter và phân trang
     * @return danh sách vai trò cơ bản (id, code, roleName, status)
     */
    PageResponseDto<RoleListItemDto, RoleFilterDto> search(PageRequestDto<RoleFilterDto> request);
    
    /**
     * Lấy danh sách vai trò cho dropdown/combobox.
     * 
     * @return danh sách id và tên vai trò
     */
    List<LookupItemDto> getOptions();
    
    /**
     * Lấy chi tiết vai trò theo id.
     * 
     * @param id ID của vai trò
     * @return thông tin chi tiết vai trò
     */
    RoleDetailDto getById(Long id);
    
    /**
     * Tạo vai trò mới.
     * 
     * @param request dữ liệu vai trò cần tạo
     * @return thông tin chi tiết vai trò vừa tạo
     */
    RoleDetailDto create(RoleCreateRequest request);
    
    /**
     * Cập nhật vai trò.
     * 
     * @param id ID của vai trò
     * @param request dữ liệu cần cập nhật
     * @return thông tin chi tiết vai trò sau cập nhật
     */
    RoleDetailDto update(Long id, RoleUpdateRequest request);
    
    /**
     * Xóa vai trò.
     * 
     * @param id ID của vai trò
     */
    void delete(Long id);
}
