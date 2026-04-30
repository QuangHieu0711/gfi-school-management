package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.evaluation.EvaluationBulkUpsertRequest;
import com.gfi.backend.models.dtos.evaluation.EvaluationGenerateCommentRequest;
import com.gfi.backend.models.dtos.evaluation.EvaluationSheetDto;

public interface EvaluationService {
    EvaluationSheetDto getSheet(Long classroomId, Long subjectId, Long semesterId);
    void bulkUpsert(EvaluationBulkUpsertRequest request);
    String generateComment(EvaluationGenerateCommentRequest request);
}
