package com.lockerroom.resourceservice.infrastructure.exceptions;

import com.lockerroom.resourceservice.infrastructure.exceptions.FieldError;

import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;

import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;

import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.infrastructure.utils.MessageUtils;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        String message = MessageUtils.getMessage(errorCode.getCode());
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("CustomException: {} - {}", errorCode.getCode(), message);
        } else {
            log.warn("CustomException: {} - {}", errorCode.getCode(), message);
        }

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldError>>> handleValidationException(
            MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();

        log.warn("Validation failed: {}", fieldErrors);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(
                        ErrorCode.INVALID_INPUT.getCode(),
                        MessageUtils.getMessage(ErrorCode.INVALID_INPUT.getCode()),
                        fieldErrors));
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            MissingServletRequestParameterException.class,
            HttpRequestMethodNotSupportedException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        log.warn("Bad request: {}", e.getMessage());

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(
                        ErrorCode.BAD_REQUEST.getCode(),
                        MessageUtils.getMessage(ErrorCode.BAD_REQUEST.getCode())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);

        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        MessageUtils.getMessage(ErrorCode.INTERNAL_SERVER_ERROR.getCode())));
    }
}
