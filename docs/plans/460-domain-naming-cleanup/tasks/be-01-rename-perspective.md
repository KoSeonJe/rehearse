# BE Task 01 — `Perspective` → `AnswerFeedbackPerspective` rename

## 목적

`interview.Perspective` (7개 enum) 클래스명을 `AnswerFeedbackPerspective` 로 변경. `feedback.FeedbackPerspective` 와의 단어 충돌 제거 (Task 2 와 한 쌍으로 단어 중복 0 달성).

## 변경 파일

### 신규 / 이름 변경
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/Perspective.java` → `AnswerFeedbackPerspective.java` (파일 + 클래스명)

### 임포트 / 사용 갱신 (main, 6 파일)
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/AskedPerspectives.java`
  - `record AskedPerspectives(List<Perspective> values)` → `List<AnswerFeedbackPerspective> values`
- `backend/src/main/java/com/rehearse/api/domain/interview/service/AnswerAnalyzer.java`
  - 임포트 + 시그니처 / 변수 타입 갱신 (line 51, 91, 101 등)
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/AnswerAnalyzerPromptBuilder.java`
  - 임포트 + 시그니처 갱신 (line 49, 54, 60, 70 등)
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/AudioTurnAnalyzerPromptBuilder.java`
  - 임포트 + 시그니처 갱신 (line 48, 50, 56, 66 등)
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/FollowUpPromptBuilder.java`
  - 임포트 + 시그니처 갱신 (line 70, 103, 106, 126 등)
- `backend/src/main/java/com/rehearse/api/infra/ai/context/AnswerAnalysisJsonRenderer.java`
  - 임포트 + 시그니처 갱신 (line 24, 27, 47 등)

### 테스트 갱신 (6 파일)
- `backend/src/test/java/com/rehearse/api/infra/ai/prompt/AnswerAnalyzerPromptBuilderTest.java`
- `backend/src/test/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandlerTest.java`
- `backend/src/test/java/com/rehearse/api/domain/interview/entity/AskedPerspectivesTest.java`
- `backend/src/test/java/com/rehearse/api/domain/interview/AnswerAnalysisTest.java`
- `backend/src/test/java/com/rehearse/api/domain/interview/service/AnswerAnalyzerTest.java`
- `backend/src/test/java/com/rehearse/api/domain/interview/service/FollowUpServiceTest.java`

## 핵심 변경 (요지)

- 클래스명만 변경. enum 값 7개 (`TECHNICAL_DEPTH`, `TRADEOFF`, ... 등) **그대로 유지**.
- `AskedPerspectives` 클래스명 / 변수명 (`askedPerspectives` 41건) = **잔존** (product-spec 카탈로그 #10 보류). record 컴포넌트 제네릭 타입만 변경.
- IntelliJ Safe Rename 사용 권장 — cascade 자동 처리 (tech-spec L367, handoff `컨텍스트 메모`).

## 비스코프 (절대 미터치)

- 변수명 `askedPerspectives` 41건 — Phase 2 후보.
- enum 값 자체 (`TECHNICAL`, `EXPERIENCE` 등) — Phase 2 후보 (LLM 프롬프트 영향).

## 테스트

- 임포트 갱신만 — 단언 / 시나리오 변경 0.
- 카테고리: Domain Unit (≥60% 비중 유지).
- 실행: `./gradlew test --tests "com.rehearse.api.domain.interview.*" "com.rehearse.api.infra.ai.prompt.AnswerAnalyzerPromptBuilderTest"`

## 완료 기준

- [ ] `grep -rn "import com.rehearse.api.domain.interview.entity.Perspective\b" backend/src/main/java` = 0
- [ ] `grep -rn "import com.rehearse.api.domain.interview.entity.AnswerFeedbackPerspective" backend/src/main/java` ≥ 6 (위 6 파일)
- [ ] `./gradlew compileJava` GREEN
- [ ] 위 6 테스트 클래스 GREEN
- [ ] `AskedPerspectives` 변수명 `askedPerspectives` = 잔존 (절대 미수정)

## 의존

- 선행: 없음. T2/T5 와 병렬 가능.
- 후행: T4 (PromptFormatters 시그니처가 `AnswerFeedbackPerspective` 사용), T6 (`selectedAnswerFeedbackPerspective` 단어 일관성).

## 커밋

```
refactor(BE): Perspective → AnswerFeedbackPerspective 클래스 rename
```
