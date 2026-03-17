# Task 1: DB 스키마 — question_set 기반 테이블 + 엔티티 설계

## Status: Not Started

## Issue: #78

## Why

새 파이프라인은 질문세트 단위로 녹화/분석/피드백을 관리함.
기존 interview + feedback 2테이블 구조로는 질문세트별 상태 추적, 답변 구간 관리, 타임스탬프 피드백 저장이 불가능.

스키마 설계: `docs/architecture/recording-analysis-pipeline.md` DB 스키마 섹션

## 의존성

- 선행: Task 0 (Legacy 정리)
- 후행: Task 3, 4, 5 (API 구현에 엔티티 필요)

## 구현 계획

### PR 1: [BE] Flyway V3 + JPA 엔티티 + Repository

**Flyway 마이그레이션 (V3):**

테이블 4개:
- `question_set` — 질문세트 (면접 ID, 카테고리, 질문 텍스트, 모범답변, 분석 상태, 변환 상태)
- `question_set_answer` — 답변 구간 (질문세트 ID, 답변 타입, 시작/종료 ms)
- `question_set_feedback` — 질문세트 피드백 (점수, 코멘트)
- `timestamp_feedback` — 타임스탬프별 피드백 (언어/비언어 점수, STT, 필러워드 등)

**JPA 엔티티:**
- `QuestionSet.java`
- `QuestionSetAnswer.java`
- `QuestionSetFeedback.java`
- `TimestampFeedback.java`

**Enum:**
- `AnalysisStatus`: PENDING, PENDING_UPLOAD, ANALYZING, COMPLETED, FAILED
- `AnalysisProgress`: STARTED, EXTRACTING, STT_PROCESSING, VERBAL_ANALYZING, NONVERBAL_ANALYZING, FINALIZING, FAILED
- `ConvertStatus`: PENDING, CONVERTING, COMPLETED, FAILED
- `AnswerType`: MAIN, FOLLOWUP_1, FOLLOWUP_2, FOLLOWUP_3
- `QuestionCategory`: RESUME, CS

**Repository 4개:**
- `QuestionSetRepository`
- `QuestionSetAnswerRepository`
- `QuestionSetFeedbackRepository`
- `TimestampFeedbackRepository`

**interview 테이블 변경:**
- `overall_score` (INT, nullable) 추가
- `overall_comment` (TEXT, nullable) 추가

**검증:** `./gradlew build` + 테스트 통과 + H2 스키마 생성 확인

- Implement: `backend`
- Review: `architect-reviewer` — 엔티티 관계, 인덱스, 컬럼 타입 검증

## Acceptance Criteria

- [ ] Flyway V3 마이그레이션 성공
- [ ] JPA 엔티티 4개 + 기존 Interview 엔티티 수정
- [ ] Enum 5개 정의
- [ ] Repository 4개 기본 CRUD 동작
- [ ] H2/MySQL 양쪽 스키마 정상 생성
