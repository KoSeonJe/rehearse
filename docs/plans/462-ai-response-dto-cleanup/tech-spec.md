# Tech Spec — LLM 응답 DTO 위치/네이밍/검증 통일

> **작성자**: backend agent
> **답하는 질문**: 어떻게? 위치 / 네이밍 / 검증 어노테이션 적용 / 도메인 변환 분리
> **갱신 사유**: 이슈(#462) 정렬 단순화. 영향도(HIGH/LOW) 분기 / parseOrRetry SRP 분리 / cause 분기 / 신규 도메인 record 도입 등 이슈 외 정책 결정 모두 제거.
> **승인 게이트**: ★ 본 spec 사용자 명시 승인 후 implement-be.md 진입 ★

---

## Why → Goal (1줄 미러)

LLM 응답 매핑 클래스 10종 = 위치(`infra/ai/dto/`) / 네이밍(`Generated*`) / 검증(record compact constructor) 통일 + 도메인 로직 보유 클래스는 도메인 객체로 분리 + `Generated*.toDomain()` 변환 → 도메인-인프라 경계 회복.

## 비스코프 (Issue 명문)

- prompt 자체 / `parseOrRetry` 재시도 정책 변경 X
- LLM 모델 / 토큰 정책 변경 X
- AI Client (`OpenAiClient` / `ClaudeApiClient` / `ResilientAiClient`) 시그니처 변경 X
- 영향도(HIGH/LOW) 차등 fallback 정책 X — 모든 케이스 기존 동작(`parseOrRetry` BusinessException throw) 유지
- `parseOrRetry` SRP 분리 / cause 분기 신규 도입 X — Jackson `ValueInstantiationException` (extends `JsonProcessingException`) wrap 으로 기존 catch 가 자동 흡수
- 도메인 entity 의 `models/entity/` 위치 마이그레이션 X — 본 작업은 DTO 분리만

### Issue 본문 위치이동 표 vs 본 spec 분기 (명시)

Issue 본문 "위치 이동" 표는 5종(`AnswerAnalysis` / `TurnAnalysisResult` / `RubricScoringResult` / `SessionFeedbackPayload` / `CompactionSummaryResult`) 모두 `infra/ai/dto/` 이동 명시. 본 spec 은 도메인 메서드 보유 3종(`AnswerAnalysis` / `TurnAnalysisResult` / `RubricScoringResult`)을 **도메인 위치 유지 + Generated\* 신규 추가**로 분기.

**사유**: issue 본문의 "변환 정책" 절 ("도메인 로직 필요 시 → 해당 DTO 에 toEntity()/toDomain() 메서드 추가 → 도메인 entity/VO 변환 후 비즈니스 로직 사용") 과 정합. 위치 이동 + 도메인 메서드 동거 = `Generated*` 의 raw 정책 자체 위배. 두 절 사이 충돌은 "변환 정책" 우선 해석.

---

## Evidence

### 현재 구조 (실측)

- **infra/ai/dto/ (5종)**: `ExtractedResumeSkeleton`, `GeneratedFollowUp`, `GeneratedQuestion`, `GeneratedQuestionsWrapper`, `GeneratedInterviewPlan`.
- **domain/interview/entity/ (2종)**: `AnswerAnalysis`, `TurnAnalysisResult`.
- **domain/feedback/rubric/entity/ (1종)**: `RubricScoringResult`.
- **domain/feedback/session/dto/ (1종)**: `SessionFeedbackPayload`.
- **infra/ai/context/compaction/ (1종)**: `CompactionSummaryResult`.

### 도메인 메서드 보유 분류

| 클래스 | 도메인 메서드 | 분리 필요 |
|---|---|---|
| `AnswerAnalysis` | `applyL1FalseNegativeGuard` / `withRecommendedNextAction` / `withTurnId` / `empty` + `implements TurnAnalysis` | **YES** |
| `TurnAnalysisResult` | `withAnswerAnalysis` (+ `fromJson` fallback) | **YES** |
| `RubricScoringResult` | `empty` / `isEmpty` | **YES** |
| `SessionFeedbackPayload` | 없음 (nested record 5종) | NO — `Generated*` 단일 |
| `CompactionSummaryResult` | `toCompactString()` (단순 포맷터, infra-side) | NO — `Generated*` 단일 (메서드 동거) |
| `ExtractedResumeSkeleton` | `toDomain()` (이미 분리됨) | (기 분리) |
| `GeneratedFollowUp` / `GeneratedQuestion` / `GeneratedQuestionsWrapper` / `GeneratedInterviewPlan` | 없음 | NO |

### 현재 검증 / 흐름

- 매핑 시점 검증 적용 1/10 (`AnswerAnalysis` compact constructor 만).
- LLM 재시도 진입점: `AiResponseParser.parseOrRetry` (`backend/src/main/java/com/rehearse/api/infra/ai/AiResponseParser.java:54-73`). `JsonProcessingException` catch → schema hint 1회 재호출 → 2차 실패 시 `BusinessException(AI_PARSE_FAILED)`.
- compact constructor 의 `IllegalArgumentException` → Jackson `ValueInstantiationException` (extends `JsonProcessingException`) wrap → 기존 `parseOrRetry` catch 가 자동 흡수. **코드 변경 불필요.**
- 호출부 약 33+ (import 변경 대상).

---

## Architecture

### 디렉토리 / 클래스 구조 (Post)

```
infra/ai/dto/
├── GeneratedAnswerAnalysis.java          [신규] (Jackson + 검증 + toDomain)
├── GeneratedTurnAnalysis.java            [신규] (Jackson + 검증 + toDomain)
├── GeneratedRubricScoring.java           [신규] (Jackson + 검증 + toDomain)
├── GeneratedSessionFeedback.java         [이동+리네이밍] (검증)
├── GeneratedCompactionSummary.java       [이동+리네이밍] (검증 + toCompactString 동거)
├── GeneratedResumeSkeleton.java          [리네이밍] (ExtractedResumeSkeleton → Generated*. 검증 추가)
├── GeneratedFollowUp.java                [현행 유지 + 검증 추가]
├── GeneratedQuestion.java                [현행 유지 + 검증 추가]
├── GeneratedQuestionsWrapper.java        [현행 유지 + 검증 추가]
└── GeneratedInterviewPlan.java           [현행 유지 + 검증 추가]

domain/interview/entity/                  [위치 유지]
├── AnswerAnalysis.java                   [수정] (Jackson 어노테이션/compact constructor 제거. 도메인 메서드만)
└── TurnAnalysisResult.java               [수정] (Jackson 어노테이션/fromJson 제거. withAnswerAnalysis 만)

domain/feedback/rubric/entity/            [위치 유지]
└── RubricScoringResult.java              [수정 없음] (Jackson 사용 안 함, 변경 불필요)
```

### 정책 요약

- **`Generated*` (infra/ai/dto/)**: Jackson 어노테이션 + record + compact constructor 검증. 매핑 시점 거절. 도메인 메서드 0.
- **도메인 객체 (domain/{feat}/entity/)**: Jackson 0. 도메인 메서드만. 위치 유지.
- **`Generated*.toDomain()`**: 도메인 객체로 변환. application service / domain service 가 호출.
- **단순 데이터 클래스** (`SessionFeedbackPayload` / `CompactionSummaryResult` / `ExtractedResumeSkeleton`): 도메인 객체 분리 없음. `Generated*` 단독. 단 `Generated*` 가 raw 데이터 유지 + 단순 helper(`toCompactString`) 동거 허용 (infra-side 포맷터 한정).

### 시퀀스 (Post)

```
[AiClient.chat()] → ChatResponse(raw text)
   ↓
[AiResponseParser.parseOrRetry(raw, GeneratedXxx.class, ...)]
   ├─ readValue → record compact constructor 검증
   │     검증 실패 → IllegalArgumentException → Jackson wrap (ValueInstantiationException)
   ├─ catch (JsonProcessingException) — wrap 포함 자동 흡수 [기존 동작]
   │     └─ schema retry 1회 → 2차 실패 시 BusinessException(AI_PARSE_FAILED)
   ↓ GeneratedXxx (검증 통과)
[Application/Domain Service]
   ├─ 도메인 객체 필요 시 → GeneratedXxx.toDomain() → 도메인 객체
   └─ 단순 데이터 사용 시 → GeneratedXxx 직접 (예: SessionFeedback 호출부)
   ↓
[비즈 로직 / Repository / Event 페이로드]
```

### 검증 표기 패턴 예시 (`GeneratedAnswerAnalysis`)

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedAnswerAnalysis(
        @JsonProperty("turn_id") long turnId,
        @JsonProperty("claims") List<Claim> claims,
        @JsonProperty("missing_perspectives") List<Perspective> missingPerspectives,
        @JsonProperty("unstated_assumptions") List<String> unstatedAssumptions,
        @JsonProperty("answer_quality") int answerQuality,
        @JsonProperty("recommended_next_action") RecommendedNextAction recommendedNextAction
) {
    public GeneratedAnswerAnalysis {
        if (answerQuality < 1 || answerQuality > 5) {
            throw new IllegalArgumentException(
                    "GeneratedAnswerAnalysis.answerQuality 는 1~5 범위여야 합니다: " + answerQuality);
        }
        if (recommendedNextAction == null) {
            throw new IllegalArgumentException(
                    "GeneratedAnswerAnalysis.recommendedNextAction 는 null 일 수 없습니다.");
        }
        claims = claims != null ? List.copyOf(claims) : List.of();
        missingPerspectives = missingPerspectives != null ? List.copyOf(missingPerspectives) : List.of();
        unstatedAssumptions = unstatedAssumptions != null ? List.copyOf(unstatedAssumptions) : List.of();
    }

    public AnswerAnalysis toDomain() {
        return new AnswerAnalysis(turnId, claims, missingPerspectives, unstatedAssumptions,
                answerQuality, recommendedNextAction);
    }
}
```

### 도메인 record 패턴 예시 (`AnswerAnalysis`, 변경본)

```java
package com.rehearse.api.domain.interview.entity;

import java.util.List;

public record AnswerAnalysis(
        long turnId,
        List<Claim> claims,
        List<Perspective> missingPerspectives,
        List<String> unstatedAssumptions,
        int answerQuality,
        RecommendedNextAction recommendedNextAction
) implements TurnAnalysis {

    public AnswerAnalysis withRecommendedNextAction(RecommendedNextAction newAction) { ... }
    public AnswerAnalysis withTurnId(long newTurnId) { ... }
    public static AnswerAnalysis empty(long turnId) { ... }
    public AnswerAnalysis applyL1FalseNegativeGuard() { ... }
}
```

→ Jackson 어노테이션 / compact constructor 제거. 검증은 `GeneratedAnswerAnalysis` 책임.

---

## 10종 record 검증 룰 표

| Generated 클래스 | 필수 필드 | range / enum | null → empty | 도메인 객체 분리 |
|---|---|---|---|---|
| `GeneratedAnswerAnalysis` | `recommendedNextAction` | `answerQuality` 1-5 | claims / missing / assumptions null → `List.of()` | `AnswerAnalysis` |
| `GeneratedTurnAnalysis` | — (`answerAnalysis null` 통과, `toDomain()` 시 `AnswerAnalysis.empty(0L)`) | — | `answerText` null → `""` | `TurnAnalysisResult` |
| `GeneratedFollowUp` | `skip == false` 시 `question` non-blank (skip 분기 보존) | — (type 등 enum 검증 X — 기존 동작 유지) | — | — |
| `GeneratedQuestion` | `content` non-blank, `questionCategory` non-blank | — | — | — |
| `GeneratedQuestionsWrapper` | `questions` non-empty | — | questions null → 거절 | — |
| `GeneratedInterviewPlan` | `projectPlans` non-empty, `sessionPlanId` non-blank | — | nested record list null → `List.of()` | — |
| `GeneratedResumeSkeleton` | `projects` non-null | — | nested null → empty | (기 `toDomain()` 패턴) |
| `GeneratedCompactionSummary` | — (모든 필드 optional list) | — | list null → `List.of()` | — (`toCompactString()` 동거) |
| `GeneratedRubricScoring` | — (`dimensionScores` non-empty 검증 **X** — adapter fallback 보존) | 각 score 1-3 (실 SCORE_MIN/MAX) | `dimensionScores` null → `Map.of()`, `scoredDimensions` null → `List.of()` | `RubricScoringResult` |
| `GeneratedSessionFeedback` | nested 5종 non-null | — | nested list null → empty | — |

> `GeneratedSessionFeedback` 의 cardinality / abstract phrase / cross-category 도메인 룰 = `SessionFeedbackParser` 잔존 (별도 책임).
>
> **GeneratedRubricScoring 검증 완화 사유** — `RubricScoringAdapter.buildFallbackScore` 가 LLM 빈 `dimensionScores` 응답을 `notApplicable` 맵으로 fallback 처리 (운영 정책). compact constructor 에서 non-empty 강제 시 검증 거절 → Jackson wrap → `parseOrRetry` schema retry → 2차 실패 시 `BusinessException(AI_PARSE_FAILED)` = **기존 fallback 우회 = 행위 변경 = 비스코프 침범**. 따라서 `dimensionScores` non-empty 는 검증 룰 제외, score range (1-3) 만 검증 적용. fallback 정책은 adapter 책임 그대로.
>
> **GeneratedFollowUp 검증 완화 사유** — `skip=true` 케이스는 `question` 비어도 정상 흐름 (질문 생략 분기). compact constructor 에서 무조건 question non-blank 강제 시 skip 응답 거절 = 행위 변경. `skip=false` 일 때만 question non-blank 검증.
>
> **GeneratedTurnAnalysis silent fallback 보존 사유** — 이전 `TurnAnalysisResult.fromJson` (`@JsonCreator`) 가 LLM `answer_analysis` 누락 응답 시 `AnswerAnalysis.empty(0L)` 로 silent 진행하던 동작을 `Generated*` 분리 후에도 보존. compact constructor 에서 `answerAnalysis null` 거절 시 BusinessException → 인터뷰 흐름 차단 = 행위 변경 = 비스코프 침범. 따라서 `answerAnalysis null` 통과 + `toDomain()` 시점에 `AnswerAnalysis.empty(0L)` 로 fallback (turnId 은 `AudioTurnAnalyzer.commit` 의 `withTurnId` 가 덮어씀).

### class → record 전환 영향 (실측)

`GeneratedFollowUp` / `GeneratedQuestion` / `GeneratedQuestionsWrapper` 는 현재 **class** (Lombok `@Getter` + `@NoArgsConstructor` 등) 상태. record 전환 시:

- 호출부 getter accessor 변경: `getQuestion()` → `question()` 등 (컴파일러 검출, 약 41+ 라인). 단순 sed 치환 가능.
- 가변 필드 (예: `withAnswerText` mutator) 가 record incompatible 일 경우 = with-style 헬퍼 record 메서드로 재정의.
- `GeneratedInterviewPlan` 은 이미 record (sessionPlanId / totalProjects / projectPlans + nested records) → 검증 추가만.

---

## Trade-offs

### 도메인 객체 분리 vs `Generated*` 단일

#### 채택: 도메인 메서드 보유 클래스만 분리 (3종)

- `AnswerAnalysis` / `TurnAnalysisResult` / `RubricScoringResult` 만 도메인 객체 유지.
- 그 외 7종 = `Generated*` 단독 (helper 메서드 동거 허용 — `toCompactString` 등 infra 포맷터 한정).

**장점**: 이슈 명문 정합 ("도메인 로직 필요 시 → toDomain()"). 도메인 객체 신규 record 추가 0 (기존 클래스 위치/이름 유지, Jackson 만 제거).
**단점**: 단순 데이터 클래스 (`SessionFeedbackPayload` / `CompactionSummaryResult`) 는 호출부가 `Generated*` 직접 사용 → ArchUnit 화이트리스트 필요.

#### 폐기: 모든 클래스 도메인 record 신규 생성

폐기 사유: 이슈 비스코프 침범. simplicity.md 위반. SessionFeedback / CompactionSummary 는 도메인 로직 0 → 분리 가치 0.

---

## Data Model

DB 스키마 변경 **없음**.

## API Contract

외부 API 변경 **없음**. BE 내부 리팩토링.

---

## Non-Functional 11개 (요약)

| NF | 영향 | 근거 |
|---|---|---|
| 영향 범위 | BE only | API / DB 무변. import / 클래스명 변경 |
| 데이터 정합성 | 향상 | 매핑 시점 거절 → silent 결함 차단 |
| 실시간성 | 변화 없음 | LLM call 자체 변경 없음 |
| 부하 / 처리량 | 미세 증가 (≪1ms/req — 추정) | record 검증 = 단순 분기 + List.copyOf. LLM 응답 매핑은 사용자 요청당 1-3회. 마이크로초 단위 |
| 동시성 | 영향 없음 | 모든 record 불변 |
| 확장성 | 향상 | 신규 LLM 응답 타입 = 위치/네이밍/검증 패턴 강제 |
| 마이그레이션 | DB 무관 | 코드 리팩토링 1회 |
| 외부 의존 | 무변 | AI Client 시그니처 미수정 |
| 보안 | 향상 | LLM 응답 신뢰 경계 검증 (OWASP A03) |
| 관찰성 | 변화 없음 | 메트릭 / 로그 변경 없음 (비스코프) |
| 롤백 | 단순 | PR revert. feature flag 불필요 |

---

## Verification (완료 판정)

### Domain Unit (Generated* 검증)

- [ ] `GeneratedAnswerAnalysis` — 정상 / `answerQuality < 1` 또는 `> 5` 거절 / `recommendedNextAction == null` 거절 / null list → empty / `toDomain()` 등가성
- [ ] `GeneratedTurnAnalysis` — 정상 / `answerAnalysis null` 통과 / `toDomain()` 시 `answerAnalysis null` → `AnswerAnalysis.empty(0L)` silent fallback
- [ ] `GeneratedRubricScoring` — 정상 / `dimensionScores` 빈 거절 / 각 `score` range 거절 / `toDomain()`
- [ ] `GeneratedSessionFeedback` — 정상 / nested 5종 누락 거절 (cardinality 는 parser 잔존)
- [ ] `GeneratedCompactionSummary` — 정상 / null list → empty / `toCompactString()` 호환
- [ ] `GeneratedResumeSkeleton` — 정상 / nested record / `toDomain()` 호환
- [ ] `GeneratedFollowUp` — 정상 / `skip=true` + `question=null` 통과 / `skip=false` + `question=blank` 거절
- [ ] `GeneratedQuestion` — 정상 / `content blank` 거절 / `questionCategory blank` 거절
- [ ] `GeneratedQuestionsWrapper` — 정상 / `questions null/empty` 거절
- [ ] `GeneratedInterviewPlan` — 정상 / `projectPlans empty` 거절 / `sessionPlanId blank` 거절 / nested record null list → empty

### Domain Unit (도메인 객체)

- [ ] `AnswerAnalysis` 기존 테스트 통과 (`applyL1FalseNegativeGuard / with* / empty`)
- [ ] `TurnAnalysisResult` 기존 테스트 통과 (`withAnswerAnalysis`)
- [ ] `RubricScoringResult` 기존 테스트 통과 (`empty / isEmpty`)

### Service Integration (회귀)

- [ ] 기존 통과 테스트 그대로: `AnswerAnalyzerTest` / `AudioTurnAnalyzerTest` / `TextFallbackTurnAnalyzerTest` / `TurnAnalysisPipelineTest` / `FollowUpQuestionGeneratorTest` / `QuestionGeneratorTest` / `InterviewPlanGeneratorTest` / `ResumeSkeletonExtractorTest` / `RubricScorerTest` / `RubricScoringEventListenerTest` / `SessionFeedbackParserTest` / `DialogueCompactorTest`

### Infra Integration

- [ ] `SchemaExampleRegistryTest` — Generated* 신규 키 등록 / 매핑 갱신
- [ ] `AiResponseParserTest` **신규 케이스**: compact constructor `IllegalArgumentException` 거절 → Jackson `ValueInstantiationException` wrap → 기존 `JsonProcessingException` catch 자동 흡수 → schema retry 발동 → 2차 응답 정상 시 통과 / 2차 실패 시 `BusinessException(AI_PARSE_FAILED)` 전파. (본 가정이 spec 의 "코드 변경 0" 전제 근거 — 가정 검증 필수)
- [ ] `RubricScoringAdapterTest` **회귀 케이스**: 기존 LLM 응답 fixture (`backend/src/test/resources/.../rubric-scoring-*.json` 등) 1건 그대로 `GeneratedRubricScoring` record 매핑 통과 + `toDomain()` 결과가 기존 raw map 매핑 결과와 동등 (`dimensionScores` Map 키/값 / `scoredDimensions` 순서 / `levelFlag` 포함). 빈 `dimensionScores` 응답 = `buildFallbackScore` 진입 정상 동작 (검증 거절 X, fallback 정책 그대로)

### ArchUnit (구조 룰)

```java
// backend/src/test/java/com/rehearse/api/architecture/AiDtoArchitectureTest.java

@Test
void domain_layer_must_not_depend_on_infra_ai_dto_except_service_packages() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .and().resideOutsideOfPackages("..domain..service..", "..domain..models.service..")
        .should().dependOnClassesThat().resideInAPackage("..infra.ai.dto..")
        .check(importedClasses);
}
```

- 화이트리스트: `..domain..service..` (애플리케이션 서비스) + `..domain..models.service..` (도메인 서비스 / port). `Generated*` import 후 `toDomain()` 변환 책임.
- 블랙리스트: `..domain..(controller|repository|event|dto|exception|entity|vo)..` 가 `infra.ai.dto` import 시 fail.

### 빌드 / 통합

- [ ] `./gradlew compileJava` 통과
- [ ] `./gradlew test` 전체 통과
- [ ] `./gradlew check` (ArchUnit 포함) 통과

---

## Pre / Post State

### Pre (현재)

- 매핑 검증: 1/10 (`AnswerAnalysis` 만)
- 위치: 5개 `infra/ai/dto/`, 5개 도메인/혼재
- 네이밍: `Generated*` 4개 / `Extracted*` 1개 / Result 접미사 3개 / Payload 1개 / Summary 1개
- 도메인 비즈 로직이 LLM raw 매핑(`AnswerAnalysis` 등) 직접 import 가능 (ArchUnit 룰 부재)
- `parseOrRetry` 동작: `JsonProcessingException` catch → schema retry → 2차 실패 시 `BusinessException(AI_PARSE_FAILED)`

### Post (구현 후)

- 매핑 검증: 10/10 (Generated* compact constructor)
- 위치: 10개 모두 `infra/ai/dto/`
- 네이밍: `Generated*` 통일 (10개)
- 도메인 객체 분리: 3종 (`AnswerAnalysis` / `TurnAnalysisResult` / `RubricScoringResult`) — Jackson 제거, 도메인 메서드만 보유, 위치 유지
- ArchUnit 룰: 도메인 비즈 영역 → `infra.ai.dto` 의존 차단 (application/domain service 화이트리스트)
- `parseOrRetry` 동작: **변경 없음** (기존 catch 가 Jackson wrap 검증 실패 자동 흡수)

### Diff 요약 (단일 PR)

- **추가 (신규 파일)**:
  - `infra/ai/dto/GeneratedAnswerAnalysis.java`
  - `infra/ai/dto/GeneratedTurnAnalysis.java`
  - `infra/ai/dto/GeneratedRubricScoring.java`
  - `infra/ai/dto/GeneratedSessionFeedback.java`
  - `infra/ai/dto/GeneratedCompactionSummary.java`
  - `test/.../architecture/AiDtoArchitectureTest.java`
- **변경 (수정)**:
  - `infra/ai/dto/ExtractedResumeSkeleton.java` → `GeneratedResumeSkeleton.java` (리네이밍 + 검증)
  - `infra/ai/dto/GeneratedFollowUp.java` / `GeneratedQuestion.java` / `GeneratedQuestionsWrapper.java` / `GeneratedInterviewPlan.java` (검증 추가)
  - `domain/interview/entity/AnswerAnalysis.java` (Jackson + compact constructor 제거. 도메인 메서드만)
  - `domain/interview/entity/TurnAnalysisResult.java` (Jackson + `fromJson` 제거. `withAnswerAnalysis` 만)
  - 호출부 33+ (import / 타입 갱신, `Generated*.toDomain()` 호출 도입)
  - `infra/ai/SchemaExampleRegistry` (Generated* 키 매핑)
  - `infra/ai/RubricScoringAdapter` — 기존 raw map 파싱 → `GeneratedRubricScoring` record 매핑 + `toDomain()` 호출
- **삭제**:
  - `infra/ai/context/compaction/CompactionSummaryResult.java` (→ `GeneratedCompactionSummary`)
  - `domain/feedback/session/dto/SessionFeedbackPayload.java` (→ `GeneratedSessionFeedback`)

### 호출부 영향 (단일 PR)

- `AnswerAnalysis` 17 — 위치 동일, 사용 방식 = service 가 `GeneratedAnswerAnalysis.toDomain()` 호출 후 도메인 메서드 사용
- `TurnAnalysisResult` 4 — 동상
- `RubricScoringResult` 5 — 동상
- `SessionFeedbackPayload` 6 → `GeneratedSessionFeedback` (직접 사용. 도메인 객체 분리 없음)
- `CompactionSummaryResult` 1 → `GeneratedCompactionSummary` (직접 사용)
- `ExtractedResumeSkeleton` ≈ 3 → `GeneratedResumeSkeleton` (기존 `toDomain()` 패턴 유지)

---

## 위험 / 마이그레이션 / 롤백

### 위험

1. **호출부 import 누락** (낮음): 컴파일러 검출.
2. **`AnswerAnalysis` 위치 유지 + Jackson 제거**: 기존 위치(`domain/interview/entity/`) 그대로. 호출부 import 변경 0 (단, 이전 Jackson `@JsonCreator` 통한 직접 deserialize 호출 지점 있는지 grep 필요 — 구현 시 확인). LLM raw → service 진입점은 `GeneratedAnswerAnalysis` 로 강제 전환.
3. **`RubricScoringAdapter` raw map 매핑 전환**: 기존 `Map<String, DimensionScore>` 직접 매핑 → `GeneratedRubricScoring` record 매핑 + `toDomain()`. 기존 `RubricScoringAdapterTest` 회귀 통과로 검증.
4. **`SessionFeedbackParser` 도메인 룰**: `Generated*` = JSON shape, parser = cardinality / abstract phrase / cross-category 잔존. 중복 0.

### 마이그레이션

- DB 무변. 코드 리팩토링 1회. zero-downtime.
- 단일 PR. 호출부 33+ 컴파일러 흡수.

### 롤백

- PR revert 1회. feature flag 불필요. 호환성 단방향 이슈 없음 (외부 API / DB schema 무변).

---

## 분기 결정

- [x] **단일 영역 (BE only)** → `implement-be.md` 1개 + 단일 PR.
- [ ] BE+FE 동시
- [ ] BE 선행 강제

→ Frontend 영향 0.

---

## 사용자 결정 필요 항목

**잔존 결정 = 0.**

이슈(#462) 명문 그대로 정렬. 영향도 분기 / SRP 메서드 분리 / 신규 도메인 record 도입 등 이슈 외 정책 모두 제거.
