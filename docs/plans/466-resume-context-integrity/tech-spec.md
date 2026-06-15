# Tech Spec — Resume 면접 LLM prompt 맥락 정합성 회복 (#466 + #457)

> **작성자**: backend agent (메인 세션 위임 전 초안)
> **답하는 질문**: 어떻게? 구조 / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

> **범위**: 본 tech-spec = Phase 1 (모드 전환 정합) + Phase 2 (OPENER 정합) **만** 다룬다.
> Phase 3 (fallback 5건 점검) = invariant 확인이 implement 단계 코드 분석에 의존 → 본 spec 머지 후 별도 mini sub-spec (`tech-spec-phase3.md`) 으로 분리. product-spec Goal Phase 3 우선순위는 그대로 유효.

---

## Why → Goal (1줄 미러)

Resume 면접에서 LLM 이 받는 입력의 가짜 컨텍스트 (모드 전환 시 직전 답변 단절 / 분석 결과 부재 시 평균값 메우기 / OPENER 프로젝트 무관 진부) 제거. 본 spec = Phase 1 (사용자 체감 즉시) + Phase 2 (OPENER 정합). Phase 3 = 별도 sub-spec.

---

## Evidence

### 현재 구조 (확인)

**Phase 1 영역**:
- `domain/resume/service/ResumeInterviewOrchestrator.java:177` — `handlePlayground` 가 `result.switchedToInterrogation()=true` 시 `interrogationHandler.handle(..., null, null, plan, previousExchanges)` 호출. 직전 PLAYGROUND turn 의 `answerText` / `analysis` 가 시그니처에 들어와 있으나 (`:166-168`) `null` 명시 전달 — silent drop. 의도 / 주석 0.
- `domain/resume/service/InterrogationModeHandler.java:27` — `private static final int DEFAULT_ANSWER_QUALITY = 2;`
- `:83` — `int answerQuality = analysis != null ? analysis.answerQuality() : DEFAULT_ANSWER_QUALITY;`
- `domain/interview/entity/AnswerAnalysis.java:27` — `answerQuality` 1~5 invariant 검증.
- `:49-50` — `empty(turnId)` factory = `(quality=1, RecommendedNextAction.CLARIFICATION)`. "분석 불가 → 명료화" 정책 단일 소스.
- `:55-57` — `applyL1FalseNegativeGuard()` = `noClaims && lowQuality(<=1)` → `CLARIFICATION` 강제.
- `domain/interview/entity/TurnAnalysisResult.java:18-20` — `fromJson` factory 가 `answerAnalysis == null` 시 `empty()` 동등 객체 반환 (already graceful). 즉 LLM 응답 파싱 단계 = null safety 일부 존재.
- `domain/interview/service/AnswerAnalyzer.java:76-89` — `aiResponseParser.parseOrRetry` (이미 repair retry 패턴 일부 존재) → `withTurnId` → `applyL1FalseNegativeGuard` → return. **반환값 null 가능성 = `parseOrRetry` 동작 의존 (implement 단계 grep 1회 확인 필요)**.
- `infra/ai/context/layer/FocusLayer.java:81` — chain interrogator prompt 에 `"ANSWER_QUALITY: " + h.answerQuality()` 흘려보냄.
- `prompts/template/resume/resume-chain-interrogator.txt:68` — `"PROJECT_NAME 이 비어 있거나 명시되지 않은 경우, 임의 명칭을 만들어내지 말고 CURRENT_CHAIN 의 topic 만으로 질문을 구성하세요 (예: \"방금 답변하신 [topic] 의 ~\")."` — 거짓 다리 phrasing 예시 mimic 원천.
- `:81` — 안전 폴백: `"방금 답변하신 부분의 구체적인 구현 방식을 좀 더 설명해주실 수 있을까요?"` — fallback 자체에 거짓 다리 phrasing.

