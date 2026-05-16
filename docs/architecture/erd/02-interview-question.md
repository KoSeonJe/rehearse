# Interview + Question Aggregate

## 범위

- `domain/interview/entity/Interview` (+ 보조 테이블 2)
- `domain/question/entity/QuestionSet`
- `domain/question/entity/Question`
- `domain/question/entity/QuestionAnswer`
- `domain/question/entity/QuestionPool`
- `domain/question/entity/QuestionSetAnalysis`
- `domain/file/entity/FileMetadata`

## 다이어그램

```mermaid
erDiagram
    INTERVIEW {
        bigint id PK
        varchar public_id UK
        bigint user_id "id ref (nullable)"
        enum position
        varchar position_detail
        enum level "InterviewLevel"
        int duration_minutes
        enum tech_stack "nullable"
        enum status "InterviewStatus"
        enum question_generation_status
        text failure_reason
        int question_gen_retry_count
        datetime question_gen_last_retried_at
    }

    INTERVIEW_INTERVIEW_TYPES {
        bigint interview_id FK
        enum interview_type "InterviewType"
    }

    INTERVIEW_CS_SUB_TOPICS {
        bigint interview_id FK
        enum cs_sub_topic "CsSubTopic"
    }

    QUESTION_SET {
        bigint id PK
        bigint interview_id FK
        enum category "InterviewType"
        int order_index
        bigint file_metadata_id FK "nullable"
    }

    QUESTION_SET_ANALYSIS {
        bigint id PK
        bigint question_set_id FK,UK
        enum analysis_status
        enum convert_status
        boolean is_verbal_completed
        boolean is_nonverbal_completed
        varchar failure_reason
        text failure_detail
        varchar convert_failure_reason
        bigint version "@Version"
    }

    QUESTION {
        bigint id PK
        bigint question_set_id FK
        enum question_type "QuestionType"
        text question_text
        text tts_text
        text best_answer
        int order_index
        bigint question_pool_id FK "nullable"
    }

    QUESTION_ANSWER {
        bigint id PK
        bigint question_id FK
        bigint start_ms
        bigint end_ms
    }

    QUESTION_POOL {
        bigint id PK
        varchar cache_key
        text content
        text tts_content
        varchar category
        text best_answer
        boolean is_active
    }

    FILE_METADATA {
        bigint id PK
        enum file_type
        enum status "FileStatus"
        varchar s3_key
        varchar streaming_s3_key
        varchar bucket
        varchar content_type
        bigint file_size_bytes
        varchar failure_reason
        text failure_detail
        bigint version "@Version"
    }

    INTERVIEW ||--o{ INTERVIEW_INTERVIEW_TYPES : "ElementCollection"
    INTERVIEW ||--o{ INTERVIEW_CS_SUB_TOPICS : "ElementCollection"
    INTERVIEW ||--o{ QUESTION_SET : "interview"
    QUESTION_SET ||--o| QUESTION_SET_ANALYSIS : "1:0..1 (cascade=ALL)"
    QUESTION_SET ||--o| FILE_METADATA : "file_metadata_id (OneToOne)"
    QUESTION_SET ||--o{ QUESTION : "questionSet (cascade=ALL, orphanRemoval)"
    QUESTION_POOL ||--o{ QUESTION : "question_pool_id (nullable)"
    QUESTION ||--o{ QUESTION_ANSWER : "question"
```

## 메모

- `Interview` 는 `interviewTypes` / `csSubTopics` 두 ElementCollection 보조 테이블 보유. 각각 별도 row.
- `QuestionSet ↔ QuestionSetAnalysis` = 1:0..1. `cascade=ALL` 로 함께 생성/삭제. analysis 가 없으면 `AnalysisStatus.PENDING` 으로 간주.
- `QuestionSet ↔ FileMetadata` = OneToOne. 파일 없는 question_set 가능 (resume-only 등).
- `QuestionPool` 은 `Question` 다수가 참조하는 캐시 풀. nullable — 풀 미사용 질문 (resume-based) 도 존재.
- `Question.bestAnswer` = AI 모범 답안. `QuestionAnswer` 는 사용자 답변 타임스탬프 (start/end ms).
- `QuestionSetAnalysis.version` / `FileMetadata.version` = `@Version` 낙관락 (동시성 제어).
