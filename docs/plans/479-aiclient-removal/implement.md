# Implement Plan — AiClient God Interface 제거

> 참조: `product-spec.md` (본 폴더). tech-spec 생략 — 본 문서가 호출부별 구현 계획 + 명명 규칙 + commit 단위 + 검증 기준 통합.

## 1. PR #515 패턴 분석 — 적용 청사진

### 1.1 5층 구성 요소

| 층 | 파일 위치 | 책임 | PR #515 예시 |
|----|----------|------|--------------|
| **Port** | `domain/{feat}/models/service/` 인터페이스 | 도메인 어휘 메서드 1개. 인프라 DTO 최소 노출. | `ResumeSkeletonExtractor.extract(byte[], String)` |
| **Adapter** | `infra/ai/adapter/OpenAi*.java` | port 구현. prompt 로드 + client 호출 + 응답 파싱. `@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")` | `OpenAiResumeSkeletonExtractor` |
| **Client** | `infra/ai/client/OpenAi*Client.java` | HTTP 단일 책임 (RestClient 호출 + 4xx/5xx 분기 + RetryableApiException). `@ConditionalOnExpression` 동일 가드. | `OpenAiResumeExtractorClient` |
| **Config** | `infra/ai/config/OpenAi*Properties.java` + `OpenAi*RestClientConfig.java` | `@ConfigurationProperties(prefix=...)` + RestClient bean (timeout / baseUrl) | `OpenAiResumeSkeletonProperties`, `OpenAiResumeExtractorRestClientConfig` |
| **Mock** | `infra/ai/Mock*.java` | `@ConditionalOnMissingBean(OpenAi*.class)` — API 키 부재 시 자동 등록 | `MockResumeSkeletonExtractor` |

### 1.2 핵심 룰

- **Port 이름 = 책임 명사** (책임 동사 명사화). 도메인 어휘. LLM 함의 어휘 회피 (`Llm*`, `Ai*` 접미사 X).
- **Adapter 가 prompt 파일 로드**. `@PostConstruct` + `ClassPathResource`. 도메인 service / domain service 가 prompt 알면 안 됨.
- **Adapter 가 응답 파싱**. `AiResponseParser.parseJsonResponse` 또는 `parseWithRetry(text, clazz, Supplier<ChatResponse>)` 호출. `parseOrRetry(... AiClient ...)` 시그니처 폐기.
- **Properties 기본값 = record 생성자 정규화** (model / timeout / maxTokens / baseUrl 기본값 강제).
- **MockBean = `@ConditionalOnMissingBean(OpenAi*.class)`**. Provider 추가 시 동일 패턴.

### 1.3 PR #515 가 안 한 것 (본 작업 추가)

PR #515 = OpenAI 단일 provider. 본 작업 = **Claude fallback 보존 필요**. 추가 층:

| 층 | 파일 | 책임 |
|----|------|------|
| **Claude Adapter** | `infra/ai/adapter/Claude*.java` | OpenAI adapter 와 동등 (port 구현). Claude prompt + client. |
| **Claude Client** | `infra/ai/client/Claude*Client.java` | Claude HTTP 단일 책임. |
| **Resilient Wrapper** | `infra/ai/adapter/Resilient*.java` | `@Primary` + `@ConditionalOnExpression("openAi || claude")`. OpenAI primary → Claude fallback. port 구현. `AiCallMetrics.recordChat` 래핑. |

도메인은 port 만 의존 — Resilient wrapper 가 `@Primary` 로 주입됨.

## 2. 적용 대상 — 호출부 7개 + 어댑터 3개

### 2.1 호출부 인벤토리

`grep -rn "aiClient\." backend/src/main/java/com/rehearse/api/domain` 결과:

