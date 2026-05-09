# Implement — Resume 면접 LLM prompt 맥락 정합성 회복 (#466 + #457)

> **작성자**: 구현 agent (Staff Engineer 분해)
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: 단일 영역 (BE only). FE / lambda 영향 0.
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★
> **범위**: tech-spec.md Phase 1 + Phase 2. Phase 3 = 별도 sub-spec.

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 PR | 의존 |
|-------|------|--------------|--------|------|
| 1 | 모드 전환 정합 (silent drop 차단 + empty() graceful + chain phrasing 정정) | `backend` | PR 1 | - |
| 2 | OPENER 정합 (planner 어휘 좁힘 + project_name 강제 + few-shot 보강) | `backend` | PR 2 | Phase 1 머지 |

> Phase 2 = prompt template only. Phase 1 머지 후 시작 (tech-spec 분기 결정 명시).
> 분리 임계 (Task 8개 / 단일 50줄+) 미초과 → 단일 `implement.md` 유지.

---

## Phase 1: 모드 전환 정합

- **구현**: `backend` — Orchestrator silent drop 차단 + InterrogationModeHandler 정책 단일 소스 정합 + chain-interrogator prompt 거짓 다리 phrasing 제거.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java`
  - `:177` 모드 전환 분기 — `interrogationHandler.handle(..., null, null, ...)` → `answerText` / `safeAnalysis` 실값 전달. `analysis == null` 도달 시 `AnswerAnalysis.empty(0L)` graceful fallback + `log.warn` (관찰성).
  - `:113` `turnEventPublisher.publish` 페이로드 = 무변경 (publish 인자 = 사용자 turn 분석. 본 변경 = handle 매개변수 전달만. listener 영향 0).

- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java`
  - `:27` `DEFAULT_ANSWER_QUALITY = 2` 상수 제거.
  - `:83` `int answerQuality = analysis.answerQuality();` 직접 사용 (analysis 항상 non-null invariant — 호출자 강제).

- `backend/src/main/resources/prompts/template/resume/resume-chain-interrogator.txt`
  - `:68` 거짓 다리 phrasing 예시 (`"방금 답변하신 [topic] 의 ~"`) 제거 + 가이드 보정: 직전 답변 내용 미참조 시 "방금 답변하신" 류 표현 금지.
  - `:81` 안전 폴백 일반화: `"현재 chain 의 추가 측면을 설명해주실 수 있을까요?"`.

- 신규 / 갱신 테스트:
  - `backend/src/test/java/.../resume/service/InterrogationModeHandlerTest.java` — Domain Unit. `analysis = empty()` 케이스 + `analysis = 정상값` 케이스 `answerQuality` 정확 전달.
  - `backend/src/test/java/.../resume/service/ResumeInterviewOrchestratorIntegrationTest.java` — Service Integration. PLAYGROUND→INTERROGATION 전환 시나리오. chain prompt builder 입력 캡처 → `answerText` / `analysis` 흘러감 검증. analysis null 케이스 = `ANSWER_QUALITY: 1` + 5xx 0 검증.
  - `backend/src/test/java/.../resume/eval/ResumeChainInterrogatorLiveEvalTest.java` — Live LLM eval fixture 5종 (PLAYGROUND ↔ chain pool topic disjoint) + analysis empty 1종. `@EnabledIfEnvironmentVariable(name="RUN_LIVE_API", matches="true")`. 자동 정규식 매칭.

### 핵심 로직 / 변경 요약

```java
// ResumeInterviewOrchestrator.handlePlayground (모드 전환 분기)
if (result.switchedToInterrogation()) {
    runtimeStateStore.update(...INTERROGATION);
    InterviewRuntimeState refreshed = runtimeStateStore.get(interviewId);

    AnswerAnalysis safeAnalysis = (analysis != null)
            ? analysis
            : AnswerAnalysis.empty(0L);
    if (analysis == null) {
        log.warn("[ResumeOrchestrator] analysis null on mode transition — empty() fallback. interviewId={}",
                 interviewId);
    }

    interrogationHandler.handle(interviewId, refreshed,
        answerText, safeAnalysis,
        plan, previousExchanges);
}
```

