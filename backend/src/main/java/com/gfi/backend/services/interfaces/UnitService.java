package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.unit.UnitCreateRequest;
import com.gfi.backend.models.dtos.unit.UnitFilterDto;
import com.gfi.backend.models.dtos.unit.UnitItemDto;
import com.gfi.backend.models.dtos.unit.UnitUpdateRequest;

public interface UnitService {
    PageResponseDto<UnitItemDto, UnitFilterDto> search(PageRequestDto<UnitFilterDto> request);
    List<LookupItemDto> getOptions();
    UnitItemDto getById(Long id);
    UnitItemDto create(UnitCreateRequest request);
    UnitItemDto update(Long id, UnitUpdateRequest request);
    void delete(Long id);
}
