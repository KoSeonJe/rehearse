# Phase 2: [BE] 프롬프트 빌더 리팩토링 + 토큰 최적화

> **상태**: TODO
> **브랜치**: `feat/prompt-builder-refactor`
> **PR**: PR-2 → develop
> **의존**: Phase 1 (PR-1 머지 후)

---

## 개요

현재 `ClaudePromptBuilder`의 모놀리식 프롬프트 생성을 `PersonaResolver` 기반의 새 빌더 구조로 교체한다.
`prompt-optimized.md` (v3)의 7가지 토큰 최적화 전략을 모두 적용하여 System Prompt 토큰을 ~38% 절감한다.

### 토큰 절감 예상 (BACKEND × JAVA_SPRING, JUNIOR 기준)

| 프롬프트 | 현재 (v2) | 최적화 (v3) | 절약률 |
|---------|----------|------------|-------|
| 질문 생성 | ~1,161 tok | ~732 tok | 37% |
| 후속 질문 | ~630 tok | ~435 tok | 31% |
| **합계 (BE)** | **~1,791 tok** | **~1,167 tok** | **35%** |

---

## Task 2-1: LevelGuideProvider 생성

- **Implement**: `backend`
- **Review**: `code-reviewer`

### 파일

- 신규: `backend/src/main/java/com/rehearse/api/infra/ai/prompt/LevelGuideProvider.java`

### 구현 상세

```java
public class LevelGuideProvider {

    private static final Map<InterviewLevel, String> GUIDES = Map.of(
        InterviewLevel.JUNIOR,
            "JUNIOR: 기본 개념의 정확한 이해. 원리 중심 질문, CS 기초와 논리적 사고력 평가. 실무 경험보다 학습 의지.",
        InterviewLevel.MID,
            "MID: 실무 적용 경험과 문제 해결. 기술 선택 이유, 트레이드오프, 장애 시 판단력 평가.",
        InterviewLevel.SENIOR,
            "SENIOR: 아키텍처 의사결정, 팀 리딩, 기술 방향성. 시스템 조망, 조직 기술 영향력 평가."
    );

    public static String get(InterviewLevel level) {
        return GUIDES.get(level);
    }
}
```

> v3 전략 2: 기존 3개 레벨 가이드 전부 포함 → 해당 레벨 1개만. ~129 tok 절약

---

## Task 2-2: 프롬프트 템플릿 파일 작성

- **Implement**: `backend`
- **Review**: `code-reviewer` — 프롬프트 품질, 토큰 최적화

### 파일

- 신규: `backend/src/main/resources/prompts/template/question-generation.txt`
- 신규: `backend/src/main/resources/prompts/template/follow-up.txt`

### question-generation.txt (v3 최적화 적용)

```
{FULL_PERSONA}

면접 질문을 생성합니다.

## 평가 관점
{BASE_EVALUATION_PERSPECTIVE}

## 출제 가이드
{FILTERED_INTERVIEW_TYPE_GUIDE}

{CONDITIONAL_CS_SUBTOPIC_BLOCK}

## 난이도
{SINGLE_LEVEL_GUIDE}

## 질문 수
(면접시간(분)÷3) 반올림, 최소2 최대24. 유형별 균등 배분.

{CONDITIONAL_RESUME_BLOCK}

## 모범답변
- CS 질문: referenceType="MODEL_ANSWER", 핵심개념+실무예시 포함
- RESUME 질문: referenceType="GUIDE", 답변방향+좋은답변 조건
- questionCategory: 이력서/경험→"RESUME", 기술/CS→"CS"

## 응답
JSON만 응답. 형식:
{"questions":[{"content":"","category":"","order":1,"evaluationCriteria":"2-3문장","questionCategory":"RESUME|CS","modelAnswer":"","referenceType":"MODEL_ANSWER|GUIDE"}]}
```

### follow-up.txt (v3 최적화 적용)

> **[M2 수정]** `{STACK_OVERLAY_FOLLOWUP_DEPTH}` 플레이스홀더 제거.
> `ResolvedProfile.followUpDepth()`가 이미 base + overlay를 merge한 값이므로 `{FOLLOWUP_DEPTH}` 하나로 통일.

