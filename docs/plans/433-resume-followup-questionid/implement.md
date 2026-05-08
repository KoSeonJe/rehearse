# Implement — RESUME 트랙 FollowUpResponse.questionId 누락 정상화

> **작성자**: backend agent (초안 by Claude)
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: 단일 영역 (BE only). FE 변경 없음 (product-spec 비스코프).
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 PR | 의존 |
|-------|------|--------------|--------|------|
| 1 | 핸들러 buildResponse questionId 주입 | `backend` | #N | - |
| 2 | orchestrator validateResponseQuestionId 가드 + 호출 순서 | `backend` | #N (동일 PR 또는 #N+1) | Phase 1 |
| 3 | RESUME 트랙 questionId 매핑 회귀 테스트 | `backend` | #N (동일 PR 또는 #N+2) | Phase 1+2 |

> Phase 3개 각 본문 50줄 미만, 단일 PR 가능. PR 분할은 구현 시점 작업 진척에 따라 결정.

---

## Phase 1: 핸들러 buildResponse questionId 주입

- **구현**: `backend` — RESUME 핸들러 3곳에서 응답 DTO 가 자기 질문 ID 를 보유하도록 빌더 시그니처에 questionId 추가

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandler.java` — `buildResponse` 시그니처에 `Long questionId` 추가 + `.questionId(questionId)` 주입. 호출처 (`handleOpener`, `handleTurn` 내부) 도 questionId 전달
- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java` — 동일 패턴. `buildResponse(InterrogationResult, int currentLevel, Long questionId)` + 호출처 수정
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java:122-130` — `startSession` 의 `existingOpener` 재사용 분기에서 `.questionId(opener.getId())` 주입

### 핵심 로직 / 변경 요약

```java
// PlaygroundModeHandler.buildResponse — 시그니처 + 주입
private FollowUpResponse buildResponse(String question, String ttsQuestion,
                                        String reason, boolean transitioned,
                                        Long questionId) {
    return FollowUpResponse.builder()
            .questionId(questionId)
            .question(question)
            .ttsQuestion(ttsQuestion)
            .reason(reason)
            .type("RESUME_PLAYGROUND")
            .skip(false)
            .presentToUser(true)
            .followUpExhausted(transitioned)
            .build();
}

// 호출처 (handleOpener / handleTurn 내부) — persister 가 반환한 questionId 를 buildResponse 로 전달
Long questionId = questionPersister.persist(...);
FollowUpResponse response = buildResponse(..., questionId);
return new OpenerResult(response, questionId);  // 또는 PlaygroundTurnResult
```

InterrogationModeHandler 동일 패턴. `buildExhaustedResponse` (questionId 의도적 null) 는 변경 안 함.

orchestrator startSession:
```java
return FollowUpResponse.builder()
        .questionId(opener.getId())          // 신규
        .question(opener.getQuestionText())
        .ttsQuestion(opener.getTtsText())
        .presentToUser(true)
        .type("RESUME_OPENER")
        .build();
```

### 의존

- 선행 phase: 없음
- 외부 의존: 없음

### Verification Hook

- 명령: `./gradlew build`
- 통과 기준: 컴파일 성공 + 기존 테스트 회귀 0건
- 관찰 가능 동작: dev 환경 RESUME 트랙 1턴 진행 → FE devtools Network 응답 JSON 에 `questionId` 필드 포함 확인

### 커밋 메시지 (예상)

```
fix(BE): RESUME 핸들러 응답 DTO questionId 주입
```

---

## Phase 2: orchestrator validateResponseQuestionId 가드 + 호출 순서

- **구현**: `backend` — 응답 DTO 와 TurnHandlerResult 의 questionId mismatch 사일런트 차단 가드 도입

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java`
  - 신규 메서드 `validateResponseQuestionId(Long interviewId, long turnIndex, ResumeMode mode, TurnHandlerResult result)`
  - `processUserTurnInternal:103` 부근 호출 순서 — `shouldSkipTurnCompletedEvent` true 분기 통과 후 → `validateQuestionId` → `validateResponseQuestionId` → `turnEventPublisher.publish`

### 핵심 로직 / 변경 요약

```java
// 신규 메서드
private void validateResponseQuestionId(Long interviewId, long turnIndex,
                                         ResumeMode mode, TurnHandlerResult result) {
    Long handlerId = result.questionId();
    Long responseId = result.response().getQuestionId();
    if (responseId == null || !responseId.equals(handlerId)) {
        log.warn("[진행차단진단] interviewId={} track=RESUME stage={} reason=response-questionid-mismatch handlerQuestionId={} responseQuestionId={} turnIndex={}",
                interviewId, mode.name().toLowerCase(), handlerId, responseId, turnIndex);
    }
}

// processUserTurnInternal 호출 순서 (shouldSkip 통과 후)
validateQuestionId(interviewId, turnIndex, currentMode, handlerResult);
validateResponseQuestionId(interviewId, turnIndex, currentMode, handlerResult);
turnEventPublisher.publish(...);
```

의도적 null 케이스 (`hardTimeoutResponse`, `contextBudgetExceededResponse`, `buildExhaustedResponse`) 는 `shouldSkipTurnCompletedEvent` true 분기에서 조기 return 되어 신규 검증 미경유. startSession OPENER 재사용 분기는 `processUserTurnInternal` 경유 X → 빌더 시점 주입 (Phase 1) 으로 정합성 확보.

