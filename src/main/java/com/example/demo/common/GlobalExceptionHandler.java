package com.example.demo.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .headers(ex.getHeaders())
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ApiError(Instant.now(), HttpStatus.METHOD_NOT_ALLOWED.value(),
                "METHOD_NOT_ALLOWED", "Phương thức " + ex.getMethod() + " không được hỗ trợ cho đường dẫn này",
                request.getRequestURI(), Map.of()));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
        return response(ex.getStatus(), ex.getCode(), ex.getMessage(), request, Map.of());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(error ->
            fields.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu không hợp lệ", request, fields);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class, ConstraintViolationException.class,
        HandlerMethodValidationException.class})
    ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
            "JSON hoặc tham số không hợp lệ", request, Map.of());
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "DATA_CONFLICT",
            "Dữ liệu bị trùng hoặc đang được bản ghi khác tham chiếu", request, Map.of());
    }
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ApiError> handleNotFound(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "Đường dẫn không tồn tại", request, Map.of());
    }
    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message,
            HttpServletRequest request, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(),
            code, message, request.getRequestURI(), fields));
    }
}
