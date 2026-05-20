# Tech Spec — Resume 질문 생성 프롬프트 직무 적합성 + 깊이 강제

> **작성자**: backend agent (Staff Engineer 페르소나, create-tech-spec 스킬)
> **답하는 질문**: 어떻게? 구조 / 데이터 / Trade-off / 검증
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄 미러)

Resume 트랙 질문 생성 시스템 프롬프트에 직무별 페르소나 / 평가 관점 / 깊이 5 유형 분배 + 표층 금지 패턴을 주입하고, skeleton 추출 단계에서 깊이 신호 (트레이드오프 / 대안 / 수치 / 의사결정 근거) 를 구조화 보존해 main 질문이 직무·깊이 특화되도록 한다. Goal 검증 = 사용자 수동 샘플링 (5건 비교 + Live E2E 단언).

---

## Evidence

### 현재 구조 (관련 클래스 / 파일)

- `backend/src/main/java/com/rehearse/api/domain/question/service/QuestionGenerationService.java:24-34` — Resume 트랙 분기 시 `position` / `techStack` 보유. `ResumeTrackInitiator` 로 전파만 추가하면 됨 (Interview 엔티티 조회 불필요).
- `backend/src/main/java/com/rehearse/api/domain/question/service/ResumeTrackInitiator.java:51-89` — `initiate(interviewId, resumeFileHash, resumePdfBytes, durationMinutes)`. 시그니처 확장 대상.
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FixedContextLayer.java:103-116` — L1 SYSTEM block 빌더. `GLOBAL_CORE + raw resume-question-generator.txt` 결합. 토큰 치환 없음.
- `backend/src/main/java/com/rehearse/api/infra/ai/context/FocusHints.java:27-32` — `ResumeQuestionGeneratorHints(skeletonJson, openerCount, mainCount, primaryProjectName)` — L4 USER fragment 전용 입력. 직무/스택 = SYSTEM 영역 → 본 plan 에서 미확장.
- `backend/src/main/java/com/rehearse/api/infra/ai/context/ContextBuildRequest.java:3-16` — `(callType, focusHints, providerHint)`. 직무 전파는 1급 필드 확장 (`position`, `techStack`) 으로 처리 — L1 이 L4 입력 캐스팅 회피.
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/QuestionGenerationPromptBuilder.java:41-66` — 표준 트랙 토큰 치환 패턴. **동일 패턴 답습 가능**. PersonaResolver 위임 검증된 자산.
- `backend/src/main/java/com/rehearse/api/infra/ai/persona/PersonaResolver.java:30-54` — `resolve(position, techStack)` → `ResolvedProfile { fullPersona, evaluationPerspective, followUpDepth, ... }`. 자산 그대로 활용.
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/Position.java:3` — enum (BACKEND / FRONTEND / DEVOPS / DATA_ENGINEER / FULLSTACK).
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/TechStack.java:10` — enum (스택 14종).
- `backend/src/main/resources/prompts/template/resume/resume-question-generator.txt:1-28` — 28줄, 토큰 슬롯 부재.
- `backend/src/main/resources/prompts/template/resume/resume-extractor.txt:1-87` — 추출 본문 87줄. 출력 schema 4 필드 + 예시. depth_signals 부재.
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedResumeQuestions.java:9-29` — `(openers, mains: [{question, ttsQuestion, bestAnswer}])`. `depthType` 부재.
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/Project.java:7-24` — record `(projectId, projectName, techStack, role, architecture, decisions)`. `@JsonIgnoreProperties(ignoreUnknown=true)` 보유 → 신규 필드 추가 시 구버전 JSON 안전.
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ResumeSkeletonEntity.java:41-42` — `skeleton_json` `columnDefinition = "JSON"`. 스키마 변경 불필요.
- `backend/src/main/java/com/rehearse/api/domain/question/entity/Question.java:23-37` — 엔티티. `depth_type` 컬럼 없음 — V51 추가 후보.
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeQuestionPersister.java:32-58` — `ResumeQuestionDraft (questionType, questionText, ttsText, bestAnswer, orderIndex)` 확장 + `Question.resume()` 팩토리 확장.

