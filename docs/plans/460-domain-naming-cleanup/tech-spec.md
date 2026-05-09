# Tech Spec — 도메인 enum/필드/변환 네이밍 충돌 정리 (Phase 1)

> **작성자**: 메인 세션 (Staff Engineer 페르소나, `/create-tech-spec` 스킬)
> **답하는 질문**: 어떻게? 구조 / 리네이밍 매핑 / 변환 단일 출처화 / 분기
> **승인 게이트**: ★ 사용자 명시 승인 후 implement-be.md / implement-fe.md 진입 ★

---

## Why → Goal (1줄 미러)

같은 의미를 나타내는 코드 식별자가 여러 다른 이름으로 존재 → "같은 의미면 같은 이름" 원칙으로 통일. 동작 변경 0, BE 단위/통합/E2E 100% 통과.

## Evidence

- 현재 구조 (분석 직접 확인 완료):
  - `interview.Perspective` (7개) — `interview/entity/Perspective.java`
  - `feedback.FeedbackPerspective` (3개: TECHNICAL/BEHAVIORAL/EXPERIENCE) — `feedback/entity/FeedbackPerspective.java`
  - `QuestionSetCategory` (12개) — `question/entity/QuestionSetCategory.java`
  - `InterviewType` (12개, +CacheStrategy) — `interview/entity/InterviewType.java` — **값 12개 100% 동일**
- 변환 함수 좌표 (정의 위치 / 호출 위치 분리):
  - `formatPerspectives()` **정의 5곳**: `AnswerAnalyzerPromptBuilder:70`, `AudioTurnAnalyzerPromptBuilder:66`, `FollowUpPromptBuilder:126`, `AnswerAnalysisJsonRenderer:47`, `AnswerAnalyzer:101`. 호출 위치: `AnswerAnalyzerPromptBuilder:54`, `AudioTurnAnalyzerPromptBuilder:50`, `FollowUpPromptBuilder:103,106`, `AnswerAnalysisJsonRenderer:24,27`, `AnswerAnalyzer:52`.
  - `toReferenceLabel()` **정의 3곳**: `AnswerAnalyzerPromptBuilder:60`, `AudioTurnAnalyzerPromptBuilder:56`, `AnswerAnalyzer:91`. 호출 위치: `AnswerAnalyzerPromptBuilder:49`, `AudioTurnAnalyzerPromptBuilder:48`, `AnswerAnalyzer:51`.
  - `FollowUpRequest.FollowUpExchange.answer` 1곳 (`FollowUpRequest.java:34`) vs 다수 `answerText` 표준.
- `FollowUpExchange` cascade 영향 (BE 15 파일):
  - DTO: `FollowUpRequest.java`
  - Domain: `AskedPerspectives.java`, `InterviewRuntimeState.java`, `TurnAnalysisPipeline.java`
  - Resume Domain: `ResumeQuestionResultGenerator.java`, `PlaygroundModeHandler.java`, `ResumeInterviewOrchestrator.java`, `InterrogationModeHandler.java`
  - Infra: `FollowUpGenerationRequest.java`, `DialogueCompactor.java`, `ContextBuildRequest.java`, `DialogueHistoryLayer.java`, `AbstractResumeJsonPromptBuilder.java`, `ResumeChainInterrogatorPromptBuilder.java`, `ResumePlaygroundPromptBuilder.java`
  - Lombok `@Getter` 생성 `.answer()` getter 호출 = `.answerText()` 일괄 갱신.
- `selectedPerspective` 필드 사용처 (5곳):
  - `FollowUpRequest.java:36` (Phase 1 변경 대상 = JSON 요청 키)
  - `FollowUpResponse.java:24` (Phase 1 변경 대상 = JSON 응답 키)
  - `FollowUpService.java:162` (`.selectedPerspective(followUp.getSelectedPerspective())` Builder 호출)
  - `GeneratedFollowUp.java:36,55` (infra DTO 필드)
  - `InterviewRuntimeState.java:128-129` (주석 — 의미만 언급, 코드 변경 없음)
