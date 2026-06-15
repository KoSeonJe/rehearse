package com.rehearse.api.global.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.global.common.ErrorResponse;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        log.warn("Validation failed: uri={}", request.getRequestURI());

        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.FieldError(
                        error.getField(),
                        error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                        error.getDefaultMessage()))
                .toList();

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "입력값이 올바르지 않습니다.",
                fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpServletRequest request) {

        if (isCsSubTopicEnumViolation(e)) {
            log.warn("CS 세부 주제 enum 위반: uri={}, message={}",
                    request.getRequestURI(), e.getMostSpecificCause().getMessage());
            ErrorResponse response = ErrorResponse.of(
                    InterviewErrorCode.CS_SUB_TOPIC_INVALID.getStatus().value(),
                    InterviewErrorCode.CS_SUB_TOPIC_INVALID.getCode(),
                    InterviewErrorCode.CS_SUB_TOPIC_INVALID.getMessage());
            return ResponseEntity.status(InterviewErrorCode.CS_SUB_TOPIC_INVALID.getStatus()).body(response);
        }

        log.warn("요청 본문 파싱 실패: uri={}", request.getRequestURI());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "MESSAGE_NOT_READABLE",
                "요청 본문을 해석할 수 없습니다.");

        return ResponseEntity.badRequest().body(response);
    }

    private boolean isCsSubTopicEnumViolation(HttpMessageNotReadableException e) {
        Throwable cause = e.getMostSpecificCause();
        if (!(cause instanceof InvalidFormatException ife)) {
            return false;
        }
        return ife.getPath().stream()
                .anyMatch(ref -> "csSubTopics".equals(ref.getFieldName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {

        log.warn("Type mismatch: uri={}, parameter={}", request.getRequestURI(), e.getName());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "TYPE_MISMATCH",
                "요청 파라미터 타입이 올바르지 않습니다.");

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    protected ResponseEntity<ErrorResponse> handleRateLimited(
            RequestNotPermitted e, HttpServletRequest request) {

        log.warn("Rate limit exceeded: uri={}", request.getRequestURI());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "RATE_LIMITED",
                "현재 요청이 많습니다. 잠시 후 다시 시도해주세요.");

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    protected ResponseEntity<ErrorResponse> handleAuthorizationDenied(
            AuthorizationDeniedException e, HttpServletRequest request) {

        log.warn("Authorization denied: uri={}", request.getRequestURI());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "AUTH_003",
                "권한이 없습니다.");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    private static final String SERVER_ERROR_GENERIC_MESSAGE = "서버 내부 오류가 발생했습니다.";

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e, HttpServletRequest request) {

        boolean is5xx = e.getStatus().is5xxServerError();
        if (is5xx) {
            log.error("Business exception 5xx: uri={}, code={}, message={}",
                    request.getRequestURI(), e.getCode(), e.getMessage(), e);
        } else {
            log.warn("Business exception: uri={}, code={}, message={}",
                    request.getRequestURI(), e.getCode(), e.getMessage());
        }

        String clientMessage = is5xx ? SERVER_ERROR_GENERIC_MESSAGE : e.getMessage();
        ErrorResponse response = ErrorResponse.of(
                e.getStatus().value(),
                e.getCode(),
                clientMessage);

        return ResponseEntity.status(e.getStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(
            Exception e, HttpServletRequest request) {

        log.error("Unhandled exception: uri={}", request.getRequestURI(), e);

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다.");

        return ResponseEntity.internalServerError().body(response);
    }
}
