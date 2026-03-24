# Plan 06: 단위/통합 테스트

> 상태: Draft
> 작성일: 2026-03-23

## Why

트랜잭션 분리(plan-02)와 RateLimiter(plan-04)의 정상 동작을 자동화된 테스트로 검증한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/test/.../interview/service/FollowUpTransactionHandlerTest.java` | 신규 — TX 핸들러 단위 테스트 |
| `backend/src/test/.../interview/service/InterviewServiceTest.java` | 수정 — generateFollowUp 테스트 업데이트 |
| `backend/src/test/.../infra/ai/RateLimiterIntegrationTest.java` | 신규 — RateLimiter 통합 테스트 |

## 상세

### FollowUpTransactionHandlerTest (단위 테스트)

- `loadFollowUpContext()` 정상: IN_PROGRESS 면접 → FollowUpContext 반환
- `loadFollowUpContext()` 예외: NOT_IN_PROGRESS → BusinessException
- `loadFollowUpContext()` 예외: QuestionSet 미존재 → BusinessException
- `loadFollowUpContext()` 예외: followUp 라운드 초과 → BusinessException
- `saveFollowUpResult()` 정상: Question 생성 + 저장 확인

### InterviewServiceTest 업데이트

- `generateFollowUp()` 호출 순서 검증: STT → loadFollowUpContext → Claude → saveFollowUpResult
- `FollowUpTransactionHandler`를 mock으로 교체
- 외부 API 실패 시 DB 변경 없음 확인

### RateLimiterIntegrationTest (통합 테스트)

- `@SpringBootTest` + 테스트 전용 설정 (`limitForPeriod=2, limitRefreshPeriod=10s`)
- limit 이내 요청: 정상 통과
- limit 초과 요청: `RequestNotPermitted` 발생
- GlobalExceptionHandler를 통한 429 응답 매핑 확인

## 담당 에이전트

- Implement: `test-engineer` — 테스트 작성
- Review: `code-reviewer` — 테스트 커버리지 적절성

## 검증

- `./gradlew test` 전체 통과
- 신규 테스트 3개 파일 존재 확인
- `progress.md` 상태 업데이트 (Task 6 → Completed)