```java
// InterrogationModeHandler — DEFAULT_ANSWER_QUALITY 제거 후
int answerQuality = analysis.answerQuality(); // analysis 항상 non-null
```

```
# resume-chain-interrogator.txt:68 (After)
PROJECT_NAME 비어 있을 때 CURRENT_CHAIN.topic 만으로 질문 구성.
직전 답변 내용을 실제로 참조하지 않은 경우 "방금 답변하신" 류 표현 금지.

# :81 (After)
현재 chain 의 추가 측면을 설명해주실 수 있을까요?
```

### 의존

- 선행 phase: 없음.
- 외부 의존: `AnswerAnalysis.empty(turnId)` factory (`:49-50`) 단일 소스. `FocusLayer:81` 가 `ANSWER_QUALITY=1` 흘림 — 기존 정책 그대로.
- **Pre-implement grep 의무 (tech-spec Verification 강제)**:
  - `TurnAnalysisPipeline.analyze` → `AnswerAnalyzer.analyze:76-89` → `aiResponseParser.parseOrRetry` 반환값 null 가능성 path:line 단위 확인.
  - 결과 PR 본문 첨부. null 가능 시 = Orchestrator 측 fallback 코드 그대로 유지. null 불가 시 = 1줄 방어 코드 (invariant).

### Verification Hook

- 명령:
  ```bash
  ./gradlew test --tests "InterrogationModeHandlerTest" \
                 --tests "ResumeInterviewOrchestratorIntegrationTest"
  RUN_LIVE_API=true ./gradlew test --tests "ResumeChainInterrogatorLiveEvalTest"
  ```
- 통과 기준:
  - 단위 / 통합 = 모든 케이스 green.
  - Live eval fixture 5종 = chain 첫 질문 출력에 `방금\s*답변\s*하신` / `방금\s*말씀하신` 0건. PLAYGROUND turn 답변 명사 1+ AND chain topic 1+ 포함 (AND).
  - Live eval analysis empty 1종 = 명료화 정규식 `(다시|구체적으로|좀 더 자세히|어떤 의미)` 매칭 1+.
  - 회귀: 기존 `ResumeInterviewOrchestratorIntegrationTest` 정상 흐름 통과.
- 관찰 가능 동작: docker log grep `empty\(\) fallback` → fallback 발동 횟수 파악.

### 커밋 메시지 (예상)

```
fix(BE): 모드 전환 시 직전 답변 silent drop 차단 + empty() graceful fallback
```

추가 (분할 시):

```
fix(BE): chain-interrogator prompt 거짓 다리 phrasing 제거
test(BE): Resume chain interrogator Live eval fixture 5종 추가
```

---

## Phase 2: OPENER 정합

- **구현**: `backend` — planner prompt template 어휘 좁힘 + project_name 강제 + few-shot 보강.

### 변경 파일

- `backend/src/main/resources/prompts/template/resume/resume-interview-planner.txt`
  - `:34` 허용 어휘 좁힘:
    - Before: `"허용: '설명', '역할', '맡으셨', '흐름', '인상 깊은 / 어려웠던 경험' 류 intro / 감정·서사 oriented 어휘."`
    - After: `"허용: '설명', '역할', '맡으셨', '흐름' 류 intro 어휘. '인상 깊은 / 어려웠던 경험' 류 감정·서사 anchor 금지 (응시자 무관 진부 패턴 회귀 방지)."`
  - `:35` project_name 강제 + 1개 anchor 룰:
    - Before: `"project_name 명시된 경우 opener_question 자체에 자연스럽게 포함 ..."`
    - After: `"skeleton.projects[] 중 정확히 1개를 anchor (어느 거든 OK — 다양성 환영, 동일 skeleton 재호출 시 다른 프로젝트 선택 가능). 선택한 project_name 은 opener_question 에 반드시 명시. 누락 / 'this project' / '해당 프로젝트' / '프로젝트들' 지시·복수 표현 금지. 가공 명칭 / skeleton 외 명칭 생성 금지."`
  - `:88` few-shot 보강:
    - Before: `"{__example_name__} 프로젝트에 대해 설명해주시고, 어떤 역할을 맡으셨는지 설명해주세요."`
    - After: `"{__example_name__} 프로젝트에 대해 간단히 설명해주시고, 어떤 역할을 맡으셨는지 말씀해주세요."`