### 자산 (변경 없이 활용)

- `backend/src/main/resources/prompts/base/{backend,frontend,devops,data-engineer,fullstack}.yaml` — 직무 페르소나 5종.
- `backend/src/main/resources/prompts/overlay/{domain}/*.yaml` — 스택 overlay 14종.

### 컨벤션 / 룰

- `backend/.claude/rules/conventions.md` — port/adapter, App Service 트랜잭션, Flyway DDL 전용, `@Slf4j` 한국어 로깅, Lombok 룰.
- `backend/.claude/rules/testing.md` — Domain Unit ≥60%, Service Integration 시 외부 API 만 Mock, Live E2E `@Disabled` + `RUN_LIVE_API`.
- 루트 `.claude/rules/security.md` — RESUME_SKELETON 입력 = 데이터로만 취급 (기존 본문 유지).

### 사용자 결정

- depthType 노출 채널 = **BE 단독** (DB / dev 조회). FE 응답 DTO 노출 X. → 단일 영역 `implement.md`.
- 축 A+B+C 단일 PR phase 통합 (product-spec).

### 추정 / 미확인 가정

- 신규 system 토큰 증가 ~+500 (페르소나 + 평가 관점 + 5유형 + 금지 패턴). product-spec Goal "5초 변동 내" 충족 추정 — Live E2E 단계 측정.
- LLM (GPT-4o-mini / Claude) 가 출력 schema 의 `depth_type` enum 5 값을 95%+ 정확히 분류 — 추정. JSON Schema Structured Output / response_format 가용성 확인 implement 단계.

---

## Trade-offs

### Option A (채택): 별도 `ResumeQuestionPromptBuilder` + L1 위임 + `ContextBuildRequest` 1급 필드 확장 + `depth_type` 컬럼 + skeleton `depth_signals` 신규 필드

- 장점:
  - 표준 트랙 `QuestionGenerationPromptBuilder` 패턴 답습 → 일관성, 검증된 자산 재활용 (PersonaResolver).
  - `Project.decisions` 보존 + 신규 `depthSignals` 추가 → 구버전 JSON 역직렬화 안전 (`@JsonIgnoreProperties`).
  - depth_type 컬럼화 → AC #2 식별성 강제 (dev DB 조회 채널).
  - position/techStack = `ContextBuildRequest` 1급 필드 → L1 (`FixedContextLayer`) 가 L4 전용 `FocusHints` 캐스팅 없이 직무 컨텍스트 접근 (책임 경계 보전).
  - L1 캐싱 효익 손실 최소 (resume_question_generator = 인터뷰 시작 1회 호출).
- 단점:
  - 신규 빌더 클래스 + Flyway V51 + DTO/엔티티/record + ContextBuildRequest 시그니처 확장 → 변경 파일 10+.
  - `ContextBuildRequest` 시그니처 확장이 다른 callType 에는 null 필드 → 호출부 nullable 명시 필요.
- 채택 사유: AC #1 (직무 차이), AC #2 (depth 식별), AC #4 (skeleton 깊이 보존) 모두 충족. 자산 재활용 / 호환 안전 / 패턴 일관성. ContextLayer 잔존 구조 (Issue #518 후속 제거 예정) 위에서 책임 경계 유지.

### Option A-알트 (폐기): `FocusHints.ResumeQuestionGeneratorHints` 에 position/techStack 추가

