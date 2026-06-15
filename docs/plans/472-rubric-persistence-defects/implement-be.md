# Implement (Backend) — rubric 채점 결과 적재 결함 정합화

> **작성자**: backend agent
> **답하는 질문**: BE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★

---

## Phase 0: API Contract 확인

본 plan = API contract **변경 없음**. tech-spec.md `## API Contract` 명시 ("BE/FE contract 변경 없음").

- [x] Endpoint 경로 / 메서드 = 변경 없음
- [x] Request / Response schema = 변경 없음 (`TimestampFeedbackResponse.questionType` 이미 노출됨)
- [x] Error 코드 매핑 = 변경 없음

→ FE 와 contract 합의 별도 게이트 불필요. BE/FE 병렬 진행.

---

## Phase / Step 개요

| Phase | 제목 | 구현 | 예상 PR | 의존 |
|-------|------|------|--------|------|
| 1 | INTERROGATION mode publish 정합 (`TurnHandlerResult.effectiveMode`) | `backend` | PR-A | Phase 0 |
| 2 | Nonverbal 적재 가시성 보강 (`NonverbalScorePersister` 2분기 로그) | `backend` | PR-B | 없음 (Phase 1 병렬 가능) |
| 3 | verbal rubric 정책 결정 문서 영속 (코드 변경 0) | `backend` | PR-B 동거 | Phase 2 |

> Task 3개 — 단일 파일 유지 (분리 임계 8 미달).

---

## Phase 1: INTERROGATION mode publish 정합

- **구현**: `backend` — PLAYGROUND→INTERROGATION 전환 turn 의 publish mode 정확화. `TurnHandlerResult` record 에 `effectiveMode` 필드 추가하고 handler 가 반환 책임.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java`
  - line 247 `TurnHandlerResult` record 에 `ResumeMode effectiveMode` 필드 추가
  - line 99-101 `dispatchByMode` 반환 결과에서 `effectiveMode` 사용
  - line 113-114 `turnEventPublisher.publish` 시 `currentMode` → `handlerResult.effectiveMode()` 전달
  - line 148-162 `dispatchByMode` switch 문 — PLAYGROUND case = `handlePlayground` 결과 그대로 / INTERROGATION case = `new TurnHandlerResult(r.response(), r.questionId(), ResumeMode.INTERROGATION)`
  - line 164-187 `handlePlayground`:
    - switch 미발생 케이스 = `new TurnHandlerResult(result.response(), result.questionId(), ResumeMode.PLAYGROUND)`
    - switch 발생 케이스 = `new TurnHandlerResult(interrogationResult.response(), interrogationResult.questionId(), ResumeMode.INTERROGATION)`

### 핵심 로직

```java
// before
record TurnHandlerResult(FollowUpResponse response, Long questionId) {}

// after
record TurnHandlerResult(FollowUpResponse response, Long questionId, ResumeMode effectiveMode) {}

// processUserTurnInternal line 113-114
turnEventPublisher.publish(
    interviewId, turnIndex, analysis,
    handlerResult.effectiveMode(),  // ← currentMode → effectiveMode
    currentChainLevel, skeleton, answerText, handlerResult.questionId());
```

### 의존

- 선행: Phase 0 (contract 합의 = 변경 없음 통과)
- 외부: 없음

### Verification

- `./gradlew test --tests "*ResumeInterviewOrchestrator*"`
- `./gradlew test --tests "*TurnHandlerResult*"`
- `./gradlew test --tests "*ResumeTurnEventPublisher*"`
- `./gradlew test --tests "*RubricScoring*"`
- 신규 테스트 (`backend/.claude/rules/testing.md` Service Integration):
  - `ResumeInterviewOrchestratorTest` — PLAYGROUND→INTERROGATION 전환 turn publish 인자 `resumeMode == INTERROGATION` ArgumentCaptor assert
  - 회귀 1: PLAYGROUND→PLAYGROUND 연속 turn → publish 인자 = PLAYGROUND
  - 회귀 2: INTERROGATION→INTERROGATION 연속 turn → publish 인자 = INTERROGATION
- 통과 기준: 위 4 명령 + 신규 테스트 모두 green

### 커밋 메시지

```
fix(BE): PLAYGROUND→INTERROGATION 전환 turn 의 publish mode 정확화
```

---

## Phase 2: Nonverbal 적재 가시성 보강

- **구현**: `backend` — `NonverbalScorePersister.persistOne` silent return 직전 2분기 분류 로그 추가. payload null = 정상 / 그 외 = 결함.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalScorePersister.java`
  - line 42-75 `persistOne(...)` 메서드 안 silent return (line 64) 직전 분기 로그 추가
  - 신규 private 메서드 `classifySkipReason(item, score)` 또는 인라인 분기

