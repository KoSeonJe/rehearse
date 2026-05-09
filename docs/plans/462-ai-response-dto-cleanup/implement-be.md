# Implement (Backend) — LLM 응답 DTO 위치/네이밍/검증 통일

> **작성자**: main session
> **답하는 질문**: BE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md 사용자 명시 승인 후 시작 ★

---

## Phase 0: 사전 확인

- [ ] tech-spec.md 사용자 명시 승인 완료
- [ ] API contract 변경 없음 (BE 내부 리팩토링)
- [ ] DB schema 변경 없음
- [ ] 단일 PR 결정 확정 (C2)

---

## Phase / Step 개요

| Phase | 제목 | 구현 | PR | 의존 |
|-------|------|------|----|------|
| 1 | `Generated*` record 신규 / 리네이밍 (10종) | `backend` | 단일 PR | Phase 0 |
| 2 | 도메인 객체 정리 (3종 — Jackson 제거) | `backend` | 단일 PR | Phase 1 |
| 3 | 호출부 import / 타입 갱신 (33+) | `backend` | 단일 PR | Phase 2 |
| 4 | `RubricScoringAdapter` record 매핑 전환 | `backend` | 단일 PR | Phase 1 |
| 5 | `SchemaExampleRegistry` Generated\* 키 매핑 | `backend` | 단일 PR | Phase 1 |
| 6 | ArchUnit 룰 추가 + 테스트 작성 | `backend` | 단일 PR | Phase 1-5 |
| 7 | 빌드 / 전체 테스트 통과 확인 | `backend` | 단일 PR | Phase 6 |

> 단일 PR 이지만 Phase 별 commit 분리 (커밋 단위 = 논리 1개, `.claude/rules/commit.md` 정합).

---

## Phase 1: `Generated*` record 신규 / 리네이밍

- **구현**: `backend` — `infra/ai/dto/` 10종 통일

### 변경 파일

**신규 (5)**:
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedAnswerAnalysis.java`
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedTurnAnalysis.java`
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedRubricScoring.java`
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedSessionFeedback.java`
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedCompactionSummary.java`

**리네이밍 (1)**:
- `infra/ai/dto/ExtractedResumeSkeleton.java` → `GeneratedResumeSkeleton.java`

**검증 추가 (4 — 위치 / 이름 유지)**:
- `infra/ai/dto/GeneratedFollowUp.java` — `question` non-blank / `intent` enum 유효
- `infra/ai/dto/GeneratedQuestion.java` — `question` non-blank / `topic` non-blank
- `infra/ai/dto/GeneratedQuestionsWrapper.java` — `questions` non-empty
- `infra/ai/dto/GeneratedInterviewPlan.java` — `phases` non-empty / `totalQuestions` ≥ 1

**삭제** (Phase 3 호출부 갱신 후 제거):
- `infra/ai/context/compaction/CompactionSummaryResult.java`
- `domain/feedback/session/dto/SessionFeedbackPayload.java`

### 핵심 로직

각 `Generated*` 패턴 (tech-spec.md `검증 표기 패턴 예시` 참조):

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedXxx(
        @JsonProperty("...") ...
) {
    public GeneratedXxx {
        // 필수 필드 / range / enum 검증 → IllegalArgumentException
        // List null → List.of() (List.copyOf 불변)
    }

    public DomainXxx toDomain() { ... }   // 도메인 분리 3종만
}
```

검증 룰 = tech-spec.md `10종 record 검증 룰 표` 그대로.

### Verification

- [ ] `./gradlew test --tests "com.rehearse.api.infra.ai.dto.*"` 통과
- [ ] 10종 각각 Domain Unit 테스트 (정상 / 거절 / null → empty / `toDomain()` 등가성)

### 커밋 메시지

```
feat(BE): infra/ai/dto/ Generated* record 10종 통일 + 매핑 시점 검증
```

---

## Phase 2: 도메인 객체 정리 (Jackson 제거)

