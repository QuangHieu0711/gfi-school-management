package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.staff.*;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import java.util.List;

public interface StaffService {
    PageResponseDto<StaffItemDto, StaffFilterDto> search(PageRequestDto<StaffFilterDto> request);
    StaffDetailDto getById(Long id);
    StaffDetailDto create(StaffCreateRequest request);
    StaffDetailDto update(Long id, StaffUpdateRequest request);
    void delete(Long id);
    List<StaffGradeItemDto> getByGrade(Long gradeLevelId, Long unitId);
}