```
{MEDIUM_PERSONA}

답변 기반으로 후속 질문을 1개 생성합니다.

## 후속 유형
DEEP_DIVE(기술심화) | CLARIFICATION(모호함구체화) | CHALLENGE(약점/대안탐색) | APPLICATION(다른상황적용)

## 심화 방향
{FOLLOWUP_DEPTH}

## 규칙
- 질문 1개만. 복합 질문 금지.
- 이전 후속 질문과 다른 유형 사용.
- 모범답변(modelAnswer) 2-4문장 포함.

## 응답
JSON만 응답. 형식:
{"question":"","reason":"","type":"DEEP_DIVE|CLARIFICATION|CHALLENGE|APPLICATION","modelAnswer":""}
```

---

## Task 2-3: AiClient Parameter Object 도입 + 프롬프트 빌더 생성

- **Implement**: `backend`
- **Review**: `architect-reviewer` — 인터페이스 설계, SOLID

> **[H1 수정]** 기존 7~8개 파라미터 나열 → Request DTO로 래핑하여 fat interface 해소.
> **[H2 수정]** 빌더가 `Interview` 엔티티 직접 의존하지 않고 Request DTO를 받도록 설계.

### 파일

- 신규: `backend/src/main/java/com/rehearse/api/infra/ai/dto/QuestionGenerationRequest.java`
- 신규: `backend/src/main/java/com/rehearse/api/infra/ai/dto/FollowUpGenerationRequest.java`
- 신규: `backend/src/main/java/com/rehearse/api/infra/ai/prompt/QuestionGenerationPromptBuilder.java`
- 신규: `backend/src/main/java/com/rehearse/api/infra/ai/prompt/FollowUpPromptBuilder.java`
- 수정: `backend/src/main/java/com/rehearse/api/infra/ai/AiClient.java`

### QuestionGenerationRequest (Record)

```java
public record QuestionGenerationRequest(
    Position position,
    String positionDetail,
    InterviewLevel level,
    Set<InterviewType> interviewTypes,
    Set<String> csSubTopics,
    String resumeText,
    Integer durationMinutes,
    TechStack techStack   // nullable → PersonaResolver에서 기본 스택 적용
) {}
```

### FollowUpGenerationRequest (Record)

```java
public record FollowUpGenerationRequest(
    Position position,
    TechStack techStack,
    InterviewLevel level,
    String questionContent,
    String answerText,
    String nonVerbalSummary,
    List<FollowUpExchange> previousExchanges
) {}
```

### AiClient 인터페이스 변경

```java
public interface AiClient {

    // Before: 7개 개별 파라미터
    // After: Request DTO 1개 → 향후 필드 추가 시 시그니처 불변
    List<GeneratedQuestion> generateQuestions(QuestionGenerationRequest request);

    GeneratedFollowUp generateFollowUpQuestion(FollowUpGenerationRequest request);
}
```

### QuestionGenerationPromptBuilder

