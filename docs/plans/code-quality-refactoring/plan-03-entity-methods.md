# Plan 03: 도메인 엔티티 팩토리/위임 메서드 추가

> 상태: Draft
> 작성일: 2026-04-13

## Why

Wave 4에서 서비스의 로직을 엔티티로 위임하려면, 먼저 엔티티에 수용할 메서드가 있어야 한다. 이 Wave는 서비스 리팩토링의 기반을 마련하는 단계이다.

## 전제조건: 테스트 확인

| 대상 | 테스트 | 상태 | 조치 |
|------|--------|------|------|
| TimestampFeedback (entity) | — | ❌ 없음 | **Plan 01에서 작성 완료 전제** |
| QuestionSet (entity) | QuestionSetTest | ✅ | — |
| QuestionGenerationService | QuestionGenerationServiceTest | ✅ | — |

TimestampFeedback 테스트는 Plan 01에서 작성됨. 미작성 시 이 Wave 시작 전에 반드시 작성한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `domain/feedback/service/TimestampFeedbackMapper.java` | **신규** — InternalQuestionSetService의 빌더 체인 + serializeCommentBlock() + toJson() 흡수 |
| `domain/questionset/entity/QuestionSet.java` | updateStreamingS3Key(), getFileS3Key() 위임 메서드 추가 |
| `domain/interview/generation/service/QuestionGenerationService.java` | buildQuestionSet() 공통 팩토리 private 메서드 추출 |

## 상세

### Task 3.1: TimestampFeedbackMapper 생성

```java
@Component
@RequiredArgsConstructor
public class TimestampFeedbackMapper {

    private final ObjectMapper objectMapper;

    public TimestampFeedback toEntity(SaveFeedbackRequest.TimestampFeedbackItem item, Question question) {
        return TimestampFeedback.builder()
                .question(question)
                .startMs(item.getStartMs())
                .endMs(item.getEndMs())
                // ... 20개 필드 매핑
                .verbalComment(serializeCommentBlock(item.getVerbalComment()))
                .fillerWords(toJson(item.getFillerWords()))
                // ...
                .build();
    }

    // serializeCommentBlock(), toJson()을 InternalQuestionSetService에서 이동
}
```

- Entity에 ObjectMapper를 넣으면 CODING_GUIDE 위반 ("Entity must not make external calls")
- `@Component`로 ObjectMapper 주입받는 매퍼가 적절

### Task 3.2: QuestionSet 위임 메서드

```java
// QuestionSet.java에 추가
public void updateStreamingS3Key(String key) {
    if (this.fileMetadata != null) {
        this.fileMetadata.updateStreamingS3Key(key);
    }
}

public String getFileS3Key() {
    return this.fileMetadata != null ? this.fileMetadata.getS3Key() : null;
}
```

- InternalQuestionSetService의 `analysis.getQuestionSet().getFileMetadata().xxx()` Feature Envy 해소 준비

### Task 3.3: QuestionSet/Question 생성 팩토리 추출

`provideCacheableQuestions()`과 `provideFreshQuestions()`에서 QuestionSet+Question 빌더 코드가 중복. 공통 private 메서드 추출:

```java
private QuestionSet buildQuestionSet(QuestionSetCategory category, String questionText,
                                      String ttsText, String modelAnswer,
                                      String referenceType, FeedbackPerspective perspective,
                                      QuestionPool poolRef) {
    QuestionSet qs = QuestionSet.builder()
            .category(category).orderIndex(0).build();
    Question question = Question.builder()
            .questionType(QuestionType.MAIN)
            .questionText(questionText).ttsText(ttsText)
            .modelAnswer(modelAnswer)
            .referenceType(parseReferenceType(referenceType))
            .feedbackPerspective(perspective)
            .orderIndex(0).questionPool(poolRef).build();
    qs.addQuestion(question);
    return qs;
}
```

## 담당 에이전트

- Implement (테스트): `test-engineer` — TimestampFeedbackMapper 단위 테스트, QuestionSet 위임 메서드 테스트
- Implement (코드): `backend` — 매퍼 생성, 위임 메서드, 팩토리 추출
- Review: `architect-reviewer` — Entity 순수성, Law of Demeter, 책임 배치

## 검증

- `./gradlew test` — 전체 통과
- 신규 테스트: `TimestampFeedbackMapperTest`, `QuestionSetDelegationTest` 통과
- 기존 테스트 영향 없음 확인 (순수 추가)
- `progress.md` 상태 업데이트 (Task 3 → Completed)
