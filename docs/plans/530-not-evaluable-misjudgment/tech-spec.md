# Tech Spec — 정상 답변 turn 의 NOT_EVALUABLE 오판정 해소

> **작성자**: 구현 agent (backend)
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄 미러)

정상 답변 turn 이 `RubricScorer` blank-guard 에서 잘못 NOT_EVALUABLE 로 차단되는 문제 해소. **단일 판정 기준 = Live E2E 5종 시나리오 통과**.

## Evidence

### 현재 구조

| 위치 | 역할 | 본 작업 영향 |
|------|------|------------|
| `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScorer.java:32-53` | 차원별 채점 진입점. `isBlankAnswer(userAnswer)` (`userAnswer == null \|\| strip().length() <= 3`) → 전 차원 NOT_EVALUABLE 반환 | **변경** — analysis.transcript fallback 사용 |
| `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScoringEventListener.java:35-46` | 이벤트 수신 → `rubricScorer.score(question, questionSet, interview, event.userAnswer(), event.analysis())` | 변경 없음 (analysis 이미 전달 중) |
| `backend/src/main/java/com/rehearse/api/domain/interview/entity/AnswerAnalysis.java` | 도메인 record (claims / dimensionGaps / weakestDimension / unstatedAssumptions / recommendedNextAction). **DB 비저장 — pure in-memory payload** | **변경** — `transcript` 필드 추가 |
| `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedAnswerAnalysis.java` | LLM 응답 DTO. `toDomain()` 매핑 | **변경** — `transcript` 필드 + 매핑 추가 |
| `backend/src/main/java/com/rehearse/api/infra/ai/schema/GeneratedAnswerAnalysisSchema.java:39-81` | strict JSON schema (`additionalProperties: false`). CS / Resume 트랙별 `required` 목록 | **변경** — `transcript` 추가 (required + properties) |
| `backend/src/main/resources/prompts/template/audio-turn-analyzer.txt` | AudioTurnAnalyzer 시스템 프롬프트 템플릿 (`AudioTurnAnalyzerPromptBuilder` 가 로드). LLM 응답 = `{"answer_analysis": {...}}` wrapper 구조 (`GeneratedTurnAnalysis`). `ResponseFormat.JSON_OBJECT` 사용 (strict schema 미적용) | **변경** — "오디오 전사를 `answer_analysis.transcript` 필드에 반환" 지시 추가 |
| `backend/src/main/resources/prompts/template/answer-analyzer.txt` | `AnswerAnalyzer` 가 사용하는 text-only 분석 프롬프트 (`AnswerAnalyzerPromptBuilder`). 호출 경로 = (a) `TextFallbackTurnAnalyzer` (audio 실패 fallback) + (b) 기타 text-only 진입 전체. `ResponseFormat.JSON_SCHEMA` strict (`GeneratedAnswerAnalysisSchema`) 적용 | **변경** — "USER_ANSWER 입력 텍스트를 `transcript` 필드에 그대로 복사" 지시 추가 |
| `backend/src/main/java/com/rehearse/api/infra/ai/adapter/RubricScoringAdapter.java:187-200` | Trigger 2: LLM `observation` 이 `"관련 발언 없음"` sentinel 시작 시 차원 NOT_EVALUABLE | **변경 없음** — 현 코드 정상 (사용자 보고 증상 = Trigger 1) |
| `backend/src/main/java/com/rehearse/api/infra/ai/adapter/RubricScorerResponseValidator.java:14-42` | Trigger 3: score 범위 / observation 누락 / evidence_quote 미일치 시 retry → 실패 차단 | **변경 없음** — Live E2E 회귀 가드만 |
| `frontend/src/hooks/use-answer-flow.ts:106-115` | `getCurrentAnswerText()` = Web Speech `isFinal=true` join. 빈 문자열 가능 | **변경 없음** (product-spec Non-Goals) |

### 사용자 발화 (특정 결정 근거)

- **"AnswerAnalysis 에 transcript 필드 추가"** (2026-05-21 Trade-off 결정) — 사용자가 정확도 우선 선택. claims-based heuristic 폐기.
- **"e2e live test만 성공시켜줘"** (product-spec Goal) — 완료 판정 단일 기준.

