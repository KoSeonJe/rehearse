# Implement — Resume 4-layer 컨텍스트 결함 (P0 + P1)

> **작성자**: backend agent
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: 단일 영역 (BE only). 5건 모두 BE 코드 / 메트릭.
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★
> **관련 plan**: `docs/plans/410-resume-context-defects/tech-spec.md`
> **관련 Issue**: #410

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 PR | 의존 | 커밋 |
|-------|------|--------------|--------|------|------|
| 1 | AiErrorCode 신규 + log.warn 인프라 정비 | `backend` | PR #1 | - | `feat(BE): CONTEXT_BUDGET_EXCEEDED ErrorCode 추가` |
| 2 | L4 FocusLayer P0 graceful (cap truncate + 미등록 callType) | `backend` | PR #1 | Phase 1 | `fix(BE): L4 cap 초과 / 미등록 callType graceful 처리` |
| 3 | L3 DialogueHistoryLayer raw fallback (P1-1) | `backend` | PR #2 | - | `fix(BE): L3 압축 미완료 시 raw fallback 추가` |
| 4 | InterviewContextBuilder + Orchestrator 전체 cap graceful 종료 (P1-2) + 메트릭 1종 | `backend` | PR #2 | Phase 1 | `fix(BE): 전체 컨텍스트 cap 초과 시 면접 graceful 종료` |
| 5 | InterrogationModeHandler 3-phase lock 분리 (P1-3) | `backend` | PR #2 | - | `refactor(BE): Interrogation 핸들러 lock 경계 chain state 변경 구간만으로 축소` |
| 6 | TODO 부채 정리 + 통합 검증 | `backend` | PR #2 | Phase 3 | `chore(BE): DialogueCompactor sync fallback TODO 정리` |

> Task 6개. 단일 implement.md (분리 임계 미초과).

---

## Phase 1: AiErrorCode 신규 + log.warn 인프라 정비

- **구현**: `backend` — `AiErrorCode` enum 에 `CONTEXT_BUDGET_EXCEEDED` 추가. ContextEngineeringMetrics 에 `incrementTokensExceeded(String callType)` 메서드 추가.

### 변경 파일
- `backend/src/main/java/com/rehearse/api/infra/ai/exception/AiErrorCode.java` — `CONTEXT_BUDGET_EXCEEDED` enum 추가 (BusinessException 용)
- `backend/src/main/java/com/rehearse/api/infra/ai/context/metrics/ContextEngineeringMetrics.java` — `incrementTokensExceeded(callType)` 메서드 추가 (`rehearse.ai.context.tokens.exceeded` Micrometer Counter, callType 태그)

### 핵심 로직 / 변경 요약
- AiErrorCode 기존 enum 패턴 그대로 (`{DOMAIN}_{3자리}` — `AI_006` 등 자동 부여, conventions.md 준수)
- ContextEngineeringMetrics 기존 패턴 따라 `Counter.increment(Tags.of("callType", callType))`

### 의존
- 선행 phase: 없음
- 외부 의존: Micrometer (이미 의존성 존재)

### Verification Hook
- 명령: `./gradlew compileJava`
- 통과 기준: 컴파일 통과
- 관찰 가능 동작: 신규 ErrorCode + 메서드 사용처 (Phase 4) 에서 컴파일 정상

### 커밋 메시지 (예상)
```
feat(BE): CONTEXT_BUDGET_EXCEEDED ErrorCode 추가
```

---

## Phase 2: L4 FocusLayer P0 graceful 처리

- **구현**: `backend` — `FocusLayer.render()` cap 초과 시 본문 절단 + `handleEmpty()` 미등록 callType graceful 폴백. 5xx 차단.

### 변경 파일
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java` — `render()` cap 초과 throw 제거 후 본문 절단 (지시문 보존, cap × 0.9 안전 마진). `handleEmpty()` 미등록 callType throw 제거 후 log.warn + `List.of()` 반환
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java` — `truncateBodyWithSafetyMargin(fragment, cap, callType)` private 메서드 추가
- `backend/src/test/java/com/rehearse/api/infra/ai/context/layer/FocusLayerTest.java` — 신규 / 보강 (cap 초과 절단 / 미등록 callType / 9종 callType 회귀)