| # | 도메인 호출자 (현재 service) | 현재 의존 | 새 Port 명 | 응답 DTO | OpenAI callType |
|---|------------------------------|-----------|------------|----------|------------------|
| 1 | `domain/question/service/StandardQuestionProvider` | `aiClient.generateQuestions` | `StandardQuestionGenerator` | `List<GeneratedQuestion>` | `generate_questions` |
| 2 | `domain/question/service/ResumeTrackInitiator` | `aiClient.chat` | `ResumeQuestionGenerator` | `GeneratedResumeQuestions` | `resume_question_generator` |
| 3 | `domain/interview/service/AnswerAnalyzer` | `aiClient.chat` | `AnswerAnalyzer` (port) → service rename `AnswerAnalysisService` | `GeneratedAnswerAnalysis` | `answer_analyzer` |
| 4 | `domain/interview/service/FollowUpQuestionWriter` | `aiClient.chat` | `FollowUpQuestionGenerator` | `GeneratedFollowUp` | `follow_up_generator_v3` |
| 5 | `domain/interview/service/AudioTurnAnalyzer` | `aiClient.chatWithAudio` | `AudioTurnAnalyzer` (port) → service rename `AudioTurnAnalysisService` | `GeneratedTurnAnalysis` | `audio_turn_analyzer` |
| 6 | `domain/feedback/rubric/service/RubricScorer` (+ `RubricScoringAdapter`) | `aiClient.chat` (via adapter) | `RubricScorer` (port) → service rename `RubricScoringService` | `RubricScoringResult` | `rubric_scoring` |
| 7 | `domain/feedback/session/synthesis/SessionFeedbackSynthesizer` | `aiClient.chat` | `SessionFeedbackSynthesizer` (port) → service rename `SessionFeedbackService` | `GeneratedSessionFeedback` | `session_feedback_synthesizer` |

**Port 명과 도메인 service 명 충돌 해결**: 책임이 동일하면 PR #515 패턴대로 app service 를 `*Service` 접미사로 풀어쓰고 port 가 책임 명사 점유. 충돌 없는 1, 2, 4 는 service 명 유지.

### 2.2 폐기 대상 (마지막 commit)

- `infra/ai/AiClient.java` — God Interface
- `infra/ai/AbstractAiClient.java`
- `infra/ai/ResilientAiClient.java` — 일률 fallback 추상화
- `infra/ai/OpenAiClient.java` — `chat`/`chatWithAudio`/`generateQuestions`/`generateFollowUpQuestion`/`generateFollowUpWithAudio` 모두 호출부별 client 로 분리됨
- `infra/ai/ClaudeApiClient.java` — 동일
- `infra/ai/MockAiClient.java` — 도메인별 Mock 으로 대체
- `infra/ai/adapter/QuestionGenerationAdapter.java`
- `infra/ai/adapter/FollowUpGenerationAdapter.java`
- `infra/ai/adapter/RubricScoringAdapter.java`
- `infra/ai/AiResponseParser.java` 의 `parseOrRetry(... AiClient ...)` 시그니처 (parseWithRetry / parseJsonResponse 만 유지)

### 2.3 잔존 자산 (재배치만)

- `infra/ai/dto/ChatMessage.java`, `ResponseFormat.java`, `JsonSchemaSpec.java`, `CachePolicy.java` — 일부 adapter 내부에서만 쓰이거나, 동등한 record 로 흡수. Non-Goals 명시.
- `infra/ai/dto/openai/OpenAiResponse.java`, `claude/ClaudeResponse.java` — provider 별 응답 파싱용. 그대로.
- `infra/ai/dto/Generated*.java` — port 응답 타입. 그대로 (port 가 도메인 어휘 우선이라도 이 DTO 들은 LLM 응답 스키마 매핑이라 infra 잔존 자연스러움).
- `infra/ai/prompt/*PromptBuilder` — adapter 내부 의존. 그대로.
- `infra/ai/context/InterviewContextBuilder` — adapter 가 직접 의존. 도메인 service 의존 끊음.
- `infra/ai/metrics/AiCallMetrics` — Resilient wrapper 에서 사용.
- `infra/ai/SchemaExampleRegistry`, `OpenAiResponsesOutputTextExtractor`, `SttService` / `WhisperService` / `MockSttService`, `AiRetryListener` — 그대로.

## 3. 호출부별 구현 계획 — 7 commit + 1 cleanup

각 commit = 1 호출부 분리 완료. 빌드 + 테스트 통과 보장. AiClient 의존 부분 제거는 모든 호출부 분리 완료 후 마지막 commit.

### 3.1 Commit 1 — StandardQuestionGenerator 분리

