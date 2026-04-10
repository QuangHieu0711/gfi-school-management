package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.classroom.ClassroomCreateRequest;
import com.gfi.backend.models.dtos.classroom.ClassroomDetailDto;
import com.gfi.backend.models.dtos.classroom.ClassroomFilterDto;
import com.gfi.backend.models.dtos.classroom.ClassroomListItemDto;
import com.gfi.backend.models.dtos.classroom.ClassroomUpdateRequest;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;

public interface ClassroomService {
    PageResponseDto<ClassroomListItemDto, ClassroomFilterDto> search(PageRequestDto<ClassroomFilterDto> request);
    List<LookupItemDto> getOptions(Long unitId, Long gradeLevelId, Long schoolYearId);
    ClassroomDetailDto getById(Long id);
    ClassroomDetailDto create(ClassroomCreateRequest request);
    ClassroomDetailDto update(Long id, ClassroomUpdateRequest request);
    void delete(Long id);
}
