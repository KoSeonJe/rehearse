# Tech Spec — RESUME 트랙 Question 4개 필드 적재 정상화

> **작성자**: backend agent (Staff Engineer 페르소나)
> **답하는 질문**: 어떻게? 구조 / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄 미러)

RESUME 트랙 Question 의 사용자 가치 메타 (`tts_text` / `model_answer`) DB 적재 결손 + 도메인 고정 분류 메타 (referenceType / feedbackPerspective) 단일 출처 부재 해소. 신규 RESUME Question 100% `tts_text` / `model_answer` not blank + `QuestionType` 단일 출처 매핑 조회 가능.

## Evidence

- 현재 구조:
  - `domain/resume/service/ResumeQuestionPersister.java:25-34` — persist 시그니처 4개 필드 인자 부재
  - `domain/question/entity/Question.java:70-83` — `Question.resume()` 팩토리 4개 필드 미설정
  - `domain/resume/service/PlaygroundModeHandler.java:42-54, 77-99` — `result.ttsQuestion()` 보유 but persist 미전달
  - `domain/resume/service/InterrogationModeHandler.java:48-65` — 동일
  - `domain/resume/service/WrapUpModeHandler.java:35-52` — 동일
  - `infra/ai/prompt/ResumePlaygroundPromptBuilder.java:73-92` — `PlaygroundOpenerResult` / `PlaygroundResponderResult` records 에 `modelAnswer` 필드 부재
  - `infra/ai/prompt/ResumeChainInterrogatorPromptBuilder.java:51-69` — `InterrogationResult` 동일
  - `infra/ai/prompt/ResumeWrapUpPromptBuilder.java:48-54` — `WrapUpResult` 동일
  - `domain/question/entity/QuestionType.java` — enum 단순 (속성 부재)
  - `domain/question/entity/ReferenceType.java` — `MODEL_ANSWER` / `GUIDE` 2값
  - `domain/feedback/entity/FeedbackPerspective.java` — `TECHNICAL` / `BEHAVIORAL` / `EXPERIENCE` 3값
  - `domain/resume/service/ResumeFallbackQuestions.java` — 모드별 폴백 텍스트 상수 패턴 (재사용)
- 다운스트림 영향 검증 (Option X 전제):
  - `domain/feedback/rubric/entity/RubricFamily.java:44-61` `MappingRule#matches()` — `resumeTrack=true` 가 다른 필드 우선 분기. 일치 시 즉시 반환
  - `resources/rubric/_mapping.yaml:21-23` — `resumeTrack: true → resume-v1` = rules 첫 룰
  - → RESUME 트랙 perspective NULL 무영향 **확정** (단언 검증 완료)
  - `domain/feedback/rubric/service/RubricScoringEventListener.java:57` — perspective null 안전 처리
  - `domain/question/dto/QuestionDetailResponse.java:17-28` / `AnswerResponse.java:15-33` — referenceType / feedbackPerspective 응답 노출. 본 PR 후 RESUME 트랙 NULL 유지 (비스코프)
- 외부 레퍼런스: STANDARD 트랙 `QuestionSetAssembler` 4개 필드 적재 패턴 (참고만, 본 PR 영향 X)
- 사용자 발화 (특정 결정 근거):
  - "option x로 가자" — DB 적재 vs enum 속성 정규화 결정
  - "일단은 technical 로 가고 추후 개선하자" — INTERROGATION = TECHNICAL 정적 매핑 (다양성 별도 plan)
  - "이력서인데 다 guide" — referenceType 모두 GUIDE
- 추정 / 미확인 가정:
  - 없음 (RubricFamily 우선순위 + yaml 순서 + Listener null 처리 모두 코드 단언)

## Trade-offs

### Option A (채택): Generator 단일 컴포넌트 + enum 속성 정적 매핑

