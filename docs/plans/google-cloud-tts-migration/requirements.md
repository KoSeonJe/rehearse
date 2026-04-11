# ElevenLabs → Google Cloud TTS 마이그레이션 — 요구사항 정의

> 상태: Draft
> 작성일: 2026-04-11

## Why

1. **Why?** — ElevenLabs는 외부 유료 API로 비용 부담이 있음. 당초 AWS Polly로 전환 검토했으나 **한국어 남성 음성이 없음**(Seoyeon/Jihye 여성만 제공). 면접관 음성은 남성 톤이 더 적합하다는 판단 → Google Cloud TTS로 방향 전환.
2. **Goal** — ElevenLabs 의존성을 완전히 제거하고 **Google Cloud Text-to-Speech**로 교체. 면접관 음성은 **`ko-KR-Chirp3-HD-Schedar`** (최신 HD 모델, 한국어 남성)로 고정.
3. **Evidence** — 샘플 비교 결과:
   - AWS Polly: 한국어는 여성(Seoyeon/Jihye)만 제공, 남성 없음
   - OpenAI TTS: 다국어 음성 — 한국어 네이티브 톤 아님
   - **Google Cloud TTS Chirp3-HD**: 한국어 남성 16종 제공, Schedar가 면접관 톤에 가장 적합
   - 무료 티어: Chirp3-HD **100만자/월 무료** — 면접 서비스 사용량 대비 충분
4. **Trade-offs**
   - (+) 한국어 남성 음성 확보, 최신 HD 품질
   - (−) AWS 외 GCP 의존성 추가 → 이중 클라우드 관리
   - (−) API 키 방식 미지원 → 서비스 계정 JSON 파일 관리 필요
   - (−) Chirp3-HD는 월 100만자 초과 시 $30/100만자 (Standard/Wavenet 보다 비쌈)

## 아키텍처 / 설계

기존 흐름 유지, TTS 서비스 구현체만 교체:

```
FE: speak(text)
  → POST /api/v1/tts { text }
  → BE: GoogleCloudTtsService → Google Cloud TTS API
          (ADC 인증, ko-KR-Chirp3-HD-Schedar)
  → audio/mpeg 바이트 반환
  → FE: Blob URL → Audio 재생
```

### 인증 방식

Google Cloud TTS REST API는 **API 키를 지원하지 않음**. 서비스 계정 JSON + ADC(Application Default Credentials) 방식 사용:

| 환경 | 인증 방식 |
|------|----------|
| 로컬 개발 | `gcloud auth application-default login` (`~/.config/gcloud/application_default_credentials.json`) |
| Docker/EC2 | 서비스 계정 JSON을 `backend/gcp-credentials.json`에 배치 → 컨테이너에 마운트 + `GOOGLE_APPLICATION_CREDENTIALS` 환경변수 |

Java SDK `TextToSpeechClient.create()`는 위 경로를 자동으로 감지.

### 음성 선택

CLI로 한국어 남성 음성 샘플을 비교해 `ko-KR-Chirp3-HD-Schedar`로 결정:

```bash
# 한국어 음성 목록
curl "https://texttospeech.googleapis.com/v1/voices?languageCode=ko-KR" \
  -H "Authorization: Bearer $(gcloud auth application-default print-access-token)" \
  -H "x-goog-user-project: gen-lang-client-0294632335"

# 샘플 생성
curl "https://texttospeech.googleapis.com/v1/text:synthesize" \
  -H "Authorization: Bearer $(gcloud auth application-default print-access-token)" \
  -H "x-goog-user-project: gen-lang-client-0294632335" \
  -H "Content-Type: application/json" \
  -d '{
    "input": {"text": "안녕하세요"},
    "voice": {"languageCode": "ko-KR", "name": "ko-KR-Chirp3-HD-Schedar"},
    "audioConfig": {"audioEncoding": "MP3"}
  }'
```

## Scope

- **In**: Google Cloud TTS Java SDK 통합, 서비스 계정 설정, EC2 배포, YAML/Docker 설정
- **Out**: 사용자 보이스 선택 UI, 다국어 TTS, 커스텀 음성 학습, SSML 마크업, 음성 캐싱

## 제약조건 / 환경

- GCP 프로젝트: `gen-lang-client-0294632335` (로컬 `gcloud auth` 완료)
- Cloud Text-to-Speech API: 활성화 완료
- 인증: 서비스 계정 JSON 파일 기반 (API 키 미지원)
- 로컬 환경(`google.tts.enabled: false`)에서는 TTS 비활성화 → Web Speech API 폴백
- 프론트엔드 변경 없음 (기존 `POST /api/v1/tts` 인터페이스 유지)
- 테스트 환경: `google.tts.enabled: false`로 빈 미생성, 기존 273개 테스트 통과 유지

## 재사용 자산

기존 AWS Polly 마이그레이션 시도에서 만든 공급자 중립 코드는 **수정 없이 재사용**:

- `backend/src/main/java/com/rehearse/api/domain/tts/service/TtsService.java` — 인터페이스
- `backend/src/main/java/com/rehearse/api/domain/tts/exception/TtsErrorCode.java` — 에러 코드 (TTS_001, TTS_002)
- `backend/src/main/java/com/rehearse/api/domain/tts/controller/TtsController.java` — `@ConditionalOnProperty` prefix만 `aws` → `google.tts`로 변경