- `AskedPerspectives` (record, `interview/entity/AskedPerspectives.java`):
  - `record AskedPerspectives(List<Perspective> values)` 의 제네릭 타입 = Perspective rename 시 동반 변경 (`List<AnswerFeedbackPerspective>`). 클래스명 / 변수명 (`askedPerspectives` 41건) 자체는 product-spec 카탈로그 #10 = "보류" → Phase 1 비스코프.
- `AnswerResponse.from()` 호출처:
  - 정의: `AnswerResponse.java:22` `from(QuestionAnswer, QuestionSetCategory)`
  - 호출: `InternalQuestionSetService.java:88` `AnswerResponse.from(answer, questionSet.getCategory())` — `questionSet.getCategory()` 반환 타입이 `InterviewType` 으로 변경되면 시그니처도 cascade.
- DB 컬럼 검증:
  - `question.feedback_perspective` 컬럼 — V16 추가 → **V46 에서 DROP 완료** (`V46__drop_question_classification_meta.sql`). 현재 DB 스키마 없음. 코드 = QuestionType 정적 매핑 derive.
  - `question_set.category VARCHAR(50)` — V21 마이그 후 값 = `RESUME_BASED` / `CS_FUNDAMENTAL` / 그 외 12개 enum 값. **InterviewType 12개와 100% 동일** → `@Enumerated(STRING)` 타입 교체 시 DDL/DML 0.
- FE wire 검증 (현재 양쪽 일치 = broken 아님):
  - 송신: `frontend/src/hooks/use-answer-flow.ts:342-346` `previousExchanges = history.map((e) => ({ question, answer, followUpType }))` — 키 `answer`.
  - 수신: BE `FollowUpRequest$FollowUpExchange.answer` (line 34) — 키 `answer`. 일치 ✅. Phase 1 변경 = 양쪽 동시 `answerText`.
  - FE `interview-store.ts:223` `answer: answerText` = store 내부 Map 객체 키 (BE wire 와 무관).
- product-spec 매핑: 카탈로그 #1, #5, #7, #8, #9 → Phase 1.
- 사용자 결정 근거 (이번 세션):
  - `interview.Perspective` → `AnswerFeedbackPerspective` (Staff 우려 = cross-domain word pollution 사용자 override).
  - `feedback.FeedbackPerspective` → `RubricCategory` ("Perspective 단어 제거 + 사용처(rubric 도메인) 단어 일치"). 파일 이동: `feedback/entity/` → `feedback/rubric/entity/`.
  - `FollowUpExchange.answer` → `answerText` + JSON 키 동시 변경 (BE+FE Phase 1).
  - `AnswerResponse.feedbackPerspective` (top-level 필드 / JSON 키) / `TimestampFeedbackResponse$TechnicalFeedback.perspective` (inner 필드 / JSON 키, `technicalFeedback` 객체 내부) → `rubricCategory` (BE+FE 동시).
  - `selectedPerspective` (필드명 + JSON 키) → `selectedAnswerFeedbackPerspective` Phase 1 포함.
  - `QuestionSet.category` → `InterviewType` 교체. **CacheStrategy 노출 수용** (YAGNI: 현재 호출처 없음 → 노출 발생 시 분리).
  - 운영 윈도우 = BE+FE 즉시 연속 머지 수용 (JsonAlias 호환 레이어 미사용).
- 추정 / 미확인:
  - YAML `_mapping.yaml:38` `feedbackPerspective:` → `rubricCategory:` 키 변경 = 코드만 영향 (값 EXPERIENCE 등은 Phase 2). Phase 1 처리 가능.
  - V46 dropped column 재추가 흔적 없음 → DB 마이그 0.

## Trade-offs

### Trade-off 1: `interview.Perspective` 새 이름

#### Option A (채택): `AnswerFeedbackPerspective`
- 장점: 사용 맥락 (답변 분석 시 "어떤 관점으로 답했는가") 이름 직접 반영. 사용자 결정.
- 단점: `Feedback` 단어가 interview 도메인에 등장 (cross-domain word pollution). `Perspective` 도 잔존하지만 `feedback.FeedbackPerspective → RubricCategory` 와 동시 변경 시 **단어 중복 0** 달성.
- 사유: 사용자 명시 결정. feedback 측 동시 리네이밍으로 Perspective 단어 충돌 해소.

