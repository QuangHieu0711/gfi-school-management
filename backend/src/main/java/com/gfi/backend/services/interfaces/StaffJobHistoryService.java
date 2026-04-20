package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.staff.StaffJobHistoryCreateRequest;
import com.gfi.backend.models.dtos.staff.StaffJobHistoryDto;
import com.gfi.backend.models.dtos.staff.StaffJobHistoryFilterDto;
import com.gfi.backend.models.dtos.staff.StaffJobHistoryUpdateRequest;

public interface StaffJobHistoryService {
    PageResponseDto<StaffJobHistoryDto, StaffJobHistoryFilterDto> search(PageRequestDto<StaffJobHistoryFilterDto> request);
    StaffJobHistoryDto getById(Long id);
    StaffJobHistoryDto create(StaffJobHistoryCreateRequest request);
    StaffJobHistoryDto update(Long id, StaffJobHistoryUpdateRequest request);
    void delete(Long id);
}
