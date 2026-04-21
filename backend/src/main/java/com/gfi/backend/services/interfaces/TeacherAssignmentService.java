package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.staff.TeacherAssignmentItemDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentFilterDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentCreateRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentDetailRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentDetailResponse;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;

import java.util.List;

public interface TeacherAssignmentService {
    PageResponseDto<TeacherAssignmentItemDto, TeacherAssignmentFilterDto> search(PageRequestDto<TeacherAssignmentFilterDto> request);
    TeacherAssignmentDetailResponse getDetail(TeacherAssignmentDetailRequest request);
    List<TeacherAssignmentItemDto> create(TeacherAssignmentCreateRequest request);
    List<TeacherAssignmentItemDto> update(TeacherAssignmentCreateRequest request);
    void delete(Long id);
}
