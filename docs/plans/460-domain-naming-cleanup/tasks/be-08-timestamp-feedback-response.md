# BE Task 08 — `TimestampFeedbackResponse$TechnicalFeedback.perspective` → `rubricCategory` (inner 필드 + JSON 키)

## 목적

`TimestampFeedbackResponse$TechnicalFeedback` (inner static class) 의 `perspective` 필드 + JSON 응답 키 + 매핑 호출 → `rubricCategory`.

> 주의: top-level 필드 X. `TimestampFeedbackResponse.technicalFeedback` 객체 **내부** inner 필드. JSON 경로 = `technicalFeedback.perspective` → `technicalFeedback.rubricCategory`.

## 변경 파일

### 정의 변경
- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/TimestampFeedbackResponse.java`
  - inner class `TechnicalFeedback` (line 75): `private final String perspective;` (line 76) → `private final String rubricCategory;`
  - 매핑 식 line 155-156: `question.getQuestionType().feedbackPerspective().name()` → `question.getQuestionType().rubricCategory().name()` (Task 2 에서 `QuestionType.feedbackPerspective()` getter rename 완료 전제)
  - Builder 호출 line 170 `.perspective(perspective)` → `.rubricCategory(perspective)` (지역변수 `perspective` 명은 유지 — 변수명 정리 = Phase 2 비스코프)

### 테스트 갱신
- `backend/src/test/java/com/rehearse/api/domain/feedback/service/TimestampFeedbackBatchTest.java` — JSON 단언 (`technicalFeedback.perspective` → `technicalFeedback.rubricCategory`) 갱신
- `backend/src/test/java/com/rehearse/api/domain/question/entity/TimestampFeedbackTest.java` — 갱신 (해당 단언 있는 경우)

## 핵심 변경 (요지)

- inner class 필드 타입 = `String` (enum 값 `.name()` 직렬화) 유지.
- JSON 키 변경 = FE 동시 갱신 (FE 측 `TechnicalFeedback.perspective` 타입 정의 동기화).

## API Contract (BE+FE 동시)

```jsonc
// 변경 전
{
  "technicalFeedback": { "perspective": "EXPERIENCE", "rubricId": "...", ... }
}
// 변경 후
{
  "technicalFeedback": { "rubricCategory": "EXPERIENCE", "rubricId": "...", ... }
}
```

## 테스트

- 카테고리: Service Integration (`TimestampFeedbackBatchTest`) + Domain Unit.
- 실행: `./gradlew test --tests "Timestamp*"`

## 완료 기준

- [ ] `grep -n "perspective" backend/src/main/java/com/rehearse/api/domain/feedback/dto/TimestampFeedbackResponse.java` = 매핑 식 지역변수 `String perspective = ...` 1건 외 0 (inner field 명 / Builder 호출 모두 `rubricCategory`)
- [ ] JSON 응답 = `technicalFeedback.rubricCategory` 키 직렬화
- [ ] `TimestampFeedbackBatchTest` GREEN

## 의존

- 선행: T2 (`RubricCategory` + `QuestionType.rubricCategory()` 메서드명 rename).
- 후행: 없음. T7 과 병렬 가능 [parallel].

## 커밋

```
refactor(BE): TimestampFeedbackResponse$TechnicalFeedback.perspective → rubricCategory (inner 필드 + JSON 키)
```
