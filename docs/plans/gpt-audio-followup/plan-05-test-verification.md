# Plan 05: 테스트 + 검증

> 상태: Draft
> 작성일: 2026-03-27

## Why

GPT-audio 통합이 기존 기능을 깨뜨리지 않고, fallback이 정상 동작하며, 응답시간이 실제로 개선되었는지 검증해야 한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/test/java/.../InterviewServiceTest.java` | `generateFollowUp()` 테스트 수정 |
| `backend/src/test/java/.../ResilientAiClientTest.java` | audio fallback 테스트 추가 (필요 시) |

## 상세

### 단위 테스트

1. **InterviewServiceTest** — `generateFollowUp()`:
   - `aiClient.generateFollowUpWithAudio()` mock으로 정상 반환 확인
   - `answerText`가 응답에 포함되는지 확인
   - `resolveAnswerText()` 삭제 후 기존 테스트 수정

2. **ResilientAiClient 테스트** (필요 시):
   - primary(GPT-audio) 성공 경로
   - primary 실패 → fallback(Whisper + Claude) 경로
   - 둘 다 실패 → 예외 전파

### 통합 검증

1. `./gradlew test` — 전체 테스트 통과
2. 실제 GPT-4o-mini-audio-preview API 호출 (수동):
   - 오디오 파일 전송 → answerText + 후속질문 반환 확인
   - 응답시간 ~3.5초 확인
3. Mock 모드 동작 확인 (OpenAI API 키 없을 때)

## 담당 에이전트

- Implement: `test-engineer` — 테스트 작성
- Review: `qa` — 기능 검증, 회귀 테스트

## 검증

- `./gradlew test` 전체 통과 (기존 + 신규)
- fallback 경로 테스트 통과
- 응답시간 실측: ~4.9초 → ~3.5초 확인
- `progress.md` 상태 업데이트 (Task 5 → Completed)
