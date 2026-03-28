package com.gfi.backend.controllers.exceptions;

import com.gfi.backend.models.global.CommonErrorCode;

import lombok.Getter;

@Getter
public class UserMessageException extends RuntimeException {
    private final int code;

    public UserMessageException(String message) {
        super(message);
        this.code = CommonErrorCode.BAD_REQUEST.getCode();
    }

    public UserMessageException(int code, String message) {
        super(message);
        this.code = code;
    }

    public UserMessageException(CommonErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
