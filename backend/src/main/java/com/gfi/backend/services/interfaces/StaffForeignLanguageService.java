package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.staff.StaffForeignLanguageCreateRequest;
import com.gfi.backend.models.dtos.staff.StaffForeignLanguageDto;
import com.gfi.backend.models.dtos.staff.StaffForeignLanguageFilterDto;
import com.gfi.backend.models.dtos.staff.StaffForeignLanguageUpdateRequest;

public interface StaffForeignLanguageService {
    PageResponseDto<StaffForeignLanguageDto, StaffForeignLanguageFilterDto> search(
            PageRequestDto<StaffForeignLanguageFilterDto> request);

    StaffForeignLanguageDto getById(Long id);

    StaffForeignLanguageDto create(StaffForeignLanguageCreateRequest request);

    StaffForeignLanguageDto update(Long id, StaffForeignLanguageUpdateRequest request);

    void delete(Long id);
}
