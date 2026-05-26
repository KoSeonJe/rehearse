# Implement — 정상 답변 turn 의 NOT_EVALUABLE 오판정 해소

> **작성자**: 구현 agent (backend)
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: BE 단일 영역. FE / lambda 변경 없음.
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 PR | 의존 |
|-------|------|--------------|--------|------|
| 1 | AnswerAnalysis transcript 도입 (도메인 + DTO + Schema + 프롬프트) | `backend` | #1 | - |
| 2 | RubricScorer blank-guard transcript fallback | `backend` | #2 | Phase 1 |
| 3 | 테스트 (Domain Unit + Service Integration + Live E2E + audio fixtures) | `backend` | #3 | Phase 2 |

---

## Phase 1: AnswerAnalysis transcript 도입

- **구현**: `backend` — AnswerAnalysis 도메인 record + LLM 응답 DTO + strict schema + 프롬프트 템플릿 2개에 `transcript` 필드 일관 추가. canonical constructor 로 null 정규화. 기존 `new AnswerAnalysis(...)` 호출처 일괄 마이그레이션.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/interview/entity/AnswerAnalysis.java` — record 에 `String transcript` 필드 추가 (1번째 인자). canonical constructor 에서 `transcript = transcript != null ? transcript : ""` 정규화. `empty()` 의 transcript = "".
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedAnswerAnalysis.java` — `@JsonProperty("transcript") String transcript` 필드 추가. `toDomain()` 에 transcript 매핑.
- `backend/src/main/java/com/rehearse/api/infra/ai/schema/GeneratedAnswerAnalysisSchema.java` — `build()` 의 `rootProps` 에 `transcript: {type: string}` + `required` 목록 맨 앞에 `"transcript"` 추가. `additionalProperties: false` 유지.
- `backend/src/main/resources/prompts/template/audio-turn-analyzer.txt` — "오디오에서 전사한 한국어 텍스트를 응답 JSON 의 `answer_analysis.transcript` 필드에 그대로 반환. transcribe 누락 시 빈 문자열 ''" 1문단 추가.
- `backend/src/main/resources/prompts/template/answer-analyzer.txt` — "USER_ANSWER 입력 텍스트를 응답 JSON 의 root `transcript` 필드에 그대로 복사" 1문단 추가.
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedTurnAnalysis.java` — 변경 0 (`answerAnalysis.toDomain()` 호출 그대로). 확인용.
- 기존 호출처 마이그레이션 (canonical constructor null 정규화 덕분에 신규 인자 추가만):
  - `backend/src/test/java/com/rehearse/api/domain/feedback/rubric/service/RubricScoringEventListenerTest.java`
  - `backend/src/test/java/com/rehearse/api/domain/feedback/rubric/service/RubricScorerTest.java`
  - `backend/src/test/java/com/rehearse/api/domain/interview/entity/AnswerAnalysisTest.java`
  - `backend/src/test/java/com/rehearse/api/domain/interview/service/FollowUpServiceIntegrationTest.java`
  - MockAiClient / Infra Integration 의 JSON 응답 fixture (strict schema 통과를 위해 `transcript` 필드 채움)

### 핵심 로직 / 변경 요약

```java
// AnswerAnalysis canonical constructor
public AnswerAnalysis {
    transcript = transcript != null ? transcript : "";
    // 기존 null-guard 들 유지
}

// GeneratedAnswerAnalysis.toDomain()
return new AnswerAnalysis(
        transcript != null ? transcript : "",
        claims, dimensionGaps, weakestDimension, unstatedAssumptions, recommendedNextAction);

// GeneratedAnswerAnalysisSchema.build()
rootProps.put("transcript", Map.of("type", "string"));
schema.put("required", List.of(
        "transcript", "claims", "dimension_gaps", "weakest_dimension",
        "unstated_assumptions", "recommended_next_action"));
