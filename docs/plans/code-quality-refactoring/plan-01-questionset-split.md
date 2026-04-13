# Plan 01: questionset 도메인 독립 분리

> 상태: Draft
> 작성일: 2026-04-13

## Why

questionset 도메인이 38개 클래스로 비대하다. question/answer, analysis, feedback 3개 관심사가 한 패키지에 혼재되어 있어 코드 탐색과 책임 파악이 어렵다. 기존 도메인과 동일한 flat 구조의 독립 도메인으로 분리하여 일관성과 가독성을 확보한다.

## 전제조건: 테스트 작성

아래 엔티티는 테스트가 없으므로 **패키지 이동 전 테스트를 먼저 작성**한다.

| 엔티티 | 필요한 테스트 | 테스트 내용 |
|--------|-------------|-----------|
| Question | QuestionTest | assignQuestionSet() 동작, 빌더 생성 검증 |
| QuestionAnswer | QuestionAnswerTest | 빌더 생성, question 연관 검증 |
| QuestionSetFeedback | QuestionSetFeedbackTest | addTimestampFeedback(), getTimestampFeedbacks() 불변 반환 |
| TimestampFeedback | TimestampFeedbackTest | assignQuestionSetFeedback(), 빌더 20개 필드 검증 |

## 생성/수정 파일

### 신규 생성 — question 도메인

| 파일 | 작업 |
|------|------|
| `domain/question/entity/Question.java` | questionset에서 이동 |
| `domain/question/entity/QuestionAnswer.java` | questionset에서 이동 |
| `domain/question/entity/QuestionType.java` | questionset에서 이동 |
| `domain/question/entity/ReferenceType.java` | questionset에서 이동 |
| `domain/question/dto/QuestionDetailResponse.java` | questionset에서 이동 |
| `domain/question/dto/AnswerResponse.java` | questionset에서 이동 |
| `domain/question/dto/AnswersResponse.java` | questionset에서 이동 |
| `domain/question/dto/QuestionsWithAnswersResponse.java` | questionset에서 이동 |
| `domain/question/dto/SaveAnswersRequest.java` | questionset에서 이동 |
| `domain/question/repository/QuestionRepository.java` | questionset에서 이동 |
| `domain/question/repository/QuestionAnswerRepository.java` | questionset에서 이동 |
| `domain/question/exception/QuestionErrorCode.java` | QuestionSetErrorCode에서 question 관련 코드 분리 |

### 신규 생성 — analysis 도메인

| 파일 | 작업 |
|------|------|
| `domain/analysis/entity/QuestionSetAnalysis.java` | questionset에서 이동 |
| `domain/analysis/entity/AnalysisStatus.java` | questionset에서 이동 |
| `domain/analysis/entity/ConvertStatus.java` | questionset에서 이동 |
| `domain/analysis/dto/UpdateConvertStatusRequest.java` | questionset에서 이동 |
| `domain/analysis/repository/QuestionSetAnalysisRepository.java` | questionset에서 이동 |
| `domain/analysis/service/AnalysisScheduler.java` | questionset에서 이동 |
| `domain/analysis/exception/AnalysisErrorCode.java` | QuestionSetErrorCode에서 analysis 관련 코드 분리 |

### 신규 생성 — feedback 도메인

| 파일 | 작업 |
|------|------|
| `domain/feedback/entity/QuestionSetFeedback.java` | questionset에서 이동 |
| `domain/feedback/entity/TimestampFeedback.java` | questionset에서 이동 |
| `domain/feedback/entity/FeedbackPerspective.java` | questionset에서 이동 |
| `domain/feedback/dto/QuestionSetFeedbackResponse.java` | questionset에서 이동 |
| `domain/feedback/dto/TimestampFeedbackResponse.java` | questionset에서 이동 |
| `domain/feedback/dto/SaveFeedbackRequest.java` | questionset에서 이동 |
| `domain/feedback/repository/QuestionSetFeedbackRepository.java` | questionset에서 이동 |
| `domain/feedback/repository/TimestampFeedbackRepository.java` | questionset에서 이동 |
| `domain/feedback/exception/FeedbackErrorCode.java` | QuestionSetErrorCode에서 feedback 관련 코드 분리 |

