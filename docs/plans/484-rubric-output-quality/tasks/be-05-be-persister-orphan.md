# Task BE-05 — `SaveFeedbackRequest` 재정의 + `NonverbalScorePersister` 재작성 + `QuestionScorePersister.saveNonverbal` 삭제 + orphan 삭제 + ArchUnit

> **Phase**: 2a (Lambda + BE 묶음 PR)
> **답하는 질문**: BE 가 영역 키 분리 페이로드를 어떻게 수신·적재? 보조 매퍼 자산을 어떻게 안전히 삭제?

---

## 목적

DTO (`SaveFeedbackRequest.NonverbalScore`) 를 영역 키 분리 (`{vocal?, vision?}`) 로 재정의 + persister 가 두 영역 키 dimension 머지 + 유효성 필터 + `questionScorePersister.saveRubric(..., "nonverbal-v1", ...)` 단일 진입점 호출. `QuestionScorePersister.saveNonverbal(...)` 메서드 삭제 (rubric_id 하드코딩 결함 P0-2 해소). 보조 매퍼 (`NonverbalRubricScorer` / `NonverbalContextWeightsLoader`) + 의존 test 3 파일 orphan 삭제. ArchUnit 로 재발 방지.

## 에이전트

- **구현**: `backend` — DTO Bean Validation / persister 트랜잭션 / orphan 검증 / ArchUnit.
- **리뷰**: PR#2 머지 직전 `code-reviewer-backend`.

## 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/SaveFeedbackRequest.java`
  - line 65-73 `NonverbalScore` inner class **재정의** — 기존 `fluency / confidenceTone / eyeContactPosture / composure / rawSignals` 필드 제거:
    ```java
    @Getter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NonverbalScore {
        @Valid private AreaScore vocal;
        @Valid private AreaScore vision;
    }

    @Getter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AreaScore {
        @Valid private List<DimensionScoreItem> dimensions;
    }

    @Getter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DimensionScoreItem {
        @JsonProperty("dimension_ref") @NotBlank private String dimensionRef;
        @NotNull @Min(1) @Max(3) private Integer score;
        @NotBlank private String observation;
        @JsonProperty("evidence_quote") @NotBlank private String evidenceQuote;
    }
    ```
  - line 40-43 `CommentBlock nonverbalComment / overallComment / vocalComment / attitudeComment` 4개 필드 **잔존** (Phase 2a — `@JsonIgnoreProperties` 호환). Phase 3 (BE-08) 삭제.
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalScorePersister.java` (line 42-93)
  - `NonverbalRubricScorer.score(...)` 호출 제거
  - `payload.vocal` + `payload.vision` 두 영역 키 dimension 머지 + 유효성 필터 (`score / observation / evidence_quote` NOT NULL) → `LinkedHashMap<String, DimensionScore>` 조립
  - `questionScorePersister.saveRubric(questionId, interviewId, "nonverbal-v1", null, dims)` 명시 호출 (rubric_id 인자 주입)
  - 분기 로그: `[정상 skip] payloadNull` / `[결함 skip] areasEmpty` / `[결함 skip] allInvalid`
  - `resolveTrack` / `composure` 분기 폐기
- `backend/src/main/java/com/rehearse/api/domain/feedback/score/service/QuestionScorePersister.java` (line 55-84)
  - `saveNonverbal(...)` 메서드 **삭제** (rubric_id="nonverbal" 하드코딩 결함 P0-2 해소)
  - 호출자 (`NonverbalScorePersister`) 가 기존 `saveRubric(...)` 시그니처 사용
  - `grep -rn "saveNonverbal" backend/src` 0 매치 검증
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalRubricScorer.java` — **파일 삭제** (orphan)
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalContextWeightsLoader.java` — **파일 삭제** (orphan)
- 의존 test 3 파일 (`NonverbalRubricScorerTest` / `NonverbalContextWeightsLoaderTest` / `NonverbalScorePersisterTest` 중 매퍼 의존 부분) — 삭제 또는 갱신
- `backend/src/test/java/com/rehearse/api/arch/NonverbalOrphanArchTest.java` — **신규** (ArchUnit, P1):
  - `classes().that().haveSimpleName("NonverbalRubricScorer").or().haveSimpleName("NonverbalContextWeightsLoader").should().notExist()` 동등 룰
- `backend/src/test/java/.../NonverbalScorePersisterTest.java` — 재작성 (5 시나리오)
- `backend/src/test/java/.../QuestionScorePersisterTest.java` — `saveRubric_with_nonverbal_v1_rubric_id` 시나리오 + `saveNonverbal` 호출 테스트 삭제

## 핵심 로직 / 변경 요약

```
[Pre]  SaveFeedbackRequest.NonverbalScore = int 4개 (fluency/confidenceTone/eyeContactPosture/composure)
       NonverbalScorePersister = NonverbalRubricScorer.score(...) 호출 →
                                  DimensionScore.of(score, null, null) 4 row 적재
       QuestionScorePersister.saveNonverbal = rubric_id="nonverbal" 하드코딩