```

LLM JSON 응답 구조 (경로별):
- audio chat (`AudioTurnAnalyzer`): `{"answer_analysis": {"transcript": "...", "claims": [...], ...}}` (JSON_OBJECT 모드, strict 미적용)
- text-only (`AnswerAnalyzer`): `{"transcript": "...", "claims": [...], ...}` (JSON_SCHEMA strict)

### 의존

- 선행 phase: 없음
- 외부 의존: 없음 (Flyway DDL 0, FE 영향 0)

### Verification Hook

- 명령: `./gradlew build`
- 통과 기준: 컴파일 통과 + 기존 테스트 회귀 0
- 관찰 가능 동작: `RubricScoringEventListenerTest` / `RubricScorerTest` / `FollowUpServiceIntegrationTest` 모두 green

### 커밋 메시지 (예상)

```
feat(BE): AnswerAnalysis transcript 필드 추가 + LLM 프롬프트 갱신
```

---

## Phase 2: RubricScorer blank-guard transcript fallback

- **구현**: `backend` — `RubricScorer.isBlankAnswer` 시그니처에 `AnswerAnalysis analysis` 추가. userAnswer 가 임계 이하면 `analysis.transcript()` fallback. NOT_EVALUABLE 적용 INFO 로그에 `transcript_len` 필드 추가.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScorer.java` — `isBlankAnswer(userAnswer)` → `isBlankAnswer(userAnswer, analysis)` 시그니처 확장. `effectiveText` private 추출. NOT_EVALUABLE INFO 로그에 `transcript_len={}` 추가.

### 핵심 로직 / 변경 요약

```java
private boolean isBlankAnswer(String userAnswer, AnswerAnalysis analysis) {
    String effective = effectiveText(userAnswer, analysis);
    return effective == null || effective.strip().length() <= BLANK_ANSWER_LENGTH_THRESHOLD;
}

private static String effectiveText(String userAnswer, AnswerAnalysis analysis) {
    // 의도: userAnswer 임계 초과 시 우선, 임계 이하면 transcript fallback.
    if (userAnswer != null && userAnswer.strip().length() > BLANK_ANSWER_LENGTH_THRESHOLD) {
        return userAnswer;
    }
    return analysis != null ? analysis.transcript() : null;
}
```

호출부 (`RubricScorer.score(...)` line 47-53 근처): `isBlankAnswer(userAnswer)` → `isBlankAnswer(userAnswer, analysis)`.

### 의존

- 선행 phase: Phase 1 (`AnswerAnalysis.transcript()` 접근 가능)
- 외부 의존: 없음

### Verification Hook

- 명령: `./gradlew test --tests "com.rehearse.api.domain.feedback.rubric.service.RubricScorerTest"`
- 통과 기준: 기존 boundary 케이스 (null / "" / "잘몰라" / 공백 / 4자) green 유지 + 새 시그니처 컴파일 통과
- 관찰 가능 동작: `RubricScoringEventListenerTest` 회귀 green

### 커밋 메시지 (예상)

```
feat(BE): RubricScorer blank-guard 에 transcript fallback 적용
```

---

## Phase 3: 테스트 (Domain Unit + Service Integration + Live E2E + audio fixtures)

- **구현**: `backend` — RubricScorer 신규 분기 5개 케이스 + AnswerAnalysis empty 회귀 + Live E2E 5 시나리오 + audio fixture 2개 생성.

### 변경 파일

- `backend/src/test/java/com/rehearse/api/domain/feedback/rubric/service/RubricScorerTest.java` — 신규 `@Nested` "transcript fallback" 그룹. 케이스 (a)~(e) 5개 추가 (tech-spec §Verification §Service Integration 매핑).
- `backend/src/test/java/com/rehearse/api/domain/interview/entity/AnswerAnalysisTest.java` — `AnswerAnalysis.empty().transcript() == ""` + `new AnswerAnalysis(null, ...)` canonical constructor 정규화 확인.
- `backend/src/test/java/com/rehearse/api/e2e/RubricScoringNotEvaluableLiveE2ETest.java` (신규) — `@Disabled("Live LLM E2E — RUN_LIVE_API=true 환경변수로만 활성")` + `@EnabledIfEnvironmentVariable(named = "RUN_LIVE_API", matches = "true")` extends `ServiceIntegrationSupport`. 시나리오 1-5 (product-spec AC §6).
- `backend/src/test/resources/fixtures/audio/not-evaluable/long-korean.webm` (신규) — 30초+ 한국어 자체 녹음, webm/opus, ≤10MB.
- `backend/src/test/resources/fixtures/audio/not-evaluable/silent-5s.webm` (신규) — `ffmpeg -f lavfi -i anullsrc=r=48000:cl=mono -t 5 -c:a libopus silent-5s.webm` 생성.