- 신규 / 갱신 테스트:
  - `backend/src/test/java/.../resume/service/PlaygroundModeHandlerTest.java` — `handleOpener` 정상 흐름 회귀.
  - `backend/src/test/java/.../resume/eval/ResumeInterviewPlannerLiveEvalTest.java` — Live LLM eval fixture 5종 (다른 프로젝트 skeleton) + 멤버십 1종 (동일 skeleton 2회 호출 → 양쪽 모두 skeleton 내 1개 anchor 검증, 동일성 X).

### 핵심 로직 / 변경 요약

prompt template only. 코드 변경 X. Live eval 자동 매칭:

- `project_name` 문자열 포함 (5/5)
- `(설명|소개|어떤\s*프로젝트|간단히)` 1+ (5/5) — planner.txt:34 허용 어휘 풀과 정합
- `(역할|맡으)` 1+ (5/5) — `담당` 어휘 풀 외 → 정규식 제외
- `(인상\s*깊|어려웠던|기억에\s*남)` 0건 (5/5)
- 멤버십: 동일 skeleton 2회 호출 → 양쪽 모두 `skeleton.projects[].name` 중 1개 anchor (동일성 X / 1개 anchor — 복수 프로젝트명 동시 등장 0건 assert)

### 의존

- 선행 phase: **Phase 1 PR 머지 완료 후 시작** (tech-spec 분기 결정 명시).
- 외부 의존: PR #463 (skeleton 메타 4종 추출) = 머지 완료 (커밋 30ed71e). project_name 입력 신호 토대 확보.
- token 영향 측정 권장: `python3 backend/eval/context/measure_tokens.py`.

### Verification Hook

- 명령:
  ```bash
  ./gradlew test --tests "PlaygroundModeHandlerTest" \
                 --tests "ResumeInterviewPlannerLiveEvalTest"
  RUN_LIVE_API=true ./gradlew test --tests "ResumeInterviewPlannerLiveEvalTest"
  ```
- 통과 기준:
  - 단위 / 통합 = 모든 케이스 green.
  - Live eval 5종 = 위 4가지 자동 매칭 모두 통과 (5/5).
  - 멤버십: 동일 skeleton 2회 호출 → 양쪽 모두 `skeleton.projects[].name` 중 1개 anchor (skeleton 내 존재 검증 + 1개 anchor / 복수 프로젝트명 동시 등장 0건). 동일성 assert X.
  - 회귀: 기존 `PlaygroundModeHandlerTest` 정상 흐름 통과.

### 커밋 메시지 (예상)

```
fix(BE): OPENER 프로젝트명 강제 + 진부 어휘 차단 (planner prompt)
```

추가 (분할 시):

```
test(BE): Resume planner Live eval fixture 5종 + 재현성 케이스 추가
```

---

## 통합 Verification

- [ ] tech-spec.md Verification 항목 모두 통과 (Phase 1 + Phase 2 + 공통)
- [ ] 추가 회귀 체크: `./gradlew build` 통과
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Pre-implement grep 결과 = PR 본문 첨부

## 후속 액션 (PR 1 머지 직후)

- [ ] **Phase 2 PR 즉시 진입** — PR 1 머지 ~ PR 2 머지 사이 OPENER 결함 잔존 시간 최소화 (tech-spec 위험 항목 M1 완화).
- [ ] **Phase 3 GitHub Issue 신규 생성** — 제목 예: `Resume fallback 5건 정합성 점검 (Phase 3)`. 본 plan handoff.md 에 Issue 번호 반영 (잊힘 위험 차단 / tech-spec 위험 항목 M2 완화).

---

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] 지정 리뷰어 실행 (구현 완료 직후 — 메인 세션 책임)
  - BE only → `code-reviewer-backend`
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `backend/.claude/rules/testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md "Pre / Post State" 섹션 기준)
