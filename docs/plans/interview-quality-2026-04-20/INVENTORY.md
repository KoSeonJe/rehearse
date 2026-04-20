# Codebase Inventory — Interview Quality 2026-04-20

> 실측일: 2026-04-20
> Source: plan-00a
> Status: Completed

---

## 1. AI Infrastructure

패키지 루트: `backend/src/main/java/com/rehearse/api/infra/ai/`

### 핵심 클래스

| 클래스 | 어노테이션 | 역할 |
|--------|-----------|------|
| `AiClient.java` | interface | AI 호출 계약 |
| `ResilientAiClient.java` | @Component @Primary | Fallback 이중화 래퍼 |
| `OpenAiClient.java` | @ConditionalOnExpression(openai.api-key not empty) | OpenAI 구현체 |
| `ClaudeApiClient.java` | @ConditionalOnExpression(claude.api-key not empty) | Claude 구현체 |
| `MockAiClient.java` | @ConditionalOnMissingBean(ResilientAiClient.class) | 테스트/로컬 스텁 |
| `AiResponseParser.java` | — | JSON 파싱 유틸 |
| `PdfTextExtractor.java` | — | PDF → 텍스트 추출 (기존 클래스, plan-05에서 확장) |
| `WhisperService.java` | @ConditionalOnExpression(openai.api-key not empty) | STT 구현체 |
| `MockSttService.java` | @ConditionalOnMissingBean(WhisperService.class) | STT 스텁 |

#### AiClient 인터페이스 메서드 시그니처

```java
public interface AiClient {
    List<GeneratedQuestion> generateQuestions(QuestionGenerationRequest request);
    GeneratedFollowUp generateFollowUpQuestion(FollowUpGenerationRequest request);
    GeneratedFollowUp generateFollowUpWithAudio(MultipartFile audioFile, FollowUpGenerationRequest request);
}
```

#### ResilientAiClient 상세

- 생성자 의존: `@Nullable OpenAiClient`, `@Nullable ClaudeApiClient`, `@Nullable SttService`
- Fallback 정책: OpenAI (max 1 retry) → 비재시도 오류 시 Claude 전환
- 양쪽 실패 시: `AiErrorCode.SERVICE_UNAVAILABLE`
- `isNonRetryableError()` switch: `CLIENT_ERROR`, `PARSE_FAILED` = 비재시도

#### OpenAiClient 상수

| 상수 | 값 |
|------|----|
| MAX_TOKENS_QUESTION | 8192 |
| MAX_TOKENS_FOLLOW_UP | 1024 |
| TEMPERATURE_FOLLOW_UP | 0.7 |
| MAX_RETRY_ATTEMPTS | 2 |
| model | `${openai.model:gpt-4o-mini}` |

#### ClaudeApiClient 상세

- FOLLOW_UP_MODEL: `claude-haiku-4-5-20251001`
- question model: `${claude.model:claude-sonnet-4-20250514}`
- `generateFollowUpWithAudio()` 미구현 — fallback은 Whisper STT + Claude text 경로

#### PdfTextExtractor

```java
public class PdfTextExtractor {
    public static final int MAX_TEXT_LENGTH = 5000;
    public String extract(MultipartFile file) throws BusinessException { ... }
}
```
- 라이브러리: Apache PDFBox
- **plan-05에서 확장 (신규 생성 아님)**

#### AiResponseParser