### 핵심 로직 / 변경 요약
```java
// render
if (estimated > cap) {
    log.warn("[FocusLayer] L4 cap 초과 → 본문 절단: callType={}, estimated={}, cap={}",
            callType, estimated, cap);
    fragment = truncateBodyWithSafetyMargin(fragment, cap, callType);
}
return List.of(ChatMessage.of(USER, fragment));

// truncateBodyWithSafetyMargin
// 1. 본문 marker 식별 (USER_ANSWER / CURRENT_CHAIN 등)
// 2. 본문 영역 char 단위 절단
// 3. 절단 후 토큰 재추정 = TokenEstimator.estimate (cap × 0.9 이내까지 반복 절단)
// 4. 지시문 끝부분 (마지막 줄 "위 ... JSON 한 객체로만 응답하세요") 보존

// handleEmpty
if ("compaction_summarizer".equals(callType)) return List.of();
log.warn("[FocusLayer] L4 미등록 callType 진입: callType={}", callType);
return List.of();
```

### 의존
- 선행 phase: Phase 1 (별도 의존 없으나 동일 PR)
- 외부 의존: TokenEstimator (기존)

### Verification Hook
- 명령: `./gradlew test --tests "FocusLayerTest"`
- 통과 기준: cap 초과 본문 절단 + 정상 반환 / 미등록 callType empty 반환 / 9종 callType 회귀 통과
- 관찰 가능 동작: `WARN [FocusLayer] L4 cap 초과 → 본문 절단: ...` 로그 출력

### 커밋 메시지 (예상)
```
fix(BE): L4 cap 초과 / 미등록 callType graceful 처리
```

---

## Phase 3: L3 DialogueHistoryLayer raw fallback (P1-1)

- **구현**: `backend` — `buildWithCompaction()` 분기에서 runtimeState null / 압축 in-flight / summary 부재 모두 olderTurns raw 포함 + log.warn (reason 별도).

### 변경 파일
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/DialogueHistoryLayer.java` — `buildWithCompaction` runtimeState null 분기 추가 + 압축 in-flight / summary 부재 분기에 raw fallback 추가
- `backend/src/test/java/com/rehearse/api/infra/ai/context/layer/DialogueHistoryLayerTest.java` — 신규 / 보강 (4 분기 + 회귀)

### 핵심 로직 / 변경 요약
```java
if (runtimeState == null) {
    log.warn("[DialogueHistoryLayer] L3 raw fallback 발동: interviewId={}, windowEnd={}, reason=null_runtime_state", ...);
    result.addAll(renderAlternating(olderTurns));
} else {
    runtimeState.getCompactedSummary(windowEnd).ifPresentOrElse(
        summary -> result.add(...),
        () -> {
            String reason = runtimeState.hasCompactionInFlight(windowEnd)
                ? "compaction_in_flight" : "summary_absent";
            log.warn("[DialogueHistoryLayer] L3 raw fallback 발동: ..., reason={}", ..., reason);
            result.addAll(renderAlternating(olderTurns));
            triggerCompactionIfPossible(...);
        }
    );
}
result.addAll(renderAlternating(recentTurns));
```

### 의존
- 선행 phase: 없음 (Phase 1 / 2 와 독립)
- 외부 의존: `InterviewRuntimeState.hasCompactionInFlight(windowEnd)` 메서드 (존재 확인 필요. 부재 시 본 phase 에서 추가)

### Verification Hook
- 명령: `./gradlew test --tests "DialogueHistoryLayerTest"`
- 통과 기준: runtimeState null / 압축 in-flight / summary 부재 / summary 존재 / window 미초과 5분기 모두 통과
- 관찰 가능 동작: `WARN [DialogueHistoryLayer] L3 raw fallback 발동: ..., reason=...` 로그

### 커밋 메시지 (예상)
```
fix(BE): L3 압축 미완료 시 raw fallback 추가
```

---

## Phase 4: InterviewContextBuilder + Orchestrator 전체 cap graceful 종료 (P1-2)

- **구현**: `backend` — `InterviewContextBuilder.build()` cap 초과 시 throw + 메트릭. `ResumeInterviewOrchestrator.processFollowUp()` 단일 catch + `contextBudgetExceededResponse()` 응답.

### 변경 파일
- `backend/src/main/java/com/rehearse/api/infra/ai/context/InterviewContextBuilder.java` — total > maxContextTokens 시 log.warn + `incrementTokensExceeded(callType)` + `throw new BusinessException(AiErrorCode.CONTEXT_BUDGET_EXCEEDED)`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java` — `processFollowUp` try-catch 추가 + `contextBudgetExceededResponse()` 메서드 추가 (followUpExhausted=true, skip=true, presentToUser=false, type="CONTEXT_BUDGET_EXCEEDED")
- `backend/src/test/java/com/rehearse/api/infra/ai/context/InterviewContextBuilderIntegrationTest.java` — 신규 / 보강 (cap 초과 throw + 메트릭 검증)
- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestratorIntegrationTest.java` — cap 초과 시 contextBudgetExceededResponse 200 응답 검증

### 핵심 로직 / 변경 요약
```java
// InterviewContextBuilder.build
if (total > properties.maxContextTokens()) {
    log.warn("[InterviewContextBuilder] 전체 cap 초과 → graceful 종료: callType={}, total={}, max={}",
            req.callType(), total, properties.maxContextTokens());
    contextMetrics.incrementTokensExceeded(req.callType());
    throw new BusinessException(AiErrorCode.CONTEXT_BUDGET_EXCEEDED);
}