- **구현**: `backend` — 3종 (`AnswerAnalysis` / `TurnAnalysisResult` / `RubricScoringResult`)

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/interview/entity/AnswerAnalysis.java`
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/TurnAnalysisResult.java`
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/entity/RubricScoringResult.java` (Jackson 미사용 → 사실상 변경 없음)

### 핵심 로직

`AnswerAnalysis` / `TurnAnalysisResult`:
- Jackson 어노테이션 제거 (`@JsonCreator`, `@JsonProperty`)
- compact constructor 검증 제거 (Generated\* 가 책임)
- `fromJson` 정적 팩토리 제거 (TurnAnalysisResult)
- 도메인 메서드만 보유 (`with*` / `empty` / `applyL1FalseNegativeGuard`)

`RubricScoringResult`:
- 변경 없음 (Jackson 미사용)

### Verification

- [ ] `./gradlew test --tests "AnswerAnalysisTest"` 기존 통과
- [ ] `./gradlew test --tests "TurnAnalysisResultTest"` (있으면) 통과
- [ ] `./gradlew test --tests "RubricScoringResultTest"` (있으면) 통과

### 커밋 메시지

```
refactor(BE): 도메인 record 3종 Jackson 의존 제거 (Generated* 분리)
```

---

## Phase 3: 호출부 import / 타입 갱신

- **구현**: `backend` — 33+ 호출부

### 변경 영역

| 영역 | 호출부 추정 | 변경 내용 |
|---|---|---|
| `domain/interview/**` | `AnswerAnalysis` 17 | import 동일 (위치 유지). LLM 응답 진입점만 `GeneratedAnswerAnalysis.toDomain()` 호출 |
| `domain/interview/**` | `TurnAnalysisResult` 4 | 동상. 진입점 `GeneratedTurnAnalysis.toDomain()` |
| `domain/feedback/rubric/**` | `RubricScoringResult` 5 | 동상. 진입점 `GeneratedRubricScoring.toDomain()` |
| `domain/feedback/session/**` | `SessionFeedbackPayload` 6 → `GeneratedSessionFeedback` | import + 타입 변경 (도메인 객체 분리 X — 직접 사용) |
| `infra/ai/context/compaction/**` | `CompactionSummaryResult` 1 → `GeneratedCompactionSummary` | import + 타입 변경 |
| `infra/ai/**` + `domain/resume/**` | `ExtractedResumeSkeleton` ≈ 3 → `GeneratedResumeSkeleton` | import 변경 |

### LLM 응답 진입점 (toDomain 호출 도입)

- `AnswerAnalyzer` / `AudioTurnAnalyzer` / `TextFallbackTurnAnalyzer` — `parseOrRetry` 호출 후 `Generated*.toDomain()`
- `RubricScoringAdapter` — Phase 4 참조
- `SessionFeedbackParser` 호출부 — `GeneratedSessionFeedback` 직접 (도메인 객체 X)
- `DialogueCompactor` — `GeneratedCompactionSummary` 직접 (`toCompactString()` 사용)
- `ResumeSkeletonExtractor` — 기존 `toDomain()` 패턴 유지

### Verification

- [ ] `./gradlew compileJava` 통과 (import 일괄 검출)
- [ ] 기존 Service 테스트 회귀 통과: `AnswerAnalyzerTest` / `AudioTurnAnalyzerTest` / `TextFallbackTurnAnalyzerTest` / `TurnAnalysisPipelineTest` / `FollowUpQuestionGeneratorTest` / `QuestionGeneratorTest` / `InterviewPlanGeneratorTest` / `ResumeSkeletonExtractorTest` / `RubricScoringEventListenerTest` / `SessionFeedbackParserTest` / `DialogueCompactorTest`

### 커밋 메시지

```
refactor(BE): 호출부 33+ Generated* 진입점에서 toDomain() 매핑
```

---

## Phase 4: `RubricScoringAdapter` record 매핑 전환

- **구현**: `backend` — raw map 파싱 → `GeneratedRubricScoring` record 매핑

### 변경 파일

- `backend/src/main/java/com/rehearse/api/infra/ai/RubricScoringAdapter.java`
- `backend/src/test/java/com/rehearse/api/infra/ai/RubricScoringAdapterTest.java`

### 핵심 로직

기존:
```java
Map<String, Map<String, Object>> raw = objectMapper.readValue(json, ...);
// raw map 직접 매핑 → DimensionScore 생성
```

변경:
```java
GeneratedRubricScoring generated = aiResponseParser.parseOrRetry(raw, GeneratedRubricScoring.class, ...);
return generated.toDomain();   // → RubricScoringResult
```

- adapter 자체 retry (evidence_quote 보강 등) 유지 (비스코프).
- 기존 `parseDimensionScores` 보일러플레이트 제거.

### Verification

- [ ] `./gradlew test --tests "RubricScoringAdapterTest"` 회귀 통과
- [ ] **신규 케이스**: 기존 LLM 응답 fixture 1건 → `GeneratedRubricScoring` 매핑 통과 + `toDomain()` 결과 = 기존 raw map 매핑 결과 동등 (`dimensionScores` Map 키/값 / `scoredDimensions` 순서 / `levelFlag`)
- [ ] **신규 케이스**: 빈 `dimensionScores` LLM 응답 = 매핑 거절 → schema retry 발동

### 커밋 메시지

```
refactor(BE): RubricScoringAdapter raw map → GeneratedRubricScoring record 매핑
```

---

## Phase 5: `SchemaExampleRegistry` Generated\* 키 매핑

- **구현**: `backend` — 신규/리네이밍 키 등록

### 변경 파일

- `backend/src/main/java/com/rehearse/api/infra/ai/SchemaExampleRegistry.java` (또는 동등 클래스)
- `backend/src/test/java/com/rehearse/api/infra/ai/SchemaExampleRegistryTest.java`

### 핵심 로직

- 기존 키 (`AnswerAnalysis.class` / `TurnAnalysisResult.class` 등) → `Generated*.class` 로 갱신
- 신규 키 (`GeneratedSessionFeedback` / `GeneratedCompactionSummary`) 등록

### Verification

- [ ] `./gradlew test --tests "SchemaExampleRegistryTest"` 통과
- [ ] schema retry 시 hint prompt 정상 부여

### 커밋 메시지

```
refactor(BE): SchemaExampleRegistry Generated* 키 매핑 갱신
```

---

## Phase 6: ArchUnit 룰 + 신규 테스트

- **구현**: `backend` — 도메인-인프라 경계 강제

### 변경 파일

**신규**:
- `backend/src/test/java/com/rehearse/api/architecture/AiDtoArchitectureTest.java`
- `backend/src/test/java/com/rehearse/api/infra/ai/AiResponseParserTest.java` 신규 케이스 추가

### 핵심 로직

```java
// AiDtoArchitectureTest

@Test
void domain_layer_must_not_depend_on_infra_ai_dto_except_service_packages() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .and().resideOutsideOfPackages("..domain..service..", "..domain..models.service..")
        .should().dependOnClassesThat().resideInAPackage("..infra.ai.dto..")
        .check(importedClasses);
}
```

```java
// AiResponseParserTest 신규 케이스

@Test
void compact_constructor_거절시_schema_retry_발동() {
    // 1차 응답 = 검증 위반 (예: answerQuality = 0)
    // → IllegalArgumentException → Jackson ValueInstantiationException wrap
    // → 기존 JsonProcessingException catch 흡수 → schema retry 발동
    // 2차 응답 = 정상 → 통과
}

@Test
void schema_retry_2차_실패시_BusinessException_전파() {
    // 1차/2차 모두 검증 위반 → BusinessException(AI_PARSE_FAILED)
}
```

### Verification

- [ ] `./gradlew test --tests "AiDtoArchitectureTest"` 통과
- [ ] `./gradlew test --tests "AiResponseParserTest"` 신규 케이스 통과

### 커밋 메시지

```
test(BE): AiDto ArchUnit + parseOrRetry 검증 거절 회귀 케이스 추가
```

---

## Phase 7: 빌드 / 전체 테스트

- **구현**: `backend` — 통합 회귀

### Verification

- [ ] `./gradlew compileJava` 통과
- [ ] `./gradlew test` 전체 통과
- [ ] `./gradlew check` (ArchUnit 포함) 통과

---

## 통합 Verification

- [ ] tech-spec.md Verification 모든 항목 통과
- [ ] product-spec.md AC 6개 모두 충족
- [ ] FE 영향 없음 (BE 내부 리팩토링 — API contract 무변)

---

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제.

- [ ] `code-reviewer-backend` 호출 (구현 완료 직후 — 메인 세션 책임)
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md `Pre / Post State` 참조)

---

## 위험 점검 (구현 진입 전)

- [ ] `parseOrRetry` Jackson wrap 가정 검증 = Phase 6 의 `AiResponseParserTest` 신규 케이스로 강제
- [ ] `RubricScoringAdapter` 매핑 전환 회귀 = Phase 4 fixture 동등성 케이스로 강제
- [ ] 호출부 import 누락 = `compileJava` 컴파일러 검출
