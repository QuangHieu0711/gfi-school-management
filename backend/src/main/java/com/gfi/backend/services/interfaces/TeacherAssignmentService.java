package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.staff.TeacherAssignmentItemDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentFilterDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentCreateRequest;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;

public interface TeacherAssignmentService {
    PageResponseDto<TeacherAssignmentItemDto, TeacherAssignmentFilterDto> search(PageRequestDto<TeacherAssignmentFilterDto> request);
    TeacherAssignmentItemDto getById(Long id);
    TeacherAssignmentItemDto create(TeacherAssignmentCreateRequest request);
    TeacherAssignmentItemDto update(Long id, TeacherAssignmentCreateRequest request);
    void delete(Long id);
}