- 장점:
  - 검증 / 재시도 / 폴백 책임 단일화 (`ResumeQuestionResultGenerator`) — modeHandler 4곳 중복 제거
  - promptBuilder 책임 = 프롬프트 + LLM 호출 + 파싱만 유지 (검증 책임 격리)
  - `QuestionType` enum 속성 = 다운스트림이 `questionType.referenceType()` 단일 호출로 매핑 조회. 단일 출처
  - DB 적재 = `ttsText` / `modelAnswer` 만 (사용자 가치 인스턴스별 다름). 정규화 유지
  - 클래스명 `*Generator` = 컨벤션 책임 명사 표 정합 (`backend/.claude/rules/conventions.md` "클래스 접미사")
  - 위치 `domain/resume/service/` = 애플리케이션 서비스 / 외부 의존 (promptBuilder → ResilientAiClient) 조립 + 검증 책임. modeHandler 와 동거 (`ResumeFallbackQuestions` 패턴 재사용)
- 단점:
  - Generator 메서드 4개 (모드별 시그니처 다름) — 단일 클래스에 4 메서드 동거. modeHandler 와 1:1 결합
  - INTERROGATION perspective = TECHNICAL 정적. 다양성 표현 불가 (별도 plan 으로 미룸)
- 사유: 사용자 가치 (modelAnswer 적재) + 정규화 (도메인 고정값 enum) + 단순성 (Generator 단일) 균형. 본 PR 핵심 결손 해소

### Option B (폐기): modeHandler 안 검증 인라인

- 장점: Provider 신규 클래스 부재. 단순
- 폐기 사유: 검증 / retry / 폴백 로직 4곳 중복. record retry 시 객체 전체 교체 필요 → 인라인 시 modeHandler 비대화

### Option C (폐기): perspective LLM 결정 + DB 적재

- 장점: INTERROGATION 다양성 표현
- 폐기 사유: Option X 무효 — 인스턴스별 다름 = DB 적재 부활. 정규화 위반. 본 PR 범위 ↑. 사용자 결정 "추후 개선"

### Option D (폐기): `ResumeFallbackModelAnswers` 정적 텍스트 vs LLM 재시도 무한

- 폐기 사유: 무한 재시도 = latency / 비용 무제한. **1회 재시도 + 폴백 정적 텍스트** 채택 (product-spec AC 합의)

## Architecture

```
[modeHandler]                         [Generator]                          [PromptBuilder]
PlaygroundModeHandler.handleOpener()
   ↓ generator.generateOpener(...)
                              ResumeQuestionResultGenerator
                              .generateOpener()
                                 ↓ playgroundBuilder.buildOpener()
                                                                 → LLM (ResilientAiClient)
                                                                 ← PlaygroundOpenerResult
                                 ← result
                                 modelAnswer blank? 
                                  → 1회 retry: playgroundBuilder.buildOpener() 재호출
                                    여전히 blank?
                                     → result.withModelAnswer(ResumeFallbackModelAnswers.OPENER)
                                 return result (modelAnswer not blank 보장)
   ← result
   ↓ questionPersister.persist(interviewId, RESUME_OPENER, question, ttsText, modelAnswer, orderIndex)
                                                                                 ↓
                                                                       Question.resume(qs, type, q, tts, ma, idx)
                                                                                 ↓
                                                                       questionRepository.save(q)
```

4 modeHandler 모두 동일 패턴 (모드별 메서드 시그니처만 다름).

**신규 클래스 패키지 / 위치**:
- `com.rehearse.api.domain.resume.service.ResumeQuestionResultGenerator` (`@Component`, public)
- `com.rehearse.api.domain.resume.service.ResumeFallbackModelAnswers` (package-private final, 정적 상수만 — 기존 `ResumeFallbackQuestions` 패턴 일치)

**Generator 메서드 시그니처** (4개, 모드별 promptBuilder 인자 그대로 위임):

| 메서드 | 인자 | 반환 |
|---|---|---|
| `generateOpener` | `interviewId, state, project, phase` | `PlaygroundOpenerResult` |
| `generatePlaygroundResponder` | `interviewId, state, previousExchanges, userAnswer, expectedClaims, turnCount, cumulativeLength` | `PlaygroundResponderResult` |
| `generateInterrogation` | `interviewId, state, previousExchanges, chainTopic, currentLevel, answerQuality, userAnswer, consecutiveStay` | `InterrogationResult` |
| `generateWrapUp` | `interviewId, state, previousExchanges, sessionSummary, remainingMinutes, isRetrospective` | `WrapUpResult` |

