package com.rehearse.api.infra.tts;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TtsErrorCode implements ErrorCode {
    API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "TTS_001", "음성 합성 API 호출에 실패했습니다."),
    EMPTY_TEXT(HttpStatus.BAD_REQUEST, "TTS_002", "변환할 텍스트가 비어있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
