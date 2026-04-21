package com.gfi.backend.controllers.exceptions;

import java.nio.file.AccessDeniedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gfi.backend.models.global.ApiResult;
import com.gfi.backend.models.global.CommonErrorCode;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<String>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResult.fail(CommonErrorCode.ACCESS_DENIED.getCode(), CommonErrorCode.ACCESS_DENIED.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResult<String>> handleSecurityException(SecurityException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResult.fail(CommonErrorCode.ACCESS_DENIED.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(UserMessageException.class)
    public ResponseEntity<ApiResult<String>> handleUserMessageException(UserMessageException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        FieldError firstError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = firstError != null ? firstError.getDefaultMessage() : CommonErrorCode.BAD_REQUEST.getMessage();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(CommonErrorCode.BAD_REQUEST.getCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Object>> handleAllException(Exception ex) {
        ex.printStackTrace();
        ApiResult<Object> result = new ApiResult<>();
        result.setCode(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode());
        result.setUserMessage(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        result.setInternalMessage(ex.getMessage());
        return ResponseEntity.internalServerError().body(result);
    }
}
