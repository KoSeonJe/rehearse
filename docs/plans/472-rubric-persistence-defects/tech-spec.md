# Tech Spec — rubric 채점 결과 적재 결함 정합화

> **작성자**: backend agent (Staff Engineer 페르소나)
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement-be.md / implement-fe.md 진입 ★

---

## Why → Goal (1줄 미러)

INTERROGATION 4차원 / 비언어 nonverbal-v1 / OPENER UX / verbal 정책 4축에서 rubric 채점 결과가 의도대로 적재·노출되도록 결함 분기를 fix 하고 운영 가시성을 보강한다.

## Evidence

- 현재 구조:
  - `backend/.../resume/service/ResumeInterviewOrchestrator.java:74-117` — `processUserTurnInternal` 진입 시 line 95 에서 `currentMode` 캡처 → line 99 `dispatchByMode` 안에서 `runtimeStateStore.update(transitionTo(INTERROGATION))` 가능 (`handlePlayground` line 173-174) → line 113 publish 시 캡처된 PLAYGROUND mode 로 전달.
  - `backend/.../resume/service/ResumeTurnEventPublisher.java:30-55` — `currentMode` 인자를 `TurnCompletedEvent.ofResumeTrack(...)` 에 그대로 매핑.
  - `backend/.../feedback/rubric/event/TurnCompletedEvent.java:9-21` — `resumeMode` 필드가 listener 까지 전달.
  - `backend/.../feedback/rubric/service/RubricScoringEventListener.java:33-67` — async AFTER_COMMIT listener. score.isEmpty 시 `[정상 skip]`, 예외 시 `[결함 skip]` 로 이미 구분 마커 존재.
  - `backend/.../feedback/rubric/service/RubricScorer.java:33-63` → `Rubric.selectDimensions(resumeMode)` → `resume-rubric.yaml:26-28` per_turn_rules 매핑. PLAYGROUND = 1차원 / INTERROGATION = 4차원.
  - `backend/.../feedback/service/QuestionSetFeedbackPersister.java:21-33` — `persist()` 마지막에 `nonverbalScorePersister.persistAll(...)` 호출. 호출 흐름 정상.
  - `backend/.../feedback/rubric/service/NonverbalScorePersister.java:42-75` — `nonverbalRubricScorer.score(item.getNonverbalScore(), category, track, mode, difficulty)` 결과 `hasAnyScore()=false` → silent return (line 64). 결함성/정상 구분 로그 부재.
  - `backend/.../feedback/rubric/service/NonverbalRubricScorer.java:21-36` — `score == null` 이면 `NonverbalTurnScore.empty()` 반환 = 모든 차원 null. weights resolve 결과 `composureEnabled() = false` 면 composure null 만 — fluency/confidenceTone/eyeContactPosture 가 score 에 있으면 적재됨.
  - `backend/.../feedback/dto/SaveFeedbackRequest.java` — `TimestampFeedbackItem.nonverbalScore` payload 매핑 (lambda → BE).
  - `backend/.../question/entity/QuestionType.java:10` — `RESUME_OPENER(GUIDE, EXPERIENCE)`. OPENER 카테고리 = EXPERIENCE 부여됨. null 도달 원인 ≠ 카테고리 부재.
  - **OPENER null 도달 진짜 경로** (재추적 결과): `PlaygroundModeHandler.handle` (line 92-99) 가 사용자 OPENER 답변 처리 시 **새 `RESUME_PLAYGROUND` question 생성** → `ResumeInterviewOrchestrator.processUserTurnInternal` line 113-114 publish 시 `handlerResult.questionId()` = 새 PLAYGROUND question id → `RubricScoringEventListener` 가 PLAYGROUND question 한테 채점 적재. **OPENER question 한테는 어떤 채점 listener 도 호출 안 함** → `question_score` row 미생성 → `TimestampFeedbackResponse.toTechnicalFeedback` (line 151) `questionScore == null` 분기 → `technicalFeedback = null` 반환 → FE 가 `FALLBACK_COPY` 노출.
  - `backend/.../feedback/dto/TimestampFeedbackResponse.java:24,125` — 응답 DTO 에 `questionType` 필드 이미 노출 (BE wire 변경 불필요). FE 분기 활용 가능.
  - `frontend/src/components/feedback/content-tab.tsx:3-4` — `ContentTab` props = `technicalFeedback` 만 수신. `questionType` 미수신 → 분기 불가 상태.
  - `frontend/src/components/feedback/content-tab.tsx:17-20` — `FALLBACK_COPY = { title: '기술 피드백', emptyMessage: '해당 턴은 평가 대상이 아닙니다.' }`. 현재 도달 케이스 = OPENER + 결함 turn 혼재.
  - `lambda/analysis/analyzers/gemini_analyzer.py` — Lambda 가 Gemini 통합 분석으로 `fillerWords` / `speechPace` / `toneConfidenceLevel` 등 verbal 신호 이미 출력. BE rubric 별도 도입 시 중복.