```java
public class AiResponseParser {
    public <T> T parseJsonResponse(String json, Class<T> targetClass) { ... }
    public String extractJson(String text) { ... }  // ```json 펜스 제거
}
```

#### SttService 인터페이스

```java
public interface SttService {
    String transcribe(MultipartFile audioFile);
}
```

WhisperService: model=whisper-1, language=ko

### Sub-packages

| 패키지 | 내용 |
|--------|------|
| `dto/` | FollowUpGenerationRequest, GeneratedFollowUp, GeneratedQuestion, GeneratedQuestionsWrapper, QuestionGenerationRequest |
| `dto/claude/` | CacheControl, ClaudeRequest, ClaudeResponse, SystemContent |
| `dto/openai/` | OpenAiRequest, OpenAiResponse |
| `exception/` | AiErrorCode (CLIENT_ERROR/PARSE_FAILED/EMPTY_RESPONSE/SERVER_ERROR/TIMEOUT/SERVICE_UNAVAILABLE), RetryableApiException, WhisperErrorCode (FILE_READ_FAILED/API_CALL_FAILED) |
| `persona/` | BaseProfile, PersonaDepth, PersonaResolver, ProfileYamlLoader, ResolvedProfile, StackOverlay |
| `prompt/` | FollowUpPromptBuilder, LevelGuideProvider, QuestionCountCalculator, QuestionGenerationPromptBuilder |

---

## 2. Interview Domain

패키지 루트: `backend/src/main/java/com/rehearse/api/domain/interview/`

> **중요**: `InterviewSession` 클래스 **미존재**. Aggregate root는 `Interview` 엔티티.
> **중요**: `InterviewTurnService` **미존재**. 해당 로직은 `FollowUpService`에 위치.

### 핵심 엔티티: Interview

| 필드 | 타입 | 비고 |
|------|------|------|
| id | Long | PK |
| publicId | String | 공개 식별자 |
| userId | Long | 소유자 |
| position | Position enum | |
| positionDetail | String | |
| level | InterviewLevel enum | |
| interviewTypes | Set\<InterviewType\> | |
| csSubTopics | Set\<CsSubTopic\> | |
| durationMinutes | Integer | |
| techStack | TechStack enum | |
| status | InterviewStatus enum | 상태 기계 |
| questionGenerationStatus | QuestionGenerationStatus enum | |
| failureReason | String | |
| questionSets | List\<QuestionSet\> | ONE-TO-MANY |

### 열거형(Enum) 전체 값

**InterviewLevel**: `JUNIOR / MID / SENIOR`

**InterviewStatus**: `READY / IN_PROGRESS / COMPLETED` (상태 기계 내장)

**InterviewType** (12값):
`CS_FUNDAMENTAL / BEHAVIORAL / RESUME_BASED / LANGUAGE_FRAMEWORK / SYSTEM_DESIGN / FULLSTACK_STACK / UI_FRAMEWORK / BROWSER_PERFORMANCE / INFRA_CICD / CLOUD / DATA_PIPELINE / SQL_MODELING`

**Position**: `BACKEND / FRONTEND / DEVOPS / DATA_ENGINEER / FULLSTACK`

**QuestionGenerationStatus**: `PENDING / GENERATING / COMPLETED / FAILED`

**TechStack** (18값):
`JAVA_SPRING / PYTHON_DJANGO / NODE_NESTJS / GO / KOTLIN_SPRING / REACT_TS / VUE_TS / SVELTE / ANGULAR / AWS_K8S / GCP / AZURE / SPARK_AIRFLOW / FLINK / DBT_SNOWFLAKE / REACT_SPRING / REACT_NODE / NEXTJS_FULLSTACK`

### 서비스 (모두 @Transactional 경계 보유)

| 서비스 | 역할 |
|--------|------|
| `InterviewService` | updateStatus, retryQuestionGeneration, skipRemainingQuestionSets, getInterviews(Pageable), getStats |
| `FollowUpService` | **plan-01 Intent 분기 삽입 지점** — 꼬리질문 생성 메인 진입점 |
| `InterviewCreationService` | 면접 생성 |
| `InterviewCompletionService` | 면접 완료 처리 |
| `InterviewDeletionService` | 면접 삭제 |
| `InterviewFinder` | 조회 헬퍼 |
| `InterviewQueryService` | 쿼리 전용 |
| `FollowUpTransactionHandler` | loadFollowUpContext, saveFollowUpResult |

#### FollowUpService 진입 메서드 (plan-01 분기 삽입 위치)

```java
// FollowUpService.java:31
public FollowUpResponse generateFollowUp(
    Long id,
    Long userId,
    FollowUpRequest request,
    MultipartFile audioFile
)
```

처리 흐름:
1. Phase 1: DB load + validate
2. Phase 2: STT + follow-up 생성
3. Phase 3: save / skip

### generation 서브패키지

**generation/pool/**: QuestionPool entity, repo, CacheableQuestionProvider, FreshQuestionProvider, KeywordMatcher, PoolSelectionCriteria, QuestionGenerationLock, QuestionPoolService, QuestionCacheKeyGenerator, StringListJsonConverter, CsSubTopic entity

**generation/service/**: QuestionGenerationEventHandler, QuestionGenerationService, QuestionGenerationTransactionHandler

### Sub-packages 목록

`controller / dto / entity / exception / event / generation / repository / service / vo`

---

## 3. Feedback Domain

패키지 루트: `backend/src/main/java/com/rehearse/api/domain/feedback/`

### 기존 엔티티

**QuestionSetFeedback**:
- id, questionSet(OneToOne unique), questionSetComment(TEXT), timestampFeedbacks(OneToMany), createdAt
- Lambda Gemini 결과 저장

**TimestampFeedback**:
- id, questionSetFeedback(ManyToOne), question(ManyToOne)
- startMs, endMs, transcript
- verbalComment, fillerWordCount
- eyeContactLevel, postureLevel, expressionLabel, nonverbalComment
- overallComment, isAnalyzed

**FeedbackPerspective** enum: `TECHNICAL / BEHAVIORAL / EXPERIENCE`

### 기존 서비스 / 리포지토리

- `TimestampFeedbackMapper` — toEntity()
- `QuestionSetFeedbackRepository`
- `TimestampFeedbackRepository`

### 신규 예정 (plan-08)

`domain/feedback/rubric/` 하위:
- `RubricDimension`, `RubricFamily`, `Rubric`, `DimensionRef`, `DimensionScore`, `RubricScore`, `RubricLoader`, `RubricScorer`
- `entity/RubricScoreEntity`
- `repository/RubricScoreRepository`
- `dto/RubricScoreResponse`

### 신규 예정 (plan-09, plan-00e 결정 기반)

`domain/feedback/session/` 하위:
- `SessionFeedback` (Entity, V27 매핑), `SessionFeedbackService`, `SessionFeedbackRepository`, `dto/SessionFeedbackResponse`
- `controller/AdminSessionFeedbackController`

### 신규 예정 (plan-11)

`domain/feedback/rubric/nonverbal/` 하위:
- `NonverbalRubricScorer`, `NonverbalTurnScore`, `NonverbalContextWeightsLoader`
- `entity/NonverbalScoreEntity` (V28 매핑)
- `repository/NonverbalScoreRepository`
- `dto/NonverbalScoreResponse`

---

## 3.5. Rubric Resources (신설 예정 — plan-08)

디렉토리: `backend/src/main/resources/rubric/` (신설)

| 파일 | 내용 |
|------|------|
| `_dimensions.yaml` | 10차원 마스터 (D1~D10). plan-11에서 D11~D14 추가 수정 |
| `_mapping.yaml` | QuestionSetCategory + FeedbackPerspective + ResumeTrack → rubric_id 선언적 매핑. plan-11에서 always_apply: nonverbal-v1 추가 수정 |
| `concept-cs-fundamental-rubric.yaml` | CS_FUNDAMENTAL. 4차원 (D2/D3/D4/D8) |
| `lang-fw-backend-rubric.yaml` | LANGUAGE_FRAMEWORK + BACKEND. 5차원 (D2/D3/D4/D5/D8) |
| `lang-fw-frontend-rubric.yaml` | LANGUAGE_FRAMEWORK/UI_FRAMEWORK + FRONTEND. 5차원 |
| `experience-backend-rubric.yaml` | FeedbackPerspective=EXPERIENCE + backend. 5차원 (D1/D2/D3/D6/D8) |
| `experience-collaboration-rubric.yaml` | BEHAVIORAL. 4차원 (D1/D3/D6/D7) |
| `resume-backend-rubric.yaml` | RESUME_BASED / ResumeTrack. 5차원 (D2/D3/D6/D9/D10) + mode-aware |
| `fallback-generic-rubric.yaml` | 매핑 실패 fallback. 3차원 (D2/D3/D8) |
| `nonverbal-rubric.yaml` | (plan-11) 비언어 D11~D14. scope=global |
| `nonverbal-context-weights.yaml` | (plan-11) category/track/mode/difficulty 별 multiplier |
| `nonverbal-improvement-actions.yaml` | (plan-11) D11~D14 × level 개선 액션 템플릿 |

---

## 3.6. Enum Quick Reference (plan-08 매핑용)

### QuestionSetCategory (12값)
| 값 | 매핑 Rubric | 비고 |
|----|------------|------|
| CS_FUNDAMENTAL | concept-cs-fundamental-rubric | D2/D3/D4/D8 |
| BEHAVIORAL | experience-collaboration-rubric | D1/D3/D6/D7 |
| RESUME_BASED | resume-backend-rubric | D2/D3/D6/D9/D10 |
| LANGUAGE_FRAMEWORK | lang-fw-backend or lang-fw-frontend (domain 분기) | |
| UI_FRAMEWORK | lang-fw-frontend-rubric | |
| SYSTEM_DESIGN | fallback-generic-rubric | 전용 rubric 추후 |
| FULLSTACK_STACK | fallback-generic-rubric | |
| BROWSER_PERFORMANCE | fallback-generic-rubric | |
| INFRA_CICD | fallback-generic-rubric | |
| CLOUD | fallback-generic-rubric | |
| DATA_PIPELINE | fallback-generic-rubric | |
| SQL_MODELING | fallback-generic-rubric | |

### FeedbackPerspective (3값)
`TECHNICAL / BEHAVIORAL / EXPERIENCE`

- EXPERIENCE → experience-backend-rubric (단, Category가 우선; Category 없을 때만 적용)

### ReferenceType (2값)
`MODEL_ANSWER / GUIDE`

> **주의**: `CONCEPT / EXPERIENCE` 값 없음. plan 문서에서 언급되는 CONCEPT/EXPERIENCE는 질문 유형 분류(QuestionType 또는 FeedbackPerspective) 맥락이며 ReferenceType enum 값이 아님.

### QuestionType
(entity 확인 필요 — 현재 인벤토리에서 별도 값 목록 미수집. plan-02/03에서 `referenceType: CONCEPT/EXPERIENCE` 언급은 FeedbackPerspective를 가리키는 것으로 해석)

### Rubric Dimension 전체
| ID | 차원명 | 카테고리 |
|----|--------|---------|
| D1 | Problem Framing | 기술 |
| D2 | Technical Depth | 기술 |
| D3 | Reasoning Communication | 기술 |
| D4 | Conceptual Accuracy | 기술 |
| D5 | Practical Application | 기술 |
| D6 | Experience Concreteness | 경험 |
| D7 | Collaboration Awareness | 경험 |
| D8 | Recovery from Gaps | 공통 |
| D9 | Factual Consistency | Resume 전용 |
| D10 | Chain Depth | Resume 전용 |
| D11 | Fluency | 비언어 (plan-11) |
| D12 | Confidence Tone | 비언어 (plan-11) |
| D13 | Eye Contact & Posture | 비언어 (plan-11) |
| D14 | Composure | 비언어 (plan-11) |

---

## 4. Question / QuestionSet Domains

### domain/question/

**Question** entity 필드:
- questionSet (@ManyToOne)
- questionType (QuestionType enum)
- questionText, ttsText, modelAnswer
- referenceType (ReferenceType enum: `MODEL_ANSWER / GUIDE`)
- feedbackPerspective (FeedbackPerspective enum)
- orderIndex
- questionPool (@ManyToOne)

**QuestionAnswer** entity

**QuestionType** enum (값 목록은 소스 직접 확인 필요)

**ReferenceType** enum: `MODEL_ANSWER / GUIDE` (**CONCEPT/EXPERIENCE 없음**)

기타: QuestionErrorCode, 2개 repository, 5개 DTO

### domain/questionset/

**QuestionSet** entity 필드:
- interview (@ManyToOne)
- category (QuestionSetCategory enum)
- orderIndex
- fileMetadata (@OneToOne)
- analysis (@OneToOne mapped)
- questions (@OneToMany)

**QuestionSetCategory** enum (12값):
`CS_FUNDAMENTAL / BEHAVIORAL / RESUME_BASED / LANGUAGE_FRAMEWORK / SYSTEM_DESIGN / FULLSTACK_STACK / UI_FRAMEWORK / BROWSER_PERFORMANCE / INFRA_CICD / CLOUD / DATA_PIPELINE / SQL_MODELING`

기타:
- 2개 controller (public `QuestionSetController` + internal `InternalQuestionSetController`)
- 2개 service (`QuestionSetService` + `InternalQuestionSetService`)
- 1개 repository
- 5개 DTO

---

## 5. Resume Domain

> **현재 미존재**: `backend/src/main/java/com/rehearse/api/domain/resume/` 패키지 **없음**.
> plan-05, plan-06, plan-07이 본 스프린트에서 신규 생성.

### plan-05 신규 생성 예정

`domain/resume/`:
- `ResumeIngestionService.java` — PdfTextExtractor 호출 + 언어 감지 + 섹션 분리
- `ResumeExtractionService.java` — 텍스트 → Skeleton 변환
- `ResumeSkeletonCache.java` — 세션 스코프 캐시 (2h TTL)
- `domain/ResumeSkeleton.java` (record)
- `domain/Project.java`
- `domain/ResumeClaim.java`
- `domain/InterrogationChain.java`

### plan-06 신규 생성 예정

- `ResumeInterviewPlanner.java`
- `domain/InterviewPlan.java`
- `domain/ProjectPlan.java`

### plan-07 신규 생성 예정

- `ResumeInterviewOrchestrator.java` — 메인 진입점
- `PlaygroundModeHandler.java`
- `InterrogationModeHandler.java`
- `ChainStateTracker.java`
- `domain/ResumeMode.java` (enum: `PLAYGROUND / INTERROGATION`)

---
