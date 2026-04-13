# Plan 04: 서비스 God Class 분해 및 도메인 로직 위임

> 상태: Draft
> 작성일: 2026-04-13

## Why

InterviewService(의존성 9개), InternalQuestionSetService(8개)가 God Class이다. 삭제 책임 분리, Transaction Script 제거, Feature Envy 해소를 통해 서비스 클래스의 책임을 적정 수준으로 줄인다.

## 전제조건: 테스트 확인

| 대상 | 테스트 | 상태 |
|------|--------|------|
| InterviewService | InterviewServiceTest | ✅ |
| InternalQuestionSetService | InternalQuestionSetServiceTest | ✅ |
| QuestionSetService | QuestionSetServiceTest | ✅ |

모든 대상에 테스트 존재. 즉시 리팩토링 가능.

**신규 서비스 생성 시**: `InterviewDeletionService`에 대한 테스트도 함께 작성한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `domain/interview/service/InterviewDeletionService.java` | **신규** — deleteInterview() + 5개 리포지토리 |
| `domain/interview/service/InterviewService.java` | deleteInterview() 제거, 4개 리포지토리 의존성 제거 |
| `domain/interview/controller/InterviewController.java` | InterviewDeletionService 주입 추가 |
| `domain/questionset/service/InternalQuestionSetService.java` | saveFeedback() → TimestampFeedbackMapper 사용, ObjectMapper 제거, Feature Envy 제거 |

## 상세

### Task 4.1: InterviewService에서 삭제 책임 분리

**Before**: InterviewService에 deleteInterview() + 4개 questionset 리포지토리 의존성
```
InterviewService → 9 deps:
  InterviewFinder, InterviewRepository, QuestionSetRepository,
  QuestionSetService, QuestionAnswerRepository, TimestampFeedbackRepository,
  QuestionSetFeedbackRepository, QuestionSetAnalysisRepository,
  ApplicationEventPublisher
```

**After**: InterviewDeletionService로 삭제 로직 분리
```
InterviewService → 5 deps:
  InterviewFinder, InterviewRepository, QuestionSetRepository,
  QuestionSetService, ApplicationEventPublisher

InterviewDeletionService → 6 deps:
  InterviewFinder, InterviewRepository, QuestionSetRepository,
  QuestionAnswerRepository, TimestampFeedbackRepository,
  QuestionSetFeedbackRepository, QuestionSetAnalysisRepository
```

### Task 4.2: InternalQuestionSetService.saveFeedback() 리팩토링

**Before (55줄)**:
```java
// 22줄 빌더 체인 + serializeCommentBlock() + toJson() private 메서드
TimestampFeedback tf = TimestampFeedback.builder()
    .question(question)
    .startMs(item.getStartMs())
    // ... 20개 필드
    .build();
```

**After (~20줄)**:
```java
TimestampFeedback tf = timestampFeedbackMapper.toEntity(item, question);
feedback.addTimestampFeedback(tf);
```

- `serializeCommentBlock()`, `toJson()` private 메서드 제거 (매퍼로 이동됨)
- `ObjectMapper` 의존성 제거 → 의존성 8개 → 7개

### Task 4.3: InternalQuestionSetService Feature Envy 제거

**Before**:
```java
QuestionSet qs = analysis.getQuestionSet();
var file = qs.getFileMetadata();
if (file != null) {
    file.updateStreamingS3Key(request.getStreamingS3Key());
}
```

**After** (Wave 3의 위임 메서드 사용):
```java
analysis.getQuestionSet().updateStreamingS3Key(request.getStreamingS3Key());
```

## 담당 에이전트

- Implement: `backend` — 서비스 추출, 로직 위임
- Implement (테스트): `test-engineer` — InterviewDeletionServiceTest 작성, 기존 테스트 업데이트
- Review: `architect-reviewer` — SRP, 의존성 방향
- Review: `code-reviewer` — 동작 보존, 회귀 없음

## 검증

- `./gradlew test` — 전체 통과
- InterviewService 의존성 수: 9 → 5 확인
- InternalQuestionSetService 의존성 수: 8 → 7 확인
- saveFeedback() 메서드 길이: 55줄 → ~20줄 확인
- `progress.md` 상태 업데이트 (Task 4 → Completed)
