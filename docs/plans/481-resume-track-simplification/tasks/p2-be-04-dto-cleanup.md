# Task 04 — FollowUp DTO `selectedAnswerFeedbackPerspective` 필드 제거

> **위치**: `tasks/p2-be-04-dto-cleanup.md`
> **답하는 질문**: FollowUp 요청/응답/AI DTO 어떻게 정리?

---

## 목적

`FollowUpResponse` / `FollowUpRequest.FollowUpExchange` / `GeneratedFollowUp` 의 `selectedAnswerFeedbackPerspective` 필드 제거. FE grep 0 확인됨 — BE 단독 안전 제거.

## 에이전트

- **구현**: `backend` — DTO record 필드 제거 + 응답 빌더 / mapper 갱신
- **리뷰**: `code-reviewer-backend` — DTO 시그니처 / API contract 정합

## 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpResponse.java:24` — `selectedAnswerFeedbackPerspective` 필드 제거
- `backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpRequest.java:36` — `FollowUpExchange.selectedAnswerFeedbackPerspective` 필드 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedFollowUp.java:17` — `selectedAnswerFeedbackPerspective` 필드 제거
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` — 응답 빌더에서 perspective 필드 매핑 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/...` — `GeneratedFollowUp` 매핑 사용처 갱신
- `backend/src/test/.../FollowUpResponseTest.java` / `FollowUpRequestTest.java` — fixture 갱신

## 핵심 로직

```java
// Before
public record FollowUpResponse(
    Long questionId, String question, QuestionType type,
    String selectedAnswerFeedbackPerspective,  // 제거
    ...
) { }

// After
public record FollowUpResponse(
    Long questionId, String question, QuestionType type,
    ...
) { }
```

응답 schema = tech-spec §API Contract After (L424-430) 정합.

## 의존
- 선행 Task: 03 (`GeneratedFollowUp` LLM prompt schema 변경 정합)
- 외부: 없음

## 테스트 케이스
- [ ] `FollowUpResponse` record 5 필드 (perspective 부재)
- [ ] `FollowUpRequest.FollowUpExchange` perspective 부재
- [ ] `GeneratedFollowUp` deserialization — `selected_perspective` key 부재 응답 정상 처리
- [ ] FE grep 0 재확인 (`frontend/src` 기준 `selectedAnswerFeedbackPerspective`)

## 완료 기준
- [ ] DTO 3개 + 빌더 모두 정리
- [ ] grep `selectedAnswerFeedbackPerspective` 잔존 0 (BE + FE 둘 다)
- [ ] FollowUp 응답 schema = tech-spec §API Contract After 일치
- [ ] code-reviewer-backend 실행

## 커밋 메시지

```
refactor(BE): FollowUp DTO selectedAnswerFeedbackPerspective 필드 제거
```

## 비고

FE 송신 측 (`FollowUpRequest`) = grep 0 = BE 가 무시해도 안전. schema 정합 위해 BE record 도 제거.
