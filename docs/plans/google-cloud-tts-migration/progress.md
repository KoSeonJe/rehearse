# ElevenLabs → Google Cloud TTS 마이그레이션 — 진행 상황

## 태스크 상태

| # | 태스크 | 상태 | 비고 |
|---|--------|------|------|
| 0 | 브랜치 rename (`feat/aws-polly-migration` → `feat/google-cloud-tts-migration`) | Completed | 로컬 전용 |
| 1 | AWS Polly 흔적 제거 | Completed | `plan-01-cleanup-polly.md` |
| 2 | Google Cloud TTS SDK 의존성 + TextToSpeechClient 빈 | Completed | `plan-02-google-sdk.md` |
| 3 | GoogleCloudTtsService 구현 + 컨트롤러 조건 변경 | Completed | `plan-03-service.md` |
| 4 | YAML + docker-compose + .env.example 재작성 | Completed | `plan-04-config.md` |
| 5 | GCP 서비스 계정 발급 + EC2 배포 | Draft | BE PR 머지 후 `plan-05-infra.md` |

## 의존성

```
Task 0 (branch rename)
  ↓
Task 1 (Polly 제거) ─┐
                     ├→ Task 3 (Service + Controller)
Task 2 (SDK + Bean) ─┘
  ↓
Task 4 (Config)
  ↓
Task 5 (Infra + 배포)
```

## 결정 사항

- **선택 음성**: `ko-KR-Chirp3-HD-Schedar` (한국어 남성, Chirp3-HD, 무료 100만자/월)
- **인증**: 서비스 계정 JSON + ADC (`GOOGLE_APPLICATION_CREDENTIALS`)
- **조건부 활성화**: `google.tts.enabled` 프로퍼티 (test 프로파일은 false)
- **재사용**: `TtsService` 인터페이스, `TtsErrorCode`, `TtsController` — 수정 없음(컨트롤러 `@ConditionalOnProperty` prefix만 변경)

## 진행 로그

- 2026-04-11: AWS Polly 방향 전환 결정 (한국어 남성 음성 부재)
- 2026-04-11: Google Cloud TTS Chirp3-HD-Schedar 샘플 비교 후 채택
- 2026-04-11: 로컬 `gcloud auth application-default login` 완료, Text-to-Speech API 활성화 완료
- 2026-04-11: 기존 `docs/plans/aws-polly-migration/` 삭제 및 `docs/plans/google-cloud-tts-migration/` 신규 작성
- 2026-04-11: Plan 0~4 구현 완료. `./gradlew test` 273개 전체 통과. `grep polly|ElevenLabs` 잔존 코드 없음 확인.
- 2026-04-11: 로컬 smoke test는 MySQL 의존으로 skip. EC2 배포(Plan 5) 시점에 실환경 검증 예정.
