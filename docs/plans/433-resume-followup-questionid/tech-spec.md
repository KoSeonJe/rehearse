# Tech Spec — RESUME 트랙 FollowUpResponse.questionId 누락 정상화

> **작성자**: backend agent (초안 by Claude)
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄 미러)

RESUME 트랙 turn 답변이 자기 질문에 정확히 매핑되도록 응답 DTO 의 `questionId` 를 정상화 + 동일 결함 사일런트 재발 차단 가드 도입.

## Evidence

- 현재 구조:
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java`
    - `:65-108` `processUserTurnInternal` — `dispatchByMode` 결과 `TurnHandlerResult(response, questionId)` 받아 이벤트 발행 + 응답 반환
    - `:111-137` `startSession` — 기존 OPENER 재사용 분기에서 응답 빌드
    - `:204-211` `validateQuestionId` — `handlerResult.questionId()` null 검증 (TurnHandlerResult 측)
    - `:213` `private record TurnHandlerResult(FollowUpResponse response, Long questionId)`
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandler.java:132` `buildResponse` — `.questionId()` 미주입
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java:125` — 동일 누락
  - `backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpResponse.java:10` — `questionId` 필드 존재 (`@Builder(toBuilder=true)`)
- 외부 레퍼런스: 없음
- 사용자 발화 (특정 결정 근거):
  - 운영 데이터 백필 미수정
  - WRAP_UP fix 는 Issue #424 (WRAP_UP 모드 제거) 가 handler 자체 제거로 자동 해소 — 본 작업에서 다루지 않음
- 추정 / 미확인 가정:
  - dev id=26 외 운영 RESUME 인터뷰의 동일 패턴 빈도 미확인 (영향 범위 추정에만 사용, 본 작업 검증 영향 없음)

## Trade-offs

### Trade-off 1 — buildResponse 책임 위치

#### Option A (채택): 핸들러 `buildResponse` 시그니처에 questionId 추가

- 장점:
  - 핸들러가 자기 응답을 완성 — SRP, 미래 유사 결함 재발 차단
  - 컴파일 타임에 빠짐 검출 (시그니처 강제)
- 단점:
  - 핸들러 3곳 (Playground / Interrogation / startSession 분기) 시그니처 수정
- 사유: 응답 후처리 분리 패턴은 "응답 미완성 핸들러" 결함을 잔존시킨다. 핸들러 자체에서 완성해야 정합성 단일 출처.

#### Option B (폐기): orchestrator 후처리

- 장점: 핸들러 수정 최소 (`response.toBuilder().questionId(handlerResult.questionId()).build()`)
- 단점: 응답 후처리가 orchestrator 에 분리 책임으로 잔존 → 미래 핸들러 추가 시 동일 결함 재발 가능
- 폐기 사유: 결함 본질이 "핸들러가 자기 응답을 완성하지 않음" 인데 후처리는 본질 회피.

### Trade-off 2 — 가드 강도

#### Option A (채택): orchestrator 응답 단계 mismatch 검증 + WARN

- 장점:
  - 회귀 사일런트 차단 — `handlerResult.questionId()` 와 `response.questionId` mismatch 시 운영자 식별 가능
  - 기존 `validateQuestionId` (TurnHandlerResult 측) 와 책임 분리
  - RESUME 트랙 한정 영향
- 단점: WARN 로그 1건 추가
- 사유: 결함이 사일런트로 진입한 본질 (채점은 정상, 매핑만 깨짐) 차단. RESUME 트랙 진입점에서 단일 검증.

#### Option B (폐기): `FollowUpResponse.builder().build()` 단계 require

- 장점: 모든 트랙 동시 보장
- 단점: 빌더 호출처 전수 영향 (`aiSkip` 등 questionId 의도적 null 케이스 존재) → 기존 흐름 회귀 위험
- 폐기 사유: scope 침범, 본 spec 비스코프 (다른 트랙 / 다른 흐름) 위반.

## Architecture

### 변경 전 (Pre)

```
processUserTurnInternal
  → dispatchByMode(...)
       → PlaygroundModeHandler.handle / InterrogationModeHandler.handle
            → buildResponse(question, ttsQuestion, reason, transitioned)
                 → FollowUpResponse.builder()...build()   (questionId 누락)
       → return TurnHandlerResult(response, questionId)
  → validateQuestionId(handlerResult)                     (TurnHandlerResult 측만 검증)
  → turnEventPublisher.publish(..., handlerResult.questionId())
  → return handlerResult.response()                       (응답 DTO 내 questionId == null)