**폴백 적용 시 로깅** (P1-4): Generator 가 폴백 텍스트 적용 시 `log.warn` 한국어 + interviewId + mode key=value placeholder. 기존 `ResumeFallbackQuestions` 사용 시 modeHandler 의 `log.warn("[Handler] 안전 폴백 사용 감지: interviewId={}, ...")` 패턴 일치. 예시:
```java
log.warn("[ResumeQuestionResultGenerator] modelAnswer 폴백 적용: interviewId={}, mode={}", interviewId, "OPENER");
```

**Interrogation 동시성 (P1-5)**: 현 `InterrogationModeHandler` 구조 — Phase 2 (LLM 호출) = lock 밖 / Phase 3 (persist + tracker 상태) = lock 안. **Generator retry 는 Phase 2 (lock 밖) 안에서만 처리**. Phase 3 lock 점유 시간 무변경. 다른 인터뷰 턴 지연 영향 0.

`QuestionType` enum 정적 속성:
```
RESUME_OPENER       → (GUIDE, EXPERIENCE)
RESUME_PLAYGROUND   → (GUIDE, EXPERIENCE)
RESUME_INTERROGATION→ (GUIDE, TECHNICAL)
RESUME_WRAP_UP      → (GUIDE, BEHAVIORAL)
MAIN / FOLLOWUP     → (null, null)   // STANDARD 트랙은 entity 컬럼 사용 (변경 없음)
```

다운스트림 활용 (향후): `question.getQuestionType().referenceType()` 또는 `QuestionType.RESUME_OPENER.feedbackPerspective()`. 본 PR 은 enum 속성 정의 + 매핑 조회 가능까지만.

## Data Model

**스키마 변경 없음**. RESUME 트랙 `reference_type` / `feedback_perspective` 컬럼 NULL 유지 (STANDARD 트랙 사용 중이라 컬럼 자체 유지).

### Entity 변경

`Question.java`:

```java
public static Question resume(QuestionSet questionSet, QuestionType type,
                              String questionText, String ttsText, String modelAnswer,
                              int orderIndex) {
    requireValidQuestionText(questionText);
    requireNonNullQuestionType(type);
    if (type == QuestionType.MAIN || type == QuestionType.FOLLOWUP) {
        throw new IllegalArgumentException("resume() 팩토리는 RESUME_* 타입만 허용합니다: " + type);
    }
    Question q = new Question();
    q.questionSet = questionSet;
    q.questionType = type;
    q.questionText = questionText;
    q.ttsText = ttsText;
    q.modelAnswer = modelAnswer;
    q.orderIndex = orderIndex;
    // referenceType / feedbackPerspective = null (Option X)
    return q;
}
```

### Enum 변경

`QuestionType.java`:

```java
public enum QuestionType {
    MAIN(null, null),
    FOLLOWUP(null, null),
    RESUME_OPENER(ReferenceType.GUIDE, FeedbackPerspective.EXPERIENCE),
    RESUME_PLAYGROUND(ReferenceType.GUIDE, FeedbackPerspective.EXPERIENCE),
    RESUME_INTERROGATION(ReferenceType.GUIDE, FeedbackPerspective.TECHNICAL),
    RESUME_WRAP_UP(ReferenceType.GUIDE, FeedbackPerspective.BEHAVIORAL);

    private final ReferenceType referenceType;
    private final FeedbackPerspective feedbackPerspective;

    QuestionType(ReferenceType refType, FeedbackPerspective perspective) {
        this.referenceType = refType;
        this.feedbackPerspective = perspective;
    }

    public ReferenceType referenceType() { return referenceType; }
    public FeedbackPerspective feedbackPerspective() { return feedbackPerspective; }
}
```

### Record 변경 (4종)

