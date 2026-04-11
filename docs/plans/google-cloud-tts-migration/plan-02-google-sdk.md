# Plan 02: Google Cloud TTS SDK 의존성 + TextToSpeechClient 빈 등록

> 상태: Draft
> 작성일: 2026-04-11

## Why

Google Cloud TTS Java SDK(`google-cloud-texttospeech`)를 프로젝트에 도입하고, ADC 기반 인증으로 `TextToSpeechClient`를 Spring 빈으로 등록. 기존 `AwsConfig` 패턴과 일관성을 유지하되 구글 인프라 설정은 독립된 패키지(`infra/google`)로 분리.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/build.gradle.kts` | Google Cloud BOM + `google-cloud-texttospeech` 의존성 추가 |
| `backend/src/main/java/com/rehearse/api/infra/google/GoogleTtsConfig.java` | **신규** — `TextToSpeechClient` 빈 등록 |

## 상세

### build.gradle.kts

기존 `dependencies` 블록에 추가:

```kotlin
// Google Cloud TTS
implementation(platform("com.google.cloud:libraries-bom:26.47.0"))
implementation("com.google.cloud:google-cloud-texttospeech")
```

**BOM 사용 이유**: gRPC/protobuf/guava 등 transitive 의존성 버전 충돌 방지. BOM이 모든 Google Cloud 클라이언트 라이브러리의 호환 버전을 일괄 관리.

### GoogleTtsConfig.java

```java
package com.rehearse.api.infra.google;

import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ConditionalOnProperty(prefix = "google.tts", name = "enabled", havingValue = "true")
public class GoogleTtsConfig {

    @Bean(destroyMethod = "close")
    public TextToSpeechClient textToSpeechClient() throws IOException {
        return TextToSpeechClient.create();
    }
}
```

**핵심 사항**:
- `TextToSpeechClient.create()`는 ADC(Application Default Credentials)를 자동 감지
  - 로컬: `gcloud auth application-default login` 후 `~/.config/gcloud/application_default_credentials.json`
  - 서버: `GOOGLE_APPLICATION_CREDENTIALS` 환경변수가 가리키는 서비스 계정 JSON
- `destroyMethod = "close"` — `TextToSpeechClient`는 `AutoCloseable`, Spring 컨텍스트 종료 시 자동 close
- `@ConditionalOnProperty` — `google.tts.enabled=false`(테스트 프로파일)에서는 빈 미생성 → 다운스트림 `GoogleCloudTtsService`, `TtsController`도 미생성

## 담당 에이전트

- Implement: `backend` — 의존성 추가 + 빈 등록
- Review: `architect-reviewer` — 기존 `AwsConfig` 패턴과의 일관성 및 패키지 구조

## 검증

- `./gradlew compileJava` 성공
- `./gradlew dependencies --configuration runtimeClasspath | grep -i google-cloud` 로 의존성 확인
- gRPC/protobuf 충돌 에러 없음
- `progress.md` 상태 업데이트 (Task 2 → Completed)
