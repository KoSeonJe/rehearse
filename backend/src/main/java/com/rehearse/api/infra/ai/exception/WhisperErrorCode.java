package com.rehearse.api.infra.ai.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WhisperErrorCode implements ErrorCode {
    API_KEY_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "WHISPER_001", "Whisper API 키가 설정되지 않았습니다."),
    FILE_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WHISPER_002", "오디오 파일 읽기에 실패했습니다."),
    API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "WHISPER_003", "음성 인식 API 호출에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
