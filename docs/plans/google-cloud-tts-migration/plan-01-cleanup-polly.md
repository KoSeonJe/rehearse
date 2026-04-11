# Plan 01: AWS Polly 흔적 제거

> 상태: Draft
> 작성일: 2026-04-11

## Why

AWS Polly 마이그레이션 중 방향 전환이 결정됨(한국어 남성 음성 부재). 이미 unstaged 상태로 적용된 Polly 관련 변경을 모두 되돌려 Google Cloud TTS 도입을 위한 클린 상태로 만듦.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/build.gradle.kts` | `implementation("software.amazon.awssdk:polly")` 줄 제거 |
| `backend/src/main/java/com/rehearse/api/infra/aws/AwsConfig.java` | `PollyClient` 빈 메서드 및 관련 import 제거 |
| `backend/src/main/java/com/rehearse/api/infra/aws/AwsPollyTtsService.java` | **삭제** |
| `backend/src/main/resources/application-dev.yml` | `aws.polly` 블록 제거 |
| `backend/src/main/resources/application-prod.yml` | `aws.polly` 블록 제거 |
| `backend/.env.example` | `GOOGLE_CLOUD_API_KEY` 줄 제거 (API 키 방식 미지원) |

## 상세

### build.gradle.kts

다음 라인 제거:
```kotlin
implementation("software.amazon.awssdk:polly")
```
`s3` 의존성은 유지 (다른 기능에서 사용 중).

### AwsConfig.java

`PollyClient` 빈 메서드 통째 제거 + `import software.amazon.awssdk.services.polly.PollyClient;` 제거.

### application-dev.yml / application-prod.yml

다음 블록 제거:
```yaml
polly:
  voice-id: ${AWS_POLLY_VOICE_ID:Seoyeon}
  engine: ${AWS_POLLY_ENGINE:neural}
```

`aws.s3`는 유지.

### .env.example

`GOOGLE_CLOUD_API_KEY=...` 줄 제거. (API 키는 TTS REST에서 미지원이며, 실제 인증은 서비스 계정 JSON 방식으로 대체됨 — plan-04에서 처리)

## 담당 에이전트

- Implement: `backend` — 파일 수정/삭제
- Review: `code-reviewer` — Polly 잔존 참조 없는지 확인

## 검증

- `./gradlew compileJava` 성공
- `grep -rn "polly\|Polly\|AwsPolly" backend/src/` 결과 없음
- `progress.md` 상태 업데이트 (Task 1 → Completed)