### 추정 / 미확인 가정

- **(추정)** Gemini / GPT-4o-mini 가 system prompt 에 transcript 필드 추가 지시 시 안정적으로 채움. 확신 근거 = 동일 트랙 `claims / observation` 등 자연어 필드 이미 안정 추출 중.
- **(추정)** 정상 음성 답변 turn 의 audio chat 분석에서 transcript 길이 = 실제 답변 텍스트 길이와 동일 또는 약간 짧음 (LLM transcribe 누락 5% 이내). 측정 부재. Live E2E 시나리오 (1) 로 검증.
- **(확신)** AnswerAnalysis DB 컬럼 없음 → Flyway DDL 마이그레이션 0개. grep `@Entity / @Column` 결과 없음.
- **(확신)** FE 응답 노출 없음 → API contract 변경 0. grep `AnswerAnalysis` in `domain/interview/dto/` / `controller/` 결과 없음.

### 외부 레퍼런스

- OpenAI strict JSON schema `additionalProperties: false` 제약 — `GeneratedAnswerAnalysisSchema.build()` 이미 적용.
- 관련 PR: #527 (NOT_EVALUABLE 차원 상태 도입, 본 issue 의 regression source).

## Trade-offs

### Option A (채택): AnswerAnalysis 에 `transcript` 필드 추가

LLM 한테 음성 transcribe 시켜 텍스트 원본을 `AnswerAnalysis.transcript` 에 보존. `RubricScorer.isBlankAnswer(userAnswer, analysis)` 시그니처 확장 — userAnswer 비면 `analysis.transcript()` fallback.

- **장점**:
  - 정확한 텍스트 원본 확보 — claims heuristic 환각 오탐 회피.
  - FE Web Speech 실패 / 부분 누락 / 빈 송신 모두 BE 단독 복구.
  - downstream (RubricScorer 외 다른 채점 / 분석 단계) 도 transcript 활용 여지.
- **단점**:
  - 변경 범위 5곳: AnswerAnalysis record + GeneratedAnswerAnalysis DTO + Schema + 프롬프트 템플릿 2개 (audio / text fallback) + `AnswerAnalysis.empty()` 호출하는 기존 테스트 다수.
  - 토큰 비용 ↑ — LLM 응답 payload 에 수백자 transcript 매 turn 포함 (대략 한 turn 당 +300~500 토큰 estimate).
  - LLM transcribe 실패 / 누락 시 transcript 부정확. 단 `audioFile` 본체 이미 분석된 후 부산물 → 분석 정상이면 transcribe 도 정상 가정 합리.
- **사유**: 사용자 명시 결정 (정확도 우선). Live E2E 시나리오 (1) "긴 음성 답변 + FE 송신 빈 문자열" 통과 위해 BE 단독 텍스트 복구 경로 확보 필수.

### Option B (폐기): claims-based heuristic

`RubricScorer.isBlankAnswer` 조건 = `userAnswer ≤3자 AND analysis.claims 비어있음`. schema / 프롬프트 / 토큰 비용 불변.

- **장점**: 최소 변경 (1개 메서드 + 테스트). 토큰 비용 0 증가.
- **폐기 사유**: 사용자 명시 결정 (2026-05-21) — 정확도 우선. claims 환각 오탐 가능성 (LLM 가 무응답 turn 에 claims 1개 추출하는 경우 — 실측 없음, 추정) 회피 가치 > 변경 비용.

### Option C (폐기): FE answerText 송신 보강

- **폐기 사유**: product-spec Non-Goals 명시 (STT 자체 개선 X).

## Architecture

### 흐름 (Pre → Post 차이만 강조)

