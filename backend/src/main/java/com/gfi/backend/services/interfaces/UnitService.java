package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.unit.UnitCreateRequest;
import com.gfi.backend.models.dtos.unit.UnitDetailDto;
import com.gfi.backend.models.dtos.unit.UnitFilterDto;
import com.gfi.backend.models.dtos.unit.UnitImportResultDto;
import com.gfi.backend.models.dtos.unit.UnitListItemDto;
import com.gfi.backend.models.dtos.unit.UnitUpdateRequest;
import com.gfi.backend.models.enums.ExportType;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface service quản lý đơn vị.
 * Định nghĩa contract cho các phép CRUD và business logic.
 */
public interface UnitService {
    /**
     * Tìm kiếm và phân trang units với filter.
     * Trả về DTO list view chứa thông tin tối thiểu.
     */
    PageResponseDto<UnitListItemDto, UnitFilterDto> search(PageRequestDto<UnitFilterDto> request);
    byte[] export(PageRequestDto<UnitFilterDto> request, ExportType exportType);
    UnitImportResultDto importExcel(MultipartFile file);
    
    /**
     * Lấy danh sách units để dropdown/select.
     */
    List<LookupItemDto> getOptions();
    
    /**
     * Lấy chi tiết unit theo ID.
     * Trả về DTO detail view chứa đầy đủ thông tin.
     */
    UnitDetailDto getById(Long id);
    
    /**
     * Tạo unit mới.
     * Trả về DTO detail view của unit vừa tạo.
     */
    UnitDetailDto create(UnitCreateRequest request);
    
    /**
     * Cập nhật unit hiện có.
     * Trả về DTO detail view của unit vừa update.
     */
    UnitDetailDto update(Long id, UnitUpdateRequest request);
    
    /**
     * Xóa unit theo ID.
     */
    void delete(Long id);
}