startSession
  → existingOpener 재사용 시 FollowUpResponse.builder()...build()   (questionId 누락)
```

### 변경 후 (Post)

```
processUserTurnInternal
  → dispatchByMode(...)
       → PlaygroundModeHandler.handle / InterrogationModeHandler.handle
            → buildResponse(question, ttsQuestion, reason, transitioned, questionId)
                 → FollowUpResponse.builder().questionId(questionId)...build()
       → return TurnHandlerResult(response, questionId)
  → if (shouldSkipTurnCompletedEvent(handlerResult)) return handlerResult.response()
                                                          (의도적 null 분기 — hardTimeout / contextBudgetExceeded / INTERROGATION exhausted. 검증 우회)
  → validateQuestionId(interviewId, turnIndex, mode, handlerResult)   (기존 — TurnHandlerResult.questionId null throw)
  → validateResponseQuestionId(interviewId, turnIndex, mode, handlerResult)
                                                          (신규 — handlerResult.questionId() ↔ handlerResult.response().getQuestionId() mismatch WARN)
  → turnEventPublisher.publish(..., handlerResult.questionId())
  → return handlerResult.response()

startSession (OPENER 재사용 분기)
  → existingOpener 재사용 시 FollowUpResponse.builder().questionId(opener.getId())...build()
                                                          (orchestrator 검증 미경유 — 빌더 시점 주입으로 정합성 확보)
