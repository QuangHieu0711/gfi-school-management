package com.gfi.backend.services.interfaces;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.dtos.staff.StaffCreateRequest;
import com.gfi.backend.models.dtos.staff.StaffDetailDto;
import com.gfi.backend.models.dtos.staff.StaffFilterDto;
import com.gfi.backend.models.dtos.staff.StaffGradeItemDto;
import com.gfi.backend.models.dtos.staff.StaffImportResultDto;
import com.gfi.backend.models.dtos.staff.StaffItemDto;
import com.gfi.backend.models.dtos.staff.StaffUpdateRequest;
import com.gfi.backend.models.enums.ExportType;

public interface StaffService {
    PageResponseDto<StaffItemDto, StaffFilterDto> search(PageRequestDto<StaffFilterDto> request);

    byte[] export(PageRequestDto<StaffFilterDto> request, Long unitId, ExportType exportType);

    byte[] exportExcelTemplate(Long unitId);

    StaffImportResultDto importExcel(Long unitId, MultipartFile file);

    TemporaryFileDto getImportErrorFile(String token);

    StaffDetailDto getById(Long id);

    StaffDetailDto create(StaffCreateRequest request);

    StaffDetailDto update(Long id, StaffUpdateRequest request);

    void delete(Long id);

    List<StaffGradeItemDto> getByGrade(Long gradeLevelId, Long unitId);
}
