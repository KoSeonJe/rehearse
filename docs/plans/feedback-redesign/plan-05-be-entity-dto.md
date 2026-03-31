# Plan 05: BE 엔티티 + DTO 변경

> 상태: Draft
> 작성일: 2026-03-30

## Why

Lambda에서 생성하는 피드백 구조가 변경되므로, BE의 엔티티와 DTO도 새 필드에 맞게 수정해야 한다. score 컬럼을 nullable하고, technical 관련 필드와 라벨 필드를 추가한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/.../questionset/entity/TimestampFeedback.java` | score 필드 유지(nullable), technical 필드 추가, 라벨 필드 추가 |
| `backend/src/.../questionset/dto/TimestampFeedbackResponse.java` | 응답 DTO 변경 (technical 블록 추가, 라벨 필드) |
| `backend/src/.../questionset/dto/SaveFeedbackRequest.java` | 내부 API 요청 DTO 변경 |
| `backend/src/.../questionset/entity/QuestionSetAnalysis.java` | questionSetScore 제거 (필요 시) |
| `backend/src/.../questionset/dto/QuestionSetFeedbackResponse.java` | questionSetScore 제거 |
| `backend/src/main/resources/db/migration/V{N}__feedback_redesign.sql` | 신규 — DB 마이그레이션 |

## 상세

### 1. TimestampFeedback 엔티티 변경

**기존 유지 (nullable):**
- `verbalScore` (Integer)
- `eyeContactScore` (Integer)
- `postureScore` (Integer)
- `toneConfidence` (Integer)

**추가 필드:**
- `accuracyIssues` (String, TEXT) — `[{"claim":"...","correction":"..."}]` JSON
- `coachingStructure` (String, VARCHAR 500) — 답변 구조 코칭
- `coachingImprovement` (String, VARCHAR 500) — 개선 방향 코칭
- `eyeContactLevel` (String, VARCHAR 20) — `GOOD|AVERAGE|NEEDS_IMPROVEMENT`
- `postureLevel` (String, VARCHAR 20)
- `toneConfidenceLevel` (String, VARCHAR 20)

### 2. TimestampFeedbackResponse DTO 변경

```java
public record TimestampFeedbackResponse(
    // 기존 필드 (id, questionId, questionType, questionText, modelAnswer, startMs, endMs, transcript, isAnalyzed)
    TechnicalResponse technical,
    NonverbalResponse nonverbal,
    VocalResponse vocal
) {
    public record TechnicalResponse(
        String verbalComment,
        List<AccuracyIssue> accuracyIssues,
        CoachingResponse coaching,
        Integer fillerWordCount
    ) {}

    public record AccuracyIssue(
        String claim,
        String correction
    ) {}

    public record CoachingResponse(
        String structure,
        String improvement
    ) {}

    public record NonverbalResponse(
        String eyeContactLevel,    // GOOD|AVERAGE|NEEDS_IMPROVEMENT
        String postureLevel,
        String expressionLabel,
        String nonverbalComment
    ) {}

    public record VocalResponse(
        String fillerWords,         // JSON 배열
        String speechPace,
        String toneConfidenceLevel, // GOOD|AVERAGE|NEEDS_IMPROVEMENT
        String emotionLabel,
        String vocalComment
    ) {}
}
```

### 3. DB 마이그레이션

```sql
-- 새 컬럼 추가
ALTER TABLE timestamp_feedback ADD COLUMN accuracy_issues TEXT NULL;
ALTER TABLE timestamp_feedback ADD COLUMN coaching_structure VARCHAR(500) NULL;
ALTER TABLE timestamp_feedback ADD COLUMN coaching_improvement VARCHAR(500) NULL;
ALTER TABLE timestamp_feedback ADD COLUMN eye_contact_level VARCHAR(20) NULL;
ALTER TABLE timestamp_feedback ADD COLUMN posture_level VARCHAR(20) NULL;
ALTER TABLE timestamp_feedback ADD COLUMN tone_confidence_level VARCHAR(20) NULL;
```

### 4. 하위 호환

이미 분석된 피드백(score는 있고 level은 없음)에 대해:
- BE 조회 시 score → level 변환 로직:
  - 0-49: `NEEDS_IMPROVEMENT`
  - 50-74: `AVERAGE`
  - 75-100: `GOOD`
- `accuracyIssues`, `coaching` 필드가 null이면 FE에서 해당 섹션 숨김

### 5. interviewType 전달

`InternalQuestionSetController`의 answers API 응답에 `interviewType` 필드 추가:
- Lambda가 호출하는 `/internal/question-sets/{id}/answers` 응답에 포함
- Interview 엔티티에서 조회하여 전달

## 담당 에이전트

- Implement: `backend` — 엔티티, DTO, 마이그레이션, 서비스
- Review: `architect-reviewer` — 엔티티 설계, 하위 호환 전략
- Review: `code-reviewer` — DTO 구조, JSON 파싱 안전성

## 검증

- 기존 분석 데이터 조회 시 에러 없이 fallback 동작하는지 확인
- 새 데이터 저장 시 새 필드가 정상 저장되는지 확인
- BE 테스트 통과 (`Backend CI`)
- `progress.md` 상태 업데이트 (Task 5 → Completed)