#### Option B (폐기): `AnswerAnalysisDimension` / `AskedDimension`
- 장점: Feedback 단어 미사용 → cross-domain 청결.
- 폐기 사유: 사용자 결정 우선. AnswerFeedbackPerspective 가 코드에서 사용되는 의미 (답변 피드백 시 어떤 차원) 와 직결.

### Trade-off 2: `feedback.FeedbackPerspective` 새 이름

#### Option A (채택): `RubricCategory`
- 장점: 최대 사용처 (RubricLoader/_mapping.yaml) 가 rubric 도메인 → 단어 일치. Perspective 단어 제거 (interview 측 AnswerFeedbackPerspective 의 Perspective 와 단어 분리).
- 단점: 정의 위치 `feedback/entity/` 와 단어(Rubric) 가 약간 이질적. 그러나 사용자 명시 결정으로 수용.
- 사유: 사용자 결정 (2026-05-09) — 사용처(rubric 도메인) 단어 일치 우선.

#### Option B (폐기): `FeedbackCategory` / `EvaluationCategory`
- 장점: Feedback 도메인 소속 명시.
- 폐기 사유: 최대 사용처(rubric)와 단어 불일치. 사용자 결정으로 RubricCategory 채택.

### Trade-off 3: `QuestionSetCategory` 처리

#### Option A (채택): `InterviewType` 으로 교체 (`QuestionSetCategory` enum 삭제)
- 장점: 동일 12값 두 enum 활성 상태 해소. DDL/DML 0 (값 동일 + `@Enumerated(STRING)`).
- 단점:
  - `question` 도메인이 `interview` 도메인 enum 임포트 (cross-domain). 인접 plan `427-standard-track-classification-enum` 마이그 흔적 = 통합 방향 ✅.
  - **CacheStrategy 부속 노출**: `InterviewType` 은 `CacheStrategy` (CACHEABLE / FRESH) 부속 보유. `questionSet.getCategory().getCacheStrategy()` 호출 가능해짐 = caching 관심사가 question 도메인에 노출. **사용자 결정 = 수용** (YAGNI: 현재 호출처 0건 → 실제 노출 발생 시 그때 분리).
- 사유: V21 마이그 + 12값 일치 = "이미 통합 의도" 단서. 두 enum 양쪽 활성 = 부채. CacheStrategy 노출은 잠재적 이슈로만 인지하고 발생 시 별도 처리.

#### Option B (폐기): `QuestionSet.getCategory()` getter wrapping 으로 CacheStrategy 숨김
- 장점: caching 관심사 노출 차단.
- 폐기 사유: 추가 코드 = product-spec Phase 1 "동작 변경 0 / 코드 식별자 변경만" 원칙에 추상화 도입. YAGNI 위반.

#### Option C (폐기): 분리 유지 (책임 명시 문서화)
- 장점: 도메인 경계 보존.
- 폐기 사유: 12값 100% 동일 + 의미 차이 부재 → 분리 사유 없음.

### Trade-off 4: `formatPerspectives` / `toReferenceLabel` 위치

#### Option A (채택): `infra/ai/prompt/PromptFormatters` 유틸 클래스 (final + private 생성자)
- 장점: 사용 맥락 (PromptBuilder) 와 같은 패키지. infra-only 의존. 단일 출처.
- 단점:
  - 정적 유틸 = 테스트 시 모킹 어려움 (값 단순 변환 → 모킹 불필요).
  - `infra/ai/prompt` 가 `interview` (AnswerFeedbackPerspective) + `question` (ReferenceType) 두 도메인 entity 임포트 (현재도 PromptBuilder 들이 동일 의존 = 새로운 결합 도입 아님).
- 사유: PromptBuilder 모두 infra 패키지. 도메인 모델 의존성 최소화.

#### Option B (폐기): `domain/interview/service` 또는 `feedback/` 하위
- 장점: 도메인 응집도.
- 폐기 사유: 변환 = LLM 프롬프트 라벨 출력 (infra 책임). 도메인 로직 아님.