- 외부 레퍼런스:
  - `docs/plans/409-question-score-missing/tech-spec.md` — 일반 트랙 적재 누락 패턴 참고 (silent skip 가시성 / async listener).
- 사용자 발화:
  - Phase 4 = tech-spec 안에서 결정. BE+FE 분리. Goal 측정 = DB row 매칭 우선.
- 추정 / 미확인 가정:
  - Phase 2 root cause 후보 2개 — (a) Lambda payload 의 `nonverbalScore` 가 실제 항상 null 도착 (Lambda 책임 = 정상 skip), (b) scorer 가 payload 받았는데 결과 hasAnyScore=false 또는 예외 (BE 결함 의심). `NonverbalRubricScorer` weights 는 composure 1차원만 영향 → "weights all-disabled" 분류 분기 사실상 무의미 (다른 3차원이 항상 적재됨). dev 진단 절차로 (a)/(b) 비율 확정.
  - Phase 1 fix 가 INTERROGATION 첫 turn (= PLAYGROUND→INTERROGATION 전환 turn) 만 영향. PLAYGROUND→PLAYGROUND 연속 turn 및 INTERROGATION→INTERROGATION 연속 turn 은 정상 동작 (추정. 본 spec verification 으로 두 회귀 케이스 모두 확정).

## Trade-offs

### Phase 1 fix 방식

#### Option A (채택): handler 결과에 전이 후 mode 포함
- `TurnHandlerResult` 또는 `dispatchByMode` 반환값에 `effectiveMode` 추가. `handlePlayground` 가 `switchedToInterrogation` 케이스에서 `INTERROGATION` 반환. `processUserTurnInternal` 가 publish 시 effectiveMode 사용.
- 장점: 명시적. 추가 IO 0. 의도 코드로 표현됨.
- 단점: `TurnHandlerResult` record 확장 (signature 변경). 호출부 1곳만 영향이라 작음.
- 채택 사유: 모드 전이 책임을 handler 가 가지므로 handler 가 결과로 보고하는 것이 자연. 단일 turn 처리 흐름 내부에서 effectiveMode 가 method local 로 전달되어 캐시 재조회 (Option B) 의 transitionTo→재조회 사이 다른 코드 개입 가능성 0.

#### Option B (폐기): publish 직전 runtimeStateStore 재조회
- `currentMode = runtimeStateStore.get(interviewId).getResumeMode()` 를 publish 직전 한 번 더.
- 장점: 시그니처 변경 0. 한 줄 수정.
- 단점: 캐시 재조회 비용 + handler 와 publish 사이 다른 코드가 mode 변경 시 silently 영향. 의도 모호.
- 폐기 사유: 모드 전이 출처가 handler 라 명시적 반환이 더 정합.

### Phase 2 가시성 보강

#### Option A (채택): NonverbalScorePersister 에 2분기 분류 로그 추가
- silent return (line 64) 직전 2분기 로그:
  - `payload null` (입력 점수 자체 부재 = Lambda 미생성) → `log.info("[정상 skip] Nonverbal payloadNull ...")`
  - 그 외 (`scorer 결과 hasAnyScore=false` 인데 payload 는 있음) → `log.warn("[결함 skip] Nonverbal scoreEmpty ...")`
