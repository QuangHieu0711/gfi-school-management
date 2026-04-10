package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelCreateRequest;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelFilterDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelDetailDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelListItemDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelUpdateRequest;

public interface GradeLevelService {
    PageResponseDto<GradeLevelListItemDto, GradeLevelFilterDto> search(PageRequestDto<GradeLevelFilterDto> request);
    List<LookupItemDto> getOptions();
    GradeLevelDetailDto getById(Long id);
    GradeLevelDetailDto create(GradeLevelCreateRequest request);
    GradeLevelDetailDto update(Long id, GradeLevelUpdateRequest request);
    void delete(Long id);
}
