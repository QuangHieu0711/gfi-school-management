package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelCreateRequest;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelFilterDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelItemDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelUpdateRequest;

public interface GradeLevelService {
    PageResponseDto<GradeLevelItemDto, GradeLevelFilterDto> search(PageRequestDto<GradeLevelFilterDto> request);
    List<LookupItemDto> getOptions();
    GradeLevelItemDto getById(Long id);
    GradeLevelItemDto create(GradeLevelCreateRequest request);
    GradeLevelItemDto update(Long id, GradeLevelUpdateRequest request);
    void delete(Long id);
}
