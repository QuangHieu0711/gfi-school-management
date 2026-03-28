package com.gfi.backend.controllers;

import java.util.function.Supplier;

import org.springframework.http.ResponseEntity;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.models.global.CommonErrorCode;

public abstract class ApiBaseController {

    protected <T> ResponseEntity<ApiResult<T>> executeApiResult(Supplier<ApiResult<T>> supplier) {
        try {
            ApiResult<T> result = supplier.get();
            return ResponseEntity.ok(result);
        } catch (UserMessageException ex) {
            return ResponseEntity.badRequest().body(ApiResult.fail(ex.getCode(), ex.getMessage()));
        } catch (Exception ex) {
            ApiResult<T> error = new ApiResult<>();
            error.setStatus(false);
            error.setCode(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode());
            error.setUserMessage(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage());
            error.setInternalMessage(ex.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