**Phase 2 영역**:
- `domain/resume/service/PlaygroundModeHandler.java:35-55` — `handleOpener` 가 `resultGenerator.generateOpener(interviewId, state, project, firstPlan.playgroundPhase())` 호출. `playgroundPhase.openerQuestion` = planner 가 사전 생성한 텍스트 (`InterviewPlan` JSON 에 박힘).
- `infra/ai/prompt/ResumeInterviewPlannerPromptBuilder.java:58-76` — planner build 시점에 `SKELETON_JSON / DURATION_MIN / USER_LEVEL / ALLOWED_CHAIN_IDS` 만 inject. skeleton 메타 4종 (techStack/role/architecture/decisions) 은 skeleton.projects[].claims 안에 잠재 — planner 가 자율 활용.
- `prompts/template/resume/resume-interview-planner.txt:34` — `"허용: \"설명\", \"역할\", \"맡으셨\", \"흐름\", \"인상 깊은 / 어려웠던 경험\" 류 intro / 감정·서사 oriented 어휘."` — "인상 깊은 / 어려웠던 경험" 어휘가 응시자 무관 진부 anchor 원천.
- `:35` — `"project_name 명시된 경우 opener_question 자체에 자연스럽게 포함 (예: \"{project_name} 프로젝트에서 어떤 역할을 맡으셨나요?\")."` — 조건부 ("명시된 경우"). 항상 강제 아님.
- `:88` — few-shot 예시 1종: `"{__example_name__} 프로젝트에 대해 설명해주시고, 어떤 역할을 맡으셨는지 설명해주세요."` — 사용자 의도 패턴과 거의 일치 (프로젝트명 + 설명 + 역할). 그러나 `:34` 허용 어휘가 LLM 을 다른 방향 (인상 깊은 경험) 으로 끌어당김.

**Phase 3 영역 (참고만 — 별도 sub-spec)**:
- 5건 위치 (path:line) 는 product-spec Phase 3 Evidence 에 정리됨. invariant 확인 후 별도 sub-spec 작성.

### 외부 레퍼런스
- `backend/.claude/rules/conventions.md` — Flyway DDL only / @Transactional / 로깅 / Lombok / Entity 직접 반환 금지.
- `backend/.claude/rules/testing.md` — Service Integration / Domain Unit / Live LLM eval (E2E 1건 + Live 1건).
- 인접 spec `docs/plans/458-resume-skeleton-redesign/tech-spec.md` — Phase 2 wiring 패턴 참고.

### 사용자 발화 (특정 결정 근거)
- "응시자별 OPENER 차별화 본질 X. 프로젝트 이름 식별 + 프로젝트 설명 + 역할 고정 패턴 OK. 약간의 꼬리질문도 OK." → Phase 2 Option A 채택 근거.
- "P0 모두 수정 + Phase 3 별도 sub-spec 분리" → 본 spec 범위 = Phase 1+2.
- "Phase 2 few-shot 보강 적용" → planner.txt:88 변경 확정 (조건부 X).
- "Live LLM eval 통과 기준 = 자동 단순 매칭" → Verification 자동화 게이트.
- "LLM null → 그대로? 그렇게 가자 (Option B)" → Phase 1 throw 폐기 + empty() graceful 채택. 베스트 프랙티스 정합 (예외 차단 X / 시그널 enum 흘림).
- "LLM 응답 스키마 강제 통일 = 별도 epic" → 본 spec 범위 = silent drop 차단 + empty() fallback 만.
- "OPENER 동일 skeleton 매번 동일 프로젝트 강제 X — 다양성 의도 / 랜덤 1개 anchor OK" (2026-05-10) → Phase 2 Verification 동일성 assert 폐기, 멤버십 검사로 변경. planner.txt 가이드 = "정확히 1개 anchor 필수" 명시.

### 추정 / 미확인 가정
- (확인 필요 / implement 단계 grep) `TurnAnalysisPipeline.analyze` → `AnswerAnalyzer.analyze` → `aiResponseParser.parseOrRetry` 반환값이 항상 non-null 인지. null 가능 시 = Orchestrator 측 `empty()` fallback 강제 = 본 spec 채택 옵션 그대로.
- (확인) `turnEventPublisher.publish` 의 `analysis` 인자 = `processUserTurn` 매개변수 (사용자 이번 turn 답변의 분석). `handlePlayground` 가 `interrogationHandler.handle` 에 전달하던 null 은 별개 — Phase 1 변경이 publish 페이로드에 영향 0. (`ResumeInterviewOrchestrator.java:113`)
- (추정) `RecorderEventListener` / `FollowUpTransactionHandler` 등 `publish` 후 listener 는 본 변경 영향 없음 (페이로드 의미 무변경).
- (가정) prompt template 변경이 기존 LLM 답변 품질 회귀시키지 않음 — eval fixture 5종 + 자동 어휘 매칭으로 차단.
- (가정) Phase 2 prompt 변경 token 영향 미미 (`backend/eval/context/measure_tokens.py` 로 implement 단계 측정 권장).
- (가정) chain interrogator prompt 가 `ANSWER_QUALITY=1` + CLARIFICATION 시그널 받으면 명료화 질문 자연 생성. 이미 정책 정의 (`AnswerAnalysis.empty()` 단일 소스) — Live eval fixture 로 검증.

