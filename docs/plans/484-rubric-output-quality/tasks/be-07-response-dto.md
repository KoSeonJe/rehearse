# Task BE-07 — `TimestampFeedbackResponse.nonverbalFeedback` 신설 + `FeedbackService` Map 타입 확장 + legacy rubric_id skip

> **Phase**: 2a (Lambda + BE 묶음 PR)
> **답하는 질문**: BE 가 verbal + nonverbal 두 rubric 결과를 FE 응답 1건에 어떻게 담을지? 응답 DTO 시그니처를 어떻게 확장?

---

## 목적

`TimestampFeedbackResponse` 에 `nonverbalFeedback` (inner class `NonverbalRubricFeedback {rubricId, dimensions}`) 신설 (verbal `technicalFeedback` 동일 구조). `from(...)` 정적 팩토리 시그니처 = `from(TimestampFeedback feedback, List<QuestionScore> questionScores, Map<Long, List<QuestionScoreDimension>> dimsByScoreId)` 로 확장 — questionScores 순회 시 `rubricId="nonverbal-v1"` 매치 → nonverbalFeedback 빌드, 그 외 verbal rubric (`resume-v1` / `behavioral-v1` / `technical-v1`) → technicalFeedback 빌드. legacy rubric_id (`"nonverbal"` 등) row = silently skip + DEBUG 로그 (P0-D). `QuestionSetFeedbackResponse.from` Map 타입 `Map<Long, QuestionScore>` → `Map<Long, List<QuestionScore>>`. `FeedbackService` 가 group by questionId 로 조회 결과 가공.

## 에이전트

- **구현**: `backend` — DTO 정적 팩토리 시그니처 / Service repository 조회 결과 가공 / legacy skip 로그.
- **리뷰**: PR#2 머지 직전 `code-reviewer-backend`.

## 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/TimestampFeedbackResponse.java`
  - 신규 inner class `NonverbalRubricFeedback`:
    ```java
    public record NonverbalRubricFeedback(
        String rubricId,
        List<TechnicalDimensionFeedback> dimensions
    ) {}
    ```
    이름 `NonverbalRubricFeedback` 선택 사유: 기존 `NonverbalFeedback` (4컬럼 read 잔존, Phase 3 삭제) 와 컴파일 충돌 회피. 응답 JSON 키는 `nonverbalFeedback` (신규).
  - 신규 필드 `nonverbalFeedback` 추가.
  - `from(feedback, questionScore, dimensions)` 시그니처 → `from(TimestampFeedback feedback, List<QuestionScore> questionScores, Map<Long, List<QuestionScoreDimension>> dimsByScoreId)` 확장:
    - questionScores 순회 — `rubricId="nonverbal-v1"` 매치 시 `NonverbalRubricFeedback` 빌드 (dimensions = `dimsByScoreId.get(questionScore.getId())` → `TechnicalDimensionFeedback` 매핑)
    - `rubricId ∈ {resume-v1, behavioral-v1, technical-v1}` 매치 시 `TechnicalFeedback` 빌드
    - `rubricId ∉ {위 4개}` row 는 silently skip + `log.debug("legacy rubric_id 응답 미포함 questionScoreId={} rubricId={}", ...)` (P0-D)
- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/QuestionSetFeedbackResponse.java` (line 33-40 추정)
  - `from(...)` 시그니처 Map 타입 변경: `Map<Long, QuestionScore>` → `Map<Long, List<QuestionScore>>` (questionId → verbal/nonverbal 동시 포함).
  - 호출자에서 group by questionId 후 전달.
- `backend/src/main/java/com/rehearse/api/domain/feedback/service/FeedbackService.java` (또는 응답 조립 진입점)
  - `QuestionScoreRepository.findAllByInterviewId` 결과 → `Map<Long, List<QuestionScore>>` group by `questionId` 구성
  - dimension 도 동일 — `dimensionsByQuestionScoreId: Map<Long, List<QuestionScoreDimension>>` 구성
  - 신규 시그니처로 `QuestionSetFeedbackResponse.from(...)` 호출.
- `backend/src/test/java/.../TimestampFeedbackResponseTest.java` — 신규 / 갱신 (Domain Unit 또는 Service Integration):
  - verbal + nonverbal 동시 시나리오 / nonverbal 만 / verbal 만 / 둘 다 부재 / legacy rubric_id row skip 시나리오.
- `backend/src/test/java/.../FeedbackServiceTest.java` — 시나리오 추가 (group by questionId 정합 / legacy skip 회귀).

## 핵심 로직 / 변경 요약

```
[Pre]  TimestampFeedbackResponse.from(feedback, questionScore, dimensions)
        = 단일 rubric (verbal) 만 가정. questionScore 1개 입력.
       QuestionSetFeedbackResponse.from(... Map<Long, QuestionScore> ...)
        = questionId → verbal rubric 1:1 가정.
       응답 JSON: technicalFeedback 만 (nonverbal 은 delivery.* legacy).

