# Feedback Aggregate

## 범위

- `domain/feedback/entity/QuestionSetFeedback`
- `domain/feedback/entity/TimestampFeedback`
- `domain/reviewbookmark/entity/ReviewBookmark`

`QuestionSet` / `Question` / `User` 는 다른 파일 참조. 여기선 관계만 표시.

## 다이어그램

```mermaid
erDiagram
    QUESTION_SET {
        bigint id PK
    }

    QUESTION {
        bigint id PK
    }

    USERS {
        bigint id PK
    }

    QUESTION_SET_FEEDBACK {
        bigint id PK
        bigint question_set_id FK,UK
        text question_set_comment
    }

    TIMESTAMP_FEEDBACK {
        bigint id PK
        bigint question_set_feedback_id FK
        bigint question_id FK "nullable"
        bigint start_ms
        bigint end_ms
        text transcript
        int filler_word_count
        varchar eye_contact_level "GOOD/AVERAGE/NEEDS_IMPROVEMENT"
        varchar posture_level
        varchar expression_label
        text nonverbal_comment
        text overall_comment
        boolean is_analyzed
        text filler_words "JSON array"
        varchar speech_pace
        varchar tone_confidence_level
        varchar emotion_label
        text vocal_comment
        text attitude_comment
    }

    REVIEW_BOOKMARK {
        bigint id PK
        bigint user_id "id ref"
        bigint timestamp_feedback_id FK
        datetime resolved_at "nullable"
    }

    QUESTION_SET ||--o| QUESTION_SET_FEEDBACK : "questionSet (OneToOne)"
    QUESTION_SET_FEEDBACK ||--o{ TIMESTAMP_FEEDBACK : "questionSetFeedback (cascade=ALL, orphanRemoval)"
    QUESTION ||--o{ TIMESTAMP_FEEDBACK : "question (nullable)"
    USERS ||--o{ REVIEW_BOOKMARK : "user_id (id ref)"
    TIMESTAMP_FEEDBACK ||--o{ REVIEW_BOOKMARK : "timestamp_feedback_id"
```

## 메모

- `QuestionSetFeedback` = QuestionSet 1 개당 1 개 (unique). 전체 답변 종합 코멘트.
- `TimestampFeedback` = 영상 구간 (start_ms~end_ms) 단위 피드백. Lambda 비언어 분석 결과 (eye contact / posture / filler / emotion 등) 동거.
- `ReviewBookmark.uniqueConstraint = (user_id, timestamp_feedback_id)` — 동일 사용자가 같은 구간 중복 북마크 방지.
- `ReviewBookmark.resolvedAt` null = 미해결, 값 있음 = 해결됨. `markResolved()` / `reopen()` 으로 토글.
- `ReviewBookmark` 는 `@NamedEntityGraph` 로 timestampFeedback → question / questionSet → interview 까지 한 번에 fetch (조회 N+1 방지).