- 장점: 변경 시그니처 좁음 (record 1개 필드 추가).
- 단점: L4 전용 입력을 L1 이 캐스팅 → sealed interface 책임 경계 위반 (Issue #518 의 원인 그대로 누적).
- 폐기 사유: ContextLayer 제거 별도 plan 진행 전이라도 새 위반 추가 금지. 추가 callType 동일 패턴 답습 차단.

### Option B (폐기): `resume-question-generator.txt` 본문 텍스트만 5유형 + 금지 패턴 보강

- 장점: 변경 파일 1개, 가장 단순.
- 단점: 직무 분기 부재 → AC #1 미충족. depth 메타 미저장 → AC #2 미충족.
- 폐기 사유: product-spec 축 A / 축 B 모두 미커버.

### Option C (폐기): 신규 `SystemFragmentLayer` (L2) 신설해서 페르소나 주입

- 장점: ContextLayer 책임 분리.
- 단점: 1 callType 위해 신규 layer 추가 = YAGNI. 표준 트랙은 빌더 패턴 = 일관성 위배.
- 폐기 사유: Simplicity 룰 위반.

### Option D (폐기): `Project.decisions: List<String>` → `List<Decision>` 구조 직접 변경

- 장점: 단일 필드로 깊이 신호 통합.
- 단점: 구버전 skeleton JSON (`decisions: ["..."]`) 역직렬화 깨짐. `@JsonIgnoreProperties` 는 unknown 만 무시, 타입 변환은 안 함.
- 폐기 사유: AC #5 회귀 미발생 위반 위험.

---

## Architecture

### 데이터 흐름

```
[QuestionGenerationService.generateQuestions]
   ↓ position, techStack 보유 (line 24-34)
   ↓ line 32: resumeTrackInitiator.initiate(interviewId, resumeFileHash, resumePdfBytes,
   ↓                                        durationMinutes, position, techStack)  ★ 호출 시그니처 확장
   ↓
[ResumeTrackInitiator.initiate(.., position, techStack)]  ★ 시그니처 확장
   ↓
[ResumeIngestionService.ingestPdf]
   ↓ skeleton (depth_signals 포함, Project record 확장)  ★
   ↓
[ResumeSkeletonSampler.sampleDecisions]  (기존 동작 유지)
   ↓
[InterviewContextBuilder.build(ContextBuildRequest)]
   ├─ ContextBuildRequest(callType="resume_question_generator",
   │                      focusHints=ResumeQuestionGeneratorHints(.. 기존 4 필드),
   │                      providerHint, position, techStack)  ★ 1급 필드 확장
   │
   ├─ L1: [FixedContextLayer.build(req)]
   │       └─ callType == resume_question_generator
   │             → [ResumeQuestionPromptBuilder.buildSystemPrompt(req.position(), req.techStack())]
   │                  ├─ PersonaResolver.resolve(position, techStack)
   │                  └─ resume-question-generator.txt 토큰 치환:
   │                       {PERSONA_BLOCK}         ← profile.fullPersona()
   │                       {EVALUATION_PERSPECTIVE}← profile.evaluationPerspective()
   │                       {FOLLOW_UP_DEPTH}       ← profile.followUpDepth()
   │                       {DEPTH_GUIDE_5_TYPES}   ← 정적 상수 (5 유형 분배 룰)
   │                       {FORBIDDEN_PATTERNS}    ← 정적 상수 (표층 금지)
   │             → ChatMessage.ofCached(SYSTEM, GLOBAL_CORE + 치환본문)
   │
   └─ L4: [FocusLayer.build(req.focusHints())] — RESUME_SKELETON / OPENER_COUNT / MAIN_COUNT / PRIMARY_PROJECT_NAME (변경 없음)
   ↓
[AiClient.chat]
   ↓
[AiResponseParser.parseOrRetry → GeneratedResumeQuestions]
   ↓ mains: [{question, ttsQuestion, bestAnswer, depthType}]  ★ DTO 확장
   ↓
[ResumeQuestionPersister.persistAll]
   ↓ Question.resume(.., depthType)  ★ 팩토리 확장
   ↓
[question.depth_type 컬럼 적재]  ★ V51
```

### 컴포넌트 책임

- `QuestionGenerationService` (호출부 수정) — `generateQuestions` line 32 `resumeTrackInitiator.initiate(...)` 호출에 `position`, `techStack` 인자 추가. 본 메서드는 이미 두 값 보유 (line 24-27).
- `ResumeQuestionPromptBuilder` (신규) — `infra/ai/prompt/` 위치. PersonaResolver 의존 + 템플릿 토큰 치환 + 정적 상수 (DEPTH_GUIDE / FORBIDDEN_PATTERNS) 보유. `FixedContextLayer` 가 위임.
- `FixedContextLayer` (수정) — `resume_question_generator` callType 분기에서 `ResumeQuestionPromptBuilder.buildSystemPrompt(req.position(), req.techStack())` 호출. 그 외 callType 기존 raw 템플릿 유지. position/techStack 은 `ContextBuildRequest` 1급 필드에서 직접 추출 (FocusHints 캐스팅 회피).
- `ContextBuildRequest` (수정) — `position`, `techStack` nullable 1급 필드 추가. 다른 callType (answer_analyzer / follow_up_generator_v3) 호출부는 null 전달.
- `ResumeTrackInitiator` (수정) — `initiate(.., position, techStack)` 시그니처 확장. `InterviewContextBuilder.build` 호출 시 `ContextBuildRequest` 1급 필드로 전파.
- `Project` record (수정) — `DepthSignals depthSignals` (nullable) 추가.
- `DepthSignals` record (신규) — `domain/resume/entity/` 위치.
- `resume-extractor.txt` (수정) — 출력 schema 에 `depth_signals` 객체 추가 + 예시 보강.
- `resume-question-generator.txt` (재설계) — 토큰 슬롯 5개 + 출력 schema `depth_type` 추가.
- `QuestionDepthType` enum (신규) — `domain/question/entity/`. TRADEOFF / LIMITATION / QUANTITATIVE / ALTERNATIVE / PRINCIPLE.
- `Question` 엔티티 (수정) — `depthType` 필드 (`@Enumerated(EnumType.STRING)`, nullable, length 20).
- `GeneratedResumeQuestions.GeneratedResumeQuestion` (수정) — `depthType` 필드 추가 (opener 는 null 허용).
- `ResumeQuestionPersister.ResumeQuestionDraft` + `Question.resume()` (수정) — depthType 전달.
- Flyway `V51__add_question_depth_type.sql` (신규).

---

## Data Model

### 1. DDL — Flyway V51

```sql
-- backend/src/main/resources/db/migration/V51__add_question_depth_type.sql
ALTER TABLE question
    ADD COLUMN depth_type VARCHAR(20) NULL COMMENT 'Resume main 질문 깊이 유형 (TRADEOFF/LIMITATION/QUANTITATIVE/ALTERNATIVE/PRINCIPLE). opener / 표준 트랙은 NULL.';
```

근거: nullable → 기존 row + opener 영향 없음. 백필 불필요 (Goal 검증 = 신규 인터뷰만).

### 2. Java — Project + DepthSignals

```java
// backend/src/main/java/com/rehearse/api/domain/resume/entity/DepthSignals.java
@JsonIgnoreProperties(ignoreUnknown = true)
public record DepthSignals(
        List<String> tradeoffs,          // 트레이드오프 (예: "Redis vs Memcached → TTL 정책")
        List<String> alternatives,        // 대안 비교 (예: "Polling vs WebSocket → Polling 채택")
        List<String> quantitative,        // 수치 측정 (예: "p95 800ms → 120ms")
        List<String> decisionRationale    // 의사결정 근거 (예: "Lua 선택, Redis 단일 스레드 활용")
) {
    public DepthSignals {
        tradeoffs = tradeoffs == null ? List.of() : List.copyOf(tradeoffs);
        alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
        quantitative = quantitative == null ? List.of() : List.copyOf(quantitative);
        decisionRationale = decisionRationale == null ? List.of() : List.copyOf(decisionRationale);
    }

    public static DepthSignals empty() {
        return new DepthSignals(List.of(), List.of(), List.of(), List.of());
    }
}
```

```java
// backend/src/main/java/com/rehearse/api/domain/resume/entity/Project.java (수정 diff)
public record Project(
        String projectId,
        String projectName,
        List<String> techStack,
        String role,
        String architecture,
        List<String> decisions,
        DepthSignals depthSignals        // ★ 신규 (nullable)
) {
    public Project {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId 는 필수입니다.");
        }
        techStack = techStack == null ? List.of() : List.copyOf(techStack);
        role = role == null ? "" : role;
        architecture = architecture == null ? "" : architecture;
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        depthSignals = depthSignals == null ? DepthSignals.empty() : depthSignals;  // ★ 호환
    }
}
```

호환 근거: 구버전 JSON (depth_signals 키 부재) → record canonical constructor → `null` → `DepthSignals.empty()`. 신버전 코드 + 구 JSON 안전.

### 3. Java — Question depthType

```java
// backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionDepthType.java
public enum QuestionDepthType {
    TRADEOFF,       // 트레이드오프 검증
    LIMITATION,     // 한계 / 실패 시나리오
    QUANTITATIVE,   // 수치 / 측정 검증
    ALTERNATIVE,    // 대안 비교
    PRINCIPLE       // 동작 원리
}
```

```java
// Question.java 수정 diff
@Enumerated(EnumType.STRING)
@Column(name = "depth_type", length = 20)
private QuestionDepthType depthType;

// resume() factory: depthType 파라미터 추가, opener 일 때 null 허용
public static Question resume(QuestionSet questionSet, QuestionType type,
                               String questionText, String ttsText, String bestAnswer,
                               int orderIndex, QuestionDepthType depthType) {
    // ... (validation 동일)
    Question q = new Question();
    // ...
    q.depthType = depthType;
    return q;
}
```

### 4. Java — GeneratedResumeQuestions

```java
// GeneratedResumeQuestion 확장
public record GeneratedResumeQuestion(
        @JsonProperty("question") String question,
        @JsonProperty("tts_question") String ttsQuestion,
        @JsonProperty("best_answer") String bestAnswer,
        @JsonProperty("depth_type") QuestionDepthType depthType  // ★ opener 는 null
) {
    public GeneratedResumeQuestion {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question 필수");
        }
    }
}
```

### 5. ContextBuildRequest 1급 필드 확장

```java
// backend/src/main/java/com/rehearse/api/infra/ai/context/ContextBuildRequest.java
public record ContextBuildRequest(
        String callType,
        FocusHints focusHints,
        String providerHint,
        Position position,         // ★ 신규 (nullable — resume_question_generator 외 null)
        TechStack techStack        // ★ 신규 (nullable)
) {
    public ContextBuildRequest {
        if (callType == null || callType.isBlank()) {
            throw new IllegalArgumentException("callType must not be blank");
        }
        if (focusHints == null) {
            focusHints = FocusHints.EmptyHints.INSTANCE;
        }
    }
}
```

호출부 영향:
- `ResumeTrackInitiator` → `new ContextBuildRequest("resume_question_generator", hints, providerHint, position, techStack)`.
- `AnswerAnalyzer` / `FollowUpQuestionWriter` → position/techStack 자리에 `null` 전달.

`FocusHints.ResumeQuestionGeneratorHints` 시그니처 = **변경 없음** (기존 4 필드). L4 USER fragment 본문 변경 없음. position/techStack 은 SYSTEM 영역 데이터 → L4 가 알 필요 없음 (책임 경계 보전).

---

## API Contract

### 변경 없음 (BE 단독)

- Question 조회 응답 DTO 에 depthType 노출 X (사용자 결정).
- BE 신규 endpoint X.
- 외부 API 호출 (OpenAI/Claude) request shape 동일 — 메시지 본문만 변경.

### LLM 응답 schema (내부 contract)

```json
{
  "openers": [
    {"question": "...", "tts_question": "...", "best_answer": "..."}
  ],
  "mains": [
    {
      "question": "...",
      "tts_question": "...",
      "best_answer": "...",
      "depth_type": "TRADEOFF | LIMITATION | QUANTITATIVE | ALTERNATIVE | PRINCIPLE"
    }
  ]
}
```

- opener 는 `depth_type` 미사용 (Non-Goals: opener 깊이 분류 X). JSON 미포함 또는 null.
- main 은 `depth_type` 필수. 누락 / unknown 값 시 `AiResponseParser.parseOrRetry` 재시도 → 실패 시 BusinessException.

### Skeleton 추출 schema (내부 contract)

```json
{
  "resume_id": "r_<8>",
  "candidate_level": "junior|mid|senior",
  "target_domain": "backend|frontend|fullstack|devops|data_engineer",
  "projects": [
    {
      "project_id": "p1",
      "project_name": "...",
      "tech_stack": [...],
      "role": "...",
      "architecture": "...",
      "decisions": [...],
      "depth_signals": {
        "tradeoffs": ["Redis vs Memcached → TTL 정책 필요"],
        "alternatives": ["Polling vs WebSocket → Polling 채택"],
        "quantitative": ["p95 800ms → 120ms"],
        "decision_rationale": ["Lua 선택, Redis 단일 스레드 활용"]
      }
    }
  ]
}
```

부재 시 빈 배열 / 빈 객체. 창작 금지 룰 (기존 본문 #1) 적용.

---

## Verification (완료 판정)

- [ ] **Service Integration**: `ResumeQuestionPromptBuilderTest` (Spring 컨텍스트 — PersonaResolver yaml 로딩 의존)
  - 카테고리 사유: PersonaResolver = base yaml 5종 + overlay yaml 14종 classpath 로딩. 순수 Domain Unit 불가 → `ServiceIntegrationSupport` 사용.
  - `buildSystemPrompt(BACKEND, JAVA_SPRING)` → fullPersona / evaluationPerspective / followUpDepth 텍스트 substring 포함.
  - `buildSystemPrompt(FRONTEND, REACT_TS)` → backend 페르소나 어휘 부재 + frontend 페르소나 어휘 포함.
  - 출력에 5 깊이 유형 가이드 텍스트 + 표층 금지 패턴 텍스트 substring 포함.
- [ ] **Domain Unit**: `DepthSignalsTest`
  - 빈 객체 / null 필드 → 빈 List 변환.
  - 구버전 JSON (`depth_signals` 키 부재) Jackson 역직렬화 → `DepthSignals.empty()`.
- [ ] **Service Integration**: `ResumeTrackInitiatorIntegrationTest` (Testcontainers MySQL)
  - Mock LLM 응답 = 5 main 질문 + `depth_type` 다양 → Question 조회 시 `depth_type` 컬럼 적재 확인.
  - Mock 응답에 `depth_type=null` 포함 시 parse 실패 → 재시도 → 최종 BusinessException.
  - opener Question 의 `depth_type` = NULL 확인.
- [ ] **Service Integration**: `ResumeIngestionServiceIntegrationTest`
  - 구버전 skeleton JSON 데이터 (depth_signals 부재) → 신 코드 정상 역직렬화 + Project.depthSignals = empty.
- [ ] **Live LLM E2E** (`@Disabled` + `@EnabledIfEnvironmentVariable(name="RUN_LIVE_API", matches="true")`): `ResumeQuestionGenerationLiveE2ETest`
  - **자동 단언** (assertions):
    - mains size == 5 (product-spec AC #1 본문).
    - 각 main 의 `depth_type` ∈ {TRADEOFF, LIMITATION, QUANTITATIVE, ALTERNATIVE, PRINCIPLE} (enum 파싱 성공).
    - opener `depth_type` == null.
    - **편중 가드**: 5 main 중 동일 `depth_type` ≥4 점유 시 실패 (≤3 통과 — AC #4 표기 일치).
    - **표층 패턴 가드**: 5 main 질문 텍스트 중 `("왜 X 사용", "X 의 장점")` 정규식 매칭 ≤ 1 (AC #3).
  - **수동 단언** (사용자 외부 관찰):
    - 동일 이력서 PDF + BACKEND vs FRONTEND 각각 인터뷰 생성 → 질문 어휘 차이 (AC #1).
    - sample 비교 결과 = `docs/plans/00y-resume-question-prompt-upgrade/` 폴더 보고 파일 기록 (5건).
- [ ] **토큰 측정**: `python3 backend/eval/context/measure_tokens.py` 실행 → resume_question_generator system 토큰 증가량 기록 (예상 +400~600). product-spec Goal "5초 변동 내" 충족 여부 = Live E2E latency 측정 시 검증.
- [ ] **빌드 / 린트**: `./gradlew build` 통과.
- [ ] **회귀**: dev 환경 기존 인터뷰 1건 (V51 적용 전 데이터) 결과 화면 정상 표시 (depth_type = NULL 허용).
- [ ] **관찰**: docker log 에 `main 질문 적재 완료 ... depthTypeCounts={TRADEOFF=N, ...}` INFO 1건.

### 비스코프 (검증 안 함)

- **depth_type 분류 정확도 정량 가드** (예: "LLM 의 5건 분류 정확도 ≥ 80%"). 사유: product-spec Non-Goals "자동 측정 X" 명시 + 사용자 수동 5건 샘플링이 검증 채널. 향후 별도 plan 으로 분리 후보.

---

## Pre / Post State

### Pre (현재)

- `ContextBuildRequest(callType, focusHints, providerHint)` — 3 필드.
- `ResumeTrackInitiator.initiate(interviewId, resumeFileHash, resumePdfBytes, durationMinutes)`.
- `FocusHints.ResumeQuestionGeneratorHints(skeletonJson, openerCount, mainCount, primaryProjectName)`.
- `FixedContextLayer` 가 `resume_question_generator` callType 시 `GLOBAL_CORE + raw resume-question-generator.txt` 결합 (토큰 치환 없음, 직무 무관).
- `resume-question-generator.txt` 28줄, 토큰 슬롯 없음, 직무 페르소나 / 5유형 분배 / 금지 패턴 부재.
- `GeneratedResumeQuestions.GeneratedResumeQuestion (question, ttsQuestion, bestAnswer)`.
- `Question` 엔티티: `(id, questionSet, questionType, questionText, ttsText, bestAnswer, orderIndex, questionPool)`. depth_type 컬럼 없음.
- `Project (projectId, projectName, techStack, role, architecture, decisions)` — depth_signals 부재.
- `resume-extractor.txt` 출력 schema = 4 메타 필드 (tech_stack / role / architecture / decisions).

### Post (구현 후)

- `ContextBuildRequest(callType, focusHints, providerHint, position, techStack)` — 5 필드 (position/techStack nullable).
- `ResumeTrackInitiator.initiate(.., position, techStack)`.
- `FocusHints.ResumeQuestionGeneratorHints` 시그니처 **변경 없음** (4 필드 유지).
- `FixedContextLayer.build` 가 `resume_question_generator` 시 `ResumeQuestionPromptBuilder.buildSystemPrompt(req.position(), req.techStack())` 위임. 다른 callType 기존 동작 유지 (position/techStack = null).
- `AnswerAnalyzer` / `FollowUpQuestionWriter` 호출부: `ContextBuildRequest` 빌드 시 position/techStack 자리에 null 전달.
- `resume-question-generator.txt` 재설계: 토큰 5개 + 5 깊이 유형 분배 가이드 + 표층 금지 패턴 + 출력 schema `depth_type` 추가.
- `GeneratedResumeQuestion (question, ttsQuestion, bestAnswer, depthType)` — main 필수, opener nullable.
- `Question` 엔티티: `depth_type VARCHAR(20) NULL` 컬럼 추가. `resume()` 팩토리 depthType 파라미터 추가.
- `Project` record `depthSignals: DepthSignals` 추가 (nullable, empty fallback).
- `resume-extractor.txt` 출력 schema 에 `depth_signals` 객체 추가 + 예시 보강.

### Diff 요약 (변경 파일)

```
신규
  backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeQuestionPromptBuilder.java
  backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionDepthType.java
  backend/src/main/java/com/rehearse/api/domain/resume/entity/DepthSignals.java
  backend/src/main/resources/db/migration/V51__add_question_depth_type.sql
  backend/src/test/java/com/rehearse/api/infra/ai/prompt/ResumeQuestionPromptBuilderTest.java
  backend/src/test/java/com/rehearse/api/domain/resume/entity/DepthSignalsTest.java
  backend/src/test/java/com/rehearse/api/domain/question/service/ResumeTrackInitiatorIntegrationTest.java   (또는 기존 확장)
  backend/src/test/java/com/rehearse/api/e2e/ResumeQuestionGenerationLiveE2ETest.java
수정
  backend/src/main/java/com/rehearse/api/domain/question/service/ResumeTrackInitiator.java
  backend/src/main/java/com/rehearse/api/domain/question/service/QuestionGenerationService.java          (line 32 호출 인자만)
  backend/src/main/java/com/rehearse/api/infra/ai/context/ContextBuildRequest.java                       (★ 1급 필드 2개 추가)
  backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FixedContextLayer.java
  backend/src/main/java/com/rehearse/api/domain/interview/service/AnswerAnalyzer.java                    (ContextBuildRequest 호출부 null 전달)
  backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpQuestionWriter.java           (동상)
  backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedResumeQuestions.java
  backend/src/main/java/com/rehearse/api/domain/question/entity/Question.java
  backend/src/main/java/com/rehearse/api/domain/resume/entity/Project.java
  backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeQuestionPersister.java
  backend/src/main/resources/prompts/template/resume/resume-question-generator.txt
  backend/src/main/resources/prompts/template/resume/resume-extractor.txt
```

**참고**: ContextLayer 시스템 자체 제거는 별도 Issue **#518** 후속. 본 plan = ContextLayer 잔존 구조 위에서 책임 경계 보전하며 진행.

---

## 위험 / 마이그레이션 / 롤백

### 위험

- **토큰 증가로 latency / 비용 증가**: 시스템 블록 ~+500 토큰 추정. Goal "5초 변동 내" 충족 여부 = Live E2E 단계 측정. 초과 시 가이드 본문 축약 (5유형 가이드 우선 / 페르소나 follow_up_depth 생략 등) 대응.
- **LLM depth_type 분류 오류**: 출력 schema 신뢰도 = LLM 의존. `AiResponseParser.parseOrRetry` 가 enum 파싱 실패 시 재시도. 재시도 후 실패 = BusinessException → 사용자 인터뷰 시작 실패. dev 단계 Live E2E 로 분류 정확도 5건 샘플 검증.
- **`ChatMessage.ofCached` 마킹 정책**: 본 변경 후 SYSTEM 본문 = 직무/스택 조합에 따라 달라짐 → 캐시 hit 률 직무·스택 조합 단위로 분산. 호출당 1회 (인터뷰 시작) 특성상 캐시 효익 미미. **결정**: 기존 마킹 유지 (제거 불필요). 사유: OpenAI/Claude provider 의 caching 정책상 cache miss 도 무해 + 표준 트랙 빌더 패턴과 일관. 마킹 변경은 별도 Issue (Issue #518 제거 시 일괄 재검토).
- **타 callType 회귀 영향**: `AnswerAnalyzer` / `FollowUpQuestionWriter` 호출부 = `ContextBuildRequest` 신규 nullable 필드 자리 null 전달만. `FixedContextLayer` 가 `resume_question_generator` 외 callType 분기에서 position/techStack 미참조 → 동작 영향 없음. canonical constructor 도 position/techStack null 허용.

### 마이그레이션

- DDL: Flyway V51 nullable 컬럼 추가 → zero-downtime, 백필 없음.
- 데이터: skeleton_json 구버전 (depth_signals 부재) → `@JsonIgnoreProperties` + canonical constructor null → empty 처리. 데이터 변환 / backfill 불필요.

### 롤백

- 코드 revert + V51 revert (`ALTER TABLE question DROP COLUMN depth_type`).
- 신규 인터뷰는 raw 템플릿으로 회귀.
- **V51 drop 시 사용자 영향 평가**:
  - 손실 데이터 = `question.depth_type` 컬럼만. `question_text` / `tts_text` / `best_answer` 본문은 보존.
  - 사용자 시연 / 인터뷰 화면 = depth_type 미노출 (BE 단독 결정) → 사용자 가시 영향 0.
  - dev DB 조회로만 사용된 깊이 메타 손실 → 운영 영향 미미.
  - 사전 백업 권장 (mysqldump `question` 테이블) 후 drop.
- skeleton.depth_signals 는 JSON column 내부 → 코드 revert 만으로 무시 (구버전 코드는 unknown field 무시).

---

## 분기 결정

- [x] **단일 영역 → `implement.md` 1개** (BE 단독)
- [ ] BE+FE 동시
- [ ] BE 선행 강제

근거: depthType FE 미노출 (사용자 결정). 변경 = BE 전적. FE 영향 없음.
