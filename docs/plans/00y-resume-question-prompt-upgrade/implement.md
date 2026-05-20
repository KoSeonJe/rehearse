# Implement — Resume 질문 생성 프롬프트 직무 적합성 + 깊이 강제

> **작성자**: create-implement-plan skill (Staff Engineer)
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: BE 단독 작업.
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 커밋 | 의존 |
|-------|------|--------------|--------|------|
| 1 | Migration + Enum/VO 신규 | `backend` | C1 | - |
| 2 | 도메인 확장 (record / 엔티티 / DTO) | `backend` | C2 | 1 |
| 3 | ContextBuildRequest 1급 필드 확장 + 호출부 3개 | `backend` | C3 | - (Phase 1 과 병행 가능) |
| 4 | ResumeQuestionPromptBuilder + 템플릿 재설계 + L1 위임 | `backend` | C4 | 3 |
| 5 | 영속화 (Persister + Question.resume() 팩토리) | `backend` | C5 | 2, 4 |
| 6 | 테스트 (Service Integration / Domain Unit / Live E2E) | `backend` | C6 | 5 |

> Phase 6개 / 임계 8 미달 → tasks/ 분리 불필요. 단일 implement.md.
> 단일 PR (product-spec 통합 phase 결정). Phase = 커밋 분해 단위.
> **검증 / 시연 / 토큰 측정 / 사용자 5건 샘플링 / before-after 기록 = 별도 검증 plan 으로 분리** (본 plan 외).

---

## Phase 1: Migration + Enum/VO 신규

- **구현**: `backend` — Flyway DDL + QuestionDepthType enum + DepthSignals record. 다른 Phase 가 본 결과물 참조.

