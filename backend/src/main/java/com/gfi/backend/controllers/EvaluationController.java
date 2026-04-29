package com.gfi.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gfi.backend.controllers.annotations.DataScoped;
import com.gfi.backend.models.dtos.evaluation.EvaluationBulkUpsertRequest;
import com.gfi.backend.models.dtos.evaluation.EvaluationSheetDto;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.services.interfaces.EvaluationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
@Tag(name = "Đánh giá - Evaluation")
public class EvaluationController extends ApiBaseController {

    private final EvaluationService evaluationService;

    @GetMapping("/sheet")
    @DataScoped(feature = "STUDENT_EVALUATION_BOOK", action = ActionType.VIEW)
    @Operation(summary = "Bảng đánh giá theo lớp/môn/học kỳ")
    public ResponseEntity<ApiResult<EvaluationSheetDto>> getSheet(
            @RequestParam Long classroomId,
            @RequestParam Long subjectId,
            @RequestParam Long semesterId) {
        return executeApiResult(() -> ApiResult.success(
                evaluationService.getSheet(classroomId, subjectId, semesterId),
                "Lấy bảng đánh giá thành công"));
    }

    @PutMapping("/bulk")
    @DataScoped(feature = "STUDENT_EVALUATION_BOOK", action = ActionType.EDIT)
    @Operation(summary = "Lưu đánh giá hàng loạt")
    public ResponseEntity<ApiResult<String>> bulkUpsert(@Valid @RequestBody EvaluationBulkUpsertRequest request) {
        return executeApiResult(() -> {
            evaluationService.bulkUpsert(request);
            return ApiResult.success(null, "Lưu đánh giá thành công");
        });
    }
}