`PlaygroundOpenerResult` / `PlaygroundResponderResult` / `InterrogationResult` / `WrapUpResult` 모두 `modelAnswer` 필드 추가 + JSON 매핑 (`@JsonProperty("model_answer")`).

각 record `withModelAnswer(String)` 메서드 추가 (Provider 폴백 적용용 — record 불변성 유지).

### Prompt Template 변경 (4개)

`resources/prompts/template/resume/*.txt`:
- `resume-playground-opener.txt`
- `resume-playground-responder.txt`
- `resume-chain-interrogator.txt`
- `resume-wrap-up.txt`

각 JSON schema 안 `model_answer` 필드 + 가이드 텍스트 형태 명시 (RESUME 트랙 = GUIDE 성격, 정답 X / 답변 가이드라인). 글자수 제약 (예: 50-200자).

## API Contract

**변경 없음** (응답 DTO 노출 변경은 비스코프). 본 PR 은 BE 단일 영역.

## Verification (완료 판정)

구현 완료 = 아래 모두 통과.

- [ ] **Domain Unit (`QuestionTypeTest`)** — 4 RESUME_* 모드 → `referenceType()` = `GUIDE` + `feedbackPerspective()` 매핑 단언 (`OPENER/PLAYGROUND=EXPERIENCE`, `INTERROGATION=TECHNICAL`, `WRAP_UP=BEHAVIORAL`). MAIN / FOLLOWUP → 둘 다 null
- [ ] **Domain Unit (`QuestionResumeFactoryTest`)** — `Question.resume()` 6-인자 시그니처 → ttsText / modelAnswer 적재 / RESUME_* 외 type → IllegalArgumentException
- [ ] **Domain Unit (record `withModelAnswer` 4종)** — `PlaygroundOpenerResultTest` / `PlaygroundResponderResultTest` / `InterrogationResultTest` / `WrapUpResultTest` 각각 `withModelAnswer(String)` 호출 시 modelAnswer 만 교체 + 다른 필드 (question / ttsQuestion / reason / 모드별 추가 필드) 보존 검증
- [ ] **Service Integration (`ResumeQuestionResultGeneratorTest`)** — 4 modeHandler 매핑 메서드 각각:
  - 정상 LLM (modelAnswer not blank) → 1회 호출 + 결과 그대로 반환
  - 1차 blank + 2차 not blank → 2회 호출 + 2차 결과 반환
  - 1차 blank + 2차 blank → 2회 호출 + `ResumeFallbackModelAnswers.<MODE>` 적용 result 반환 + `log.warn` 발생
  - **Mock 정책**: `ResilientAiClient` Mock 만 (외부 API). `promptBuilder` = 실제 주입 (testing.md "내가 만든 코드 Mock 금지" 룰). Mock 응답 JSON = `model_answer` 필드 포함 / 미포함 / blank 시나리오 정확 구성 (promptBuilder 가 실제 파싱 가능한 JSON 형태)
- [ ] **Service Integration (`PlaygroundModeHandlerIntegrationTest` / 3개 Handler)** — provider 호출 → persist 호출 → DB row 검증:
  - `tts_text` 적재 (length ≥ 10)
  - `model_answer` 적재 (not blank)
  - `reference_type` / `feedback_perspective` = NULL (RESUME 트랙 컬럼 NULL 유지)
- [ ] **회귀 — STANDARD 트랙** — `QuestionSetAssembler` 경로 4개 필드 적재 동작 변경 0건. 기존 통합 테스트 통과
- [ ] **빌드** — `./gradlew build` 통과
- [ ] **관찰 가능 동작** — RESUME 트랙 인터뷰 1회 (4 모드 모두 트리거) → DB 직접 조회:
  ```sql
  SELECT question_type, tts_text, model_answer, reference_type, feedback_perspective
  FROM question
  WHERE question_set_id = (최근 RESUME_BASED set);
  ```
  → tts_text / model_answer 적재. reference_type / feedback_perspective = NULL (Option X 의도)

## Pre / Post State

### Pre (현재)

