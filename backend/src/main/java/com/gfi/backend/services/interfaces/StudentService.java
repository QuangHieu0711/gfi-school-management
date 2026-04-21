package com.gfi.backend.services.interfaces;

import java.util.List;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.student.StudentCreateRequest;
import com.gfi.backend.models.dtos.student.StudentFilterDto;
import com.gfi.backend.models.dtos.student.StudentItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;

public interface StudentService {
    PageResponseDto<StudentItemDto, StudentFilterDto> search(PageRequestDto<StudentFilterDto> request);
    List<LookupItemDto> getStudentsByClassroom(Long classroomId);
    StudentItemDto create(StudentCreateRequest request);
    StudentItemDto getById(Long id);
    StudentItemDto update(Long id, StudentCreateRequest request);
    void delete(Long id);
}