### 핵심 로직 / 변경 요약

```java
// RubricScorerTest 신규 @Nested
@Nested
@DisplayName("transcript fallback")
class TranscriptFallback {
    @Test
    @DisplayName("FE 텍스트 비었지만 transcript 정상 → 정상 채점")
    void should_score_when_userAnswerEmpty_butTranscriptPresent() { /* 케이스 (a) */ }

    @Test
    @DisplayName("FE 텍스트 + transcript 모두 비면 전 차원 NOT_EVALUABLE")
    void should_notEvaluable_when_bothEmpty() { /* 케이스 (b) */ }

    // (c) userAnswer 정상 + transcript 빈
    // (d) userAnswer ≤3자 + transcript 정상
    // (e) 회귀 — userAnswer ≤3자 + AnswerAnalysis.empty() → NOT_EVALUABLE
}

// RubricScoringNotEvaluableLiveE2ETest 패턴
@Disabled("Live LLM E2E — RUN_LIVE_API=true 환경변수로만 활성")
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_API", matches = "true")
class RubricScoringNotEvaluableLiveE2ETest extends ServiceIntegrationSupport {
    @Test
    @DisplayName("긴 음성 답변 + FE 송신 빈 문자열 → 차원별 점수 정상 산출")
    void scenario1() { /* audio fixture long-korean.webm + answerText="" */ }
    // scenario2..5 = product-spec AC §6 매핑
}
```

audio fixture 생성:
- `long-korean.webm`: 작성자 자체 녹음 (30초+ 한국어), PII 청결 한정.
- `silent-5s.webm`: `ffmpeg -f lavfi -i anullsrc=r=48000:cl=mono -t 5 -c:a libopus backend/src/test/resources/fixtures/audio/not-evaluable/silent-5s.webm`

### 의존

- 선행 phase: Phase 2 (RubricScorer 새 시그니처)
- 외부 의존: `RUN_LIVE_API=true` 환경변수 + `OPENAI_API_KEY` (Live E2E 실행 시)

### Verification Hook

- 명령:
  - `./gradlew test --tests "com.rehearse.api.domain.feedback.rubric.service.RubricScorerTest"`
  - `./gradlew test --tests "com.rehearse.api.domain.interview.entity.AnswerAnalysisTest"`
  - `RUN_LIVE_API=true ./gradlew test --tests "com.rehearse.api.e2e.RubricScoringNotEvaluableLiveE2ETest"` (사용자 직접 실행)
- 통과 기준:
  - Domain Unit + Service Integration green
  - Live E2E 5 시나리오 모두 green (product-spec Goal 단일 판정 기준)
- 관찰 가능 동작: `./gradlew build` 전체 통과

### 커밋 메시지 (예상)

```
test(BE): NOT_EVALUABLE 오판정 해소 Live E2E 5종 + 회귀 가드 추가
```

---

## 통합 Verification

- [ ] tech-spec.md `Verification` 섹션 항목 모두 통과 (Live E2E 5 + Service Integration + Domain Unit + 빌드 / 회귀)
- [ ] 추가 회귀 체크: 없음 (tech-spec 가 모두 포괄)

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] 지정 리뷰어 실행 (구현 완료 직후 — 메인 세션 책임)
  - BE only → `code-reviewer-backend`
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `backend/.claude/rules/testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec §Pre/Post State)
