# Task 07 — StandardFollowUpPolicy RESUME_OPENER skip 분기

> **위치**: `tasks/p2-be-07-standard-policy.md`
> **답하는 질문**: RESUME_OPENER 직후 follow-up 어떻게 skip?

---

## 목적

`StandardFollowUpPolicy.assertCanContinue` 에 분기 추가 — current main = `RESUME_OPENER` 이면 follow-up 생성 skip (응답 = `aiSkip=true` reason="opener"). `RESUME_MAIN` 이면 follow-up 1회까지 허용. `ResumeTrackPolicy` no-op 정책 폐기 = 표준 정책 단일화.

## 에이전트

- **구현**: `backend` — StandardFollowUpPolicy 분기 추가 + ResumeTrackPolicy 폐기 + InterviewTurnPolicyResolver 폐기
- **리뷰**: `code-reviewer-backend` — 정책 시그니처 / 호출자 정합

## 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/interview/policy/StandardFollowUpPolicy.java` — `assertCanContinue` 분기 추가
- `backend/src/main/java/com/rehearse/api/domain/interview/policy/ResumeTrackPolicy.java` — 파일 삭제
- `backend/src/main/java/com/rehearse/api/domain/interview/policy/InterviewTurnPolicyResolver.java` — 파일 삭제 (단일 정책 = resolver 불요)
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` — `StandardFollowUpPolicy` 직접 주입 (resolver 경유 X)
- `backend/src/test/.../StandardFollowUpPolicyTest.java` — RESUME_OPENER / RESUME_MAIN 분기 회귀

## 핵심 로직

```java
public class StandardFollowUpPolicy {

    public void assertCanContinue(Question currentMain) {
        if (currentMain.getQuestionType() == QuestionType.RESUME_OPENER) {
            throw new FollowUpSkipException("opener");  // FollowUpService 가 aiSkip 응답 변환
        }
        // RESUME_MAIN / TECH_MAIN / BEHAVIORAL_MAIN = follow-up 1회 허용 (기존 룰 유지)
    }
}
```

`FollowUpSkipException` = 기존 `recommendedNextAction.SKIP` 분기와 동일 응답 schema 재사용 (`FollowUpResponse.aiSkip=true`).

## 의존
- 선행 Task: 01 (QuestionType `RESUME_OPENER`), 06 (FollowUpService resolver 의존 정리)
- 외부: 없음

## 테스트 케이스
- [ ] `RESUME_OPENER` current main 입력 시 `FollowUpSkipException("opener")` throw
- [ ] `RESUME_MAIN` current main 입력 시 통과 (follow-up 1회 허용)
- [ ] `TECH_MAIN` / `BEHAVIORAL_MAIN` 회귀 통과
- [ ] grep `ResumeTrackPolicy` / `InterviewTurnPolicyResolver` 잔존 0

## 완료 기준
- [ ] 분기 추가 + 폐기 클래스 2개 삭제
- [ ] StandardFollowUpPolicy 회귀 통과
- [ ] code-reviewer-backend 실행

## 커밋 메시지

```
refactor(BE): StandardFollowUpPolicy RESUME_OPENER skip 분기 + 정책 단일화
```

## 비고

`FollowUpSkipException` 시그니처 = 기존 패턴 grep 후 정합. 신규 exception 도입 vs 기존 재사용은 backend agent 단 결정.
