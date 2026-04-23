package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.models.dtos.evaluation.EvaluationBulkUpsertRequest;
import com.gfi.backend.models.dtos.evaluation.EvaluationSheetDto;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.EvaluationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
@Tag(name = "Danh gia - Evaluation")
public class EvaluationController extends ApiBaseController {

    private final EvaluationService evaluationService;

    @GetMapping("/sheet")
    @Operation(summary = "Bang danh gia theo lop/mon/hoc ky")
    public ResponseEntity<ApiResult<EvaluationSheetDto>> getSheet(
            @RequestParam Long classroomId,
            @RequestParam Long subjectId,
            @RequestParam Long semesterId) {
        return executeApiResult(() -> ApiResult.success(
                evaluationService.getSheet(classroomId, subjectId, semesterId),
                "Lay bang danh gia thanh cong"));
    }

    @PutMapping("/bulk")
    @Operation(summary = "Luu danh gia hang loat")
    public ResponseEntity<ApiResult<String>> bulkUpsert(@Valid @RequestBody EvaluationBulkUpsertRequest request) {
        return executeApiResult(() -> {
            evaluationService.bulkUpsert(request);
            return ApiResult.success(null, "Luu danh gia thanh cong");
        });
    }
}