**신규**:
- `domain/question/models/service/StandardQuestionGenerator.java` (port) — `List<GeneratedQuestion> generate(QuestionGenerationRequest request)`
- `infra/ai/adapter/OpenAiStandardQuestionGenerator.java` — port 구현. `OpenAiQuestionGeneratorClient` 호출 + `QuestionGenerationPromptBuilder` + `AiResponseParser.parseJsonResponse`
- `infra/ai/adapter/ClaudeStandardQuestionGenerator.java` — 동등
- `infra/ai/adapter/ResilientStandardQuestionGenerator.java` — `@Primary`. OpenAI primary → Claude fallback. `AiCallMetrics.recordChat("generate_questions", ...)` 래핑.
- `infra/ai/client/OpenAiQuestionGeneratorClient.java` — POST `/chat/completions`. system + user prompt + response_format=json_object. `@Retryable`.
- `infra/ai/client/ClaudeQuestionGeneratorClient.java` — POST `/v1/messages`. 동등.
- `infra/ai/config/OpenAiQuestionGeneratorProperties.java` (`ai.question.generator` prefix), `OpenAiQuestionGeneratorRestClientConfig.java`
- `infra/ai/config/ClaudeQuestionGeneratorProperties.java`, `ClaudeQuestionGeneratorRestClientConfig.java`
- `infra/ai/MockStandardQuestionGenerator.java` — `@ConditionalOnMissingBean(OpenAiStandardQuestionGenerator.class)` (Mock 우선순위 룰: Resilient 부재 + OpenAI/Claude 둘 다 부재 시만 활성)

**수정**:
- `StandardQuestionProvider` — `AiClient` 의존 → `StandardQuestionGenerator` port. 80번 라인 `aiClient.generateQuestions(request)` → `standardQuestionGenerator.generate(request)`

**테스트**:
- `OpenAiStandardQuestionGeneratorTest` (Infra Integration, WireMock) — 200 응답 → DTO 파싱, 429 → Retryable, 4xx → CLIENT_ERROR
- `ClaudeStandardQuestionGeneratorTest` 동등
- `ResilientStandardQuestionGeneratorTest` (Service Integration) — OpenAI 5xx → Claude fallback 검증 (Mock adapter 2개 주입)
- `MockStandardQuestionGeneratorTest` (Domain Unit) — 시드 응답 반환
- `StandardQuestionProviderTest` 기존 — port mock 으로 교체

### 3.2 Commit 2 — ResumeQuestionGenerator 분리

**신규**:
- `domain/question/models/service/ResumeQuestionGenerator.java` — `GeneratedResumeQuestions generate(ResumeSkeleton skeleton, int openerCount, int mainCount)`
  - 시그니처 결정: `BuiltContext` 빌드 책임을 adapter 로 이전. 도메인은 `ResumeSkeleton` + 카운트만 전달. `InterviewContextBuilder` 호출은 adapter 안에서.
- `infra/ai/adapter/OpenAiResumeQuestionGenerator.java`, `ClaudeResumeQuestionGenerator.java`, `ResilientResumeQuestionGenerator.java`
- `infra/ai/client/OpenAiResumeQuestionGeneratorClient.java`, `ClaudeResumeQuestionGeneratorClient.java`
- Properties / Config 동등 (`ai.resume.question` prefix)
- `MockResumeQuestionGenerator`

**수정**:
- `ResumeTrackInitiator` — `AiClient`, `AiResponseParser`, `InterviewContextBuilder`, `ObjectMapper` 의존 제거. `ResumeQuestionGenerator` port 만 의존. `serializeSkeleton` / `generateViaLlm` 모두 port 호출로 단순화.

**테스트**: 위와 동일 5종.

### 3.3 Commit 3 — AnswerAnalyzer 분리 + service rename

**신규**:
- `domain/interview/models/service/AnswerAnalyzer.java` — `GeneratedAnswerAnalysis analyze(Long interviewId, String mainQuestion, ReferenceType referenceType, String userAnswer)`
- `infra/ai/adapter/OpenAiAnswerAnalyzer.java` (포함: `InterviewContextBuilder.build` + parseWithRetry)
- `ClaudeAnswerAnalyzer`, `ResilientAnswerAnalyzer`
- `infra/ai/client/OpenAiAnswerAnalyzerClient.java`, `ClaudeAnswerAnalyzerClient.java`
- Properties / Config (`ai.answer.analyzer` prefix)
- `MockAnswerAnalyzer`

