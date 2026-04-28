package com.lockerroom.resourceservice.infrastructure.exceptions;

import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