```java
@Component
@RequiredArgsConstructor
public class QuestionGenerationPromptBuilder {

    private final PersonaResolver personaResolver;
    private String template; // @PostConstruct에서 question-generation.txt 로드

    @PostConstruct
    void init() {
        template = new String(getClass().getResourceAsStream(
            "/prompts/template/question-generation.txt").readAllBytes());
    }

    // [H2] Interview 엔티티 대신 Request DTO의 개별 필드를 사용
    public String buildSystemPrompt(QuestionGenerationRequest req) {
        TechStack effectiveStack = req.techStack() != null
            ? req.techStack() : TechStack.getDefaultForPosition(req.position());
        ResolvedProfile profile = personaResolver.resolve(req.position(), effectiveStack);

        // v3 전략 4: 선택된 유형 가이드만 필터링
        String typeGuide = profile.interviewTypeGuideMap().entrySet().stream()
            .filter(e -> req.interviewTypes().stream().anyMatch(t -> t.name().equals(e.getKey())))
            .map(e -> "- " + e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining("\n"));

        // v3 전략 3: CS 블록 조건부
        String csBlock = "";
        if (req.interviewTypes().contains(InterviewType.CS_FUNDAMENTAL)
                && req.csSubTopics() != null && !req.csSubTopics().isEmpty()) {
            csBlock = "## CS 세부 주제\n" + String.join(", ", req.csSubTopics()) + "에서만 출제.";
        }

        // v3 전략 2: 해당 레벨만
        String levelGuide = LevelGuideProvider.get(req.level());

        // v3 전략 7: 이력서 블록 조건부
        String resumeBlock = (req.resumeText() != null && !req.resumeText().isBlank())
            ? "## 이력서 활용\nRESUME_BASED 질문은 이력서의 프로젝트, 기술, 성과를 구체적으로 언급하여 생성."
            : "";

        return template
            .replace("{FULL_PERSONA}", profile.fullPersona())
            .replace("{BASE_EVALUATION_PERSPECTIVE}", profile.evaluationPerspective())
            .replace("{FILTERED_INTERVIEW_TYPE_GUIDE}", typeGuide)
            .replace("{CONDITIONAL_CS_SUBTOPIC_BLOCK}", csBlock)
            .replace("{SINGLE_LEVEL_GUIDE}", levelGuide)
            .replace("{CONDITIONAL_RESUME_BLOCK}", resumeBlock);
    }

    public String buildUserPrompt(QuestionGenerationRequest req) {
        TechStack effectiveStack = req.techStack() != null
            ? req.techStack() : TechStack.getDefaultForPosition(req.position());
        int questionCount = ClaudePromptBuilder.calculateQuestionCount(
            req.durationMinutes(), req.interviewTypes().size());

        StringBuilder sb = new StringBuilder();
        sb.append("직무: ").append(positionKorean(req.position()))
          .append(" (").append(effectiveStack.getDisplayName()).append(")\n");
        sb.append("레벨: ").append(levelKorean(req.level())).append("\n");
        sb.append("유형: ").append(typesKorean(req.interviewTypes())).append("\n");
        sb.append("질문 수: ").append(questionCount).append("개\n");

        if (req.interviewTypes().contains(InterviewType.CS_FUNDAMENTAL)
                && req.csSubTopics() != null && !req.csSubTopics().isEmpty()) {
            sb.append("CS 세부: ").append(String.join(", ", req.csSubTopics())).append("\n");
        }
        if (req.resumeText() != null && !req.resumeText().isBlank()) {
            sb.append("이력서:\n").append(req.resumeText()).append("\n");
        }

        sb.append("세션: ").append(UUID.randomUUID()).append("\n");
        sb.append("중복 없는 새 관점의 질문을 생성하세요.");
        return sb.toString();
    }
}
```

### FollowUpPromptBuilder

```java
@Component
@RequiredArgsConstructor
public class FollowUpPromptBuilder {

    private final PersonaResolver personaResolver;
    private String template; // @PostConstruct에서 follow-up.txt 로드

    @PostConstruct
    void init() {
        template = new String(getClass().getResourceAsStream(
            "/prompts/template/follow-up.txt").readAllBytes());
    }

    public String buildSystemPrompt(FollowUpGenerationRequest req) {
        TechStack effectiveStack = req.techStack() != null
            ? req.techStack() : TechStack.getDefaultForPosition(req.position());
        ResolvedProfile profile = personaResolver.resolve(req.position(), effectiveStack);

        // [M2] 단일 플레이스홀더: profile.followUpDepth()가 이미 base+overlay merge 결과
        return template
            .replace("{MEDIUM_PERSONA}", profile.mediumPersona())
            .replace("{FOLLOWUP_DEPTH}", profile.followUpDepth());
    }

    public String buildUserPrompt(FollowUpGenerationRequest req) {
        TechStack effectiveStack = req.techStack() != null
            ? req.techStack() : TechStack.getDefaultForPosition(req.position());

        StringBuilder sb = new StringBuilder();
        sb.append("직무: ").append(positionKorean(req.position()))
          .append(" (").append(effectiveStack.getDisplayName()).append(") | ")
          .append("레벨: ").append(levelKorean(req.level())).append("\n\n");

        sb.append("질문: ").append(req.questionContent()).append("\n");
        sb.append("답변: ").append(req.answerText()).append("\n");
        sb.append("비언어: ").append(
            req.nonVerbalSummary() != null ? req.nonVerbalSummary() : "없음").append("\n");

        if (req.previousExchanges() != null && !req.previousExchanges().isEmpty()) {
            sb.append("\n이전 후속:\n");
            for (int i = 0; i < req.previousExchanges().size(); i++) {
                var ex = req.previousExchanges().get(i);
                sb.append("[").append(i + 1).append("] Q: ").append(ex.question()).append("\n");
                sb.append("[").append(i + 1).append("] A: ").append(ex.answer()).append("\n");
            }
        }

        sb.append("\n새 후속 질문을 생성하세요.");
        return sb.toString();
    }
}
```

