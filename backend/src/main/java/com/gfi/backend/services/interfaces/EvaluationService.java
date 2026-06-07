package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.evaluation.EvaluationBulkUpsertRequest;
import com.gfi.backend.models.dtos.evaluation.EvaluationEditWindowDto;
import com.gfi.backend.models.dtos.evaluation.EvaluationEditWindowRequest;
import com.gfi.backend.models.dtos.evaluation.EvaluationGenerateCommentRequest;
import com.gfi.backend.models.dtos.evaluation.EvaluationSheetDto;

import com.gfi.backend.models.dtos.evaluation.EvaluationBulkGenerateCommentRequest;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.dtos.evaluation.EvaluationImportResultDto;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface EvaluationService {
    EvaluationSheetDto getSheet(Long classroomId, Long subjectId, Long semesterId);
    EvaluationEditWindowDto getEditWindow(Long semesterId);
    EvaluationEditWindowDto saveEditWindow(EvaluationEditWindowRequest request);
    void bulkUpsert(EvaluationBulkUpsertRequest request);
    String generateComment(EvaluationGenerateCommentRequest request);
    Map<Long, String> bulkGenerateComment(EvaluationBulkGenerateCommentRequest request);
    byte[] exportExcelTemplate(Long classroomId, Long subjectId, Long semesterId);
    EvaluationImportResultDto importExcel(MultipartFile file, Long classroomId, Long subjectId, Long semesterId);
    TemporaryFileDto getImportErrorFile(String token);
}
