package com.gfi.backend.services.interfaces;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionImportResultDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionItemDto;

public interface ProgramDistributionService {
    byte[] exportExcelTemplate(Long schoolYearId, Long semesterId, Long classroomId, Long subjectId);

    ProgramDistributionImportResultDto importExcel(Long schoolYearId, Long classroomId, Long subjectId,
            MultipartFile file);

    List<ProgramDistributionItemDto> findList(Long schoolYearId, Long semesterId, Long classroomId, Long subjectId);
}
