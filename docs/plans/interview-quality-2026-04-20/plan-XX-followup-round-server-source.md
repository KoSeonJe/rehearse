# Plan XX: 꼬리질문 라운드 한도 게이트 BE 일원화

> 상태: In Progress (2026-04-25)
> 작성일: 2026-04-25
> 선행: plan-00f (`InterviewTurnPolicy` Strategy 도입)
> 후행: 없음 (FE 클라이언트 단일화 완료 후 종료)

## Why

PR #348 (`92ec0ea`) 가 BE 에 트랙별 `InterviewTurnPolicy` Strategy 를 도입했고, `FollowUpTransactionHandler.loadFollowUpContext:41` 에서 `assertCanContinue` 로 한도 초과 시 `MAX_FOLLOWUP_EXCEEDED` 400 을 던진다. 그러나 **FE 가 별도로 `MAX_FOLLOWUP_ROUNDS = 2` 상수와 `followUpRound` 카운터를 운영**하면서 다음 결함이 나타났다.

1. **트랙별 한도 무력화** — `ResumeTrackPolicy.HARD_TURN_CAP = 7` 인데 FE 가 3번째 호출부터 사전 차단 → BE 정책이 의미 없음
2. **이중 진실 소스** — `application.yml` 의 `rehearse.interview.policy.standard.max-follow-up-rounds` 만 변경해도 FE 상수가 어긋남 → 정책 변경 = FE 재배포
3. **클라이언트 우회 가능성** — devtools 로 `followUpRound = 0` 강제 시 BE 가 막아주긴 하나, 클라이언트 정책이 추가될 때마다(모바일 등) 재구현 필요
4. **PR #350 의 `presentToUser` 신호와 책임 비대칭** — BE→FE boolean echo 패턴은 BE 가 보내는데, 라운드 카운트만 FE 가 전담

**해결**: BE 가 매 응답에 `followUpExhausted: boolean` 신호를 echo. FE 는 그 신호로만 게이트. `MAX_FOLLOWUP_ROUNDS` 상수와 `followUpRound` 카운트 로직 제거. plan-00f 의 Strategy 인프라 그대로 재활용.

## 생성/수정 파일

### Backend
| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/domain/interview/policy/InterviewTurnPolicy.java` | **수정**. `int getMaxFollowUpRounds()` + `default boolean isExhausted(int)` 추가 |
| `backend/src/main/java/com/rehearse/api/domain/interview/policy/StandardFollowUpPolicy.java` | **수정**. `getMaxFollowUpRounds()` 구현 (`maxRounds` 노출) |
| `backend/src/main/java/com/rehearse/api/domain/interview/policy/ResumeTrackPolicy.java` | **수정**. `getMaxFollowUpRounds()` 구현 (`HARD_TURN_CAP=7`) |
| `backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpResponse.java` | **수정**. `followUpExhausted` 필드 + `@Builder(toBuilder=true)` |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpTransactionHandler.java` | **수정**. `saveFollowUpResult` 반환 타입 → `FollowUpSaveResult(Question, int newFollowUpCount)` record |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` | **수정**. ANSWER 경로는 `policy.isExhausted(newCount)` 계산, intent branch / aiSkip 은 `false` 로 데코레이트 |
| `backend/src/test/.../policy/StandardFollowUpPolicyTest.java` | **수정**. `getMaxFollowUpRounds`/`isExhausted` 검증 추가 |
| `backend/src/test/.../policy/ResumeTrackPolicySkeletonTest.java` | **수정**. 동일 |
| `backend/src/test/.../service/FollowUpServiceTest.java` | **수정**. `followUpExhausted` 응답 필드 검증 추가 |
| `backend/src/test/.../service/FollowUpServiceIntentBranchTest.java` | **수정**. intent branch / aiSkip 응답에 `followUpExhausted=false` 검증 |
| `backend/src/test/.../service/FollowUpTransactionHandlerTest.java` | **수정**. `saveFollowUpResult` 의 `newFollowUpCount` 검증 |

### Frontend
| 파일 | 작업 |
|------|------|
| `frontend/src/types/interview.ts` | **수정**. `FollowUpResponse.followUpExhausted?: boolean` |
| `frontend/src/stores/interview-store.ts` | **수정**. `MAX_FOLLOWUP_ROUNDS` 상수 + `followUpRound` 상태 제거. `followUpExhausted` 상태 도입 |
| `frontend/src/hooks/use-answer-flow.ts` | **수정**. 게이트를 `!state.followUpExhausted` 로 변경 |

## 호환성 / 배포 순서

1. **BE PR 머지·배포 먼저**. `followUpExhausted` 항상 응답에 포함. 기존 FE 는 필드 무시 → 회귀 없음
2. **FE PR 머지·배포**. `MAX_FOLLOWUP_ROUNDS` 제거, BE 신호 기반 게이트로 전환
3. 갭 동안 양쪽 정책이 일치하면 동작 차이 없음 (CS 트랙 max=2 동일). RESUME 트랙은 FE 배포 후에야 한도 7 활용 가능

## 검증

1. **BE 단위 테스트**: `./gradlew test --tests "*Policy*Test"`, `./gradlew test --tests "FollowUpService*"`, `./gradlew test --tests "FollowUpTransactionHandlerTest"` 그린
2. **수동 시나리오 (CS, max=2)**: 메인Q → 답변 → 꼬리Q1 (`followUpExhausted=false`) → 답변 → 꼬리Q2 (`followUpExhausted=true`) → 답변 → 다음 메인Q (API 호출 없음 확인)
3. **수동 시나리오 (RESUME, max=7)**: 꼬리질문 7회까지 진행 후 8번째 막힘 검증
4. **회귀**: 정상 답변, AI 자체 skip, 의도 분기 3종(OFF_TOPIC/CLARIFY/GIVE_UP) 모두 정상 동작
