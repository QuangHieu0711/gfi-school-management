package com.gfi.backend.controllers.exceptions;

public class UserMessageException extends RuntimeException{
    public UserMessageException(String message)
    {
        super(message);
    }
}