### 핵심 로직

```java
// persistOne 안 silent return 직전
if (!score.hasAnyScore()) {
    if (item.getNonverbalScore() == null) {
        log.info("[정상 skip] Nonverbal payloadNull. interviewId={}, questionId={}",
                interviewId, questionId);
    } else {
        log.warn("[결함 skip] Nonverbal scoreEmpty. interviewId={}, questionId={}, payload={}",
                interviewId, questionId, item.getNonverbalScore());
    }
    return;
}
```

> 참고: listener 의 `[결함 skip] Nonverbal exception` 은 기존 `RubricScoringEventListener:64` catch 가 이미 존재. 추가 변경 없음.

### 의존

- 선행: 없음 (Phase 1 과 병렬 가능)
- 외부: 없음

### Verification

- `./gradlew test --tests "NonverbalScorePersisterTest"`
- 신규 테스트 (Service Integration):
  - 시나리오 (a): payload 정상 → `question_score (rubric_id=nonverbal-v1)` row 적재 + 로그 마커 부재
  - 시나리오 (b): `item.nonverbalScore == null` → row 0 + `[정상 skip] Nonverbal payloadNull` 로그 (logback appender 또는 `OutputCaptureExtension` 검증)
  - 시나리오 (c): payload 있는데 scorer 결과 `hasAnyScore=false` → row 0 + `[결함 skip] Nonverbal scoreEmpty` 로그
- dev 채집 (가시성 머지 후): RESUME 인터뷰 3회 실행 → 4그룹 분포 (정상 적재 / payloadNull / scoreEmpty / listener exception) 표 형태로 기록
- 통과 기준: 3 시나리오 green + dev 채집 분포 기록

### 커밋 메시지

```
chore(BE): NonverbalScorePersister silent skip 분류 로그 추가
```

---

## Phase 3: verbal rubric 정책 결정 문서 영속

- **구현**: `backend` — 코드 변경 0. tech-spec `Trade-offs Phase 4 Option A` 채택 (verbal-v1 미도입 / Lambda raw 채널 유지) 가 plan 폴더에 영속됨을 확인.

### 변경 파일

- 없음 (코드 변경 0)
- 결정 영속 위치: `docs/plans/472-rubric-persistence-defects/tech-spec.md` `## Trade-offs ### Phase 4 verbal rubric 정책` 섹션 (이미 작성됨)

### 핵심 로직

- 결정: verbal-v1 rubric 미적용. Lambda `gemini_analyzer.py` 의 `fillerWords` / `speechPace` / `toneConfidenceLevel` raw 신호 = timestamp_feedback raw 채널 유지
- 후속 도입 필요 시 별도 product-spec 단위

### 의존

- 선행: Phase 2 (PR-B 동거 가능)
- 외부: 없음

### Verification

- [x] tech-spec.md 안에 결정 영속 (이미 완료)
- [ ] 본 implement-be.md commit 시 PR 본문에 "Phase 4 = verbal-v1 미도입 결정. tech-spec.md 참조" 1줄 명시

### 커밋 메시지

Phase 2 PR 동거 — 별도 commit 없음.

---

## FE 와 통합 시점

- 강결합 없음. BE/FE 병렬 진행.
- BE Phase 1 머지 직후 FE 측에 INTERROGATION 4차원 적재 동작 확인 알림 (Issue 댓글)
- BE Phase 2 머지 직후 dev 채집 결과 공유

## 통합 Verification

- [ ] tech-spec.md `## Verification` 통과
- [ ] FE Phase 통합 후 회귀 체크 (OPENER 화면 안내 카드 노출 확인)

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-backend` 실행 (구현 완료 직후 — 메인 세션 책임)
- [ ] BE+FE 동시 작업 → `code-reviewer-frontend` 와 **병렬** 호출 (단일 메시지 multiple tool_use)
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec `## Pre / Post State`)
