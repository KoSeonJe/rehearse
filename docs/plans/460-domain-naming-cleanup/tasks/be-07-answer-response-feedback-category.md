# BE Task 07 — `AnswerResponse.feedbackPerspective` → `rubricCategory` (JSON 키 포함)

## 목적

`AnswerResponse.feedbackPerspective` 필드 + JSON 응답 키 + Builder 호출 → `rubricCategory`. (`from()` 시그니처는 Task 3 에서 `InterviewType` 으로 이미 변경됨.)

## 변경 파일

### 정의 변경
- `backend/src/main/java/com/rehearse/api/domain/question/dto/AnswerResponse.java`
  - `private final String feedbackPerspective;` (line 17) → `private final String rubricCategory;`
  - `from()` 본문 (line 25) `.feedbackPerspective()` 메서드 호출 → `.rubricCategory()` (Task 2 에서 `QuestionType.rubricCategory()` 메서드명 변경 완료 가정)
  - Builder 호출 (line 34) `.feedbackPerspective(perspective)` → `.rubricCategory(perspective)` (변수명 `perspective` 자체는 잔존 가능 — Phase 2 후보. 단어 일관성 위해 본 task 에서 `category` 로 변경 권장).

### 호출처 (cascade)
- `backend/src/main/java/com/rehearse/api/domain/question/service/InternalQuestionSetService.java:88`
  - `AnswerResponse.from(answer, questionSet.getCategory())` — 시그니처 자동 호환 (Task 3 후 `getCategory() : InterviewType`).

### 테스트 갱신
- `backend/src/test/java/com/rehearse/api/domain/question/dto/AnswerResponseTest.java`
  - 필드 단언 `feedbackPerspective` → `rubricCategory`
  - JSON 직렬화 단언 (있다면) `feedbackPerspective` 키 → `rubricCategory` 키
- `InternalQuestionSetServiceTest` (관련 fixture 갱신)

## 핵심 변경 (요지)

- 필드 타입 = `String` 유지.
- JSON 키 변경 = FE 동시 갱신 (API contract).
- 변수명 `perspective` (`AnswerResponse.from` 내부 지역변수) = 단어 일관성 위해 `category` 권장. **단**, 본 작업 simplicity rule 상 사용자 명시 영역만 변경 → 필드명(`rubricCategory`) / JSON 키(`rubricCategory`)만 처리하고 지역변수는 잔존 허용.

## API Contract (BE+FE 동시)

```jsonc
// 변경 전
{ "feedbackPerspective": "TECHNICAL", ... }
// 변경 후
{ "rubricCategory": "TECHNICAL", ... }
```

## 테스트

- 카테고리: Domain Unit (`AnswerResponseTest`).
- 실행: `./gradlew test --tests "AnswerResponseTest" "InternalQuestionSetServiceTest"`

## 완료 기준

- [ ] `grep -n "feedbackPerspective" backend/src/main/java/com/rehearse/api/domain/question/dto/AnswerResponse.java` = 0
- [ ] JSON 응답 키 = `rubricCategory` (직렬화 검증 — 가능한 경우 테스트로)
- [ ] `AnswerResponseTest` GREEN
- [ ] `InternalQuestionSetServiceTest` GREEN

## 의존

- 선행: T2 (`RubricCategory` rename + `QuestionType.rubricCategory()` 메서드명), T3 (`AnswerResponse.from()` 시그니처 = `InterviewType`).
- 후행: 없음.

## 커밋

```
refactor(BE): AnswerResponse.feedbackPerspective → rubricCategory (필드 + JSON 키 + Builder)
```