- listener 예외 catch 는 기존대로 `[결함 skip] Nonverbal exception` (별도 보강 X).
- 장점: 코드 정합 (weights 분기 무의미 = 제거). 운영자 동일 로그 grep 패턴 재사용. 결함성 분포 즉시 가시화.
- 단점: 로그만으론 운영 알람 자동화 불가 (별도 plan 영역).
- 채택 사유: 코드 실제 동작과 분류 분기 정합 + product-spec 비스코프 (메트릭 namespace 확장 별도) 정합.

#### Option B (폐기): 3분기 (payload null / weights all-disabled / 예외) 분류
- 폐기 사유: weights 가 4차원 중 composure 1개만 영향 → "weights all-disabled" 케이스 코드상 거의 발생 불가. 분기 추가해도 노이즈만 발생.

#### Option B (폐기): AiCallMetrics 분기 카운터 추가
- 폐기 사유: product-spec 명시 비스코프.

### Phase 3 OPENER UX

#### Option A (채택): FE ContentTab 에 questionType prop 추가 + OPENER 전용 분기
- `ContentTab` props 에 `questionType: string | null` 추가 (BE `TimestampFeedbackResponse.questionType` 이미 wire 노출 — contract 변경 0). 호출부 (timestamp feedback 화면) 에서 `questionType` 전달.
- 분기:
  - `questionType === 'RESUME_OPENER'` → `OPENER_COPY` 카드: title="안내", emptyMessage="이 단계는 채점 대상이 아닙니다.", 보조="면접 도입 단계 답변은 점수 채점에 사용되지 않습니다."
  - 그 외 + `technicalFeedback === null` → 기존 `FALLBACK_COPY` 유지 ("해당 턴은 평가 대상이 아닙니다") = **결함성 케이스 안내** 로 의미 재정의. 사용자 / 운영자 가 거짓 안내 받지 않음.
- 장점: BE contract 변경 0. OPENER (정상 미채점) vs 결함 turn (적재 실패) 안내 분리. P0-2 / P0-3 동시 해결.
- 단점: ContentTab props signature 1개 추가 + 호출부 1곳 수정. 회귀 테스트 추가.
- 채택 사유: 결함 turn 한테 거짓 안내 노출 차단 = product-spec AC 정합. BE contract 변경 비용 0 (필드 이미 노출). 변경 범위 최소.

#### Option B (폐기): FALLBACK_COPY 문구만 교체 (OPENER 가정 = 유일 null 도달)
- 폐기 사유: P0-2 / P0-3 재리뷰 결과 — null 도달 케이스 4개 (OPENER / score-empty turn / listener 결함 skip / 비-RESUME 미채점). 결함성 turn 한테 "채점 대상 아님" 거짓 안내 노출.

#### Option C (폐기): RubricCategory enum 에 OPENER 추가 + BE/FE contract 변경
- 폐기 사유: enum 신규값 = downstream 매핑 (rubric 도메인 / Lambda 분석 / score persister) 모두 영향. OPENER 가 채점 대상 아니라는 도메인 결정과 충돌 (RubricCategory 자체에 등록 = 채점 대상 시사).

### Phase 4 verbal rubric 정책

#### Option A (채택): verbal rubric 미적용. Lambda 분석 결과를 timestamp_feedback raw 로 노출 유지
- Lambda `gemini_analyzer.py` 가 이미 `fillerWords` / `speechPace` / `toneConfidenceLevel` 등 verbal 신호 출력 중. BE 가 별도 `verbal-v1` rubric 도입 시 (a) dimension 정의 신규 / (b) scorer 신규 (LLM 호출 추가 비용) / (c) Lambda 결과 해석 로직 / (d) level_expectations 설계 모두 필요.
- 장점: 신규 도메인 확장 0. Lambda 책임 분리 유지. product-spec 비스코프 (rubric 정의 자체 변경) 와 정합.
- 단점: verbal 결과가 dimension 단위 점수로 표현되지 않음 (raw 텍스트만). 미래 점수 기반 비교 / 강약점 분석 시 별도 작업 필요.
- 채택 사유: 본 Epic = 적재 결함 정합화. 신규 도메인 도입은 별도 product-spec 단위.

