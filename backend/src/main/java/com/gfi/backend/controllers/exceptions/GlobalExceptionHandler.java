package com.gfi.backend.controllers.exceptions;

import java.nio.file.AccessDeniedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gfi.backend.models.global.ApiResult;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<String>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResult.fail("Bạn không có quyền truy cập tài nguyên này"));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResult<String>> handleSecurityException(SecurityException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResult.fail(ex.getMessage()));
    }

    @ExceptionHandler(UserMessageException.class)
    public ResponseEntity<ApiResult<String>> handleUserMessageException(UserMessageException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Object>> handleAllException(Exception ex) {
        ex.printStackTrace();
        ApiResult<Object> result = new ApiResult<>();
        result.setUserMessage("Lỗi hệ thống, vui lòng thử lại sau.");
        result.setInternalMessage(ex.getMessage());
        return ResponseEntity.internalServerError().body(result);
    }
}