// ResumeInterviewOrchestrator.processFollowUp (해당 진입점)
try {
    // 기존 turn 핸들러 호출
} catch (BusinessException e) {
    if (e.getErrorCode() == AiErrorCode.CONTEXT_BUDGET_EXCEEDED) {
        return contextBudgetExceededResponse();
    }
    throw e;
}

private FollowUpResponse contextBudgetExceededResponse() {
    return FollowUpResponse.builder()
        .followUpExhausted(true).skip(true).presentToUser(false)
        .type("CONTEXT_BUDGET_EXCEEDED").build();
}
```

### 의존
- 선행 phase: Phase 1 (`AiErrorCode.CONTEXT_BUDGET_EXCEEDED` + `incrementTokensExceeded`)
- 외부 의존: 없음

### Verification Hook
- 명령: `./gradlew test --tests "InterviewContextBuilderIntegrationTest" --tests "ResumeInterviewOrchestratorIntegrationTest"`
- 통과 기준: cap 초과 → BusinessException throw + 메트릭 increment / Orchestrator catch → 200 + type=CONTEXT_BUDGET_EXCEEDED
- 관찰 가능 동작: dev `/actuator/metrics/rehearse.ai.context.tokens.exceeded` 노출 + WARN 로그

### 커밋 메시지 (예상)
```
fix(BE): 전체 컨텍스트 cap 초과 시 면접 graceful 종료
```

---

## Phase 5: InterrogationModeHandler 3-phase lock 분리 (P1-3)

- **구현**: `backend` — `InterrogationModeHandler.handle` 을 3-phase 로 분리. Phase 1 (chain state 진입 + ChainSnapshot 캡처) lock 안 / Phase 2 (LLM + 검증 + DB persist) lock 밖 / Phase 3 (applyDecision) lock 안.

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java` — 3-phase 분리 + `ChainSnapshot` record 신규 (6 필드: chainTopic, currentLevel, consecutiveStay, currentProjectId, orderIndex, answerQuality)
- `backend/src/test/java/com/rehearse/api/domain/resume/service/InterrogationModeHandlerIntegrationTest.java` — 단일 turn 회귀 / chain 소진 회귀 / lock 경계 검증 (CountDownLatch 로 Phase 2 동안 다른 thread `withLock` 1초 이내 진입) / RESPONSE_INVALID Phase 2 lock 밖 throw 검증