[Post] SaveFeedbackRequest.NonverbalScore = {vocal?: AreaScore, vision?: AreaScore} 영역 키 분리
       NonverbalScorePersister.persistOne =
         ├─ payload null              → [정상 skip] payloadNull
         ├─ vocal/vision 둘 다 null   → [결함 skip] areasEmpty
         ├─ 두 영역 dimension 머지 + 유효성 필터
         ├─ dims empty                → [결함 skip] allInvalid
         └─ saveRubric(qid, iid, "nonverbal-v1", null, dims)
       QuestionScorePersister.saveNonverbal = 메서드 삭제 (호출처 0)
       NonverbalRubricScorer / NonverbalContextWeightsLoader = 파일 삭제
       ArchUnit = 재발 방지 룰
```

## 의존

- 선행: BE-04 (Lambda 페이로드 영역 키 분리 확정 → BE DTO 정합)
- 외부: Spring Validation (jakarta.validation) + ArchUnit (기존 의존)

## 테스트 케이스

- [ ] **`NonverbalScorePersisterTest extends ServiceIntegrationSupport`** (5 시나리오):
  1. 정상 vocal+vision 영역 키 동시 수신 → 3차원 dims 머지 → `question_score.rubric_id="nonverbal-v1"` 1 row + `question_score_dimension` 3 row + observation/evidence NOT NULL
  2. payload null → row 0 + `[정상 skip] payloadNull`
  3. vocal/vision 둘 다 null → row 0 + `[결함 skip] areasEmpty`
  4. vocal 만 산출 (vision null) → `question_score` 1 row + dimension 2 row (fluency / confidence_tone) — fault isolation
  5. 한 dimension 만 observation null → 해당 dimension skip + 나머지 적재
- [ ] composure dimension row 0건 assert
- [ ] `rubric_id="nonverbal"` (legacy) 신규 row 0건 assert (saveNonverbal 삭제 검증)
- [ ] **`QuestionScorePersisterTest.saveRubric_with_nonverbal_v1_rubric_id`**:
  - 호출자가 rubric_id="nonverbal-v1" 주입 시 정상 적재 + idempotent (`findByQuestionIdAndRubricId`)
  - `saveNonverbal` 호출 테스트 = **삭제** (메서드 부재)
- [ ] **ArchUnit** (`NonverbalOrphanArchTest`):
  - `NonverbalRubricScorer` / `NonverbalContextWeightsLoader` classes 부재 assert
- [ ] **orphan grep**:
  - `grep -rn "NonverbalRubricScorer\\|NonverbalContextWeightsLoader" backend/src` = 0
  - `./gradlew compileJava` green
- [ ] **회귀**: `./gradlew test --tests "*Nonverbal*" / "*Feedback*"` green

## 완료 기준

- [ ] 변경 파일 commit (논리 단위 분리: DTO 재정의 / persister 재작성 / saveNonverbal 삭제 / orphan 삭제 / ArchUnit)
- [ ] PR#2 묶음 회귀 green
- [ ] orphan grep 0
- [ ] **`code-reviewer-backend` 실행** (PR#2 머지 직전, MANDATORY)

## 커밋 메시지

```
refactor(BE): SaveFeedbackRequest NonverbalScore 영역 키 분리
refactor(BE): NonverbalScorePersister 영역 키 머지 + saveRubric 단일 진입
refactor(BE): QuestionScorePersister.saveNonverbal 삭제 (rubric_id 인자 주입)
chore(BE): NonverbalRubricScorer / ContextWeightsLoader orphan 삭제 + ArchUnit
```

## 비고

- DTO `CommentBlock` 4개 필드 잔존 (Phase 2a) → BE-08 (Phase 3) 에서 entity / DTO 동시 삭제.
- raw 측정치 필드 (entity 영속 6개 + DTO 잔존) → BE-08 (Phase 3) 동시 정리.
- ArchUnit 재발 방지 = orphan 부활 PR 차단 (CI 단계 fail).
- 보안 (A09): persister 로그 본문에 transcript / observation 본문 미포함 — plan-472 로그 패턴 계승.
