package com.gfi.backend.services.interfaces;

import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionImportResultDto;

public interface ProgramDistributionService {
    byte[] exportExcelTemplate(Long schoolYearId, Long semesterId, Long classroomId, Long subjectId);

    ProgramDistributionImportResultDto importExcel(Long schoolYearId, Long semesterId, Long classroomId, Long subjectId,
            MultipartFile file);
}
