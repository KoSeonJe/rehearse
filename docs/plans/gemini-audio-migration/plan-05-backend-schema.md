# Plan 05: Backend 피드백 스키마 확장

> 상태: Draft
> 작성일: 2026-03-23

## Why

Gemini 네이티브 오디오 분석이 기존에 없던 음성 특성 데이터(톤, 감정, 말빠르기, 필러워드 목록)를 생성한다. 이를 저장하고 프론트엔드에 전달하려면 DB 스키마, 엔티티, DTO를 확장해야 한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/db/migration/V12__add_vocal_feedback_columns.sql` | **신규** — DB 마이그레이션 |
| `backend/.../questionset/entity/TimestampFeedback.java` | 음성 특성 컬럼 추가 |
| `backend/.../questionset/entity/QuestionSetFeedback.java` | 종합 리포트 필드 추가 |
| `backend/.../questionset/dto/SaveFeedbackRequest.java` | DTO 필드 추가 |
| `backend/.../questionset/entity/AnalysisProgress.java` | `ANALYZING` enum 추가 |

## 상세

### DB 마이그레이션

```sql
-- V12__add_vocal_feedback_columns.sql

-- timestamp_feedback 테이블 음성 특성 필드
ALTER TABLE timestamp_feedback ADD COLUMN filler_words TEXT;
ALTER TABLE timestamp_feedback ADD COLUMN speech_pace VARCHAR(10);
ALTER TABLE timestamp_feedback ADD COLUMN tone_confidence INT;
ALTER TABLE timestamp_feedback ADD COLUMN emotion_label VARCHAR(20);
ALTER TABLE timestamp_feedback ADD COLUMN vocal_comment TEXT;

-- question_set_feedback 테이블 종합 리포트 필드
ALTER TABLE question_set_feedback ADD COLUMN verbal_summary TEXT;
ALTER TABLE question_set_feedback ADD COLUMN vocal_summary TEXT;
ALTER TABLE question_set_feedback ADD COLUMN nonverbal_summary TEXT;
ALTER TABLE question_set_feedback ADD COLUMN strengths TEXT;
ALTER TABLE question_set_feedback ADD COLUMN improvements TEXT;
ALTER TABLE question_set_feedback ADD COLUMN top_priority_advice TEXT;
```

- 모든 신규 컬럼은 **nullable** (기존 데이터 호환)
- `filler_words`, `strengths`, `improvements`는 JSON 배열을 TEXT로 저장 (JPA Converter 사용)

### AnalysisProgress enum 변경

```java
public enum AnalysisProgress {
    STARTED,
    EXTRACTING,
    STT_PROCESSING,        // 기존 유지 (폴백용)
    VERBAL_ANALYZING,      // 기존 유지 (폴백용)
    NONVERBAL_ANALYZING,   // 기존 유지 (폴백용)
    ANALYZING,             // 신규 — Gemini + Vision 병렬 단계
    FINALIZING,
    FAILED
}
```

- 기존 enum 값을 제거하지 않음 (Whisper 폴백 시 사용 가능)
- 새 `ANALYZING` 값 추가

### SaveFeedbackRequest DTO 확장

기존 필드 유지 + 신규 필드 추가 (모두 nullable):

```java
// TimestampFeedbackItem 신규 필드
private List<String> fillerWords;      // ["음", "어", "그니까"]
private String speechPace;             // "빠름" / "적절" / "느림"
private Integer toneConfidence;        // 0-100
private String emotionLabel;           // "자신감" / "긴장" / "평온" / "불안"
private String vocalComment;           // 음성 특성 피드백
```

## 담당 에이전트

- Implement: `backend` — 엔티티, DTO, 마이그레이션
- Review: `architect-reviewer` — 스키마 설계, 하위 호환성

## 검증

- H2 로컬 환경에서 마이그레이션 성공 확인
- 기존 피드백 저장 API가 신규 필드 없이도 정상 동작 (nullable)
- 신규 필드 포함 피드백 저장/조회 정상 동작
- `progress.md` 상태 업데이트 (Task 5 → Completed)