---

## Trade-offs

### Phase 1: `analysis = null` 처리 정책

**Option B (채택): silent drop 차단 + `empty()` graceful 시그널 (LLM 베스트 프랙티스 정합)**
- 동작: `Orchestrator:177` 모드 전환 시 직전 PLAYGROUND turn `answerText` / `analysis` 실값 전달. `analysis` 가 만약 null 이면 `AnswerAnalysis.empty()` 강제 (CLARIFICATION 시그널). `InterrogationModeHandler` `DEFAULT_ANSWER_QUALITY=2` 상수 제거 + null 도달 시 `empty()` 강제 (또는 호출자에서 강제 후 진입 = 동일 효과). throw 0.
- 장점:
  - `AnswerAnalysis.empty()` 단일 소스 정책 ("분석 불가 → CLARIFICATION") 일관 적용.
  - 면접 비차단. 발동 시 LLM 이 명료화 질문 자연 생성.
  - 가짜 평균값 (`quality=2`) 0. 시그널 (quality=1 + CLARIFICATION) 진실.
  - LLM 베스트 프랙티스 정합 (예외 차단 X / graceful enum 시그널 흘림).
- 단점: empty() 강제 위치 = 호출자 (`Orchestrator`) vs 수신자 (`InterrogationModeHandler`) 중 결정. implement 단계 = 호출자 측 강제 권장 (수신자는 항상 non-null analysis 가정 invariant 유지).

**Option A (폐기): `BusinessException` throw 강제**
- 폐기 사유:
  - 사용자 차단 (5xx 응답). LLM 응답 누락 = 자주 발생 가능 (parseOrRetry 실패 / 외부 의존 결함) → 차단 빈도 운영 위험.
  - LLM 베스트 프랙티스 위배: graceful 시그널이 정합 패턴.
  - `AnswerAnalysis.empty()` 단일 소스 정책이 이미 "graceful 시그널" 디자인. throw 도입 = 정책 분기.

**Option C (폐기): `DEFAULT_ANSWER_QUALITY = 2 → 1` 변경만**
- 폐기 사유: fallback 단일 소스 부재 유지. 시그널 enum (`recommendedNextAction`) 미반영 — quality=1 만으로는 CLARIFICATION 의도 불명.

**별도 epic 위임 (본 spec 범위 외)**:
- LLM 응답 스키마 강제 통일 (`json_schema strict: true` / Claude tool use schema / repair retry 통일 / Refusal 처리) = 별도 Issue.
- 본 spec = 본 변경 범위 내 silent drop 차단 + empty() graceful 만.

### Phase 2: planner prompt 변경 방향

**Option A (채택): 허용 어휘 좁히기 + project_name 강제 + few-shot 1예시 보강 (적용 확정)**
- 장점: 사용자 의도 (프로젝트 단위 anchoring) 정확 반영. 변경 사이즈 작음. 기존 few-shot 패턴 강화.
- 단점: LLM 이 어휘 가이드 무시할 가능성 → 자동 어휘 매칭 + eval fixture 5종으로 차단.
- 사유: 사용자 결정 — 다양화 본질 X. 고정 패턴 OK. few-shot 보강 = 사용자 명시 승인.

**Option B (폐기): few-shot 다양화 (4-5종 anchor 풀)**
- 폐기 사유: 사용자 결정 위배. 다양화 자체가 본질 아님.

**Option C (폐기): few-shot 미변경 (어휘만 좁히기)**
- 폐기 사유: 사용자가 적용 명시. 보강 = 패턴 강제력 ↑.

---

## Architecture

### Phase 1 — 모드 전환 정합성

