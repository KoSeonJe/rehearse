# 백엔드 코드 품질 종합 리팩토링 — 진행 상황

## 태스크 상태

| # | 태스크 | PR 제목 | 상태 | 비고 |
|---|--------|---------|------|------|
| 1 | questionset 도메인 독립 분리 | `[BE] refactor: questionset 도메인 독립 분리` | Completed | 테스트 4개 선작성 + question/analysis/feedback 분리 |
| 2 | questionpool→interview, tts→infra 이동 | `[BE] refactor: questionpool interview 병합, tts infra 이동` | Completed | generation/pool 하위 구조 + infra/tts |
| 3 | 엔티티 팩토리/위임 메서드 추가 | `[BE] refactor: 도메인 엔티티 팩토리/위임 메서드 추가` | Completed | TimestampFeedbackMapper, QuestionSet 위임, buildQuestionSet |
| 4 | 서비스 God Class 분해 | `[BE] refactor: 서비스 God Class 분해 및 도메인 로직 위임` | Completed | InterviewDeletionService 추출, saveFeedback 리팩토링, Feature Envy 제거 |
| 5 | 장문 메서드 분리 + 컨벤션 통일 | `[BE] refactor: 장문 메서드 분리 및 트랜잭션 컨벤션 통일` | Completed | PoolSelectionCriteria 통합, @Transactional(readOnly=true) 수정 |
| 6 | infra.ai.dto 패키지 정리 | `[BE] refactor: infra.ai.dto 프로바이더별 패키지 분리` | Completed | claude/, openai/ 하위 패키지 분리 |

## 리팩토링 안전 규칙

> **테스트 없는 코드는 리팩토링하지 않는다.**
> 테스트가 없으면 반드시 테스트를 먼저 작성한 뒤 리팩토링한다.

### 테스트 미작성 엔티티 (Wave 1 선행 조건)

| 엔티티 | 테스트 작성 | 상태 |
|--------|-----------|------|
| Question | QuestionTest | Completed |
| QuestionAnswer | QuestionAnswerTest | Completed |
| QuestionSetFeedback | QuestionSetFeedbackTest | Completed |
| TimestampFeedback | TimestampFeedbackTest | Completed |

## 진행 로그

### 2026-04-13
- 코드베이스 전수 분석 완료 (190개 클래스)
- 이슈 분류: 패키지 구조 4건 (A1~A4), 서비스 품질 7건 (B1~B7)
- 계획 문서 작성 완료: requirements.md + plan-01~06 + progress.md
- Wave 1~6 전체 구현 완료
  - 선행 엔티티 테스트 4개 작성
  - questionset → question/analysis/feedback 도메인 분리 (ErrorCode 분리 포함)
  - questionpool → interview/generation/pool 병합, tts → infra/tts 이동
  - TimestampFeedbackMapper 생성, QuestionSet 위임 메서드, buildQuestionSet 팩토리 추출
  - InterviewDeletionService 추출 (의존성 9→5), saveFeedback 55줄→~20줄, Feature Envy 제거
  - PoolSelectionCriteria record 통합 (6개→2개), @Transactional 컨벤션 수정
  - infra.ai.dto → claude/, openai/ 하위 패키지 분리
- `./gradlew test` 전체 통과
