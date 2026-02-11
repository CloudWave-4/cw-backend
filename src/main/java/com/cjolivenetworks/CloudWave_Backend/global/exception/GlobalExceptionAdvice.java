package com.cjolivenetworks.CloudWave_Backend.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(annotations = {RestController.class})
public class GlobalExceptionAdvice {

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ErrorResponse> handleCommonException(GlobalException e) {
        return new ResponseEntity<>(
                new ErrorResponse(e.getGlobalErrorCode(), e.getMessage()),
                e.getGlobalErrorCode().getHttpStatus());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(GlobalErrorCode.BAD_REQUEST, e.getMessage()));
    }

    // MissingServletRequestParameterException: Missing required parameters
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(GlobalErrorCode.BAD_REQUEST, e.getMessage()));
    }

    // Handle all exceptions: Default 500 server error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(GlobalErrorCode.INTERNAL_SERVER_ERROR, e.getMessage()));
    }

    //post, get
}