# Implement — RESUME 트랙 Question 4개 필드 적재 정상화

> **작성자**: backend agent (Staff Engineer 페르소나)
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: BE 단일 영역. tech-spec.md 분기결정 [x] 단일.
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 PR | 의존 |
|-------|------|--------------|--------|------|
| 1 | Enum 속성 + Question.resume() 6-인자 | `backend` | #N | - |
| 2 | 4 record `modelAnswer` + 4 prompt template schema | `backend` | #N (단일 PR) | Phase 1 |
| 3 | ResumeQuestionResultGenerator + Fallback 신규 | `backend` | #N | Phase 2 |
| 4 | Persister 6-인자 + 4 ModeHandler 통합 | `backend` | #N | Phase 3 |

> 단일 PR 강제 (tech-spec 위험 섹션 — prompt schema + record 동시 머지). 4 Phase 임계 8 미초과 → 본 파일 단일.

---

## Phase 1: Enum 속성 + Question.resume() 6-인자

- **구현**: `backend` — RESUME 트랙 도메인 토대. enum 속성 + entity 팩토리 시그니처 확장.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionType.java` — enum 에 `referenceType` / `feedbackPerspective` 속성 추가. RESUME_* 4개 매핑 정적 (`OPENER/PLAYGROUND=GUIDE,EXPERIENCE`, `INTERROGATION=GUIDE,TECHNICAL`, `WRAP_UP=GUIDE,BEHAVIORAL`). MAIN/FOLLOWUP = `(null, null)`.
- `backend/src/main/java/com/rehearse/api/domain/question/entity/Question.java` — `resume()` 팩토리 6-인자 (`ttsText`, `modelAnswer` 추가). RESUME_* 외 type → `IllegalArgumentException`. referenceType / feedbackPerspective = null 유지 (Option X).
- `backend/src/test/java/com/rehearse/api/domain/question/entity/QuestionTypeTest.java` — 신규. Domain Unit. RESUME_* 4종 매핑 + MAIN/FOLLOWUP null 단언.
- `backend/src/test/java/com/rehearse/api/domain/question/entity/QuestionResumeFactoryTest.java` — 신규. Domain Unit. 6-인자 시그니처 / RESUME_* 외 type 거부.

### 핵심 로직 / 변경 요약

```java
public enum QuestionType {
    MAIN(null, null),
    FOLLOWUP(null, null),
    RESUME_OPENER(ReferenceType.GUIDE, FeedbackPerspective.EXPERIENCE),
    RESUME_PLAYGROUND(ReferenceType.GUIDE, FeedbackPerspective.EXPERIENCE),
    RESUME_INTERROGATION(ReferenceType.GUIDE, FeedbackPerspective.TECHNICAL),
    RESUME_WRAP_UP(ReferenceType.GUIDE, FeedbackPerspective.BEHAVIORAL);
    // accessor: referenceType(), feedbackPerspective()
}

public static Question resume(QuestionSet qs, QuestionType type,
                              String questionText, String ttsText, String modelAnswer,
                              int orderIndex) { ... }
```

### 의존

- 선행 phase: 없음
- 외부 의존: `ReferenceType`, `FeedbackPerspective` enum (기존)

### Verification Hook

- 명령: `./gradlew test --tests "QuestionTypeTest" --tests "QuestionResumeFactoryTest"`
- 통과 기준: 모든 케이스 green
- 관찰 가능 동작: 컴파일 시점 — 기존 `Question.resume()` 호출처 (4 modeHandler) **빌드 실패** (시그니처 변경). Phase 4 까지 임시 컴파일 깨짐 허용 — 단일 PR 머지 강제.

### 커밋 메시지 (예상)

```
feat(BE): QuestionType 매핑 속성 + Question.resume() 6-인자
```

---

## Phase 2: 4 record `modelAnswer` + 4 prompt template schema

- **구현**: `backend` — LLM 응답 record 에 modelAnswer 필드 + 폴백용 `withModelAnswer` 메서드 + prompt JSON schema 동시 변경.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptBuilder.java` — `PlaygroundOpenerResult` / `PlaygroundResponderResult` record 에 `modelAnswer` 필드 + `@JsonProperty("model_answer")` + `withModelAnswer(String)` 메서드.
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeChainInterrogatorPromptBuilder.java` — `InterrogationResult` 동일.
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeWrapUpPromptBuilder.java` — `WrapUpResult` 동일.
- `backend/src/main/resources/prompts/template/resume/resume-playground-opener.txt` — JSON schema 안 `model_answer` 필드 + 가이드 텍스트 형태 (RESUME=GUIDE 성격, 50-200자).
- `backend/src/main/resources/prompts/template/resume/resume-playground-responder.txt` — 동일.
- `backend/src/main/resources/prompts/template/resume/resume-chain-interrogator.txt` — 동일.
- `backend/src/main/resources/prompts/template/resume/resume-wrap-up.txt` — 동일.
- `backend/src/test/java/com/rehearse/api/infra/ai/prompt/PlaygroundOpenerResultTest.java` — 신규. `withModelAnswer` 호출 시 modelAnswer 만 교체 + 다른 필드 보존.
- `backend/src/test/java/com/rehearse/api/infra/ai/prompt/PlaygroundResponderResultTest.java` — 신규. 동일.
- `backend/src/test/java/com/rehearse/api/infra/ai/prompt/InterrogationResultTest.java` — 신규. 동일.
- `backend/src/test/java/com/rehearse/api/infra/ai/prompt/WrapUpResultTest.java` — 신규. 동일.

