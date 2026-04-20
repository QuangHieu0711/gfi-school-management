package com.gfi.backend.services.interfaces;

import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionCreateRequest;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionDetailDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionFilterDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionImportResultDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionItemDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionUpdateRequest;

public interface ProgramDistributionService {
    byte[] exportExcelTemplate(Long schoolYearId, Long unitId, Long classroomId, Long subjectId);

    ProgramDistributionImportResultDto importExcel(Long schoolYearId, Long unitId, Long classroomId, Long subjectId,
            MultipartFile file);

    PageResponseDto<ProgramDistributionItemDto, ProgramDistributionFilterDto> search(
            PageRequestDto<ProgramDistributionFilterDto> request);

    ProgramDistributionDetailDto create(ProgramDistributionCreateRequest request);

    ProgramDistributionDetailDto getById(Long id);

    ProgramDistributionDetailDto update(Long id, ProgramDistributionUpdateRequest request);

    void delete(Long id);
}
