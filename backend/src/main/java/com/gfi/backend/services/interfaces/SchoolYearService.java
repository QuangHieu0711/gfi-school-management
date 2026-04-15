package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearCreateRequest;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearFilterDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearItemDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearUpdateRequest;

public interface SchoolYearService {
    PageResponseDto<SchoolYearItemDto, SchoolYearFilterDto> search(PageRequestDto<SchoolYearFilterDto> request);
    List<LookupItemDto> getOptions();
    SchoolYearItemDto getById(Long id);
    LookupItemDto getCurrentSchoolYear();
    SchoolYearItemDto create(SchoolYearCreateRequest request);
    SchoolYearItemDto update(Long id, SchoolYearUpdateRequest request);
    void delete(Long id);
}
