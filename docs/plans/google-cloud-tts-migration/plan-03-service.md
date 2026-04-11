# Plan 03: GoogleCloudTtsService 구현 + TtsController 조건 변경

> 상태: Draft
> 작성일: 2026-04-11

## Why

`TtsService` 인터페이스를 구현하는 Google Cloud TTS 서비스를 생성하고, `TtsController`의 활성화 조건을 `google.tts.enabled`로 변경. 기존 `TtsService`, `TtsErrorCode`는 공급자 중립적이므로 수정 없이 재사용.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/infra/google/GoogleCloudTtsService.java` | **신규** — `TtsService` 구현 |
| `backend/src/main/java/com/rehearse/api/domain/tts/controller/TtsController.java` | `@ConditionalOnProperty` prefix 변경 (`aws` → `google.tts`) |

**재사용(변경 없음)**:
- `backend/src/main/java/com/rehearse/api/domain/tts/service/TtsService.java`
- `backend/src/main/java/com/rehearse/api/domain/tts/exception/TtsErrorCode.java`

## 상세

### GoogleCloudTtsService.java

```java
package com.rehearse.api.infra.google;

import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import com.rehearse.api.domain.tts.exception.TtsErrorCode;
import com.rehearse.api.domain.tts.service.TtsService;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(TextToSpeechClient.class)
public class GoogleCloudTtsService implements TtsService {

    private final TextToSpeechClient ttsClient;

    @Value("${google.tts.voice-name:ko-KR-Chirp3-HD-Schedar}")
    private String voiceName;

    @Value("${google.tts.language-code:ko-KR}")
    private String languageCode;

    @Override
    public byte[] synthesize(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(TtsErrorCode.EMPTY_TEXT);
        }

        SynthesisInput input = SynthesisInput.newBuilder()
                .setText(text)
                .build();

        VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                .setLanguageCode(languageCode)
                .setName(voiceName)
                .build();

        AudioConfig audioConfig = AudioConfig.newBuilder()
                .setAudioEncoding(AudioEncoding.MP3)
                .build();

        try {
            SynthesizeSpeechResponse response =
                    ttsClient.synthesizeSpeech(input, voice, audioConfig);
            ByteString audioContent = response.getAudioContent();
            log.debug("Google Cloud TTS 완료: {}자 → {}bytes",
                    text.length(), audioContent.size());
            return audioContent.toByteArray();
        } catch (Exception e) {
            log.error("Google Cloud TTS API 호출 실패", e);
            throw new BusinessException(TtsErrorCode.API_CALL_FAILED);
        }
    }
}
```

**핵심 사항**:
- `@ConditionalOnBean(TextToSpeechClient.class)` — `GoogleTtsConfig`가 빈을 만들었을 때만 이 서비스도 생성
- `AudioEncoding.MP3` — 기존 `audio/mpeg` 응답과 호환
- 음성 이름/언어 코드는 YAML default로 고정, 환경변수 override 가능
- `ttsClient`는 `TextToSpeechClient` 빈이므로 close 책임은 Spring 컨텍스트(`destroyMethod`)에 있음 — 여기서 close하면 안 됨
- 예외는 `TtsErrorCode` 재사용

### TtsController.java 수정

```java
// Before (AWS Polly 시도 중 사용)
@ConditionalOnProperty(prefix = "aws", name = "enabled", havingValue = "true")

// After
@ConditionalOnProperty(prefix = "google.tts", name = "enabled", havingValue = "true")
```

나머지는 동일 (TtsService 인터페이스 의존).

## 담당 에이전트

- Implement: `backend` — 서비스 + 컨트롤러 조건 변경
- Review: `code-reviewer` — 에러 처리, 리소스 관리(TextToSpeechClient close 책임), 조건 일관성

## 검증

- `./gradlew compileJava` 성공
- `./gradlew test` 통과 (test 프로파일 `google.tts.enabled=false` → Controller 미등록)
- `progress.md` 상태 업데이트 (Task 3 → Completed)
