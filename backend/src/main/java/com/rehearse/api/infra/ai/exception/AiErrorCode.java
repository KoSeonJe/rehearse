package com.rehearse.api.infra.ai.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {
    CLIENT_ERROR(HttpStatus.BAD_GATEWAY, "AI_001", "AI 요청에 실패했습니다."),
    SERVER_ERROR(HttpStatus.BAD_GATEWAY, "AI_002", "AI 서비스가 일시적으로 불안정합니다."),
    EMPTY_RESPONSE(HttpStatus.BAD_GATEWAY, "AI_003", "AI 응답이 비어있습니다."),
    TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI_004", "AI 서비스 호출 시간이 초과되었습니다."),
    PARSE_FAILED(HttpStatus.BAD_GATEWAY, "AI_005", "AI 응답을 파싱할 수 없습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI_006", "AI 서비스가 일시적으로 사용 불가합니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;


}