```
[변경 전]
ResumeInterviewOrchestrator.handlePlayground:
  if (result.switchedToInterrogation()) {
      ...
      interrogationHandler.handle(interviewId, refreshed,
          null, null,                          # ← 직전 답변 / 분석 silent drop
          plan, previousExchanges);
  }

InterrogationModeHandler:
  private static final int DEFAULT_ANSWER_QUALITY = 2;
  ...
  int answerQuality = analysis != null
      ? analysis.answerQuality()
      : DEFAULT_ANSWER_QUALITY;             # ← 가짜 평균값

resume-chain-interrogator.txt:68:
  (예: "방금 답변하신 [topic] 의 ~")        # ← 거짓 다리 phrasing 예시
:81 안전 폴백:
  "방금 답변하신 부분의 구체적인 구현 방식..."  # ← fallback 자체 거짓 phrasing

[변경 후]
ResumeInterviewOrchestrator.handlePlayground:
  if (result.switchedToInterrogation()) {
      runtimeStateStore.update(... INTERROGATION);
      InterviewRuntimeState refreshed = runtimeStateStore.get(interviewId);
      // analysis null safety = empty() 강제 (graceful CLARIFICATION 시그널)
      AnswerAnalysis safeAnalysis = (analysis != null)
              ? analysis
              : AnswerAnalysis.empty(0L);
      if (analysis == null) {
          log.warn("[ResumeOrchestrator] analysis null on mode transition — empty() fallback. interviewId={}",
                   interviewId);
      }
      interrogationHandler.handle(interviewId, refreshed,
          answerText, safeAnalysis,            # ← 실값 또는 empty() 시그널
          plan, previousExchanges);
  }

# 주의: Orchestrator:113 turnEventPublisher.publish 페이로드 = 무변경.
# publish 의 analysis 인자 = processUserTurn 매개변수 (= 사용자 이번 turn 답변의 분석).
# 본 변경 = handle 내부 매개변수 전달만. publish listener 영향 0.

InterrogationModeHandler:
  // DEFAULT_ANSWER_QUALITY 상수 제거
  // analysis 는 항상 non-null invariant (호출자가 empty() fallback 강제)
  int answerQuality = analysis.answerQuality();
  // empty() = quality=1, recommendedNextAction=CLARIFICATION
  // → FocusLayer 가 chain prompt 에 "ANSWER_QUALITY: 1" 흘림
  // → LLM 이 명료화 질문 자연 생성

# 신규 ErrorCode 추가 X. throw 도입 X. graceful 시그널만.

resume-chain-interrogator.txt:
  :68 거짓 다리 예시 제거 + 가이드 보정:
    "PROJECT_NAME 비어 있을 때 CURRENT_CHAIN.topic 만으로 질문 구성.
     직전 답변 내용을 실제로 참조하지 않은 경우 '방금 답변하신' 류 표현 금지."
  :81 안전 폴백 일반화:
    "현재 chain 의 추가 측면을 설명해주실 수 있을까요?"

# CLARIFICATION 시그널 처리 = 기존 prompt 정책 (직전 답변 quality=1 → 명료화 질문).
# 별도 prompt 변경 불필요.
```

### Phase 2 — OPENER 정합성

```
resume-interview-planner.txt 변경:

:34 허용 어휘 좁히기
  Before: "허용: '설명', '역할', '맡으셨', '흐름', '인상 깊은 / 어려웠던 경험' 류 intro / 감정·서사 oriented 어휘."
  After:  "허용: '설명', '역할', '맡으셨', '흐름' 류 intro 어휘. '인상 깊은 / 어려웠던 경험' 류 감정·서사 anchor 금지 (응시자 무관 진부 패턴 회귀 방지)."

:35 project_name 강제 + 1개 anchor 룰
  Before: "project_name 명시된 경우 opener_question 자체에 자연스럽게 포함 (예: ...)."
  After:  "skeleton.projects[] 중 정확히 1개를 anchor (어느 거든 OK — 다양성 환영, 동일 skeleton 재호출 시 다른 프로젝트 선택 가능).
           선택한 project_name 은 opener_question 에 반드시 명시. 누락 / 'this project' / '해당 프로젝트' / '프로젝트들' 같은 지시·복수 표현 금지.
           가공 명칭 / skeleton 외 프로젝트 명칭 생성 금지."

:88 few-shot 보강 (적용 확정)
  Before: "{__example_name__} 프로젝트에 대해 설명해주시고, 어떤 역할을 맡으셨는지 설명해주세요."
  After:  "{__example_name__} 프로젝트에 대해 간단히 설명해주시고, 어떤 역할을 맡으셨는지 말씀해주세요."
  (약간의 꼬리질문 1~2개 허용 명시는 prompt 본문에서 처리)
```