#### Option B (폐기): verbal-v1 rubric 신규 정의 + 도입
- 폐기 사유: dimension 정의 / scorer / weights / level mapping 신규 = product-spec 수준 결정. 본 Epic 비스코프 (항목 B / B-1).

## Architecture

### Phase 1 — INTERROGATION mode publish 정합

```
[Client] → [POST /resume turn]
   ↓
[ResumeInterviewOrchestrator.processUserTurnInternal]
   ├─ currentMode = runtimeStateStore.get().getResumeMode()  // PLAYGROUND
   ├─ TurnHandlerResult result = dispatchByMode(currentMode, ...)
   │     └─ handlePlayground:
   │           ├─ playgroundHandler.handle(...) → switchedToInterrogation=true
   │           ├─ runtimeStateStore.update(transitionTo(INTERROGATION))
   │           ├─ interrogationHandler.handle(...) → INTERROGATION question
   │           └─ return new TurnHandlerResult(response, qid, **effectiveMode=INTERROGATION**)
   │     └─ INTERROGATION case: effectiveMode = currentMode (그대로)
   └─ turnEventPublisher.publish(..., result.effectiveMode(), ...)  // INTERROGATION
            ↓
       [TurnCompletedEvent (resumeMode=INTERROGATION)] → AFTER_COMMIT async
            ↓
       [RubricScoringEventListener] → RubricScorer → Rubric.selectDimensions(INTERROGATION) = 4차원
            ↓
       [QuestionScorePersister.saveRubric] → question_score / question_score_dimension (4행)
```

변경 포인트:
- `TurnHandlerResult` record 에 `ResumeMode effectiveMode` 추가.
- `dispatchByMode` / `handlePlayground` / `handleInterrogation` 모두 effectiveMode 반환 책임.
- `processUserTurnInternal` line 113 publish 시 `result.effectiveMode()` 전달.

### Phase 2 — Nonverbal 적재 결함 진단 + 가시성

```
[QuestionSetFeedbackPersister.persist]
   ├─ feedbackRepository.save(feedback)
   └─ nonverbalScorePersister.persistAll(questionSet, saved, items)
         └─ for each (timestampFeedback, item):
               persistOne(...)
                  ├─ score = nonverbalRubricScorer.score(item.nonverbalScore, ...)
                  ├─ if (!score.hasAnyScore())
                  │     ├─ classifyAndLog(item)  // ★ 신규 (2분기)
                  │     │     ├─ item.nonverbalScore == null → log.info("[정상 skip] Nonverbal payloadNull")
                  │     │     └─ else                         → log.warn("[결함 skip] Nonverbal scoreEmpty")
                  │     └─ return
                  └─ questionScorePersister.saveNonverbal(...)
```

진단 절차 (implement 단계 — 가시성 로그 머지 후 dev 채집):
1. dev 환경에서 신규 RESUME 인터뷰 3회 실행 (비언어 분석 활성화 상태).
2. `[정상 skip] Nonverbal payloadNull` / `[결함 skip] Nonverbal scoreEmpty` / `[결함 skip] Nonverbal exception` (listener) / 정상 적재 4그룹 분포 확인.
3. payloadNull 다수 → Lambda 영역 (별도 plan 분기 필요).
4. scoreEmpty 또는 exception 다수 → BE 매핑 / scorer 결함 fix.
5. 정상 적재 다수 + skip 소수 → 적재 결함 종결 (가시성 PR 만 머지).

사용자 인지 증상: feedback 화면에서 비언어 점수 항목 (눈맞춤 / 떨림 / 침착함 등) 빈칸 또는 누락. 운영자 인지 증상: 위 로그 grep 분포.