### v3 최적화 적용 체크리스트

| # | 전략 | 적용 위치 | 절약 |
|---|------|----------|------|
| 1 | 페르소나 깊이 FULL / MEDIUM | 질문=fullPersona, 후속=mediumPersona | ~120 tok (후속) |
| 2 | 레벨 가이드 필터링 | `LevelGuideProvider.get(level)` | ~129 tok |
| 3 | CS 블록 조건부 제거 | `interviewTypes.contains(CS_FUNDAMENTAL)` | ~111 tok |
| 4 | 유형 가이드 필터링 | `stream().filter()` | ~89 tok |
| 5 | JSON 스키마 압축 | 템플릿에 1줄 JSON | ~52 tok |
| 6 | 루브릭 압축 | 미해당 (질문 생성) | - |
| 7 | 이력서 블록 조건부 | `resumeText != null` | ~40 tok |

---

## Task 2-4: [C1] QuestionGenerationRequestedEvent에 techStack 추가 + 이벤트 체인 수정

- **Implement**: `backend`
- **Review**: `architect-reviewer` — 이벤트 전파 완전성

> **[CRITICAL C1]** 현재 `QuestionGenerationRequestedEvent`에 techStack 필드가 없어,
> 면접 생성 → 이벤트 발행 → 질문 생성 경로에서 techStack이 전달되지 않는 버그.
> 이 Task가 없으면 프롬프트 재설계의 핵심 경로가 단절된다.

### 파일 및 변경 사항

#### 1. Event에 techStack 필드 추가

수정: `backend/src/main/java/com/rehearse/api/domain/interview/event/QuestionGenerationRequestedEvent.java`

```java
// 현재 필드: interviewId, position, positionDetail, level, interviewTypes, csSubTopics, resumeText, durationMinutes
// 추가:
private final TechStack techStack;  // nullable
```

#### 2. InterviewService에서 이벤트 발행 시 techStack 전달

수정: `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewService.java`

- `createInterview()` (L65~74): 이벤트 생성에 `request.getTechStack()` 추가
- `retryQuestionGeneration()` (L109~133): 이벤트 생성에 `interview.getTechStack()` 추가

#### 3. QuestionGenerationService에서 Request DTO로 변환

수정: `backend/src/main/java/com/rehearse/api/domain/interview/service/QuestionGenerationService.java`

```java
// 이벤트 → Request DTO 변환
QuestionGenerationRequest request = new QuestionGenerationRequest(
    event.getPosition(),
    event.getPositionDetail(),
    event.getLevel(),
    new HashSet<>(event.getInterviewTypes()),
    new HashSet<>(event.getCsSubTopics()),
    event.getResumeText(),
    event.getDurationMinutes(),
    event.getTechStack()  // [C1] 추가
);
aiClient.generateQuestions(request);
```

---

## Task 2-5: ClaudeApiClient + MockAiClient 구현 변경

- **Implement**: `backend`
- **Review**: `code-reviewer`

### 파일 및 변경 사항

#### 1. ClaudeApiClient

수정: `backend/src/main/java/com/rehearse/api/infra/ai/ClaudeApiClient.java`

- `ClaudePromptBuilder` 의존성 → `QuestionGenerationPromptBuilder` + `FollowUpPromptBuilder` 교체
- `generateQuestions(QuestionGenerationRequest request)`: 새 빌더로 system/user prompt 생성
- `generateFollowUpQuestion(FollowUpGenerationRequest request)`: 새 빌더로 system/user prompt 생성

#### 2. MockAiClient

수정: `backend/src/main/java/com/rehearse/api/infra/ai/MockAiClient.java`

- 새 시그니처에 맞게 변경
- `log.info("TechStack: {}", request.techStack())` 추가

#### 3. InterviewService — follow-up 호출에 context 전달

수정: `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewService.java`