**수정**:
- `AnswerAnalyzer` (service) → `AnswerAnalysisService` 로 rename (file + class). `aiClient.chat` → `answerAnalyzer.analyze(...)`. `aiResponseParser`, `contextBuilder` 의존 제거.
- 호출자 변경: `git grep "AnswerAnalyzer "` 결과 모두 `AnswerAnalysisService` 로 교체.

**테스트**: 동등 5종.

### 3.4 Commit 4 — FollowUpQuestionGenerator 분리 + service rename

**신규**:
- `domain/interview/models/service/FollowUpQuestionGenerator.java` — `GeneratedFollowUp generate(String mainQuestion, String userAnswer, AnswerAnalysis analysis, ResumeSkeleton skeleton)`
- 어댑터 / 클라이언트 / 설정 / Mock 5종 동등
- Properties (`ai.followup.generator` prefix)

**수정**:
- `FollowUpQuestionWriter` (service) → `FollowUpQuestionService` 로 rename. `aiClient.chat` → `followUpQuestionGenerator.generate(...)`.

**테스트**: 동등.

### 3.5 Commit 5 — AudioTurnAnalyzer 분리 + service rename

**특이점**: Claude 미지원. Resilient wrapper 없이 OpenAI adapter 단독. text-only fallback 은 도메인 service 가 처리 (현재와 동일).

**신규**:
- `domain/interview/models/service/AudioTurnAnalyzer.java` (port) — `GeneratedTurnAnalysis analyze(MultipartFile audio, String mainQuestion, ReferenceType referenceType)` throws `AudioChatFallbackRequiredException`
- `infra/ai/adapter/OpenAiAudioTurnAnalyzer.java` — port 구현. `RestClientException`/`RetryableApiException`/`BusinessException`(non-defect) catch → `AudioChatFallbackRequiredException` 변환 (현재 `ResilientAiClient.doChatWithAudio` 로직 흡수).
- `infra/ai/client/OpenAiAudioTurnAnalyzerClient.java` — audio chat HTTP. multipart audio base64 + retry.
- Properties / Config (`ai.audio.turn-analyzer` prefix)
- `MockAudioTurnAnalyzer` — 항상 `AudioChatFallbackRequiredException` throw (또는 시드 응답 — 결정: text-only fallback 흐름 자동 진입 위해 throw)

**수정**:
- `AudioTurnAnalyzer` (service) → `AudioTurnAnalysisService` 로 rename. `aiClient.chatWithAudio` → `audioTurnAnalyzer.analyze(...)`. catch 블록 + `TextFallbackTurnAnalyzer` 위임 그대로 유지.

**테스트**: 동등. Resilient 테스트 생략 (wrapper 없음).

### 3.6 Commit 6 — RubricScorer 분리 + service rename + adapter 흡수

**특이점**: `RubricScoringAdapter` (220+ 라인) 의 validation / retry / merge / fallback 로직을 port adapter 안으로 흡수. `RubricScorerResponseValidator`, `RubricScorerPromptBuilder` 는 adapter 내부 의존으로 그대로 사용.

**신규**:
- `domain/feedback/rubric/models/service/RubricScorer.java` (port) — `RubricScoringResult score(Question question, String userAnswer, AnswerAnalysis analysis, Rubric rubric, List<String> dimensionsToScore, InterviewLevel level, Long interviewId, Long questionId)`
  - 시그니처 결정: domain service 가 prompt 빌더 호출 책임 갖지 않도록 raw 도메인 객체만 전달. adapter 가 `RubricScorerPromptBuilder.build` 호출.
- `infra/ai/adapter/OpenAiRubricScorer.java` — port 구현. 현재 `RubricScoringAdapter.adapt` 로직 + prompt 빌더 호출 통합. validate / runRetry / mergeAfterRetry / buildFallbackScore 메서드 그대로 이동.
- `ClaudeRubricScorer`, `ResilientRubricScorer`
- `infra/ai/client/OpenAiRubricScorerClient.java`, `ClaudeRubricScorerClient.java`
- Properties / Config (`ai.rubric.scorer` prefix)
- `MockRubricScorer`