## Architecture

### Phase 1 변경 범위 (BE)

#### 1. enum 클래스 rename (기계적 + 임포트 cascade)

```
domain/interview/entity/Perspective.java → AnswerFeedbackPerspective.java
  영향: 임포트 6+ 파일 (AnswerAnalyzer, FollowUp* PromptBuilder, AskedPerspectives 등)
  AskedPerspectives record 컴포넌트 타입 List<Perspective> → List<AnswerFeedbackPerspective>
  변수명 askedPerspectives (41건) = 잔존 (product-spec #10 보류 = 비스코프)

domain/feedback/entity/FeedbackPerspective.java → domain/feedback/rubric/entity/RubricCategory.java
  영향: QuestionType (정적 매핑), RubricFamily, RubricLoader, AnswerResponse,
        TimestampFeedbackResponse, 4 테스트 파일

domain/question/entity/QuestionSetCategory.java → 삭제
  QuestionSet.category 필드 타입 → InterviewType (@Enumerated(STRING) 유지)
  영향: QuestionSetRepository (메서드 시그니처), QuestionSetAssembler,
        ResumeQuestionPersister, ResumeTurnEventPublisher, ResumeInterviewOrchestrator,
        FollowUpTransactionHandler, RubricFamily, RubricLoader, AnswerResponse.from()
```

#### 2. DTO 필드명 + JSON 키 변경 (BE+FE 동시)

```
domain/interview/dto/FollowUpRequest$FollowUpExchange (line 32-45)
  필드: answer → answerText
  생성자 시그니처: FollowUpExchange(question, answer) / (question, answer, followUpType)
                → (question, answerText) / (question, answerText, followUpType)
  Lombok @Getter 생성 .answer() → .answerText() (15 파일 cascade)

domain/interview/dto/FollowUpRequest$FollowUpExchange.selectedPerspective
                → selectedAnswerFeedbackPerspective (필드 + JSON 키)

domain/interview/dto/FollowUpResponse.selectedPerspective (line 24)
                → selectedAnswerFeedbackPerspective (필드 + JSON 키 + Builder + 호출처)
  영향: FollowUpService:162 (.selectedPerspective(...)), GeneratedFollowUp.java:36,55

domain/question/dto/AnswerResponse
  feedbackPerspective → rubricCategory (필드 + Builder + JSON 키)
  AnswerResponse.from(QuestionAnswer, QuestionSetCategory)
                → from(QuestionAnswer, InterviewType)
  호출처: InternalQuestionSetService:88 (questionSet.getCategory() 반환 타입 자동 호환)

domain/feedback/dto/TimestampFeedbackResponse$TechnicalFeedback (inner static class, line 75)
  perspective → rubricCategory (inner 필드 line 76 + JSON 키, technicalFeedback 객체 내부)
  toTechnicalFeedback() Builder 호출 .perspective(...) → .rubricCategory(...) (line 170)
  매핑 식 line 155-156: question.getQuestionType().feedbackPerspective() → .rubricCategory()
  (QuestionType getter rename = Task 2 동반)
```

#### 3. 변환 함수 단일 출처화

```
infra/ai/prompt/PromptFormatters.java  (NEW, final + private 생성자)
  public static String formatPerspectives(List<AnswerFeedbackPerspective> perspectives)
  public static String toReferenceLabel(ReferenceType refType)

호출처 정리 (정의 8 → 0, 호출 9 → PromptFormatters.* 호출):
  - AnswerAnalyzerPromptBuilder: private 정의 2 제거 + 호출 2 갱신
  - AudioTurnAnalyzerPromptBuilder: private 정의 2 제거 + 호출 2 갱신
  - FollowUpPromptBuilder: private 정의 1 제거 + 호출 2 갱신
  - AnswerAnalysisJsonRenderer: private 정의 1 제거 + 호출 2 갱신
  - AnswerAnalyzer: private 정의 2 제거 + 호출 2 갱신
```

#### 4. RubricFamily.MappingRule 필드명 + YAML 키