```java
// generateFollowUp() 내부:
FollowUpGenerationRequest followUpReq = new FollowUpGenerationRequest(
    interview.getPosition(),
    interview.getEffectiveTechStack(),
    interview.getLevel(),
    request.getQuestionContent(),
    request.getAnswerText(),
    request.getNonVerbalSummary(),
    request.getPreviousExchanges()
);
aiClient.generateFollowUpQuestion(followUpReq);
```

#### 4. ClaudePromptBuilder 폐기

수정: `backend/src/main/java/com/rehearse/api/infra/ai/ClaudePromptBuilder.java`

- `@Deprecated` 추가
- `calculateQuestionCount()` 정적 메서드는 유지 (새 빌더에서 재사용) — 추후 유틸 클래스로 이동 가능

---

## Task 2-6: [H4] InterviewResponse에 techStack 추가

- **Implement**: `backend`
- **Review**: `code-reviewer`

> **[HIGH H4]** FE에서 기존 면접의 techStack을 확인할 수 없는 문제.

### 파일

수정: `backend/src/main/java/com/rehearse/api/domain/interview/dto/InterviewResponse.java`

```java
// 필드 추가 (L19 근처):
private final TechStack techStack;  // nullable

// from() 메서드에 매핑 추가 (L40~L53):
.techStack(interview.getTechStack())
```

---

## Task 2-7: [H5] CreateInterviewRequest에 techStack + 검증 로직

- **Implement**: `backend`
- **Review**: `code-reviewer` — 검증 완전성

> **[HIGH H5]** FE에서 잘못된 position-techStack 조합(예: FRONTEND + JAVA_SPRING)을 보내면 저장되는 문제.

### 파일 및 변경 사항

#### 1. DTO에 techStack 추가

수정: `backend/src/main/java/com/rehearse/api/domain/interview/dto/CreateInterviewRequest.java`

```java
// nullable — 선택하지 않으면 Position 기본 스택
private TechStack techStack;
```

#### 2. Service에서 검증

수정: `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewService.java`

```java
// createInterview() 내부, Interview 생성 전:
if (request.getTechStack() != null && !request.getTechStack().isAllowedFor(request.getPosition())) {
    throw new BusinessException(InterviewErrorCode.INVALID_TECH_STACK);
}
```

#### 3. ErrorCode 추가

수정: `backend/src/main/java/com/rehearse/api/domain/interview/exception/InterviewErrorCode.java`

```java
INVALID_TECH_STACK(HttpStatus.BAD_REQUEST, "INTERVIEW_005", "해당 직무에서 지원하지 않는 기술 스택입니다.")
```

---

## 검증

### 단위 테스트

- 신규: `backend/src/test/java/com/rehearse/api/infra/ai/prompt/QuestionGenerationPromptBuilderTest.java`
  - CS_FUNDAMENTAL 포함/미포함 시 CS 블록 토글
  - 이력서 유/무 시 블록 토글
  - 선택된 유형 가이드만 포함되는지
  - 레벨별 가이드 1개만 포함되는지

- 신규: `backend/src/test/java/com/rehearse/api/infra/ai/prompt/FollowUpPromptBuilderTest.java`
  - MEDIUM 페르소나 사용 확인
  - `{FOLLOWUP_DEPTH}` 플레이스홀더가 치환되었는지 (리터럴 잔존 없음)
  - 이전 후속 대화 유/무 처리

- 수정: 기존 `ClaudeApiClientTest.java`, `InterviewServiceTest.java`, `QuestionGenerationServiceTest.java`
  - 새 시그니처(Request DTO)에 맞게 변경
  - techStack 파라미터 전달 확인
  - 잘못된 position-techStack 조합 시 예외 발생 확인

### CI

```bash
cd backend && ./gradlew test
```

---

## 주의사항

- `calculateQuestionCount()` 정적 메서드는 `ClaudePromptBuilder`에서 유지하되, 새 빌더에서 `ClaudePromptBuilder.calculateQuestionCount()`로 참조
- JSON 필드 이름은 기존과 동일하게 유지 (파싱 호환성)
- 기존 테스트 파일에서 `AiClient` mock 시그니처가 모두 깨지므로, 테스트 수정을 Task 2-5에 포함