**수정**:
- `RubricScorer` (service) → `RubricScoringService` 로 rename. `RubricScoringAdapter`, `AiClient`, `RubricScorerPromptBuilder` 의존 제거. `RubricScorer` port 만 의존. `score(...)` = catalog 로딩 + dimensionsToScore 결정 + port 위임.

**삭제**:
- `infra/ai/adapter/RubricScoringAdapter.java`

**테스트**:
- `OpenAiRubricScorerTest` — validation 위배 시 retry + merge 검증 (기존 `RubricScoringAdapterTest` 시나리오 이전)
- `ResilientRubricScorerTest`, `MockRubricScorerTest`, `RubricScoringServiceTest`

### 3.7 Commit 7 — SessionFeedbackSynthesizer 분리 + service rename

**신규**:
- `domain/feedback/session/models/service/SessionFeedbackSynthesizer.java` (port) — `GeneratedSessionFeedback synthesize(SessionFeedbackInput input)`
- 어댑터 / 클라이언트 / 설정 / Mock 5종 동등
- Properties (`ai.session.feedback` prefix)

**수정**:
- `SessionFeedbackSynthesizer` (service) → `SessionFeedbackService` 로 rename. `aiClient.chat` → `sessionFeedbackSynthesizer.synthesize(input)`. `parser.parse` retry 로직은 adapter 안으로 이전 (또는 port 가 raw `GeneratedSessionFeedback` 반환하고 service 가 도메인 매핑만 — 결정: 1회 retry = LLM 응답 결함 보정이라 adapter 책임).

**테스트**: 동등.

### 3.8 Commit 8 — God Interface 폐기 (cleanup)

위 7개 commit 모두 머지 후 도메인은 port 만 의존. AiClient 의존 0 확인 후:

**삭제**:
- `infra/ai/AiClient.java`
- `infra/ai/AbstractAiClient.java`
- `infra/ai/ResilientAiClient.java`
- `infra/ai/OpenAiClient.java`
- `infra/ai/ClaudeApiClient.java`
- `infra/ai/MockAiClient.java`
- `infra/ai/adapter/QuestionGenerationAdapter.java`
- `infra/ai/adapter/FollowUpGenerationAdapter.java`
- (`RubricScoringAdapter` 는 commit 6 에서 이미 삭제)

**수정**:
- `AiResponseParser.parseOrRetry(... AiClient ...)` 메서드 제거. `parseWithRetry(text, clazz, Supplier<ChatResponse>)` + `parseJsonResponse(text, clazz)` 만 유지.
- `application-*.yml` — `openai.model`, `openai.audio-model`, `openai.base-url`, `claude.model`, `claude.api.url` 키 정리. 호출부별 properties 로 이전된 항목 제거. `openai.api-key` / `claude.api-key` 만 공통 잔존.
- `ArchUnit` 룰 추가: `domain` 패키지가 `infra.ai.AiClient` import 0 검증 — 회귀 방지 게이트.

**검증**:
```bash
grep -rn "AiClient" backend/src/main/java/com/rehearse/api   # = 0
grep -rn "ChatRequest\|ChatResponse" backend/src/main/java/com/rehearse/api/domain   # = 0
./gradlew test
```

## 4. 명명 규칙 결정 — 정리