```
[FE]  answerText (빈 가능)
  ↓
[FollowUpService]
  ├─ AudioTurnAnalyzer.analyze(audioFile, ...)
  │    └─ analyzeViaAudioChat:
  │         ├─ ChatRequest (system prompt = "transcribe to transcript field" ★추가)
  │         └─ aiClient.chatWithAudio → GeneratedTurnAnalysis (transcript 포함 ★추가)
  │              └─ toDomain → AnswerAnalysis(transcript=..., claims=..., ...)
  │    └─ fallback: TextFallbackTurnAnalyzer (text-only)
  │         └─ AnswerAnalysis(transcript = 원본 입력 텍스트, ...)  ★추가
  └─ publishAnswerAnalysisCompletedEvent (userAnswer, analysis)
       ↓
[AnswerAnalysisCompletedEvent]
       ↓
[RubricScoringEventListener @Async @AFTER_COMMIT]
       ↓
[RubricScorer.score(..., userAnswer, analysis)]
  └─ isBlankAnswer(userAnswer, analysis):   ★시그니처 확장
       ├─ 의도: userAnswer 가 임계 (3자) 초과 시 userAnswer 우선, 임계 이하면 transcript fallback.
       │        둘 다 임계 이하면 blank=true (= 진짜 무응답).
       ├─ effectiveText = (userAnswer strip).length() > BLANK_ANSWER_LENGTH_THRESHOLD
       │                  ? userAnswer
       │                  : analysis.transcript()
       └─ blank = effectiveText == null || strip(effectiveText).length() <= BLANK_ANSWER_LENGTH_THRESHOLD
  └─ blank=true → 전 차원 NOT_EVALUABLE (사유 = "응답 길이 3자 이하")
  └─ blank=false → RubricScoringAdapter 호출 → 정상 채점
```

### Trigger 2 / 3 처리 방침

- **변경 0 가정**. 사용자 보고 증상 = Trigger 1 단독. Trigger 2 ("관련 발언 없음" sentinel) / Trigger 3 (validator) 는 현 코드 정상.
- Live E2E 시나리오 (4) "긴 답변 + LLM observation 정상" / (5) "긴 답변 + validator 통과" = **회귀 가드**. 통과 시 변경 없음 확정. 미통과 시 별도 PR 후보 (본 plan scope 외).

## Data Model

**DB 스키마 변경 없음**. AnswerAnalysis = pure record (DB 비저장).

### Java 도메인 변경

```java
// backend/src/main/java/com/rehearse/api/domain/interview/entity/AnswerAnalysis.java
public record AnswerAnalysis(
        String transcript,                                    // ★추가
        List<Claim> claims,
        Map<String, Integer> dimensionGaps,
        String weakestDimension,
        List<String> unstatedAssumptions,
        RecommendedNextAction recommendedNextAction
) {
    public AnswerAnalysis {
        // canonical constructor 정규화 — transcript null → "" 로 통일.
        // 이유: 호출부 (toDomain / empty / 테스트 fixture) 의 null 분기 부담 ↓.
        transcript = transcript != null ? transcript : "";
        // 기존 null-guard 들 (claims / dimensionGaps / unstatedAssumptions / recommendedNextAction) 유지.
    }

    public static AnswerAnalysis empty() {
        return new AnswerAnalysis(
                "",                                           // ★추가 — 빈 문자열 기본
                List.of(), Map.of(), null, List.of(),
                RecommendedNextAction.CLARIFICATION);
    }
}
```

```java
// backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedAnswerAnalysis.java
public record GeneratedAnswerAnalysis(
        @JsonProperty("transcript") String transcript,        // ★추가
        @JsonProperty("claims") List<...> claims,
        // ... 기존 필드
) {
    public AnswerAnalysis toDomain() {
        return new AnswerAnalysis(
                transcript != null ? transcript : "",         // ★추가
                claims, dimensionGaps, weakestDimension, unstatedAssumptions, recommendedNextAction);
    }
}
```

### LLM JSON Schema 변경

```java
// backend/src/main/java/com/rehearse/api/infra/ai/schema/GeneratedAnswerAnalysisSchema.java
// build() 내부:
rootProps.put("transcript", Map.of("type", "string"));        // ★추가

schema.put("required", List.of(
        "transcript",                                         // ★추가 (맨 앞)
        "claims", "dimension_gaps", "weakest_dimension",
        "unstated_assumptions", "recommended_next_action"));
```

`additionalProperties: false` 유지 → strict 호환.

### LLM JSON 응답 구조 (경로별)

