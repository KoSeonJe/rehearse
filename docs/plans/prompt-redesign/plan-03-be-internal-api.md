# Phase 3: [BE] Internal API 확장 (Lambda용)

> **상태**: TODO
> **브랜치**: `feat/internal-api-interview-context`
> **PR**: PR-3 → develop
> **의존**: Phase 1 (PR-1 머지 후)
> **병렬**: Phase 2와 동시 진행 가능 `[parallel]`

---

## 개요

Lambda(analysis)가 verbal_analyzer에서 Position × TechStack별 프롬프트를 조립하려면
interview의 position, techStack, level 정보가 필요하다.

현재 Lambda는 `GET /api/internal/interviews/{id}/question-sets/{qsId}/answers`를 호출하여
답변 데이터를 가져오는데, 이 응답에 interview context를 추가하는 것이 가장 비침습적이다.

---

## Task 3-1: AnswersResponse에 interview context 추가

- **Implement**: `backend`
- **Review**: `code-reviewer`

### 파일 및 변경 사항

#### 1. AnswersResponse에 필드 추가

수정: `backend/src/main/java/com/rehearse/api/domain/questionset/dto/AnswersResponse.java`

```java
@Getter
@Builder
public class AnswersResponse {

    private final String analysisStatus;
    private final List<AnswerResponse> answers;

    // 추가: Lambda에서 프롬프트 조립에 사용
    private final String position;      // "BACKEND", "FRONTEND" 등
    private final String techStack;     // "JAVA_SPRING" 등 (nullable → null이면 기본 스택)
    private final String level;         // "JUNIOR", "MID", "SENIOR"
}
```

#### 2. Controller에서 InterviewFinder를 통해 interview 조회

> **[H3 수정]** `InternalQuestionSetService`에 `InterviewRepository` 의존을 추가하면 questionset→interview 교차 도메인 의존이 생긴다.
> 이미 존재하는 `InterviewFinder` (`backend/.../interview/service/InterviewFinder.java`)를 Controller에 직접 주입하여 도메인 경계를 유지한다.

수정: `backend/src/main/java/com/rehearse/api/domain/questionset/controller/InternalQuestionSetController.java`

```java
@RestController
@RequestMapping("/api/internal/interviews/{interviewId}/question-sets/{questionSetId}")
@RequiredArgsConstructor
public class InternalQuestionSetController {

    private final InternalQuestionSetService internalQuestionSetService;
    private final InterviewFinder interviewFinder;  // [H3] 추가 — 도메인 경계 유지

    @GetMapping("/answers")
    public ResponseEntity<ApiResponse<AnswersResponse>> getAnswers(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId) {

        QuestionSet questionSet = internalQuestionSetService.getQuestionSet(questionSetId);
        Interview interview = interviewFinder.findById(interviewId);  // [H3] InterviewFinder 사용

        List<AnswerResponse> answers = internalQuestionSetService.getAnswers(questionSetId).stream()
                .map(AnswerResponse::from)
                .toList();

        AnswersResponse response = AnswersResponse.builder()
                .analysisStatus(questionSet.getAnalysisStatus().name())
                .answers(answers)
                .position(interview.getPosition().name())
                .techStack(interview.getTechStack() != null
                    ? interview.getTechStack().name() : null)
                .level(interview.getLevel().name())
                .build();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    // ... 나머지 엔드포인트 변경 없음
}
```

> `InternalQuestionSetService`는 변경하지 않는다. interview 도메인 접근은 `InterviewFinder`를 통해 Controller에서 직접 수행.

---

## API 응답 예시

### 변경 전

```json
{
  "status": "OK",
  "data": {
    "analysisStatus": "STARTED",
    "answers": [
      {
        "questionId": 1,
        "questionContent": "JPA N+1 문제를 설명해주세요",
        "answerText": "...",
        "modelAnswer": "..."
      }
    ]
  }
}
```

### 변경 후

```json
{
  "status": "OK",
  "data": {
    "analysisStatus": "STARTED",
    "position": "BACKEND",
    "techStack": "JAVA_SPRING",
    "level": "JUNIOR",
    "answers": [
      {
        "questionId": 1,
        "questionContent": "JPA N+1 문제를 설명해주세요",
        "answerText": "...",
        "modelAnswer": "..."
      }
    ]
  }
}
```

> `techStack`이 null인 기존 데이터: Lambda에서 null이면 Position 기본 스택 매핑 (Phase 4에서 처리)

---

## 검증

### 단위 테스트

수정: `backend/src/test/java/com/rehearse/api/domain/questionset/controller/InternalQuestionSetControllerTest.java`
- getAnswers 응답에 position, techStack, level 필드 포함 확인
- techStack=null인 interview에서도 정상 응답 (null 값으로 반환)

수정: `backend/src/test/java/com/rehearse/api/domain/questionset/service/InternalQuestionSetServiceTest.java`
- getInterview 메서드 테스트

### 수동 검증

```bash
# 로컬에서 API 호출
curl http://localhost:8080/api/internal/interviews/1/question-sets/1/answers | jq .
# → position, techStack, level 필드 확인
```

### CI

```bash
cd backend && ./gradlew test
```
