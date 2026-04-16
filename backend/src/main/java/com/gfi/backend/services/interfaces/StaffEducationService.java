package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.staff.StaffEducationCreateRequest;
import com.gfi.backend.models.dtos.staff.StaffEducationDto;
import com.gfi.backend.models.dtos.staff.StaffEducationFilterDto;
import com.gfi.backend.models.dtos.staff.StaffEducationUpdateRequest;

public interface StaffEducationService {
    PageResponseDto<StaffEducationDto, StaffEducationFilterDto> search(PageRequestDto<StaffEducationFilterDto> request);
    StaffEducationDto getById(Long id);
    StaffEducationDto create(StaffEducationCreateRequest request);
    StaffEducationDto update(Long id, StaffEducationUpdateRequest request);
    void delete(Long id);
}