```
domain/feedback/rubric/entity/RubricFamily.java (record MappingRule)
  필드 String feedbackPerspective → rubricCategory (line 41, 54-58, 67)
  생성자 / equals / matches() 동기화

domain/feedback/rubric/service/RubricLoader.java (line 169-184)
  YAML 파싱 키 "feedbackPerspective" → "rubricCategory" (when.containsKey / get)

resources/rubric/_mapping.yaml (line 8 주석, line 38 키)
  feedbackPerspective: EXPERIENCE → rubricCategory: EXPERIENCE
  (값은 그대로 — Phase 2 영역)
```

### Phase 1 변경 범위 (FE)

```
src/types/interview.ts
  - line 170: type FeedbackPerspective → RubricCategory (export)
  - line 173: perspective: FeedbackPerspective → RubricCategory
  - line 274: previousExchanges array { answer } → { answerText }
  - 응답 DTO 필드 feedbackPerspective: string | null → rubricCategory 갱신
  - selectedPerspective 응답 필드 → selectedAnswerFeedbackPerspective

src/hooks/use-answer-flow.ts
  - line 342-346: history.map((e) => ({ question, answer, ... }))
                → { question, answerText, ... }
  - line 363: completeFollowUpRound(res.data.answerText || answerText) — 키 그대로 유지

src/stores/interview-store.ts
  - line 35,222: FollowUpExchange 객체 키 (store 내부, BE wire 무관) — 일관성 위해 동시 갱신

src/components/feedback/content-tab.tsx
  - line 22-44: FeedbackPerspective 타입 / 분기 비교 식별자 → RubricCategory

src/hooks/__tests__/use-follow-up-question.test.tsx (line 39 등 테스트 fixture 갱신)
```

### Cross-domain 경계 결정

- `question.QuestionSet` → `InterviewType` 임포트 = 허용. V21 통합 방향. `427-standard-track-classification-enum` plan 흔적과 일관.
- `infra/ai/prompt/PromptFormatters` → `domain/interview/entity/AnswerFeedbackPerspective` 임포트 = infra → domain 방향 정상.

### Phase 1 비스코프 (재확인)

- 변수명 / 파라미터명 잔존 (`askedPerspectives` 41건, `selectedPerspective` 외 일반 변수) = product-spec 카탈로그 #10 (AskedPerspectives 3가지 표현) "보류" → Phase 2 후보. Phase 1 = 타입명 / 필드명 / JSON 키만 변경.
- `EXPERIENCE` / `TECHNICAL` 값 자체 = Phase 2 (LLM 프롬프트 / YAML 값 / FE 비교 영향).

## Data Model

**스키마 변경 없음.** 사유:
- `question.feedback_perspective` 컬럼 = V46 에서 DROP 완료. 현재 미존재.
- `question_set.category VARCHAR(50)` = `QuestionSetCategory.name()` 저장 → `InterviewType.name()` 으로 교체해도 **저장 문자열 동일** (값 12개 100% 일치).
- `@Enumerated(EnumType.STRING)` 룰 그대로 유지 → DB 호환.

```sql
-- 변경 없음 (DDL 0 / DML 0)
```

## API Contract

> BE+FE 분리 작업. JSON 키 변경 = contract 합의 필수.

### 변경되는 응답 필드

#### `AnswerResponse` (현재 → Phase 1 후)

```json
// 현재
{ "feedbackPerspective": "TECHNICAL", ... }

// Phase 1 후
{ "rubricCategory": "TECHNICAL", ... }
```

#### `TimestampFeedbackResponse.technicalFeedback.perspective` (현재 → Phase 1 후)

> 주의: top-level 필드 X. `technicalFeedback` 객체 내부 inner 필드.

```json
// 현재
{
  "technicalFeedback": { "perspective": "EXPERIENCE", "rubricId": "...", ... }
}

// Phase 1 후
{
  "technicalFeedback": { "rubricCategory": "EXPERIENCE", "rubricId": "...", ... }
}
```

#### `FollowUpResponse.selectedPerspective` (현재 → Phase 1 후)