### 의존

- 선행 phase: Phase 1 (응답 DTO questionId 정상화 후에 mismatch 검증 의미)
- 외부 의존: 없음

### Verification Hook

- 명령: `./gradlew build`
- 통과 기준: 컴파일 성공 + 기존 테스트 회귀 0건
- 관찰 가능 동작: dev 환경 RESUME 트랙 정상 1턴 진행 → docker log 에 `reason=response-questionid-mismatch` WARN 미발생

### 커밋 메시지 (예상)

```
fix(BE): RESUME orchestrator 응답 questionId 정합 가드
```

---

## Phase 3: RESUME 트랙 questionId 매핑 회귀 테스트

- **구현**: `backend` — Domain Unit (Mockist) + Service Integration 으로 회귀 차단

### 변경 파일

- `backend/src/test/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandlerTest.java` — 신규. Mockist Domain Unit (LLM port + Persister Mock)
- `backend/src/test/java/com/rehearse/api/domain/resume/service/InterrogationModeHandlerTest.java` — 신규. 동일 패턴
- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestratorIntegrationTest.java` — 신규. `ServiceIntegrationSupport` 사용. 외부 LLM port 만 Mock, 내부 실제 주입

### 핵심 로직 / 변경 요약

**Domain Unit (Mockist)** — testing.md "Domain Unit ... Mock 허용 (기존 Mockist 패턴)" 적용. 사유 = persister mock 반환 ID 가 응답 DTO 에 정확히 흐르는지 검증이 본질.

```java
// PlaygroundModeHandlerTest
@DisplayName("PlaygroundModeHandler 응답 DTO 정합")
class PlaygroundModeHandlerTest {
    @Test
    @DisplayName("OPENER 처리 시 응답 DTO 가 persister 반환 questionId 를 보유한다")
    void should_return_response_with_questionId_when_handle_opener() { ... }

    @Test
    @DisplayName("PLAYGROUND turn 처리 시 응답 DTO 가 persister 반환 questionId 를 보유한다")
    void should_return_response_with_questionId_when_handle_turn() { ... }
}
```

**Service Integration** — `ServiceIntegrationSupport` 사용. 실 DB (Testcontainers) + 실 핸들러 + LLM port 만 Mock.

```java
@DisplayName("ResumeInterviewOrchestrator 응답 questionId 정합")
class ResumeInterviewOrchestratorIntegrationTest extends ServiceIntegrationSupport {
    @Test
    @DisplayName("RESUME PLAYGROUND turn 응답 DTO 가 OPENER 가 아닌 자기 질문 ID 를 보유한다")
    void should_return_response_with_self_questionId_when_resume_playground_turn() { ... }

    @Test
    @DisplayName("RESUME INTERROGATION turn 응답 DTO 가 자기 질문 ID 를 보유한다")
    void should_return_response_with_self_questionId_when_resume_interrogation_turn() { ... }

    @Test
    @DisplayName("startSession OPENER 재사용 시 응답 DTO 가 기존 OPENER 질문 ID 를 보유한다")
    void should_return_response_with_opener_questionId_when_session_start_reuse() { ... }

    @Test
    @DisplayName("응답 DTO questionId 가 handler 와 mismatch 면 WARN 로그 발생")
    void should_log_warn_when_response_questionId_mismatches_handler() { ... }

    @Test
    @DisplayName("정상 turn 흐름에서 mismatch WARN 미발생")
    void should_not_log_warn_when_response_questionId_matches_handler() { ... }
}
```

### 의존

- 선행 phase: Phase 1 + 2
- 외부 의존: Testcontainers MySQL (`ServiceIntegrationSupport` 자동)

### Verification Hook

- 명령:
  ```bash
  ./gradlew test --tests "PlaygroundModeHandlerTest"
  ./gradlew test --tests "InterrogationModeHandlerTest"
  ./gradlew test --tests "ResumeInterviewOrchestratorIntegrationTest"
  ./gradlew test
  ```
- 통과 기준: 신규 케이스 7건 green + 기존 테스트 회귀 0건
- 관찰 가능 동작: 회귀 테스트가 Phase 1+2 fix revert 시 fail 하는지 확인 (가드 본질 검증)

### 커밋 메시지 (예상)

```
test(BE): RESUME 트랙 questionId 매핑 회귀 테스트
```

---

## 통합 Verification

- [ ] tech-spec.md Verification 항목 모두 통과 (Domain Unit / Service Integration / 빌드 / 관찰 / 회귀)
- [ ] dev 환경 수동 검증: RESUME 트랙 신규 인터뷰 1회 (OPENER + PLAYGROUND 5턴) 진행 → 피드백 페이지 6건 노출 + 기술 피드백 노출 + 모범답변 노출
- [ ] DB 검증: `timestamp_feedback` row 가 turn 별 분산 (단일 OPENER 집중 X)
- [ ] docker log: `response-questionid-mismatch` WARN 미발생

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] 지정 리뷰어 실행 (구현 완료 직후 — 메인 세션 책임)
  - BE only → `code-reviewer-backend`
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `backend/.claude/rules/testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md Pre/Post 섹션)
