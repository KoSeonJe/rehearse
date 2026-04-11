# Plan 04: YAML + docker-compose + .env.example 재작성

> 상태: Draft
> 작성일: 2026-04-11

## Why

Google Cloud TTS 활성화에 필요한 모든 설정을 각 프로파일에 추가. 서비스 계정 JSON 파일을 컨테이너에 마운트하고, 민감정보가 git에 포함되지 않도록 `.gitignore` 업데이트.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/application-dev.yml` | `google.tts` 블록 추가 |
| `backend/src/main/resources/application-prod.yml` | `google.tts` 블록 추가 |
| `backend/src/test/resources/application-test.yml` | `google.tts.enabled: false` 추가 |
| `backend/docker-compose.yml` | backend 서비스에 `GOOGLE_APPLICATION_CREDENTIALS` + volume 마운트 |
| `backend/.env.example` | Google TTS 섹션 안내 추가 |
| `backend/.gitignore` | `gcp-credentials.json` 추가 |

## 상세

### application-dev.yml / application-prod.yml

기존 `claude:`, `openai:` 등과 같은 top-level에 추가:

```yaml
google:
  tts:
    enabled: ${GOOGLE_TTS_ENABLED:true}
    voice-name: ${GOOGLE_TTS_VOICE_NAME:ko-KR-Chirp3-HD-Schedar}
    language-code: ${GOOGLE_TTS_LANGUAGE_CODE:ko-KR}
```

### application-test.yml

```yaml
google:
  tts:
    enabled: false
```

이렇게 하면 테스트 프로파일에서는 `GoogleTtsConfig` → `TextToSpeechClient` → `GoogleCloudTtsService` → `TtsController` 체인이 모두 생성되지 않아 기존 273개 테스트 영향 없음.

### docker-compose.yml

backend 서비스 블록에 추가:

```yaml
services:
  backend:
    environment:
      # ...기존 환경변수
      GOOGLE_APPLICATION_CREDENTIALS: /app/gcp-credentials.json
    volumes:
      - ./gcp-credentials.json:/app/gcp-credentials.json:ro
```

**주의**: `./gcp-credentials.json` 파일은 **호스트(EC2)** 의 `~/rehearse/backend/` 경로에 배치되어야 함. 로컬 개발에서는 Docker 미사용(`./gradlew bootRun`)으로 ADC 사용, Docker 실행 시에는 개발자가 직접 JSON을 배치해야 함.

### .env.example

`GOOGLE_CLOUD_API_KEY` 제거 후:

```
# === Google Cloud TTS ===
# 서비스 계정 JSON 파일을 backend/gcp-credentials.json 에 배치해야 합니다.
# (docker-compose.yml 에서 /app/gcp-credentials.json 로 마운트됩니다)
# 음성 변경 시 아래 환경변수를 조정:
GOOGLE_TTS_VOICE_NAME=ko-KR-Chirp3-HD-Schedar
GOOGLE_TTS_LANGUAGE_CODE=ko-KR
```

### .gitignore

```
# Google Cloud 서비스 계정 JSON (절대 커밋 금지)
gcp-credentials.json
```

## 담당 에이전트

- Implement: `backend` — 설정 파일 수정
- Review: `code-reviewer` — 로컬/dev/test/prod 프로파일 일관성, 민감정보 git 제외 검증

## 검증

- `./gradlew test` 통과 (test 프로파일 → 빈 미생성)
- `git status` 에서 `gcp-credentials.json` 이 tracked에 안 잡히는지 확인
- 로컬에서 `./gradlew bootRun --args='--spring.profiles.active=dev'` 후:
  ```bash
  curl -X POST http://localhost:8080/api/v1/tts \
    -H 'Content-Type: application/json' \
    -d '{"text":"안녕하세요, 테스트입니다"}' \
    --output /tmp/tts-test.mp3
  file /tmp/tts-test.mp3  # Audio file with ID3
  ```
- `progress.md` 상태 업데이트 (Task 4 → Completed)