### Phase 3

본 spec 범위 외. 별도 sub-spec (`tech-spec-phase3.md`) 에서 다룸. 진입 시점 = Phase 2 머지 후 invariant 코드 path:line 단위 확인.

---

## Data Model

변경 없음. DDL 0. 기존 스키마 그대로.

진행 중 면접 row 영향 0. prompt template 변경 = 신규 turn 부터 반영.

---

## API Contract

변경 없음. BE 내부 로직 / prompt template only. Controller / Request / Response schema 무변경.

---

## Verification

### Live LLM Eval 게이트 (공통 룰)
- 위치: `backend/src/test/java/com/rehearse/api/.../resume/eval/` (Service Integration 카테고리).
- 활성화: `@EnabledIfEnvironmentVariable(name = "RUN_LIVE_API", matches = "true")` (testing.md Live 룰).
- 키 부재 시 자동 skip.
- 통과 기준 = **자동 단순 매칭** (정규식 grep + AssertJ assert). 의미 평가 X — 차단 어휘 / 강제 어휘 패턴만 검사.
- 결과 = JUnit 통과/실패 (별도 수동 기록 위치 X — 자동 게이트만).

### Phase 1
- [ ] **Pre-implement grep**: `TurnAnalysisPipeline.analyze` → `AnswerAnalyzer.analyze` → `aiResponseParser.parseOrRetry` 반환값 null 가능성 코드 path:line 단위 확인 결과 첨부 (implement.md / PR 본문). null 가능 시 = Orchestrator 측 `empty()` fallback 강제 코드 포함. null 불가 시 = fallback 코드는 방어 차원 1줄 (가능성 0이지만 invariant 유지).
- [ ] **Service Integration**: `ResumeInterviewOrchestratorIntegrationTest` — PLAYGROUND→INTERROGATION 전환 시나리오. 직전 turn `answerText` / `analysis` 가 chain 첫 호출 prompt 에 흘러감 검증 (chain prompt builder 입력 캡처).
- [ ] **Service Integration**: 동일 테스트 — 분석 결과 = `AnswerAnalysis.empty()` 강제 케이스. chain prompt 에 `ANSWER_QUALITY: 1` 흘러감 + 면접 정상 진행 (5xx 발생 0) 검증.
- [ ] **Domain Unit**: `InterrogationModeHandlerTest` — `analysis = empty()` 호출 시 정상 처리 + `answerQuality=1` 정확 전달 검증.
- [ ] **Domain Unit**: `InterrogationModeHandlerTest` — `analysis = 정상값` 케이스 `answerQuality` 정확 전달 검증.
- [ ] **Live LLM Eval Fixture (5종)**: PLAYGROUND 답변 ↔ chain pool topic disjoint 케이스. 자동 매칭:
  - assert: chain 첫 질문 출력에 정규식 `방금\s*답변\s*하신` / `방금\s*말씀하신` 0건. (실제 답변 참조 정당 케이스 false-positive 회피 = fixture 의미 disjoint 보장 — 답변 명사 풀과 chain topic 명사 풀 비교쳐).
  - assert: 출력에 PLAYGROUND turn 답변 명사 1+ AND chain topic 1+ 포함 (AND — OR 미사용. 답변 무시 + chain topic only 회피).
- [ ] **Live LLM Eval Fixture (analysis empty 케이스 1종)**: empty() 시그널 → chain 첫 질문 = 명료화 질문 (정규식 `(다시|구체적으로|좀 더 자세히|어떤 의미)` 매칭 1+).
- [ ] **빌드**: `./gradlew test --tests "ResumeInterview*" --tests "InterrogationModeHandlerTest"` 통과.
- [ ] **회귀**: 기존 `ResumeInterviewOrchestratorIntegrationTest` 정상 흐름 시나리오 통과.

