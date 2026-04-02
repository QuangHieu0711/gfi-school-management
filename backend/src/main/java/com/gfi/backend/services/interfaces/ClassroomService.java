package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.classroom.ClassroomCreateRequest;
import com.gfi.backend.models.dtos.classroom.ClassroomFilterDto;
import com.gfi.backend.models.dtos.classroom.ClassroomItemDto;
import com.gfi.backend.models.dtos.classroom.ClassroomUpdateRequest;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;

public interface ClassroomService {
    PageResponseDto<ClassroomItemDto, ClassroomFilterDto> search(PageRequestDto<ClassroomFilterDto> request);
    List<LookupItemDto> getOptions(Long unitId, Long gradeLevelId, Long schoolYearId);
    ClassroomItemDto getById(Long id);
    ClassroomItemDto create(ClassroomCreateRequest request);
    ClassroomItemDto update(Long id, ClassroomUpdateRequest request);
    void delete(Long id);
}