```
Question.resume(qs, type, questionText, orderIndex)         // 4-인자
ResumeQuestionPersister.persist(id, type, text, idx)        // 4-인자
PlaygroundOpenerResult(question, ttsQuestion, reason)       // modelAnswer 부재
PlaygroundResponderResult(...)                              // modelAnswer 부재
InterrogationResult(...)                                    // modelAnswer 부재
WrapUpResult(...)                                           // modelAnswer 부재
QuestionType { MAIN, FOLLOWUP, RESUME_* }                   // 속성 부재
modeHandler.handle(): promptBuilder.build() → persist() (검증 인라인)
DB question 행 RESUME 트랙: tts_text=NULL / model_answer=NULL / reference_type=NULL / feedback_perspective=NULL
```

### Post (구현 후)

```
Question.resume(qs, type, questionText, ttsText, modelAnswer, orderIndex)  // 6-인자
ResumeQuestionPersister.persist(id, type, text, ttsText, modelAnswer, idx) // 6-인자
PlaygroundOpenerResult(question, ttsQuestion, reason, modelAnswer)         // 추가
PlaygroundResponderResult(..., modelAnswer)                                // 추가
InterrogationResult(..., modelAnswer)                                      // 추가
WrapUpResult(..., modelAnswer)                                             // 추가
QuestionType { ..., RESUME_* (refType=GUIDE, perspective=...) }            // 속성 추가
ResumeQuestionResultGenerator 신규 (검증 + 1회 retry + 폴백)
ResumeFallbackModelAnswers 신규 (모드별 폴백 텍스트 상수)
modeHandler.handle(): provider.provide<Mode>() → persist() (검증 책임 Provider 위임)
DB question 행 RESUME 트랙: tts_text 적재 / model_answer 적재 / reference_type=NULL / feedback_perspective=NULL
```

## 위험 / 마이그레이션 / 롤백

- **위험**:
  - LLM 응답 schema 변경 (model_answer 추가) → 기존 prompt 응답 파싱 회귀. 영향: prompt 템플릿 4개 동시 변경 + record 4개 동시 변경 → 동시 머지 강제 (단일 PR)
  - 1회 retry → LLM 비용 / latency 일시 ↑. 정량 추정:
    - blank 빈도 추정 < 5% (LLM 정상 동작 + prompt schema 에 `model_answer` 필수 명시 시 — **추정**, 실측 데이터 부재. 머지 후 운영 모니터링으로 검증 필요)
    - 최악 1턴 = LLM 호출 2회 + 폴백 0회 (LLM X) 또는 LLM 호출 2회 + 폴백 텍스트 적용
    - 평균 추가 호출률 ≤ 5% × 1회 retry = **평균 LLM 호출량 +5% 이내** (추정)
    - latency: retry 발생 턴만 +1 LLM 호출 latency (~2-5초). 사용자 인지 (꼬리질문 응답 대기) 영역
- **마이그레이션 전략**:
  - 스키마 변경 없음 → DDL 무. 백필 비스코프
  - 신규 RESUME Question 행만 적재. 기존 행 NULL 유지 (운영 백필 SQL 별도)
- **롤백**:
  - PR 단일 머지 → revert 로 즉시 롤백 가능
  - DB 데이터 영향 없음 (NULL 행 변경 안 됨)
  - prompt 템플릿 + 코드 record 동시 revert 필요 (동시 머지로 분리 안 됨)

## 분기 결정

- [x] **단일 영역 → `implement.md` 1개**
- [ ] BE+FE 동시
- [ ] BE 선행 강제

근거: BE 단독. 응답 DTO 변경 비스코프 → FE 영향 0. `implement.md` 단일.

## 비스코프 (재확인)

product-spec 비스코프 5개 + 본 tech-spec 결정:

- DB `reference_type` / `feedback_perspective` 컬럼 적재 X (Option X)
- 응답 DTO 노출 경로 변경 X (RESUME 트랙 NULL 유지, 별도 plan)
- INTERROGATION perspective 다양성 / 분리 X (TECHNICAL 정적, 별도 plan)
- 운영 백필 SQL X
- prompt 콘텐츠 품질 튜닝 X (schema 추가만)
- #409 question_score X