진단 결과에 따라 후속 fix 범위가 (a) BE 매핑 (b) Lambda payload 정합 (c) scorer 결함 중 결정. 본 spec 의 코드 변경은 가시성 로그까지가 보장 범위. 진단 후 fix 는 implement-be.md 안에서 결정.

### Phase 3 — OPENER FE 분기 (questionType prop 활용)

```
[BE] TimestampFeedbackResponse { questionType, technicalFeedback, ... }   # questionType 이미 wire 노출
       ↓
[FE] feedback 화면 (timestamp-feedback 호출부)
   └─ ContentTab({ technicalFeedback, questionType })   # ★ questionType prop 추가
         ├─ if (questionType === 'RESUME_OPENER')
         │     → OPENER_COPY 카드 (안내 / 도입 단계 보조 카피)         # 정상 미채점
         ├─ else if (technicalFeedback 정상 = dimensions 존재)
         │     → 기존 dimension 카드 렌더
         └─ else (technicalFeedback === null + 비-OPENER)
               → 기존 FALLBACK_COPY 유지 ("해당 턴은 평가 대상이 아닙니다")  # 결함성 케이스 안내
```

변경:
- `ContentTab` props signature: `{ technicalFeedback }` → `{ technicalFeedback, questionType: string | null }`.
- 신규 상수 `OPENER_COPY = { title: '안내', emptyMessage: '이 단계는 채점 대상이 아닙니다.', secondary: '면접 도입 단계 답변은 점수 채점에 사용되지 않습니다.' }`.
- `questionType === 'RESUME_OPENER'` early return 분기 추가.
- 기존 `FALLBACK_COPY` 의미 재정의 — "결함성 적재 누락 안내" (문구 그대로 유지). OPENER 케이스 거짓 안내 차단.
- 호출부 (timestamp feedback 화면) 에서 `questionType` 전달.

BE 변경 없음. API contract 변경 없음 (필드 이미 노출).

### Phase 4 — verbal rubric 정책

코드 변경 없음. 결정 사항 본 spec 안에 명시 (`Trade-offs` Phase 4 Option A) — `verbal-v1` rubric 미도입. Lambda verbal 분석 결과는 기존 timestamp_feedback raw 채널 유지. 후속 도입 필요 시 별도 product-spec.

## Data Model

DB 스키마 변경 없음. 모든 변경 = 기존 스키마 위에서 적재 정합화.

## API Contract

BE/FE contract 변경 없음. Phase 3 = FE 단독 copy 변경. Phase 1/2/4 = BE 단독.

> AGENTS.md Section 5 (BE+FE 분리) 룰 — contract 합의 게이트 = "변경 없음" 으로 통과.

## Verification (완료 판정)

### Phase 1
- [ ] Service Integration: `ResumeInterviewOrchestratorTest` — PLAYGROUND→INTERROGATION 전환 turn 의 publish 인자 `resumeMode == INTERROGATION` assert. `TurnCompletedEvent` capture (Mockito ArgumentCaptor 또는 spy publisher). 협력자 (handler / runtimeStateStore) 는 실제 빈, AI 호출만 stub.
- [ ] Domain Unit: `TurnHandlerResultTest` — `effectiveMode` 필드가 `handlePlayground` switchedToInterrogation 케이스에서 INTERROGATION 으로 채워지는지 record 단위 assert.
- [ ] Service Integration: `RubricScoringFlowTest` — 실제 RESUME 인터뷰 시뮬레이션 (LLM 응답 stub) → INTERROGATION turn 후 `question_score_dimension` row 4건 (technical_depth / reasoning_communication / factual_consistency / chain_depth) DB assert. PLAYGROUND turn 은 1건 (experience_concreteness) assert.
- [ ] 회귀: 기존 `ResumeInterviewOrchestrator` 관련 테스트 + `TurnHandlerResult` / `ResumeTurnEventPublisher` / `TurnCompletedEvent` / `RubricScoringEventListener` 호출 체인 테스트 모두 통과. `./gradlew test --tests "*ResumeInterview*"` + `./gradlew test --tests "*Rubric*"` 명시 실행.
- [ ] 회귀 (PLAYGROUND→PLAYGROUND 연속): playgroundTurns < hardCap + switch 미발생 turn → publish 인자 `resumeMode == PLAYGROUND` assert. effectiveMode 가 currentMode 그대로 반환되는지 확인.
- [ ] 회귀 (INTERROGATION→INTERROGATION 연속): 두 번째 INTERROGATION turn 이후 publish 인자 `resumeMode == INTERROGATION` assert.