| 경로 | LLM 응답 root 구조 | transcript 위치 | enforcement |
|------|------------------|----------------|------------|
| audio chat (`AudioTurnAnalyzer`) | `{"answer_analysis": {...}}` wrapper (`GeneratedTurnAnalysis`) | `answer_analysis.transcript` (nest 내부) | `ResponseFormat.JSON_OBJECT` — strict schema 미적용 |
| text-only (`AnswerAnalyzer`) | `{...}` root 직접 (`GeneratedAnswerAnalysis`) | 루트 `transcript` | `ResponseFormat.JSON_SCHEMA` strict (`GeneratedAnswerAnalysisSchema`) |

→ `GeneratedTurnAnalysis.toDomain()` 은 변경 없음 (`answerAnalysis.toDomain()` 호출 그대로). `GeneratedAnswerAnalysis.toDomain()` 만 transcript 매핑 추가.

### 프롬프트 템플릿 변경

- `backend/src/main/resources/prompts/template/audio-turn-analyzer.txt` — "오디오에서 전사한 한국어 텍스트를 응답 JSON 의 `answer_analysis.transcript` 필드에 그대로 반환. transcribe 누락 시 빈 문자열 ''" 1문단 추가. (strict schema 미적용 경로 → 프롬프트 준수도가 1차 신뢰 근거)
- `backend/src/main/resources/prompts/template/answer-analyzer.txt` — "USER_ANSWER 입력 텍스트를 응답 JSON 의 root `transcript` 필드에 그대로 복사" 1문단 추가. (strict schema 가 누락 시 LLM 응답 자체 거부)

## API Contract

**FE 노출 변경 없음**. `AnswerAnalysis` 는 controller / DTO 경계 외부 노출 0 (grep 확인). FE 송신 (`answerText`) / FE 응답 (rubric 채점 결과) 형식 변동 없음.

→ **BE 단독 영역**. API contract 사용자 승인 게이트 불필요.

## Verification (완료 판정)

구현 완료 = 아래 모두 통과.

### Live E2E (단일 판정 기준 — product-spec Goal)

위치: `backend/src/test/java/com/rehearse/api/e2e/RubricScoringNotEvaluableLiveE2ETest.java` (신규)

패턴: `@Disabled("Live LLM E2E — RUN_LIVE_API=true 환경변수로만 활성")` + `@EnabledIfEnvironmentVariable(named = "RUN_LIVE_API", matches = "true")` + extends `ServiceIntegrationSupport` (TRUNCATE in `@BeforeEach`).

| # | 시나리오 (product-spec AC §6 매핑) | 입력 | 기대 결과 |
|---|-----------------------------------|------|----------|
| 1 | 긴 음성 답변 + FE 송신 텍스트 빈 문자열 | audio fixture (30초+ 한국어) + `answerText=""` | 전 차원 점수 정상 산출 (NOT_EVALUABLE 0건). `analysis.transcript.length() > 3` |
| 2 | 긴 음성 답변 + 정상 LLM 응답 | audio fixture + `answerText` 정상 | 전 차원 점수 정상 산출 |
| 3 | 무음 + 텍스트 임계 미만 | audio fixture (무음 5초) + `answerText="잘"` | 전 차원 NOT_EVALUABLE 정상 적용 (사유 = "응답 길이 3자 이하") |
| 4 | 긴 답변 + LLM observation 정상 (Trigger 2 회귀) | audio fixture + `answerText` 정상 | RubricScoringAdapter "관련 발언 없음" sentinel 트리거 0회 |
| 5 | 긴 답변 + validator 통과 LLM 응답 (Trigger 3 회귀) | audio fixture + `answerText` 정상 | validator 위배 0건 |

audio fixture 위치: `backend/src/test/resources/fixtures/audio/not-evaluable/` (신규).

| fixture | 출처 | 포맷 / 길이 | 크기 제약 |
|---------|------|-----------|----------|
| `long-korean.webm` (시나리오 1, 2, 4, 5) | 작성자 자체 녹음 또는 기존 `backend/src/test/resources/fixtures/audio/` 재활용 | webm/opus, 30초+, 한국어 | ≤10MB (`AudioTurnAnalyzer.MAX_AUDIO_BYTES`) |
| `silent-5s.webm` (시나리오 3) | `ffmpeg -f lavfi -i anullsrc=r=48000:cl=mono -t 5 -c:a libopus silent-5s.webm` 생성 | webm/opus, 5초 무음 | ≤10KB |

