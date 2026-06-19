package com.gfi.backend.services.interfaces;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.dtos.student.StudentCreateRequest;
import com.gfi.backend.models.dtos.student.StudentFilterDto;
import com.gfi.backend.models.dtos.student.StudentImportResultDto;
import com.gfi.backend.models.dtos.student.StudentItemDto;
import com.gfi.backend.models.dtos.student.StudentTransferClassRequest;
import com.gfi.backend.models.dtos.student.StudentTransferClassResultDto;
import com.gfi.backend.models.enums.ExportType;

public interface StudentService {
    PageResponseDto<StudentItemDto, StudentFilterDto> search(PageRequestDto<StudentFilterDto> request);

    byte[] export(PageRequestDto<StudentFilterDto> request, Long unitId, ExportType exportType);

    byte[] exportExcelTemplate(Long unitId);

    StudentImportResultDto importExcel(Long unitId, MultipartFile file);

    TemporaryFileDto getImportErrorFile(String token);

    List<LookupItemDto> getStudentsByClassroom(Long classroomId);

    StudentItemDto create(StudentCreateRequest request);

    StudentItemDto getById(Long id);

    StudentItemDto update(Long id, StudentCreateRequest request);

    StudentTransferClassResultDto transferClass(StudentTransferClassRequest request);

    void delete(Long id);
}