### Phase 2
- [ ] dev 채집: 신규 RESUME 인터뷰 최소 3회 실행 (각 인터뷰 5+ turn) → `[정상 skip] Nonverbal payloadNull` / `[결함 skip] Nonverbal scoreEmpty` / `[결함 skip] Nonverbal exception` / 정상 적재 4그룹 분포 집계. implement-be.md 진행 노트에 표 형태로 기록.
- [ ] Service Integration: `NonverbalScorePersisterTest` — 3 시나리오: (a) payload 정상 = `question_score (rubric_id=nonverbal-v1)` row 적재. (b) payload null = row 0 + `[정상 skip] payloadNull` 로그. (c) payload 있는데 scorer 결과 hasAnyScore=false = row 0 + `[결함 skip] scoreEmpty` 로그.
- [ ] 운영 검증 + 후속 분기: 동일 인터뷰 ID 로그 grep 시 정상/결함 skip 분리 노출 (AC-3). 결함 skip (`scoreEmpty` + listener `exception`) 비율 0% = Phase 2 종결 (가시성 PR 만 머지). 결함 skip > 0% 또는 payloadNull 비율 100% = root cause 판정 후 사용자에게 후속 fix 분기 (BE scorer fix / Lambda payload 정합 별도 plan) `AskUserQuestion` 으로 결정 요청.

### Phase 3
- [ ] FE Integration (Vitest + RTL): `ContentTab` 렌더 4 시나리오:
  - (a) `questionType='RESUME_OPENER'` + `technicalFeedback=null` → "안내" 카드 + "이 단계는 채점 대상이 아닙니다." + 보조 카피 "면접 도입 단계 답변은 점수 채점에 사용되지 않습니다." 3종 모두 노출 assert.
  - (b) `questionType='RESUME_PLAYGROUND'` + `technicalFeedback=null` → 기존 `FALLBACK_COPY` ("해당 턴은 평가 대상이 아닙니다") 노출 (결함성 안내 케이스).
  - (c) `questionType='RESUME_INTERROGATION'` + `technicalFeedback` 정상 (dimensions 있음) → 기존 dimension 카드 렌더 (회귀).
  - (d) `questionType='TECH_MAIN'` + `technicalFeedback` 정상 → 기존 TECHNICAL 카테고리 카드 렌더 (회귀).
- [ ] 수동 확인: dev 환경 RESUME 인터뷰 → OPENER turn 화면 = "안내" 카드 노출. 결함성 (PLAYGROUND turn 채점 미적재) 시뮬레이션 가능 시 = `FALLBACK_COPY` 노출. 스크린샷 2장 implement-fe.md 진행 노트에 첨부.

### Phase 4
- [ ] 결정 문서: 본 tech-spec `Trade-offs` Phase 4 Option A 채택이 plan 폴더 안에 영속 (코드 변경 X).
- [ ] AC-5 충족: 후속 세션이 본 결정 추적 가능.

### 공통
- [ ] 빌드: `./gradlew build` (BE), `npm run build` (FE).
- [ ] 린트: `npm run lint` (FE).

## Pre / Post State

### Pre (현재)
- `ResumeInterviewOrchestrator.processUserTurnInternal` line 113 publish 시 `currentMode = PLAYGROUND` 그대로 → INTERROGATION question 이 PLAYGROUND 룰로 채점 → 1차원 row 만 적재.
- `NonverbalScorePersister.persistOne` line 64 silent return — 정상/결함 skip 운영 구분 불가.
- FE `FALLBACK_COPY` 가 OPENER turn + 결함성 turn 모두에 동일 문구 노출 → OPENER 도입 단계 의도 전달 X + 결함성 케이스에 거짓 안내 위험.
- verbal rubric 정책 = 코드 부재 + 결정 문서 부재 → 후속 세션 cross-check 비용 ↑.