```json
// 현재
{ "selectedPerspective": "TRADEOFF", ... }

// Phase 1 후
{ "selectedAnswerFeedbackPerspective": "TRADEOFF", ... }
```

### 변경되는 요청 필드

#### `FollowUpRequest.previousExchanges[]` (현재 → Phase 1 후)

```json
// 현재
"previousExchanges": [
  { "question": "...", "answer": "...", "followUpType": "...", "selectedPerspective": "..." }
]

// Phase 1 후
"previousExchanges": [
  { "question": "...", "answerText": "...", "followUpType": "...", "selectedAnswerFeedbackPerspective": "..." }
]
```

### Error / 호환

- `@JsonProperty` / `@JsonAlias` 호환 alias **사용 안 함** (사용자 결정 = 운영 윈도우 수용).
- 운영 윈도우 처리 = **BE PR 머지 직후 FE PR 즉시 머지** (수분 윈도우 수용). 사용자 명시 결정.
- 윈도우 내 followup 호출 시 FE 응답 매핑 fallback (`?? null`) 또는 사용자 재시도로 복구.

## Verification

구현 완료 = 아래 모두 통과.

### 테스트 (testing.md 카테고리 매핑)

- [ ] **Domain Unit** (`backend/.claude/rules/testing.md` ≥60% 비중):
  - `QuestionTypeTest` — RubricCategory 임포트 갱신 + 7개 매핑 단언 GREEN
  - `AnswerResponseTest` — rubricCategory 필드 + InterviewType 파라미터 시그니처 GREEN
- [ ] **Service Integration**:
  - `RubricLoaderTest` — YAML 파싱 키 `rubricCategory:` + InterviewType.* 매칭 GREEN
  - `StandardTrackQuestionGeneratorTest` — RubricCategory 임포트 갱신 GREEN
- [ ] **E2E (RestAssured)** — 기존 인터뷰 / 피드백 / followup E2E 1건씩 통과. JSON 응답 키 검증 (`rubricCategory`, `selectedAnswerFeedbackPerspective`).
- [ ] **FE Integration** — `use-follow-up-question.test.tsx` 등 fixture `answerText` 키 갱신 + 컴포넌트 분기 식별자 (RubricCategory) 갱신 GREEN.
- [ ] **FE Unit** — `content-tab.tsx` resolveCopy 분기 갱신 후 unit 테스트 (있다면) GREEN.

### 빌드

- [ ] `./gradlew build` 통과.
- [ ] `npm run lint && npm run build && npm run test` 통과.

### grep 검증 (boolean, 정밀 패턴)

- [ ] `grep -rn "QuestionSetCategory" backend/src/main/java` = 0 (enum 파일 삭제 + 임포트/사용 0)
- [ ] `grep -rn "import com.rehearse.api.domain.feedback.rubric.entity.RubricCategory" backend/src/main/java` ≥ 6 (임포트 갱신 확인)
- [ ] `grep -rn "import com.rehearse.api.domain.feedback.entity.FeedbackPerspective" backend/src/main/java` = 0
- [ ] `grep -rn "import com.rehearse.api.domain.interview.entity.Perspective\b" backend/src/main/java` = 0 (제네릭/정적 호출 포함)
- [ ] `grep -rEn "private static String (formatPerspectives|toReferenceLabel)" backend/src/main/java` = 0 (단일 출처화)
- [ ] `grep -rEn "PromptFormatters\.(formatPerspectives|toReferenceLabel)" backend/src/main/java | wc -l` ≥ 9 (이전 호출 9곳 모두 PromptFormatters 호출로 치환)
- [ ] `grep -n "private String answer\b" backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpRequest.java` = 0
- [ ] `grep -rn "selectedPerspective" backend/src/main/java frontend/src` = 0 (selectedAnswerFeedbackPerspective 로 일괄)
- [ ] `grep -n "feedbackPerspective\|rubricCategory" backend/src/main/resources/rubric/_mapping.yaml` — feedbackPerspective = 0, rubricCategory ≥ 1 (키 변경 확인)
- [ ] 컴파일 통과 자체로 식별자 일관성 1차 보장 (Java rename refactor 안정성).