| 결정 | 채택안 | 사유 |
|------|--------|------|
| Port 명 = 책임 명사 | `StandardQuestionGenerator`, `ResumeQuestionGenerator`, `AnswerAnalyzer`, `FollowUpQuestionGenerator`, `AudioTurnAnalyzer`, `RubricScorer`, `SessionFeedbackSynthesizer` | 컨벤션 `Port 인터페이스 = 책임 단위 명사`. LLM 어휘 회피. |
| App service rename | 충돌 4건 (`AnswerAnalysisService`, `AudioTurnAnalysisService`, `RubricScoringService`, `SessionFeedbackService`) | PR #515 패턴 (`ResumeExtractionService` 가 `ResumeSkeletonExtractor` port 소유). app service = LLM 호출 + 도메인 조립이라 책임이 더 크므로 풀어쓰기. |
| Properties prefix | `ai.{feature}.{role}` (`ai.question.generator`, `ai.resume.question`, `ai.answer.analyzer`, `ai.followup.generator`, `ai.audio.turn-analyzer`, `ai.rubric.scorer`, `ai.session.feedback`) | PR #515 `ai.resume.skeleton` 패턴 일관. |
| Mock 활성 조건 | `@ConditionalOnMissingBean(OpenAi*.class)` | PR #515 패턴. Claude 만 있을 때는 Claude adapter 가 활성 = Mock 자동 비활성. |
| Fallback 보존 6개 호출 | StandardQuestion / ResumeQuestion / AnswerAnalysis / FollowUpQuestion / RubricScoring / SessionFeedback (Audio 제외) | product-spec §해결 방향 Fallback 보존 대상. |
| AiResponseParser API | `parseJsonResponse`, `parseWithRetry(text, clazz, Supplier<ChatResponse>)` 유지. `parseOrRetry(... AiClient ...)` 폐기. | AiClient 의존 끊기 + retry caller 가 ChatResponse supplier 주입. |

## 5. 검증 — DoD

### 5.1 정적 검증

```bash
# AiClient 자산 잔존 0
grep -rn "AiClient\b" backend/src/main/java/com/rehearse/api
# 도메인이 ChatRequest / ChatResponse import 0
grep -rn "infra\.ai\.dto\.ChatRequest\|infra\.ai\.dto\.ChatResponse" backend/src/main/java/com/rehearse/api/domain
# *GenerationAdapter / *ScoringAdapter 파일 0
find backend/src/main/java/com/rehearse/api/infra/ai/adapter -name "*GenerationAdapter.java" -o -name "*ScoringAdapter.java"
```

### 5.2 테스트 통과

- `./gradlew test` 전수 통과
- ArchUnit: `domain` → `infra.ai.AiClient` 의존 0
- E2E (fallback 보존 대상 6개): OpenAI 5xx 시뮬레이션 → Claude fallback 정상

### 5.3 Mock 환경 정상

- 두 provider 키 부재 시 `Mock*Generator` 7종 활성 → 인터뷰 생성 / 질문 생성 / 면접 진행 / 피드백 생성 흐름 mock 응답으로 정상 종료

## 6. 위험 / 롤백

| 위험 | 완화 |
|------|------|
| 7 commit 누적 시 머지 충돌 (특히 application.yml) | 각 commit 머지 직후 rebase. cleanup commit 전 충돌 일괄 해소. |
| Claude adapter 가 OpenAI adapter 와 동치 동작 보장 실패 (provider 별 모델 ID 차이) | 각 adapter 의 `application-*.yml` 에 provider 별 model ID 명시. 기존 `claude.model`, `claude-haiku-4-5-...` 값 그대로 이전. |
| Resilient wrapper 의 `AiCallMetrics.recordChat` 누락 시 메트릭 회귀 | 각 Resilient adapter 가 OpenAI 호출을 `aiCallMetrics.recordChat(callType, () -> ...)` 로 wrap. callType 상수 = port 별 고유. |
| RubricScorer adapter 내부 retry 로직 누락 | 기존 `RubricScoringAdapter` 의 validate / runRetry / mergeAfterRetry 전수 이전 + 테스트 시나리오 그대로 재사용. |

롤백 단위 = commit 단위. cleanup commit (8) revert 시 7개 호출부 분리 상태 유지 + AiClient 잔존. 호출부 commit revert 시 해당 호출부만 AiClient 의존 복귀.

## 7. 진행

- 본 implement.md = 시작점. 각 commit 진입 전 `backend` agent 호출. tech-spec 별도 작성 생략 — 본 문서가 명명 / 시그니처 / 테스트 / 검증 모두 명시.
- 7 commit + 1 cleanup = 단일 PR. PR title: `refactor(BE): AiClient God Interface 제거 — 호출부별 port 분리`.
- review = `code-reviewer-backend` PR 단위 1회.