```

### 핵심 변경 위치

| # | 파일 | 라인 | 변경 |
|---|------|------|------|
| 1 | `PlaygroundModeHandler.java` | 132-142 | `buildResponse` 시그니처에 `Long questionId` 추가 + `.questionId(questionId)` 주입. 호출처 (`handleOpener`, `handleTurn`) 도 questionId 전달 |
| 2 | `InterrogationModeHandler.java` | 125-135 | 동일 — `buildResponse` 시그니처 + 호출처 |
| 3 | `ResumeInterviewOrchestrator.java` | 122-130 | `startSession` 의 OPENER 재사용 응답에 `.questionId(opener.getId())` 주입 |
| 4 | `ResumeInterviewOrchestrator.java` | 신규 메서드 | `validateResponseQuestionId(Long interviewId, long turnIndex, ResumeMode mode, TurnHandlerResult result)` — `result.questionId()` ↔ `result.response().getQuestionId()` mismatch / null 시 WARN. 로그 포맷 = `[진행차단진단] interviewId={} track=RESUME stage={} reason=response-questionid-mismatch ...` (기존 `validateQuestionId` 와 통일) |
| 5 | `ResumeInterviewOrchestrator.java` | `:103` 부근 | `shouldSkipTurnCompletedEvent` true 분기 통과 후 → 기존 `validateQuestionId` → 신규 `validateResponseQuestionId` 순서. 의도적 null 케이스 (hardTimeout / contextBudgetExceeded / exhausted) 는 `shouldSkip` 분기에서 조기 return 되어 검증 미경유 |

WRAP_UP handler (`WrapUpModeHandler.java:48`) = **본 작업 비스코프**. Issue #424 가 handler 자체 제거로 자동 해소 (product-spec 명시).

## Data Model

스키마 변경 없음. `FollowUpResponse.questionId` 필드 이미 존재 (`FollowUpResponse.java:10`). DB 컬럼 / Entity 변경 없음.

## API Contract

### Endpoint

기존 — 변경 없음:
- `POST /api/v1/interviews/{id}/answers` (turn 답변 처리)
- `GET /api/v1/interviews/{id}/start` (세션 시작)

### Response schema 변경

`FollowUpResponse` JSON schema 자체 변경 없음. **데이터 정상화** 만:

#### Pre (RESUME 트랙)

```json
{
  "question": "프로젝트에서 가장 도전적이었던 부분은?",
  "ttsQuestion": "...",
  "reason": "...",
  "type": "RESUME_PLAYGROUND",
  "skip": false,
  "presentToUser": true,
  "followUpExhausted": false
}
```
- `questionId` 필드 미포함 (Lombok `@Builder` 기본값 null → JSON 에서 null 또는 생략)

#### Post (RESUME 트랙)

```json
{
  "questionId": 148,
  "question": "프로젝트에서 가장 도전적이었던 부분은?",
  "ttsQuestion": "...",
  "reason": "...",
  "type": "RESUME_PLAYGROUND",
  "skip": false,
  "presentToUser": true,
  "followUpExhausted": false
}
```

### Error

기존 동일. 본 작업 신규 에러 코드 없음.

### FE 영향

`frontend/src/hooks/use-answer-flow.ts:294-303` 폴백 분기 (`state.currentFollowUp?.questionId` null → MAIN ID) 유지. 서버 응답 정상화로 폴백 발동 안 함. FE 코드 수정 불필요 (product-spec 비스코프).

## Verification (완료 판정)

### Domain Unit (Mockist 패턴 — `testing.md` "Domain Unit ... Mock 허용 — 기존 Mockist 패턴" 적용)

핸들러는 LLM port (`ResumeQuestionResultGenerator` 등) + Persister 의존. **외부 API 만 Mock + Persister Mock 허용** — 단, Persister mock 의 반환 ID 가 응답 DTO 에 정확히 흐르는지 검증이 본 테스트의 본질이므로 Mockist 적합.

- [ ] `PlaygroundModeHandlerTest`
  - `should_return_response_with_questionId_when_handle_opener` — 응답 DTO `questionId` == persister mock 반환 ID
  - `should_return_response_with_questionId_when_handle_turn` — 동일
- [ ] `InterrogationModeHandlerTest`
  - `should_return_response_with_questionId_when_handle` — 응답 DTO `questionId` 보유

### Service Integration

`ServiceIntegrationSupport` 사용. 외부 LLM port 만 Mock, 내부 Service / Repository / Handler 실제 주입.

- [ ] `ResumeInterviewOrchestratorIntegrationTest` (신규 또는 기존 확장)
  - `should_return_response_with_self_questionId_when_resume_playground_turn` — RESUME 트랙 PLAYGROUND turn 응답 DTO `questionId` ≠ OPENER ID
  - `should_return_response_with_self_questionId_when_resume_interrogation_turn` — INTERROGATION turn 동일
  - `should_return_response_with_opener_questionId_when_session_start_reuse` — startSession OPENER 재사용 시 응답 DTO `questionId` == 기존 OPENER question.id
  - `should_log_warn_when_response_questionId_mismatches_handler` — handler 가 questionId 미주입한 응답 강제 주입 시 WARN 로그 발생 (logback appender capture, reason=`response-questionid-mismatch`)
  - `should_not_log_warn_when_response_questionId_matches_handler` — 정상 PLAYGROUND turn 흐름에서 mismatch WARN 0건 (logback appender count == 0) — 회귀 가드 본질 검증

### 빌드 / 린트

- [ ] `./gradlew build`

### 관찰 가능 동작

- [ ] dev 환경 RESUME 트랙 신규 인터뷰 1회 진행 (OPENER + PLAYGROUND 5턴 시나리오) → 종료 후 피드백 페이지에서 OPENER + 모든 PLAYGROUND turn 노출 확인 (정확히 6건)
- [ ] FE devtools Network → turn 응답 JSON 에 `questionId` 필드 포함 확인 (RESUME 트랙)
- [ ] docker log 에서 `[진행차단진단] ... reason=response-questionid-mismatch` WARN 미발생 (정상 케이스)
- [ ] DB query:
  ```sql
  SELECT tf.question_id, COUNT(*) FROM timestamp_feedback tf
  JOIN question q ON tf.question_id = q.id
  WHERE tf.interview_id = {신규 ID}
  GROUP BY tf.question_id
  ```
  → 6건 row, 각 question_id distinct (단일 OPENER ID 집중 X, OPENER 1건 + PLAYGROUND 5건)

### 회귀 체크

- [ ] 기존 `ResumeQuestionResultGeneratorTest`, `ResumeIngestionServiceTest`, `ResumeSkeletonPersisterTest` 통과
- [ ] STANDARD 트랙 응답 (`FollowUpResponse.aiSkip` 등 의도적 questionId null 케이스) 회귀 없음 — `validateResponseQuestionId` 가 RESUME 트랙 한정 호출되는지 확인

## Pre / Post State

### Pre (현재)

- `PlaygroundModeHandler.buildResponse(String, String, String, boolean)` — questionId 미주입
- `InterrogationModeHandler.buildResponse(InterrogationResult, int)` — questionId 미주입
- `ResumeInterviewOrchestrator.startSession` 의 OPENER 재사용 응답 — questionId 미주입
- `ResumeInterviewOrchestrator.validateQuestionId` — `handlerResult.questionId()` null 검증만
- 응답 DTO `questionId` 항상 null 또는 미포함 (RESUME 트랙)
- FE 폴백 발동 → DB `timestamp_feedback.question_id` 모두 OPENER 집중

### Post (구현 후)

- `PlaygroundModeHandler.buildResponse(String, String, String, boolean, Long questionId)` — questionId 주입
- `InterrogationModeHandler.buildResponse(InterrogationResult, int, Long questionId)` — questionId 주입
- `ResumeInterviewOrchestrator.startSession` OPENER 재사용 응답에 `.questionId(opener.getId())` 주입
- `ResumeInterviewOrchestrator.validateResponseQuestionId` 신규 — `handlerResult.questionId()` 와 `response.questionId` mismatch / null 시 WARN
- 응답 DTO `questionId` 항상 정상 (RESUME 트랙 turn / OPENER)
- FE 폴백 미발동 → DB `timestamp_feedback.question_id` turn 별 분산

## 비기능 영향 (NF)

- 영향 범위: BE only — RESUME orchestrator + 핸들러 3곳
- 정합성: 동기 트랜잭션 내 응답 DTO 필드 주입 — 부수 효과 없음
- 실시간성: turn 응답 latency 변경 없음 — 필드 1개 빌더 주입 + 로그 1건 추가만
- 부하: 영향 없음 — 추가 DB 쿼리 / LLM 호출 / 외부 호출 없음
- 동시성: 영향 없음 — `InterrogationModeHandler.handle` 의 `tracker` 흐름 안에서 questionId 는 기존에도 확보됨
- 마이그레이션: 스키마 변경 없음. 운영 데이터 백필 = 비스코프 (product-spec 명시)
- 외부 의존: LLM / S3 / EventBridge / Lambda 변경 없음
- 보안: 인가 / 입력 검증 변경 없음 (OWASP A01/A09 무관). `questionId` 노출은 기존 STANDARD 트랙 동일 권한 체계
- 관찰성: WARN 로그 1건 신규 (`response-questionid-mismatch`)
- 롤백: 코드 revert 단순
- 검증: Service Integration + Domain Unit (위 Verification 섹션)

## 위험 / 마이그레이션 / 롤백

- 위험:
  - `validateResponseQuestionId` 가 STANDARD 트랙 등 다른 트랙 응답에 영향 안 가도록 RESUME orchestrator 진입점 한정 호출 — Trade-off 2 Option A 의 핵심 제약
  - 기존 `FollowUpResponse.aiSkip` / `hardTimeoutResponse` / `contextBudgetExceededResponse` / `buildExhaustedResponse` 분기는 questionId 의도적 null — `shouldSkipTurnCompletedEvent` true 분기로 조기 return 되어 신규 검증 미경유 (구현 시 재확인 필요)
  - startSession OPENER 재사용 분기는 `processUserTurnInternal` 경유 X → orchestrator 검증 미경유. 빌더 시점 `.questionId(opener.getId())` 주입으로 정합성 확보
  - Issue #424 머지 전 WRAP_UP 도달 시 결함 잔존 — 도달 빈도 낮음 (hard timeout 또는 wrap-up-threshold 도달 시) 운영 임팩트 수용 (product-spec 비스코프 명시)
- 마이그레이션 전략:
  - 스키마 변경 없음. 코드 변경만.
  - 기존 운영 데이터 백필 = 비스코프 (product-spec 명시).
- 롤백 시나리오:
  - 코드 revert 단순. 신규 인터뷰만 영향, 기존 데이터 영향 없음.
  - WARN 로그 폭주 (mismatch 다발) 시 = 즉시 revert 가능.

## 분기 결정

- [x] 단일 영역 → `implement.md` 1개 (BE only)
- [ ] BE+FE 동시
- [ ] BE 선행 강제

FE 변경 불필요 (product-spec 비스코프 명시). 단일 `implement.md` 작성.