### 핵심 로직 / 변경 요약
```java
// Phase 1 (lock 안)
Optional<ChainSnapshot> snapshotOpt = tracker.withLock(() -> {
    if (!tracker.hasActiveChain()) {
        Optional<ChainReference> nextChain = tracker.resolveNextChain(plan.projectPlans());
        if (nextChain.isEmpty()) return Optional.<ChainSnapshot>empty();
        tracker.initChain(...);
    }
    int orderIndex = state.nextResumeOrderIndex();    // race 방지
    int answerQuality = analysis != null ? analysis.answerQuality() : 2;
    return Optional.of(new ChainSnapshot(
        tracker.getCurrentChainId(), tracker.getCurrentLevel(),
        tracker.getConsecutiveLevelStayCount(), tracker.getCurrentProjectId(),
        orderIndex, answerQuality));
});
if (snapshotOpt.isEmpty()) return new InterrogationTurnResult(buildExhaustedResponse(), null);
ChainSnapshot snapshot = snapshotOpt.get();

// Phase 2 (lock 밖)
InterrogationResult result = promptBuilder.build(... snapshot ...);
if (result.question() == null || result.question().isBlank())
    throw new BusinessException(AiErrorCode.RESPONSE_INVALID);
if (ResumeFallbackQuestions.INTERROGATION.equals(result.question()))
    log.warn("[InterrogationHandler] 안전 폴백 사용 감지: ...");
Long questionId = questionPersister.persist(...);

// Phase 3 (lock 안)
return tracker.withLock(() -> {
    applyDecision(tracker, result, snapshot.answerQuality(), snapshot.currentLevel());
    return new InterrogationTurnResult(buildResponse(result, tracker.getCurrentLevel()), questionId);
});
```

### 의존
- 선행 phase: 없음 (Phase 3 / 4 와 독립)
- 외부 의존: 없음

### Verification Hook
- 명령: `./gradlew test --tests "InterrogationModeHandlerIntegrationTest"`
- 통과 기준: 단일 turn 정상 / chain 소진 회귀 / Phase 2 동안 다른 thread `withLock` 진입 가능 (`latch.await(1, SECONDS)` true) / RESPONSE_INVALID lock 밖 throw → tracker state 변경 없음
- 관찰 가능 동작: 통합 테스트 통과

### 커밋 메시지 (예상)
```
refactor(BE): Interrogation 핸들러 lock 경계 chain state 변경 구간만으로 축소
```

---

## Phase 6: TODO 부채 정리 + 통합 검증

- **구현**: `backend` — `DialogueCompactor.java:65` `// TODO when sync fallback added (deferred from Task 3)` 주석 제거 (P1-1 raw fallback 으로 의도 충족). 전체 회귀 / 빌드 / 메트릭 노출 확인.

### 변경 파일
- `backend/src/main/java/com/rehearse/api/infra/ai/context/compaction/DialogueCompactor.java` — TODO 주석 제거 (라인 65)

### 핵심 로직 / 변경 요약
- L3 raw fallback (Phase 3) 도입으로 TODO 의도 (압축 미완료 시 옛 turn 누락 차단) 충족
- 회귀 테스트: `./gradlew test`

### 의존
- 선행 phase: Phase 3 (raw fallback 도입 후)
- 외부 의존: 없음

### Verification Hook
- 명령: `./gradlew test`
- 통과 기준: 전체 테스트 통과
- 관찰 가능 동작: tech-spec.md "관찰 가능 동작" 5개 항목 dev 환경에서 확인

### 커밋 메시지 (예상)
```
chore(BE): DialogueCompactor sync fallback TODO 정리
```

---

## 통합 Verification

tech-spec.md Verification 섹션 모두 통과.

- [ ] tech-spec.md Verification 항목 (Domain Unit + Service Integration + 빌드 + 관찰 가능 동작 + 회귀) 모두 통과
- [ ] PR 분할 권장: PR #1 = Phase 1 + 2 (P0 5xx 차단), PR #2 = Phase 3 + 4 + 5 + 6 (P1 + 정리)
- [ ] PR #2 = PR #1 머지 후 develop 동기화 → conflict 해소

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] 지정 리뷰어 실행 (구현 완료 직후 — 메인 세션 책임)
  - BE only → `code-reviewer-backend`
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `backend/.claude/rules/testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md Pre/Post 섹션 기준)
