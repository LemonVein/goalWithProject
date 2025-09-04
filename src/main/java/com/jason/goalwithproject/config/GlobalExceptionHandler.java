package com.jason.goalwithproject.config;

import com.jason.goalwithproject.dto.common.ErrorDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // EntityNotFoundException 를 처리하는 핸들러
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorDto> handleEntityNotFoundException(EntityNotFoundException ex) {
        ErrorDto errorResponse = new ErrorDto(ex.getMessage(), HttpStatus.NOT_FOUND.value());
        // 404 Not Found 상태 코드와 함께 에러 응답을 반환
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // AccessDeniedException 를 처리하는 핸들러
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorDto errorResponse = new ErrorDto(ex.getMessage(), HttpStatus.FORBIDDEN.value());
        // 403 Forbidden 상태 코드와 함께 에러 응답을 반환
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // IllegalArgumentException 를 처리하는 핸들러
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorDto errorResponse = new ErrorDto(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        // 400 Bad Request 상태 코드와 함께 에러 응답을 반환
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // 그 외 예외를 처리하는 핸들러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleException(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        ErrorDto errorResponse = new ErrorDto("서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value());
        // 500 Internal Server Error 상태 코드와 함께 에러 응답을 반환
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