PII 청결: 자체 녹음 한정. 외부 음성 / 실 사용자 데이터 사용 금지.

### Service Integration

위치: `backend/src/test/java/com/rehearse/api/domain/feedback/rubric/service/RubricScorerTest.java` (확장).

- `isBlankAnswer` 신규 분기:
  - (a) `userAnswer = ""` + `analysis.transcript = "..." (길이 > 3)` → blank=false → 정상 채점 호출
  - (b) `userAnswer = ""` + `analysis.transcript = ""` → blank=true → 전 차원 NOT_EVALUABLE
  - (c) `userAnswer = "정상 답변..."` + `analysis.transcript = ""` → blank=false (userAnswer 우선)
  - (d) `userAnswer = "잘"` (≤3자) + `analysis.transcript = "긴 텍스트..."` → blank=false (transcript fallback)
  - (e) **회귀 가드** — `userAnswer = "안녕"` (≤3자) + `analysis = AnswerAnalysis.empty()` (transcript="") → blank=true → 전 차원 NOT_EVALUABLE (기존 `RubricScorerTest` "잘몰라" / 공백 케이스 + opener 짧은 답변 회귀 가드)

### Domain Unit

위치: `backend/src/test/java/com/rehearse/api/domain/interview/entity/AnswerAnalysisTest.java` (확장).

- `AnswerAnalysis.empty().transcript() == ""`
- `new AnswerAnalysis(null, ...)` → transcript null 허용 확인 (legacy 호환)

### 빌드 / 회귀

- [ ] `./gradlew build` 통과 (기존 `new AnswerAnalysis(...)` 생성자 호출하는 테스트 6+ 곳 마이그레이션 후)
- [ ] `RubricScoringEventListenerTest` 통과 (opener `AnswerAnalysis.empty()` 케이스 회귀)
- [ ] `FollowUpServiceIntegrationTest` 통과 (transcript 추가 후 이벤트 페이로드 회귀)
- [ ] MockAiClient 사용 통합 테스트 (Service Integration / Infra Integration) — JSON 응답 fixture 에 `transcript` 필드 추가 후 strict schema (text 경로) 통과
- [ ] PR #527 (NOT_EVALUABLE dimension state) 회귀 영향 없음

### 관찰

- `RubricScorer` NOT_EVALUABLE 적용 시 기존 INFO 로그에 **`transcript_len={}` 필드 추가** — transcript fallback 효과 측정용. 예: `log.info("[RubricScorer] 무응답 감지 interviewId={} userAnswerLen={} transcript_len={}", ...)`.
- 운영 EC2 docker log: 본 변경 후 "응답 길이 3자 이하" 사유 빈도 감소 추세 확인 (사용자 직접 확인, 정량 임계 미설정).

## Pre / Post State

### Pre (현재)

```java
// RubricScorer.java
private boolean isBlankAnswer(String userAnswer) {
    return userAnswer == null || userAnswer.strip().length() <= BLANK_ANSWER_LENGTH_THRESHOLD;
}
// 호출부 (line 32-53): isBlankAnswer(userAnswer) — analysis 미사용

// AnswerAnalysis.java
public record AnswerAnalysis(
    List<AnswerClaim> claims,
    Map<String, Integer> dimensionGaps,
    String weakestDimension,
    List<String> unstatedAssumptions,
    RecommendedNextAction recommendedNextAction
)

// GeneratedAnswerAnalysisSchema.required
List.of("claims", "dimension_gaps", "weakest_dimension", "unstated_assumptions", "recommended_next_action")
```

증상: 정상 음성 답변 turn (FE `answerText=""`) → blank=true → 전 차원 NOT_EVALUABLE 오판정.

### Post (구현 후)