### 변경 파일
- `backend/src/main/resources/db/migration/V51__add_question_depth_type.sql` — 신규. nullable VARCHAR(20) 컬럼 추가. (tech-spec §Data Model #1)
- `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionDepthType.java` — 신규 enum 5종 (TRADEOFF / LIMITATION / QUANTITATIVE / ALTERNATIVE / PRINCIPLE).
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/DepthSignals.java` — 신규 record. canonical constructor 에서 null → 빈 List 변환. `@JsonIgnoreProperties(ignoreUnknown=true)`. `empty()` 정적 팩토리.

### 핵심 로직 / 변경 요약
- V51 DDL = `ALTER TABLE question ADD COLUMN depth_type VARCHAR(20) NULL COMMENT '...'`.
- enum = 단순 5상수.
- record `DepthSignals(tradeoffs, alternatives, quantitative, decisionRationale)` 모두 `List<String>`. canonical constructor null → `List.of()` + `List.copyOf()` 불변화.

### 의존
- 선행 phase: 없음
- 외부 의존: 없음

### Verification Hook
- `./gradlew flywayInfo` 통과 (V51 PENDING 인식)
- `./gradlew compileJava` 통과
- 통과 기준: 컴파일 + Flyway 인식

### 커밋 메시지 (예상)
```
feat(BE): Resume 질문 depth_type 컬럼 + Enum/VO 신규
```

---

## Phase 2: 도메인 확장 (record / 엔티티 / DTO)

- **구현**: `backend` — Project record 확장 + Question 엔티티 컬럼 + GeneratedResumeQuestions DTO 확장.

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/Project.java` — `DepthSignals depthSignals` 필드 추가. canonical constructor 에 null → `DepthSignals.empty()` 변환. (tech-spec §Data Model #2)
- `backend/src/main/java/com/rehearse/api/domain/question/entity/Question.java` — `@Enumerated(EnumType.STRING)` + `@Column(name="depth_type", length=20)` `depthType` 필드 추가. `resume()` 팩토리 파라미터 1개 추가 (opener 시 null 허용). (tech-spec §Data Model #3)
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedResumeQuestions.java` — `GeneratedResumeQuestion` record 에 `@JsonProperty("depth_type") QuestionDepthType depthType` 추가. opener 는 null 허용, main 은 비-null 가정 (parse 단계에서 enum 파싱 실패 = 재시도). (tech-spec §Data Model #4)

### 핵심 로직 / 변경 요약
- Project canonical constructor 끝에 `depthSignals = depthSignals == null ? DepthSignals.empty() : depthSignals;` 추가.
- Question.resume() 시그니처 = `(questionSet, type, questionText, ttsText, bestAnswer, orderIndex, depthType)`. depthType 만 마지막 인자 추가 — Phase 5 에서 호출부 업데이트.
- GeneratedResumeQuestion 검증 = 기존 `question.isBlank()` 만 유지. depth_type null 검증 = parser 단계.

### 의존
- 선행 phase: Phase 1 (QuestionDepthType / DepthSignals 사용)
- 외부 의존: 없음

### Verification Hook
- `./gradlew compileJava` 통과
- 통과 기준: 컴파일 (호출부는 Phase 5 에서 갱신 → 일시 컴파일 깨질 수 있음 — 같은 커밋 내 Phase 5 호출부도 임시 컴파일러블 상태 유지 필요)

> **주의**: Question.resume() 호출부는 본 Phase 에서 임시 null 전달로 컴파일 통과. Phase 5 에서 실제 depthType 전파.

### 커밋 메시지 (예상)
```
feat(BE): Project/Question/GeneratedResumeQuestion 에 깊이 메타 필드 확장
```

---

## Phase 3: ContextBuildRequest 1급 필드 확장 + 호출부 3개

- **구현**: `backend` — ContextBuildRequest 시그니처 + AnswerAnalyzer / FollowUpQuestionWriter / ResumeTrackInitiator 호출부.

### 변경 파일
- `backend/src/main/java/com/rehearse/api/infra/ai/context/ContextBuildRequest.java` — `Position position`, `TechStack techStack` nullable 필드 추가. canonical constructor 검증 변경 없음 (callType / focusHints 만 유지). (tech-spec §Data Model #5)
- `backend/src/main/java/com/rehearse/api/domain/interview/service/AnswerAnalyzer.java:45` — `new ContextBuildRequest(...)` 호출에 position/techStack 자리 null 전달.
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpQuestionWriter.java:41` — 동상.
- `backend/src/main/java/com/rehearse/api/domain/question/service/ResumeTrackInitiator.java:75` — `new ContextBuildRequest(...)` 호출에 position/techStack 전달. `initiate(...)` 시그니처에 `Position position, TechStack techStack` 추가.
- `backend/src/main/java/com/rehearse/api/domain/question/service/QuestionGenerationService.java:32` — line 32 호출 `resumeTrackInitiator.initiate(interviewId, resumeFileHash, resumePdfBytes, durationMinutes, position, techStack)`.

### 핵심 로직 / 변경 요약
- record 5필드로 확장 → 다른 callType 호출부는 자리 null 전달만.
- ResumeTrackInitiator.initiate 시그니처 확장 → 상위 호출 (QuestionGenerationService) 인자 추가.

### 의존
- 선행 phase: 없음 (Phase 1 과 병행 가능)
- 외부 의존: 없음

### Verification Hook
- `./gradlew compileJava` 통과
- 기존 테스트 (`AnswerAnalyzerTest`, `FollowUpQuestionWriterTest`, `ResumeTrackInitiatorTest`) 컴파일 깨짐 시 호출부 갱신 동시 적용 (같은 커밋 내).
- 통과 기준: 컴파일 + 회귀 테스트 green

### 커밋 메시지 (예상)
```
refactor(BE): ContextBuildRequest 에 position/techStack 1급 필드 추가
```

---

## Phase 4: ResumeQuestionPromptBuilder + 템플릿 재설계 + L1 위임

- **구현**: `backend` — 신규 빌더 클래스 + 템플릿 토큰 슬롯화 + FixedContextLayer 분기.

### 변경 파일
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeQuestionPromptBuilder.java` — 신규 `@Component`. `PersonaResolver` 의존 + `buildSystemPrompt(Position, TechStack)` 메서드. 정적 상수 (DEPTH_GUIDE_5_TYPES / FORBIDDEN_PATTERNS) 보유. 토큰 5개 (`{PERSONA_BLOCK}`, `{EVALUATION_PERSPECTIVE}`, `{FOLLOW_UP_DEPTH}`, `{DEPTH_GUIDE_5_TYPES}`, `{FORBIDDEN_PATTERNS}`) 치환.
- `backend/src/main/resources/prompts/template/resume/resume-question-generator.txt` — 재설계. 토큰 슬롯 5개 + 출력 schema 에 `depth_type` 필드 추가. 5 깊이 유형 분배 가이드 + 표층 금지 패턴 본문 포함.
- `backend/src/main/resources/prompts/template/resume/resume-extractor.txt` — 출력 schema 에 `depth_signals` 객체 (tradeoffs / alternatives / quantitative / decision_rationale 4 배열) 추가 + 예시 보강. (tech-spec §API Contract Skeleton)
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FixedContextLayer.java:103-116` — `build(req)` 분기 추가. `callType == "resume_question_generator"` 시 `resumeQuestionPromptBuilder.buildSystemPrompt(req.position(), req.techStack())` 위임. 그 외 callType 기존 raw 템플릿 유지. `@RequiredArgsConstructor` 의존성 주입 추가.

### 핵심 로직 / 변경 요약
```java
// FixedContextLayer.build 분기
String fixedBlock;
if ("resume_question_generator".equals(req.callType())) {
    fixedBlock = GLOBAL_CORE + "\n" + resumeQuestionPromptBuilder.buildSystemPrompt(req.position(), req.techStack());
} else {
    String skeleton = dynamicSkeletons.get(req.callType());
    // ... 기존 로직 ...
    fixedBlock = GLOBAL_CORE + "\n" + skeleton;
}
return List.of(ChatMessage.ofCached(ChatMessage.Role.SYSTEM, fixedBlock));
```

- ResumeQuestionPromptBuilder 패턴 = `QuestionGenerationPromptBuilder` 답습 (기존 자산).
- DEPTH_GUIDE_5_TYPES 정적 상수 = 5 유형 분배 룰 본문 (예: "main 5건 분배 시 동일 유형 ≥4 점유 금지").
- FORBIDDEN_PATTERNS 정적 상수 = 표층 금지 패턴 본문 (예: "'왜 X 사용', 'X 의 장점' 형식 질문 금지").
- 템플릿 출력 schema 에 `depth_type` enum 5값 명시.

### 의존
- 선행 phase: Phase 3 (ContextBuildRequest 1급 필드 사용)
- 외부 의존: `PersonaResolver` (기존), `prompts/base/*.yaml` + `prompts/overlay/*.yaml` (기존)

### Verification Hook
- `./gradlew compileJava` 통과
- 통과 기준: 컴파일 + FixedContextLayer 분기 직접 호출 테스트 (Phase 6)

### 커밋 메시지 (예상)
```
feat(BE): ResumeQuestionPromptBuilder + 5 깊이 유형 / 표층 금지 + skeleton depth_signals
```

---

## Phase 5: 영속화 (Persister + Question.resume() 팩토리)

- **구현**: `backend` — ResumeQuestionDraft / Persister / Question.resume() 호출부 갱신.

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeQuestionPersister.java:32-58` — `ResumeQuestionDraft (questionType, questionText, ttsText, bestAnswer, orderIndex, depthType)` 필드 추가. `Question.resume(...)` 호출 시 depthType 전달.
- `Question.resume()` 호출부 (Phase 2 임시 null 전달 → 실제 depthType 전파) — opener draft 빌드 시 null, main draft 빌드 시 LLM 응답의 `depthType` 전달.

### 핵심 로직 / 변경 요약
- ResumeQuestionDraft 빌드 site (Persister 내) = LLM 응답 `GeneratedResumeQuestion.depthType()` 그대로 전파.
- opener draft → depthType = null. main draft → depthType = LLM 분류값.
- `log.info("main 질문 적재 완료 ... depthTypeCounts={}", counts)` 관찰성 로그 추가 (tech-spec §Verification 관찰).

### 의존
- 선행 phase: Phase 2 (Question.resume() 시그니처), Phase 4 (LLM 응답 depthType 필드)
- 외부 의존: 없음

### Verification Hook
- `./gradlew compileJava` 통과
- 통과 기준: 컴파일 + Phase 6 통합 테스트 green

### 커밋 메시지 (예상)
```
feat(BE): ResumeQuestionPersister 가 depthType 전파 + 관찰성 로그
```

---

## Phase 6: 테스트 (Service Integration / Domain Unit / Live E2E)

- **구현**: `backend` — Service Integration 2종 + Domain Unit 1종 + Live E2E 1종.

### 변경 파일
- `backend/src/test/java/com/rehearse/api/infra/ai/prompt/ResumeQuestionPromptBuilderTest.java` — 신규. Service Integration (PersonaResolver yaml 로딩 의존). BACKEND/JAVA_SPRING + FRONTEND/REACT_TS 케이스. 5 깊이 가이드 + 금지 패턴 substring 검증.
- `backend/src/test/java/com/rehearse/api/domain/resume/entity/DepthSignalsTest.java` — 신규. Domain Unit. 빈 객체 / null 필드 → 빈 List 변환. 구버전 JSON (depth_signals 키 부재) Jackson 역직렬화 → `DepthSignals.empty()`.
- `backend/src/test/java/com/rehearse/api/domain/question/service/ResumeTrackInitiatorIntegrationTest.java` — 신규 또는 기존 확장. Testcontainers MySQL. Mock LLM = 5 main + depth_type 다양 → Question 조회 depth_type 적재 확인 / Mock 응답 depth_type=null → parse 실패 → 재시도 → BusinessException / opener depth_type = NULL 확인.
- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumeIngestionServiceIntegrationTest.java` — 기존 또는 신규. 구버전 skeleton JSON 데이터 (depth_signals 부재) → Project.depthSignals = empty.
- `backend/src/test/java/com/rehearse/api/e2e/ResumeQuestionGenerationLiveE2ETest.java` — 신규. `@Disabled` + `@EnabledIfEnvironmentVariable(name="RUN_LIVE_API", matches="true")`. 자동 단언 5개 (mains size==5 / depth_type enum / opener null / 편중 ≥4 점유 시 실패 / 표층 정규식 ≤1) + 수동 단언 (5건 샘플 docs/plans/00y-resume-question-prompt-upgrade/ 기록).

### 핵심 로직 / 변경 요약
- testing.md 카테고리 분류 강제:
  - Service Integration: PromptBuilder / TrackInitiator / IngestionService
  - Domain Unit: DepthSignals
  - Live E2E: ResumeQuestionGenerationLiveE2ETest
- 외부 API (OpenAI/Claude) 만 Mock. 내부 Service / Repository 실제 주입.
- Live E2E 환경변수 부재 시 자동 skip.

### 의존
- 선행 phase: Phase 5
- 외부 의존: Testcontainers MySQL (기존), OpenAI/Claude API key (Live E2E only)

### Verification Hook
- `./gradlew test --tests "ResumeQuestionPromptBuilderTest"`
- `./gradlew test --tests "DepthSignalsTest"`
- `./gradlew test --tests "ResumeTrackInitiatorIntegrationTest"`
- `./gradlew test --tests "ResumeIngestionServiceIntegrationTest"`
- `RUN_LIVE_API=true ./gradlew test --tests "ResumeQuestionGenerationLiveE2ETest"` (수동 실행)
- 통과 기준: Live 제외 모두 green. Live = 자동 5단언 통과 + 수동 5건 샘플 기록.

### 커밋 메시지 (예상)
```
test(BE): Resume 질문 깊이 / 직무 페르소나 테스트 4종 + Live E2E 추가
```

---

## 통합 Verification

본 plan 범위 = 코드 구현 + 자동 테스트 (Phase 1~6). 수동 검증 / 시연 / 토큰 측정 / 사용자 5건 샘플링 / before-after 기록 = **별도 검증 plan**.

- [ ] Phase 1~6 변경 파일 모두 컴파일 + 자동 테스트 green
- [ ] `./gradlew build` 통과
- [ ] tech-spec §Verification 중 자동 검증 항목만 본 plan 범위 (Domain Unit / Service Integration / Live E2E 자동 단언)
- [ ] 검증 plan 별도 진행 — Goal 측정 / AC 수동 단언 / 토큰 측정 / dev 시연 / before-after 기록 이관

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] 지정 리뷰어 실행 (구현 완료 직후 — 메인 세션 책임)
  - **BE only → `code-reviewer-backend`** (1회 통합 리뷰)
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff (tech-spec §Pre/Post) 일치

## 후속 (PR / 머지)

- PR 생성 = `git-manager` agent + `/create-pr` 스킬.
- PR base = `develop`.
- 머지 = 사용자 명시 승인 후 `gh pr merge --squash`.
- 머지 후 develop 동기화 = `git-manager` agent.