### 회귀 체크

- [ ] 인터뷰 시작 → 답변 제출 → followup → 피드백 표시 시나리오 (BE+FE 동시 머지 환경) JSON 키 정상 매핑.
- [ ] Rubric 로딩 — `_mapping.yaml` 키 변경 후 룰 매칭 정상 (RubricLoaderTest).
- [ ] question_set.category 컬럼 = 기존 row 와 호환 (Testcontainers + Flyway).

## Pre / Post State

### Pre (현재)
- `interview.Perspective` (7) + `feedback.FeedbackPerspective` (3) 별도 enum 동시 활성. Perspective 단어 도메인 양쪽 사용.
- `QuestionSetCategory` (12) + `InterviewType` (12) 동일 값 두 enum 활성.
- `formatPerspectives` 정의 5곳 / `toReferenceLabel` 정의 3곳 중복.
- `FollowUpExchange.answer` outlier (vs 다수 answerText 표준).
- `FollowUpRequest$FollowUpExchange.selectedPerspective` / `FollowUpResponse.selectedPerspective` 필드.
- `AnswerResponse.from(QuestionAnswer, QuestionSetCategory)` 시그니처.
- DTO JSON 키: `AnswerResponse.feedbackPerspective` (top-level), `TimestampFeedbackResponse.technicalFeedback.perspective` (nested inner), `FollowUpResponse.selectedPerspective`.
- YAML `_mapping.yaml` 키 `feedbackPerspective:`.

### Post (구현 후)
- `AnswerFeedbackPerspective` (interview, 7) + `RubricCategory` (feedback/rubric/entity, 3) — 도메인별 단어 중복 0 (단, AnswerFeedbackPerspective 내 'Perspective' 잔존 = 사용자 수용).
- `QuestionSetCategory` 삭제, `InterviewType` 단일 출처 (12). `QuestionSet.category : InterviewType`.
- `infra/ai/prompt/PromptFormatters` 단일 클래스. 8 정의 → 1 클래스 + 9 호출 → `PromptFormatters.*`.
- `FollowUpExchange.answerText` 통일 (필드 + 생성자 + getter cascade 15 파일).
- `selectedAnswerFeedbackPerspective` (FollowUpRequest/Response/Service/GeneratedFollowUp 일괄).
- `AnswerResponse.from(QuestionAnswer, InterviewType)` 시그니처.
- DTO `rubricCategory` JSON 키 (BE+FE 동시 머지).
- YAML `_mapping.yaml` 키 `rubricCategory:` + `RubricFamily.MappingRule.rubricCategory` 필드.
- DB 스키마 변경 0.

## 위험 / 마이그레이션 / 롤백

### 위험

- **JSON 키 운영 윈도우**: BE/FE 머지 시점 어긋남 시 응답 JSON 키 불일치 → FE undefined 분기. **사용자 결정 = 윈도우 수용**. 완화 = BE PR 머지 직후 FE PR 즉시 연속 머지 (수분 윈도우). FE 측 fallback (`?? null`) 으로 깨짐 최소화.
- **`QuestionSet.category` 타입 교체** = `@Enumerated(STRING)` 동일 → 런타임 직렬화 호환. **검증** = Testcontainers + Flyway 통합 테스트.
- **YAML 키 변경 누락**: `feedbackPerspective:` → `rubricCategory:` 동시 미변경 시 룰 미매칭. **완화** = RubricLoaderTest GREEN.
- **CacheStrategy 노출 잠재**: `questionSet.getCategory().getCacheStrategy()` 호출 발생 시 도메인 누출. **현재 호출처 0건** (사용자 결정 = 수용). 발생 시 별도 처리.
- **선택적 잠재**: `FollowUpExchange` getter cascade (15 파일) 누락 시 컴파일 실패 → IDE rename refactor 사용 권장 (Java IntelliJ Safe Rename).

### NF 11개 — 본 작업 영향

