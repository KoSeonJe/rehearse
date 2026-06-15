# BE Task 05 — `FollowUpExchange.answer` → `answerText` (필드 + 생성자 + getter cascade 15 파일 + JSON 키)

## 목적

`FollowUpRequest$FollowUpExchange.answer` outlier 필드를 `answerText` 표준으로 통일. **JSON 요청 키도 동시 변경** (BE+FE 즉시 연속 머지 윈도우 수용).

## 변경 파일

### 정의 변경
- `backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpRequest.java`
  - `private String answer;` → `private String answerText;` (line 34)
  - 생성자 시그니처 2개 cascade:
    - `FollowUpExchange(question, answer)` → `FollowUpExchange(question, answerText)`
    - `FollowUpExchange(question, answer, followUpType)` → `(question, answerText, followUpType)`
  - Lombok `@Getter` 자동 생성 `.answer()` getter → `.answerText()` 자동 변경
  - JSON 키 (Jackson 기본 = 필드명 직렬화) → 자동 `answerText` (별도 `@JsonProperty` 미사용 상태 유지)

### Lombok getter cascade — 15 파일
**Domain (interview)**:
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/AskedPerspectives.java`
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/InterviewRuntimeState.java`
- `backend/src/main/java/com/rehearse/api/domain/interview/service/TurnAnalysisPipeline.java`

**Domain (resume)**:
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeQuestionResultGenerator.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandler.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java`

**Infra (ai)**:
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/FollowUpGenerationRequest.java`
- `backend/src/main/java/com/rehearse/api/infra/ai/context/compaction/DialogueCompactor.java`
- `backend/src/main/java/com/rehearse/api/infra/ai/context/ContextBuildRequest.java`
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/DialogueHistoryLayer.java`
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/AbstractResumeJsonPromptBuilder.java`
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeChainInterrogatorPromptBuilder.java`
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptBuilder.java`

(15번째 = `FollowUpRequest.java` 자체 정의)

### 테스트 갱신
- `FollowUpServiceTest`, `FollowUpTransactionHandlerTest`, `PlaygroundModeHandlerTest`, `ResumeInterviewOrchestratorTest`, `ResumeQuestionPersisterTest`, `TestFixtures` (FollowUpExchange 팩토리), 그 외 fixture 사용 테스트 다수.

## 핵심 변경 (요지)

- **IntelliJ Safe Rename 강력 권장** (handoff `컨텍스트 메모`). 누락 시 컴파일 RED.
- 필드명 변경 = JSON 키 변경 = FE wire 동시 갱신 필요. FE Task 와 contract 합의 (tech-spec API contract 섹션).
- Jackson 직렬화 = `@JsonProperty` 미사용 → 필드명 그대로 JSON 키.

## API Contract (BE+FE 동시)

```jsonc
// 요청 (FollowUpRequest.previousExchanges[*])
// 변경 전
{ "question": "...", "answer": "...", "followUpType": "...", "selectedPerspective": "..." }
// 변경 후
{ "question": "...", "answerText": "...", "followUpType": "...", "selectedAnswerFeedbackPerspective": "..." }
```

(`selectedPerspective` 필드 변경은 Task 6.)

## 테스트

- 카테고리: Service Integration 다수 + Domain Unit 일부.
- 실행: `./gradlew test --tests "*FollowUp*" "*Resume*" "*Dialogue*"`
- 회귀 핵심: `FollowUpServiceTest` GREEN + `PlaygroundModeHandlerTest` 의 `FollowUpExchange` 객체 빌드 GREEN.

## 완료 기준

- [ ] `grep -n "private String answer\b" backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpRequest.java` = 0
- [ ] `grep -rn "\.answer()" backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpRequest.java` = 0 (FollowUpExchange 호출만 — IDE Safe Rename 후 0건)
- [ ] `grep -rn "\.answerText()" backend/src/main/java | wc -l` ≥ 15 (위 15 파일 cascade)
- [ ] `./gradlew compileJava` GREEN
- [ ] `FollowUpServiceTest`, `PlaygroundModeHandlerTest`, `ResumeInterviewOrchestratorTest` GREEN

## 의존

- 선행: 없음 (T1, T2 와 병렬 가능).
- 후행: T6 (`selectedPerspective` 변경 — 같은 `FollowUpExchange` 내 필드).

## 커밋

```
refactor(BE): FollowUpExchange.answer → answerText 통일 (필드 + 생성자 + getter cascade + JSON 키)
```

## 위험

- **15 파일 cascade 누락**: 1곳 누락 시 컴파일 RED. **완화** = IntelliJ Safe Rename + Task 직후 `./gradlew compileJava` 즉시 검증.
- **JSON 키 윈도우**: BE 머지 직후 FE 머지 즉시 (handoff 결정). 윈도우 수분 수용.