### 핵심 로직 / 변경 요약

```java
public record InterrogationResult(
        String question, String ttsQuestion, String reason,
        String nextAction, String chainAction,
        String modelAnswer  // 추가
) {
    public InterrogationResult withModelAnswer(String newModelAnswer) {
        return new InterrogationResult(question, ttsQuestion, reason,
                nextAction, chainAction, newModelAnswer);
    }
}
```

prompt template JSON schema 예시:
```json
{
  "question": "...",
  "tts_question": "...",
  "reason": "...",
  "model_answer": "<답변 가이드라인 (50-200자, 정답 X)>"
}
```

### 의존

- 선행 phase: Phase 1 (Question entity 변경 완료)
- 외부 의존: Jackson `@JsonProperty`

### Verification Hook

- 명령: `./gradlew test --tests "*ResultTest"`
- 통과 기준: 4 record `withModelAnswer` 단위 테스트 green. 다른 필드 보존 단언 통과.
- 관찰 가능 동작: prompt template 4개 파일 `model_answer` 키 존재. record 컴파일 통과.

### 커밋 메시지 (예상)

```
feat(BE): RESUME prompt result record modelAnswer + template schema 추가
```

---

## Phase 3: ResumeQuestionResultGenerator + Fallback 신규

- **구현**: `backend` — 검증 / 1회 retry / 폴백 책임 단일 컴포넌트. modeHandler 4곳 중복 제거.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeQuestionResultGenerator.java` — 신규. `@Component` public. 4 메서드 (`generateOpener`, `generatePlaygroundResponder`, `generateInterrogation`, `generateWrapUp`). promptBuilder 호출 + modelAnswer blank 검증 + 1회 retry + 폴백 적용 (`result.withModelAnswer(ResumeFallbackModelAnswers.<MODE>)`) + `log.warn`.
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeFallbackModelAnswers.java` — 신규. package-private final. 모드별 폴백 텍스트 상수 (`OPENER`, `PLAYGROUND`, `INTERROGATION`, `WRAP_UP`). `ResumeFallbackQuestions` 패턴 일치.
- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumeQuestionResultGeneratorTest.java` — 신규. Service Integration. 4 메서드 × 3 시나리오 (정상 / 1차 blank+2차 OK / 2회 blank → 폴백). Mock 정책: `ResilientAiClient` Mock 만, `promptBuilder` 실제 주입.

### 핵심 로직 / 변경 요약

```java
public InterrogationResult generateInterrogation(...) {
    InterrogationResult result = chainInterrogatorBuilder.build(...);
    if (isBlank(result.modelAnswer())) {
        result = chainInterrogatorBuilder.build(...);  // 1회 retry
        if (isBlank(result.modelAnswer())) {
            log.warn("[ResumeQuestionResultGenerator] modelAnswer 폴백 적용: interviewId={}, mode={}",
                    interviewId, "INTERROGATION");
            result = result.withModelAnswer(ResumeFallbackModelAnswers.INTERROGATION);
        }
    }
    return result;
}
```

4 메서드 동일 패턴. 모드별 promptBuilder + 폴백 상수만 다름.

**Interrogation 동시성** (tech-spec P1-5): retry 처리는 Phase 2 (lock 밖) 안에서만. tracker lock 점유 시간 무변경.

### 의존

- 선행 phase: Phase 2 (record `withModelAnswer` 메서드 존재)
- 외부 의존: 기존 promptBuilder 3종 (`ResumePlaygroundPromptBuilder`, `ResumeChainInterrogatorPromptBuilder`, `ResumeWrapUpPromptBuilder`)

### Verification Hook

- 명령: `./gradlew test --tests "ResumeQuestionResultGeneratorTest"`
- 통과 기준: 4 메서드 × 3 시나리오 = 12 케이스 green. 폴백 적용 시 `log.warn` 발생 단언 통과.
- 관찰 가능 동작: Generator 단독 호출 가능 (modeHandler 미연결 상태에서도 단위 검증 완료).

### 커밋 메시지 (예상)

```
feat(BE): ResumeQuestionResultGenerator + Fallback 추가
```

---

## Phase 4: Persister 6-인자 + 4 ModeHandler 통합

- **구현**: `backend` — Persister 시그니처 확장 + 4 modeHandler `Generator` 위임. 컴파일 깨짐 해소 + 통합 테스트.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeQuestionPersister.java` — `persist()` 6-인자 (`ttsText`, `modelAnswer` 추가). `Question.resume()` 6-인자 위임.
- `backend/src/main/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandler.java` — `generator.generateOpener(...)` / `generator.generatePlaygroundResponder(...)` 호출 변경. `persist()` 6-인자 호출. 기존 검증 인라인 제거.
- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java` — `generator.generateInterrogation(...)` 호출. Phase 2 (lock 밖) 안에서 호출. Phase 3 (lock 안) `persist()` 6-인자.
- `backend/src/main/java/com/rehearse/api/domain/resume/service/WrapUpModeHandler.java` — `generator.generateWrapUp(...)` 호출 + `persist()` 6-인자.
- `backend/src/test/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandlerIntegrationTest.java` — 신규/갱신. Service Integration. provider 호출 → persist 호출 → DB row 검증 (`tts_text` length ≥ 10, `model_answer` not blank, `reference_type` / `feedback_perspective` = NULL).
- `backend/src/test/java/com/rehearse/api/domain/resume/service/InterrogationModeHandlerIntegrationTest.java` — 동일.
- `backend/src/test/java/com/rehearse/api/domain/resume/service/WrapUpModeHandlerIntegrationTest.java` — 동일.

### 핵심 로직 / 변경 요약

```java
// InterrogationModeHandler (예시)
return tracker.withLock(() -> {
    Long questionId = questionPersister.persist(
            interviewId, QuestionType.RESUME_INTERROGATION,
            result.question(), result.ttsQuestion(), result.modelAnswer(),
            snapshot.orderIndex());
    applyDecision(...);
    return new InterrogationTurnResult(buildResponse(result, ...), questionId);
});
```

기존 `result.question() blank` 검증 / 폴백 식별 로직은 Generator 책임 이관 (modeHandler 잔존 X — `ResumeFallbackQuestions.INTERROGATION.equals(...)` 비교 로직은 Generator 안에서 처리하거나 modeHandler 유지 — 본 Phase 에서 위치 확정).

### 의존

- 선행 phase: Phase 3 (Generator 존재)
- 외부 의존: Testcontainers MySQL (Service Integration)

### Verification Hook

- 명령:
  - `./gradlew test --tests "ResumeQuestionPersisterTest"`
  - `./gradlew test --tests "*ModeHandlerIntegrationTest"`
  - `./gradlew build` (전체 컴파일 + 테스트)
- 통과 기준: 모두 green. STANDARD 트랙 회귀 테스트 통과.
- 관찰 가능 동작: RESUME 트랙 인터뷰 1회 (4 모드 모두 트리거) → DB 직접 조회:
  ```sql
  SELECT question_type, tts_text, model_answer, reference_type, feedback_perspective
  FROM question
  WHERE question_set_id = (최근 RESUME_BASED set);
  ```
  → tts_text / model_answer 적재. reference_type / feedback_perspective = NULL.

### 커밋 메시지 (예상)

```
refactor(BE): ResumeModeHandler Generator 위임 + Persister 6-인자
```

---

## 통합 Verification

- [ ] tech-spec.md Verification 항목 모두 통과
- [ ] 추가 회귀 체크: 없음 (tech-spec 회귀 항목 = STANDARD 트랙 `QuestionSetAssembler` 4개 필드 적재 변경 0건. Phase 4 통합 테스트 포함)

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] 지정 리뷰어 실행 (구현 완료 직후 — 메인 세션 책임)
  - BE only → `code-reviewer-backend`
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md "Pre / Post State" 섹션)