| NF | 영향 | 메모 |
|---|---|---|
| 영향 범위 | BE+FE | 코드 식별자 변경. 마이그/eval 0 |
| 정합성 | 해당 없음 | 동작 변경 0 |
| 실시간성 | 해당 없음 | 응답/요청 latency 영향 0 |
| 부하 | 해당 없음 | 요청 패턴 동일 |
| 동시성 | 해당 없음 | 동작 변경 0 |
| 마이그레이션 | DDL/DML 0 | `@Enumerated(STRING)` 호환 |
| 외부 의존 | 해당 없음 | LLM 프롬프트 단어/YAML 값 변경 0 (Phase 2 영역) |
| 보안 | 해당 없음 | 인증/인가/입력 영향 0 |
| 관찰성 | 해당 없음 | 로그/메트릭 영향 0 |
| 롤백 | 항목별 revert | 회귀 발견 시 해당 항목 Phase 2 이관 |
| 검증 | testing.md 카테고리 매핑 | Domain Unit / Service Integration / E2E (위 Verification) |

### 마이그레이션 전략

- DDL/DML 0. 코드만 교체.
- BE PR 1개 (모든 BE 변경 + 머지 순서 = BE 선행) → FE PR 1개 (BE 머지 직후 즉시 연속 머지).

### 롤백

- 회귀 발견 시 항목별 revert. 본 spec 회귀 발생 = 해당 항목 Phase 2 이관 명시 (product-spec AC 룰).

## 분기 결정

- [x] BE+FE 동시 → `implement-be.md` + `implement-fe.md` (API contract 합의 후 병렬, 머지 순서 = BE 선행).
- [ ] 단일 영역
- [ ] BE 선행 강제 (강결합)

**머지 순서 강제 사유**: JSON 응답/요청 키 변경 → BE 머지 후 즉시 FE 머지 (응답 키 불일치 윈도우 최소화). FE 단독 선행 = wire 키 미일치 → 전체 깨짐.

## 구현 작업 분해 (preview)

implement-be / implement-fe 단계에서 task 단위 정밀화 예정. 현재 high-level:

**BE**:
1. `Perspective` → `AnswerFeedbackPerspective` 클래스 rename + 임포트 갱신 (AskedPerspectives record 제네릭 포함)
2. `FeedbackPerspective` → `RubricCategory` 클래스 rename + 임포트 갱신 (QuestionType / RubricFamily / RubricLoader / DTO). 파일 이동: `feedback/entity/` → `feedback/rubric/entity/`
3. `QuestionSetCategory` 삭제 + `QuestionSet.category` 타입 InterviewType + 사용처 일괄 교체 (Repository / Service / DTO from() 시그니처)
4. `PromptFormatters` 신규 + 8 정의 → 1 클래스 + 9 호출 일괄 치환
5. `FollowUpExchange.answer` → `answerText` (필드 + 생성자 2 + Lombok getter cascade 15 파일)
6. `selectedPerspective` → `selectedAnswerFeedbackPerspective` (FollowUpRequest / FollowUpResponse / FollowUpService / GeneratedFollowUp)
7. `AnswerResponse.feedbackPerspective` → `rubricCategory` + from() 시그니처
8. `TimestampFeedbackResponse$TechnicalFeedback.perspective` → `rubricCategory` (inner 필드 + JSON 키, `technicalFeedback` 객체 내부)
9. `RubricFamily.MappingRule.feedbackPerspective` → `rubricCategory` + `_mapping.yaml` 키 + `RubricLoader` 파싱 키 동기화
10. 테스트 일괄 갱신 + grep 검증

**FE** (BE PR 머지 직후 연속 머지):
1. `types/interview.ts` `FeedbackPerspective` → `RubricCategory` + `previousExchanges` 키 + `selectedAnswerFeedbackPerspective`
2. `hooks/use-answer-flow.ts` history.map 송신 키 (`answer` → `answerText`)
3. `stores/interview-store.ts` 내부 객체 키 일관성 갱신
4. `components/feedback/content-tab.tsx` 타입 / 분기 갱신
5. 응답 필드 `feedbackPerspective` / `selectedPerspective` 사용처 grep + 갱신 + 테스트 fixture 갱신
6. `npm run lint && npm run build && npm run test` GREEN