```java
// RubricScorer.java
private boolean isBlankAnswer(String userAnswer, AnswerAnalysis analysis) {
    String effective = effectiveText(userAnswer, analysis);
    return effective == null || effective.strip().length() <= BLANK_ANSWER_LENGTH_THRESHOLD;
}
private static String effectiveText(String userAnswer, AnswerAnalysis analysis) {
    if (userAnswer != null && userAnswer.strip().length() > BLANK_ANSWER_LENGTH_THRESHOLD) {
        return userAnswer;
    }
    return analysis != null ? analysis.transcript() : null;
}
// 호출부: isBlankAnswer(userAnswer, analysis)

// AnswerAnalysis.java — transcript 필드 추가 (위 Data Model 참조)

// GeneratedAnswerAnalysisSchema.required
List.of("transcript", "claims", "dimension_gaps", "weakest_dimension", "unstated_assumptions", "recommended_next_action")
```

증상: 정상 음성 답변 turn → analysis.transcript fallback → blank=false → 정상 차원별 채점. 진짜 무응답 (audio 무음 + 텍스트 임계 미만) → transcript 도 짧음 → blank=true 유지.

## 위험 / 마이그레이션 / 롤백

### 위험

| 위험 | 가능성 | 영향 | 완화 |
|------|-------|------|------|
| (audio chat 경로) LLM 가 `answer_analysis.transcript` 프롬프트 지시 무시 → 빈 반환 | 중간 (strict schema 미적용 = JSON_OBJECT 모드 → 프롬프트 준수도 의존) | Trigger 1 오판정 잔존 | Live E2E 시나리오 (1) = 1차 신뢰 근거. 실패 시 retry / audio_chat 응답 후처리 transcribe API fallback 별도 PR |
| (text 경로) strict schema 에 transcript 추가 → 기존 LLM mock / fixture 호환 깨짐 | 확실 | 통합 테스트 적색 | MockAiClient / WireMock JSON 응답 fixture 일괄 transcript 필드 추가 (구현 단계) |
| transcript 토큰 비용 ↑ (~300-500/turn, **추정 — 평균 답변 200자 한국어 ≈ 300-400 토큰 가정**) | 확실 | 운영 비용 ~5-8% 추정 ↑ | 본 작업 trade-off 명시 수용. 정량 측정은 운영 후 별도 모니터 |
| 기존 코드 다수 `new AnswerAnalysis(...)` 생성자 호출 → 컴파일 실패 | 확실 | 빌드 실패 | 구현 시 호출 6+ 곳 일괄 마이그레이션 (`RubricScoringEventListenerTest`, `RubricScorerTest`, `FollowUpServiceIntegrationTest`, `AnswerAnalysisTest`, `GeneratedAnswerAnalysis.toDomain`, `GeneratedTurnAnalysis.toDomain` 호출 사이트) |
| opener / playground 짧은 userAnswer + `AnswerAnalysis.empty()` 케이스 회귀 | 낮음 (canonical constructor 가 transcript="" 정규화) | NOT_EVALUABLE 유지 (의도된 동작) | Verification Service Integration (e) 시나리오 + `RubricScoringEventListenerTest` 회귀 가드 |

### 마이그레이션

- **DB DDL 0개** (Flyway 변경 없음).
- **런타임 호환성**: AnswerAnalysis = in-memory payload 만. 배포 시점 진행 중 turn 영향 없음 (음성 분석 → 채점은 단일 트랜잭션 내 동일 인스턴스 처리).
- **LLM schema 호환**: strict mode 라 schema 변경 즉시 적용. 기존 LLM 응답 캐시 / persist 없음.

### 롤백

- 코드 revert 단독으로 복구 가능 (DB / schema / 외부 의존 변경 없음).
- 롤백 절차: PR revert → 재배포. 기존 RubricScorer behavior 복귀.
- 부분 롤백: AnswerAnalysis.transcript 필드 유지하고 RubricScorer.isBlankAnswer 시그니처만 revert 가능 (transcript 는 미사용 잔여 필드).

## 분기 결정

- [x] **단일 영역 (BE) → `implement.md` 1개**
- [ ] BE+FE 동시
- [ ] BE 선행 강제

FE 변경 없음 (Non-Goals). lambda 변경 없음. BE 단독 구현 + 테스트 + Live E2E 검증.