### 수정 — questionset (core만 잔류)

| 파일 | 작업 |
|------|------|
| `domain/questionset/exception/QuestionSetErrorCode.java` | 분리된 에러 코드 제거, core 코드만 잔류 |

### 수정 — import 업데이트 대상

| 파일 | 작업 |
|------|------|
| `domain/interview/service/QuestionGenerationService.java` | question 도메인 import 변경 |
| `domain/interview/service/FollowUpTransactionHandler.java` | question 도메인 import 변경 |
| `domain/interview/service/FollowUpService.java` | question 도메인 import 변경 |
| `domain/interview/service/InterviewService.java` | analysis/feedback repository import 변경 |
| `domain/interview/service/InterviewCompletionService.java` | analysis 도메인 import 변경 |
| `domain/interview/dto/FollowUpContext.java` | question 도메인 import 변경 |
| `domain/questionset/service/QuestionSetService.java` | question/analysis/feedback import 변경 |
| `domain/questionset/service/InternalQuestionSetService.java` | question/analysis/feedback import 변경 |
| `domain/questionset/controller/QuestionSetController.java` | question 도메인 import 변경 |
| `domain/questionset/controller/InternalQuestionSetController.java` | analysis import 변경 |
| `domain/questionset/dto/QuestionSetResponse.java` | question 도메인 import 변경 |
| `domain/reviewbookmark/service/ReviewBookmarkService.java` | feedback import 변경 |
| `domain/reviewbookmark/dto/ReviewBookmarkListItem.java` | question import 변경 |
| `domain/reviewbookmark/entity/ReviewBookmark.java` | feedback import 변경 |
| `domain/questionpool/service/CacheableQuestionProvider.java` | question import 변경 |
| `infra/ai/prompt/FollowUpPromptBuilder.java` | question import 변경 |
| `infra/ai/dto/FollowUpGenerationRequest.java` | question import 변경 |
| 테스트 파일 전체 | 동일하게 import 업데이트 |

## 상세

### ErrorCode 분리 기준

`QuestionSetErrorCode`에서 아래 기준으로 분리:

- **QuestionErrorCode**: question/answer 관련 에러 (해당 코드가 있으면 분리)
- **AnalysisErrorCode**: `INVALID_ANALYSIS_STATUS_TRANSITION`, `INVALID_CONVERT_STATUS_TRANSITION` 등
- **FeedbackErrorCode**: `FEEDBACK_NOT_FOUND` 등
- **QuestionSetErrorCode (잔류)**: `NOT_FOUND`, `FILE_NOT_FOUND` 등 core 에러

### 주의사항

- 패키지 이동만 수행, **로직 변경 없음**
- JPA entity 스캔 경로가 `com.rehearse.api` 하위 전체이므로 패키지 이동 시 별도 설정 불필요
- `@ComponentScan` 범위도 루트 패키지 기준이라 영향 없음

## 담당 에이전트

- Implement (테스트 작성): `test-engineer` — Question, QuestionAnswer, QuestionSetFeedback, TimestampFeedback 엔티티 테스트
- Implement (패키지 분리): `backend` — 도메인 생성, 파일 이동, import 업데이트
- Review: `code-reviewer` — import 누락 검증, 컴파일 확인
- Review: `architect-reviewer` — 도메인 경계 적절성

## 검증

- `./gradlew compileJava` — 컴파일 에러 없음
- `./gradlew test` — 전체 테스트 통과 (기존 + 신규 엔티티 테스트)
- questionset 패키지에 core 클래스만 잔류 확인 (~12개)
- question, analysis, feedback 각 도메인에 적절한 클래스 배치 확인
- `progress.md` 상태 업데이트 (Task 1 → Completed)