[Post] TimestampFeedbackResponse.from(feedback, questionScores: List, dimsByScoreId: Map)
        ├─ questionScores 순회
        ├─ rubricId="nonverbal-v1" → NonverbalRubricFeedback 빌드
        ├─ rubricId ∈ verbal 3종      → TechnicalFeedback 빌드
        └─ rubricId ∉ 4종 (legacy)    → silently skip + DEBUG 로그
       QuestionSetFeedbackResponse.from(... Map<Long, List<QuestionScore>> ...)
       응답 JSON: technicalFeedback + nonverbalFeedback 동시.
```

## 의존

- 선행: BE-05 (`NonverbalScorePersister` 가 `rubric_id="nonverbal-v1"` 적재 → 본 Task 가 응답에서 매치).
- 외부: 없음.

## 테스트 케이스

- [ ] **`TimestampFeedbackResponseTest.builds_both_technical_and_nonverbal_when_both_question_scores_present`** (Domain Unit, `extends DomainUnitSupport`):
  - questionScores = `[verbal(rubricId=technical-v1), nonverbal(rubricId=nonverbal-v1)]`
  - 결과 = `technicalFeedback != null` + `nonverbalFeedback != null` + dimensions 정확 매핑
- [ ] **`TimestampFeedbackResponseTest.builds_nonverbal_only_when_verbal_absent`**:
  - questionScores = `[nonverbal(rubricId=nonverbal-v1)]` → `technicalFeedback = null` + `nonverbalFeedback != null`
- [ ] **`TimestampFeedbackResponseTest.builds_technical_only_when_nonverbal_absent`**:
  - questionScores = `[verbal(rubricId=technical-v1)]` → `technicalFeedback != null` + `nonverbalFeedback = null`
- [ ] **`TimestampFeedbackResponseTest.skips_legacy_rubric_id_silently`** (P0-D):
  - questionScores = `[verbal(rubricId=technical-v1), legacy(rubricId="nonverbal")]`
  - 결과 = `technicalFeedback != null` + `nonverbalFeedback = null` + legacy row 응답 미포함 + DEBUG 로그 호출 assert (LogCaptor 사용)
- [ ] **`FeedbackServiceTest.groups_question_scores_by_question_id_when_assembling_response`** (Service Integration, `extends ServiceIntegrationSupport`):
  - questionId 1개당 verbal + nonverbal 2 row 적재 후 응답 조회
  - `Map<Long, List<QuestionScore>>` group 정합 assert + `TimestampFeedbackResponse.from` 호출 시그니처 정합
- [ ] **회귀**: `./gradlew test --tests "*Feedback*"` green

## 완료 기준

- [ ] 변경 파일 commit (논리 단위: DTO 시그니처 / Service 조회 가공 / inner class 신설 분리 권장)
- [ ] PR#2 묶음 회귀 green
- [ ] **`code-reviewer-backend` 실행** (PR#2 머지 직전, MANDATORY)

## 커밋 메시지

```
feat(BE): TimestampFeedbackResponse.nonverbalFeedback 신설 + from() 시그니처 확장
refactor(BE): QuestionSetFeedbackResponse / FeedbackService Map 타입 List 로 확장
chore(BE): legacy rubric_id 응답 silently skip + DEBUG 로그
```

## 비고

- inner class 명 `NonverbalRubricFeedback` 선택 사유 = 기존 `NonverbalFeedback` (4컬럼 read 잔존, Phase 3 삭제) 과 컴파일 충돌 회피. 응답 JSON 키 = `nonverbalFeedback` (신규).
- Phase 3 (BE-08) 에서 기존 `NonverbalFeedback` + `DeliveryFeedback` + `VocalFeedback` + `CommentBlock` inner class 모두 삭제. 본 Task 의 `NonverbalRubricFeedback` 은 잔존.
- legacy skip 정책 (P0-D) = product-spec 비스코프 A (backfill 미수행) 정합 — 과거 `rubric_id="nonverbal"` row (보조 매퍼 산물) 가 응답에 포함되면 FE 단일 카드 패턴 깨짐.
- 보안 (A09): 로그 본문에 questionScoreId / rubricId 만 (transcript / observation 본문 미포함).
