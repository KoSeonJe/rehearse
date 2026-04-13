package com.rehearse.api.global.exception;

import com.rehearse.api.global.common.ErrorResponse;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        given(request.getRequestURI()).willReturn("/api/test");
    }

    @Nested
    @DisplayName("HandleMethodArgumentNotValid")
    class HandleMethodArgumentNotValid {

        @Mock
        private MethodArgumentNotValidException exception;

        @Mock
        private BindingResult bindingResult;

        @Test
        @DisplayName("단일 필드 에러 시 400과 VALIDATION_ERROR 코드를 반환한다")
        void singleFieldError_returns400() {
            // given
            FieldError fieldError = new FieldError("dto", "name", "홍길동", false, null, null, "이름은 필수입니다.");
            given(exception.getBindingResult()).willReturn(bindingResult);
            given(bindingResult.getFieldErrors()).willReturn(List.of(fieldError));

            // when
            ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValid(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getBody().getErrors()).hasSize(1);
            assertThat(response.getBody().getErrors().get(0).getField()).isEqualTo("name");
            assertThat(response.getBody().getErrors().get(0).getValue()).isEqualTo("홍길동");
            assertThat(response.getBody().getErrors().get(0).getReason()).isEqualTo("이름은 필수입니다.");
        }

        @Test
        @DisplayName("복수 필드 에러 시 모든 에러가 응답에 포함된다")
        void multipleFieldErrors_allIncluded() {
            // given
            FieldError error1 = new FieldError("dto", "name", "이름은 필수입니다.");
            FieldError error2 = new FieldError("dto", "email", "이메일 형식이 올바르지 않습니다.");
            given(exception.getBindingResult()).willReturn(bindingResult);
            given(bindingResult.getFieldErrors()).willReturn(List.of(error1, error2));

            // when
            ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValid(exception, request);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrors()).hasSize(2);
        }

        @Test
        @DisplayName("rejectedValue가 null인 경우 빈 문자열로 처리된다")
        void nullRejectedValue_treatedAsEmptyString() {
            // given
            FieldError fieldError = new FieldError("dto", "name", null, false, null, null, "필수 값입니다.");
            given(exception.getBindingResult()).willReturn(bindingResult);
            given(bindingResult.getFieldErrors()).willReturn(List.of(fieldError));

            // when
            ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValid(exception, request);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrors().get(0).getValue()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("HandleTypeMismatch")
    class HandleTypeMismatch {

        @Test
        @DisplayName("타입 불일치 시 400과 TYPE_MISMATCH 코드를 반환한다")
        void typeMismatch_returns400() {
            // given
            MethodArgumentTypeMismatchException exception =
                    new MethodArgumentTypeMismatchException("abc", Integer.class, "id", null, null);

            // when
            ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentTypeMismatch(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getCode()).isEqualTo("TYPE_MISMATCH");
            assertThat(response.getBody().getMessage()).isEqualTo("요청 파라미터 타입이 올바르지 않습니다.");
        }
    }

    @Nested
    @DisplayName("HandleRateLimited")
    class HandleRateLimited {

        @Test
        @DisplayName("RequestNotPermitted 예외 시 429와 RATE_LIMITED 코드를 반환한다")
        void requestNotPermitted_returns429() {
            // given
            RequestNotPermitted exception = RequestNotPermitted.createRequestNotPermitted(
                    io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("test"));

            // when
            ResponseEntity<ErrorResponse> response = handler.handleRateLimited(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(429);
            assertThat(response.getBody().getCode()).isEqualTo("RATE_LIMITED");
            assertThat(response.getBody().getMessage()).isEqualTo("현재 요청이 많습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    @Nested
    @DisplayName("HandleBusinessException")
    class HandleBusinessException {

        @Test
        @DisplayName("4xx BusinessException은 해당 상태 코드를 그대로 반환한다")
        void businessException_4xx_returnsCorrectStatus() {
            // given
            BusinessException exception = new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "리소스를 찾을 수 없습니다.");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("5xx BusinessException은 해당 상태 코드를 그대로 반환한다")
        void businessException_5xx_returnsCorrectStatus() {
            // given
            BusinessException exception = new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "서비스 불가");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(503);
        }

        @Test
        @DisplayName("BusinessException의 code가 응답에 그대로 포함된다")
        void businessException_errorCodeIncluded() {
            // given
            BusinessException exception = new BusinessException(HttpStatus.BAD_REQUEST, "CUSTOM_CODE", "커스텀 오류");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception, request);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("CUSTOM_CODE");
        }

        @Test
        @DisplayName("BusinessException의 message가 응답에 그대로 포함된다")
        void businessException_messageIncluded() {
            // given
            BusinessException exception = new BusinessException(HttpStatus.BAD_REQUEST, "ERR", "사용자 정의 메시지");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception, request);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("사용자 정의 메시지");
        }
    }

    @Nested
    @DisplayName("HandleGenericException")
    class HandleGenericException {

        @Test
        @DisplayName("처리되지 않은 예외는 500을 반환한다")
        void unhandledException_returns500() {
            // given
            Exception exception = new RuntimeException("내부 상세 오류");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleException(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        }

        @Test
        @DisplayName("처리되지 않은 예외의 내부 메시지는 클라이언트에 노출되지 않는다")
        void unhandledException_internalMessageHidden() {
            // given
            Exception exception = new RuntimeException("민감한 내부 오류 메시지");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleException(exception, request);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage())
                    .isEqualTo("서버 내부 오류가 발생했습니다.")
                    .doesNotContain("민감한 내부 오류 메시지");
        }
    }

    @Nested
    @DisplayName("응답 구조")
    class ResponseStructure {

        @Test
        @DisplayName("ErrorResponse는 timestamp를 포함한다")
        void errorResponse_containsTimestamp() {
            // given
            Exception exception = new RuntimeException("오류");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleException(exception, request);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("ErrorResponse의 success 필드는 항상 false이다")
        void errorResponse_successAlwaysFalse() {
            // given
            BusinessException exception = new BusinessException(HttpStatus.BAD_REQUEST, "ERR", "오류");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception, request);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
        }
    }
}