### Phase 2
- [ ] **Service Integration**: `PlaygroundModeHandlerTest` — `handleOpener` 정상 흐름 회귀 없음.
- [ ] **Live LLM Eval Fixture (5종, 다른 프로젝트 skeleton)**: 자동 매칭:
  - assert (프로젝트명 명시): OPENER 출력에 `project_name` 문자열 포함 (5/5).
  - assert (프로젝트 설명 요청): 정규식 `(설명|소개|어떤\s*프로젝트|간단히)` 매칭 1+ (5/5). 풀 = planner.txt:34 허용 어휘 풀 (`설명/역할/맡으셨/흐름`) 정합.
  - assert (역할 요청): 정규식 `(역할|맡으)` 매칭 1+ (5/5). `담당` 어휘는 planner.txt:34 풀 외 → assert 정규식에서 제외.
  - assert (감정·서사 어휘 차단): 정규식 `(인상\s*깊|어려웠던|기억에\s*남)` 등장 0건 (5/5).
- [ ] **Live LLM Eval Fixture (멤버십)**: 동일 skeleton 2회 호출 → OPENER 양쪽 모두 `skeleton.projects[].name` 중 1개를 anchor (project_name 추출값이 skeleton 내 존재 검증). 동일성 assert X (랜덤 anchor 의도). 정확히 1개 anchor (복수 프로젝트명 동시 등장 0건) assert.
- [ ] **빌드**: `./gradlew test --tests "Playground*" --tests "ResumeInterviewPlanner*"` 통과.

### 공통
- [ ] **빌드 / 린트**: `./gradlew build` 통과.
- [ ] **컨벤션**: `backend/.claude/rules/conventions.md` 준수 (트랜잭션 / 로깅 / Lombok).

---

## Pre / Post State

### Pre (현재)
- `ResumeInterviewOrchestrator:177` 모드 전환 시 `null, null` silent drop
- `InterrogationModeHandler:27,83` `DEFAULT_ANSWER_QUALITY=2` fallback (`AnswerAnalysis.empty()` 정책과 불일치 = 가짜 평균값 흘러감)
- `resume-chain-interrogator.txt:68,81` "방금 답변하신" 거짓 다리 phrasing 예시 + 안전 폴백
- `resume-interview-planner.txt:34,35,88` "인상 깊은 경험" 어휘 허용 + project_name 명시 조건부 + few-shot 약함

### Post (구현 후)
- `ResumeInterviewOrchestrator:177` 직전 turn `answerText` / `analysis` 실값 전달. `analysis = null` 도달 시 `AnswerAnalysis.empty()` graceful fallback (CLARIFICATION 시그널 + WARN 로그). `:113 turnEventPublisher.publish` 페이로드 무변경.
- `InterrogationModeHandler` `DEFAULT_ANSWER_QUALITY` 상수 제거. `analysis` 항상 non-null invariant (호출자가 empty() fallback 강제). `analysis.answerQuality()` 직접 사용.
- `resume-chain-interrogator.txt` 거짓 다리 phrasing 예시 / 안전 폴백 제거 + 가이드 보정
- `resume-interview-planner.txt` "인상 깊은 경험" 어휘 제거 + project_name 강제 + few-shot 보강
- 신규 ErrorCode 추가 X. throw 도입 X.
- (Phase 3) = 별도 sub-spec — 본 spec Pre/Post 영향 없음

---

## 위험 / 마이그레이션 / 롤백