### Post (구현 후)
- `TurnHandlerResult` 가 `effectiveMode` 반환 → publish 시 INTERROGATION 정확 전달 → 4차원 row 적재.
- `NonverbalScorePersister.persistOne` 가 silent return 직전 `[정상 skip] / [결함 skip] Nonverbal` 분류 로그.
- FE `ContentTab` 가 `questionType` prop 분기 → OPENER = "안내" 카드 / 결함성 = `FALLBACK_COPY` 분리. 거짓 안내 차단.
- verbal rubric 정책 = "미적용 — Lambda raw 채널 유지" 결정이 본 tech-spec 에 영속.

## 위험 / 마이그레이션 / 롤백

- 위험:
  - Phase 1 — `TurnHandlerResult` record signature 변경 → 호출부 컴파일 영향 (단일 파일). 회귀 테스트로 보호.
  - Phase 2 — 진단 단계에서 root cause 가 Lambda payload 영역으로 좁혀질 경우, 본 Epic 코드 변경은 가시성 로그 추가까지가 보장 범위. Lambda 코드 진입은 본 spec 비스코프 → implement-be.md agent 는 자율 진입 금지. 사용자에게 `AskUserQuestion` 으로 분기 결정 요청 (옵션: (a) 별도 plan 폴더 신규 — Lambda payload schema 정합 / (b) 본 Epic 범위 안에서 BE 매핑 한 줄 fix 만 / (c) weights 정책 변경 별도 PR).
  - 동시성 — 동일 interviewId 의 turn 요청은 상위 (controller / orchestrator 진입 락 또는 단일 세션 직렬 호출 전제) 에서 직렬화 보장. 본 Phase 1 변경은 단일 turn 처리 컨텍스트 내부의 effectiveMode 전달이라 동시 turn racing 영향 없음. 동일 interviewId 동시 진입 보장은 본 spec 비스코프 (현 코드 전제 그대로 유지).
  - Phase 3 — `ContentTab` props signature 변경 (호출부 수정 필요). 호출부 누락 시 OPENER 분기 미동작 → `FALLBACK_COPY` 노출 (옛 동작 유지). FE Integration 테스트로 호출부 회귀 보호.
- 마이그레이션 전략: DB 변경 없음. 신규 인터뷰부터 적용. 과거 row backfill = product-spec 비스코프 (운영 SQL 분리).
- 롤백 시나리오:
  - Phase 1 — 코드 revert. data 영향 없음 (잘못 적재된 row 는 이미 존재 → backfill 비스코프).
  - Phase 2 — 로그 추가만. 롤백 = revert.
  - Phase 3 — `ContentTab` prop 추가 + 호출부 수정 revert (BE 영향 0).
  - Phase 4 — 결정 문서 revert.

## 분기 결정

- [x] BE+FE 동시 → `implement-be.md` + `implement-fe.md` (사용자 결정)
- BE 선행 강제 = X. Phase 3 FE = BE contract 무관 (FE-only). 병렬 진행 가능.
- `implement-be.md` 범위: Phase 1 (orchestrator + handler 결과 확장) + Phase 2 (NonverbalScorePersister 분류 로그 + 진단) + Phase 4 (결정 문서 영속, 코드 변경 X).
- Phase 2 진단 결과 root cause 가 Lambda 영역으로 판정 시 implement-be.md agent 는 가시성 로그 추가 PR 만 완료하고 사용자 분기 결정을 대기. Lambda 코드 변경은 본 implement 진입 금지.
- `implement-fe.md` 범위: Phase 3 (ContentTab questionType prop 추가 + OPENER 분기).
- PR 분리: BE Phase 1 / BE Phase 2 / FE Phase 3 = 3개 PR 권장 (논리 분리 + 리뷰 단위 작음). Phase 4 = BE Phase 1 또는 2 PR 에 결정 문서 commit 동거 가능 (코드 변경 0).
