# BE Task 02 — `FeedbackPerspective` → `RubricCategory` rename

## 목적

`feedback.FeedbackPerspective` (3개 enum: TECHNICAL/BEHAVIORAL/EXPERIENCE) 클래스명을 `RubricCategory` 로 변경. Perspective 단어 제거 (interview 측 `AnswerFeedbackPerspective` 와의 단어 분리 — Task 1 과 한 쌍). 파일 이동: `feedback/entity/` → `feedback/rubric/entity/`.

## 변경 파일

### 신규 / 이름 변경
- `backend/src/main/java/com/rehearse/api/domain/feedback/entity/FeedbackPerspective.java` → `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/entity/RubricCategory.java` (파일 이동 + 클래스명 변경 + 패키지 선언 갱신)

### 임포트 / 사용 갱신 (main)
- `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionType.java`
  - 임포트 + 필드 타입 (line 15, 17, 19) + getter (line 26-27 `feedbackPerspective()` 메서드명) 갱신
  - **Note**: `QuestionType.feedbackPerspective()` 메서드명 변경 = 호출처 cascade. 본 task = 클래스명 rename + 메서드명 일관성 위해 `rubricCategory()` 로 동시 변경.
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/entity/RubricFamily.java`
  - 임포트(`import com.rehearse.api.domain.feedback.rubric.entity.RubricCategory`) + record 컴포넌트 타입 (line 67) 갱신. **필드명 `feedbackPerspective` → `rubricCategory` 는 Task 9 영역**. 본 task 는 타입 임포트만 갱신.
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricLoader.java`
  - 임포트 + 변수 타입 (line 71 `FeedbackPerspective perspective`) 갱신
- `backend/src/main/java/com/rehearse/api/domain/question/dto/AnswerResponse.java`
  - 임포트 갱신만 (필드명 / JSON 키 / from() 시그니처는 Task 7)
- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/TimestampFeedbackResponse.java`
  - 임포트 갱신만 (필드명 / JSON 키는 Task 8)

### 테스트 갱신
- `backend/src/test/java/com/rehearse/api/domain/feedback/rubric/RubricLoaderTest.java`
- `backend/src/test/java/com/rehearse/api/domain/question/dto/AnswerResponseTest.java`
- `backend/src/test/java/com/rehearse/api/domain/question/entity/QuestionTypeTest.java`
- `backend/src/test/java/com/rehearse/api/domain/question/service/StandardTrackQuestionGeneratorTest.java`

## 핵심 변경 (요지)

- 클래스명 변경 (`FeedbackPerspective` → `RubricCategory`) + 파일 이동 (`feedback/entity/` → `feedback/rubric/entity/`) + 패키지 선언 갱신 (`com.rehearse.api.domain.feedback.entity` → `com.rehearse.api.domain.feedback.rubric.entity`).
- enum 값 3개 (`TECHNICAL`, `BEHAVIORAL`, `EXPERIENCE`) **그대로 유지**.
- `QuestionType.feedbackPerspective()` 메서드명 → `rubricCategory()` 로 동시 변경 (단어 일관성). 호출처 = `RubricLoader:71`, `TimestampFeedbackResponse:156`, `AnswerResponse:25` 등.
- IntelliJ Safe Rename 권장.

## 비스코프 (T2 영역에서 절대 미터치)

- `RubricFamily.MappingRule` record 컴포넌트 **필드명** `feedbackPerspective` → `rubricCategory` = Task 9.
- `_mapping.yaml` / `experience-technical-rubric.yaml` 키 = Task 9.
- `AnswerResponse.feedbackPerspective` 필드 / JSON 키 = Task 7.
- `TimestampFeedbackResponse$TechnicalFeedback.perspective` inner 필드 / JSON 키 = Task 8.
- enum 값 자체 (`EXPERIENCE` 등) = Phase 2.

## 테스트

- 임포트 / 메서드명 갱신만 — 단언 변경 0.
- 카테고리: Domain Unit (`QuestionTypeTest`, `AnswerResponseTest`) + Service Integration (`RubricLoaderTest`, `StandardTrackQuestionGeneratorTest`).
- 실행: `./gradlew test --tests "com.rehearse.api.domain.feedback.*" "com.rehearse.api.domain.question.*"`

## 완료 기준

- [ ] `grep -rn "import com.rehearse.api.domain.feedback.entity.FeedbackPerspective" backend/src/main/java` = 0
- [ ] `grep -rn "import com.rehearse.api.domain.feedback.rubric.entity.RubricCategory" backend/src/main/java` ≥ 6
- [ ] `grep -rn "import com.rehearse.api.domain.feedback.entity.FeedbackPerspective" backend/src/main/java` = 0
- [ ] `QuestionType.rubricCategory()` 메서드명 + 호출처 갱신 GREEN
- [ ] `./gradlew compileJava` GREEN
- [ ] 위 4 테스트 클래스 GREEN

## 의존

- 선행: 없음. T1/T5 와 병렬 가능.
- 후행: T3 (`RubricFamily` 시그니처 후속), T7 (`AnswerResponse`), T8 (`TimestampFeedbackResponse`), T9 (YAML 키).

## 커밋

```
refactor(BE): FeedbackPerspective → RubricCategory 클래스 rename + feedback/rubric/entity 이동
```