### 위험
- **(낮음) prompt 회귀**: prompt template 변경이 기존 LLM 답변 품질 회귀시킬 가능성. → 자동 어휘 매칭 + Live eval fixture 5종 + Phase 2 재현성 검증.
- **(낮음) empty() fallback 빈도**: 운영 중 `analysis = null` 발생 빈도 미확인. 발동 시 사용자 면접 = CLARIFICATION 명료화 질문 1회 = 면접 비차단 (graceful). 단 빈도 ↑ 시 = "면접이 자꾸 다시 설명해달라고 함" 사용자 체감 저하 가능 → WARN 로그 + 발동 횟수 메트릭 (LLM 통일 epic 에서 본격 처리). 완화: `aiResponseParser.parseOrRetry` 가 이미 repair retry 일부.
- **(낮음) 트랜잭션 / runtime state 변경 흔적**: `ResumeInterviewOrchestrator` = `@Transactional` 미적용 의도 (LLM 외부 호출 트랜잭션 분리). `runtimeStateStore` = Caffeine in-memory cache (DB X). 모드 전환 = 메모리 mutation 즉시 반영. 본 spec 변경 후 = throw 도입 X 이므로 모드 전환 후 mutation 흔적 박힘 위험 0. handle 정상 진행.
- **(낮음) Phase 2 기존 진행 중 면접 캐시 OPENER**: `startSession` 라인 130-140 = 기존 `RESUME_OPENER` 재사용 경로 → 머지 시점 진행 중 면접의 캐시된 OPENER 는 legacy phrasing 잔존. 신규 면접부터 신규 패턴. 사용자 영향 미미 (진행 중 면접 = 짧은 시간 내 종료).
- **(낮음) PR 1 단독 머지 ~ PR 2 머지 사이 OPENER 결함 잔존**: PR 1 머지 후 거짓 다리 phrasing fix 됐지만 OPENER 진부 (증상 1) 그대로. 사용자 체감 = 절반만 해결. 완화: Phase 2 = prompt template only / 작은 PR → PR 1 머지 직후 즉시 진입. PR 1~PR 2 사이 시간 최소화.
- **(낮음) Phase 3 sub-spec 진입 시점 보장 부재**: product-spec Goal Phase 3 = 별도 sub-spec 위임 → 잊힘 위험. 완화: 본 spec PR 1 머지 직후 즉시 GitHub Issue 신규 생성 (제목: "Resume fallback 5건 정합성 점검 (Phase 3)") + 본 plan handoff 에 후속 Issue 번호 반영.
- **(낮음) NF token 영향**: prompt 변경 폭 작음 (어휘 좁히기 + few-shot 1줄). `backend/eval/context/measure_tokens.py` 로 implement 단계 확인 권장.

### 외부 의존
- LLM 호출: OpenAI GPT-4o-mini primary + Claude fallback (`ResilientAiClient`). prompt 변경 = 기존 retry / fallback 경로 재사용. 비용 영향 미미 (어휘 변경만).
- LLM 응답 스키마 강제 통일 (`json_schema strict: true` / Claude tool use schema / repair retry / Refusal 처리) = **별도 epic 위임**. 본 spec 범위 외.

### 관찰성
- Phase 1 `empty()` fallback 발동 시 = `log.warn("[ResumeOrchestrator] analysis null on mode transition — empty() fallback. interviewId={}", ...)` → docker log grep `empty\(\) fallback` 으로 발동 횟수 파악.
- 알림 = **수동 docker log grep + APM 한정**. 자동 알림 (Slack / PagerDuty) 미적용 — 본 spec 범위 외 (별도 운영 모니터링 Issue).
- Phase 2 prompt 회귀 = Live eval CI 자동 차단 (수동 모니터링 불필요).

### 마이그레이션
없음 (DDL 0, 코드 + prompt template only).

### 롤백
git revert. prompt 변경 = 즉시 effective. 진행 중 면접 영향 0 (신규 turn 부터 반영).

---

## 분기 결정

- [x] **단일 영역 → `implement.md` 1개**
  - BE only (resume / ai prompt template). FE 영향 0.
- [x] **Phase 별 PR 분리**
  - PR 1: Phase 1 (Orchestrator:177 silent drop 차단 + empty() graceful fallback + DEFAULT_ANSWER_QUALITY 제거 + chain-interrogator.txt phrasing 정정)
  - PR 2: Phase 2 (planner.txt 어휘 + project_name 강제 + few-shot 보강) — Phase 1 머지 후. PR #463 = 머지 완료 (커밋 30ed71e 확인).
- [x] **Phase 3 = 별도 sub-spec**
  - `tech-spec-phase3.md` 별도 작성. 진입 시점 = 본 spec PR 2 머지 후. invariant 확인 후 5건 결정 표 + Verification.
- [x] **별도 epic — LLM 응답 스키마 강제 통일**
  - Issue 신규 생성 (본 spec 머지 후). 범위: `LlmStructuredOutputClient` 추상화 / OpenAI strict mode + JSON Schema / Claude tool use schema / repair retry 통일 / Refusal 처리 / adapter 전체 이관.
