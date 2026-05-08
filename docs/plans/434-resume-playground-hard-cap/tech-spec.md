# Tech Spec — RESUME 트랙 PLAYGROUND→INTERROGATION 자동 전환 hard cap

> **작성자**: backend agent (메인 세션 임시 작성)
> **답하는 질문**: 어떻게? 구조 / 데이터 / Trade-off / 검증
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄 미러)

PLAYGROUND→INTERROGATION 전환이 LLM 자가 신호에만 의존 → 무한 워밍업 가능. 누적 턴 수 (opener 포함 3턴) hard cap 도입 → 모든 RESUME 세션 INTERROGATION 진입 보장 + LLM 자가 전환 정상 케이스 회귀 0.

## Evidence

- 현재 구조:
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandler.java:56-114` — `handle()` 안에서 LLM `playground_responder` 호출 후 `evaluateSwitchConditions(result, turnCount+1)` 평가. hard cap 부재.
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeModeTransitionPolicy.java:11-19` — `isHardTimeoutExceeded(...)` 단일 메서드. 전환 정책 일관성 위치.
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java:160-177` — `handlePlayground` 가 `result.switchedToInterrogation()` 시 `runtimeState.transitionTo(INTERROGATION)` + `interrogationHandler.handle(..., null, null, ...)` 즉시 호출. 따라서 PlaygroundTurnResult 의 `question` / `questionId` null 이어도 orchestrator 가 INTERROGATION 결과로 응답 덮어씀 — LLM skip 안전.
  - `backend/src/main/java/com/rehearse/api/domain/interview/entity/InterviewRuntimeState.java:34,49,56` — `playgroundTurns` AtomicInteger. opener 호출 시 1, 매 사용자 답변 처리 시 +1. 동시성 안전 자료구조.
  - `backend/src/main/resources/application.yml:84` — `rehearse.resume-track.playground-max-turns: 3` orphan. 본 작업 wiring 해소.
- 외부 레퍼런스: 없음
- 사용자 발화: "opener + playground 해서 최대 3번" — opener 1턴 + playground 응답 2턴 = 누적 3턴 도달 시 다음 답변에서 INTERROGATION 강제 전환.
- 추정 / 미확인 가정: dev EC2 interview 26 사례 1건 외 운영 표본 부재. cap 발동 빈도 운영 데이터로 사후 검증 필요 (phase 2 트리거).

## Trade-offs

### Option A (채택) — LLM 호출 전 cap 체크 + skip

- 장점: cap 도달 시 LLM 비용 / latency 절약. 의도 명확. 로그 깔끔.
- 단점: `handle()` 분기 1개 추가 (early return).
- 사유: cap 도달이면 LLM 결과 무시 → 호출 의미 없음. dev 사례 5턴 LLM 호출 = 운영 비용 누적.

### Option B (폐기) — LLM 호출 후 cap 평가 (`evaluateSwitchConditions` OR 조건 추가)

- 장점: 기존 흐름 최소 변경.
- 단점: cap 도달에도 LLM 호출 1회 발생. 비용 / 의도 양면 손해.
- 폐기 사유: A 대비 단일 우위 없음.

## Architecture

```
[Client] → POST /api/v1/interviews/{id}/follow-up
       → InterviewController → InterviewService
       → ResumeInterviewOrchestrator.processUserTurn
            ↓
       dispatchByMode(PLAYGROUND)
            ↓
       handlePlayground
            ↓
       PlaygroundModeHandler.handle
            ↓
       ┌──────────────────────────────────────────┐
       │ if modeTransitionPolicy                  │
       │      .isPlaygroundHardCapReached(        │
       │          state.getPlaygroundTurns()):    │
       │   → log INFO (interviewId, turnCount,    │
       │              threshold)                  │
       │   → return PlaygroundTurnResult(         │
       │       response: question=null,           │
       │       switchedToInterrogation=true,      │
       │       questionId=null)                   │
       │  (※ playgroundTurns increment 미수행 —   │
       │     cap 도달 후 PLAYGROUND 재진입 없음.   │
       │     불필요 side-effect 회피)              │
       └──────────────────────────────────────────┘
            ↓ (else)
       기존 흐름: LLM 호출 → evaluateSwitchConditions → 결과
            ↓
       Orchestrator: switchedToInterrogation=true 감지
            ↓
       runtimeState.transitionTo(INTERROGATION)
            ↓
       InterrogationModeHandler.handle(null userAnswer, null analysis, ...)
            ↓
       첫 INTERROGATION 질문 사용자에게 노출
```

회귀 분석 — `evaluateSwitchConditions` 안의 `dTurnLimit` LLM 시그널 (turnCount 기반) 은 cap 도달 시 LLM 호출 자체가 skip 되므로 평가 미실행. cap 미도달 케이스에서는 기존 동작 그대로 유지 → `evaluateSwitchConditions` 메서드 본문 변경 불필요.

## Data Model

DB 스키마 변경 없음.

`application.yml` — 신규 키 없음. 기존 `rehearse.resume-track.playground-max-turns: 3` 그대로 사용 (orphan wiring).

## API Contract

변경 없음. 기존 follow-up 엔드포인트 응답 schema 동일.
- cap 발동 턴: `FollowUpResponse` 가 첫 INTERROGATION 질문 본문을 그대로 담아 반환 (orchestrator 가 `interrogationHandler.handle` 결과로 응답 덮어씀).
- 사용자 입장: 추가 round-trip 없이 다음 질문이 INTERROGATION 으로 노출.

## NF 결정

| 항목 | 결정 | 근거 |
|------|------|------|
| 영향 범위 | BE only | 단일 도메인 (`resume`) 내부 로직. FE / Lambda 무영향 |
| 정합성 | 트랜잭션 외부 평가 / side-effect 없음 | LLM 호출 트랜잭션 외부 (orchestrator 기존 룰). cap 평가 = 단순 비교 연산 |
| 실시간성 | cap 도달 시 latency 단축 | LLM 호출 1회 skip → 기존 대비 ~수백 ms 빠름 |
| 부하 | 비용 감소 | dev 사례 기준 cap 도달당 LLM `playground_responder` 1회 호출 절약 |
| 동시성 | 단일 인터뷰 순차 처리 가정 + AtomicInteger 의존 | `playgroundTurns` 는 read-then-act 패턴. 단일 사용자 동일 인터뷰 동시 호출은 frontend / runtime guard 차단 가정. backstop 의도상 race 시 1회 추가 호출 허용 |
| 마이그레이션 | 없음 | DB / 데이터 변경 없음. 신규 세션부터 즉시 적용 |
| 외부 의존 | 신규 의존 없음 | 기존 `ResumeModeTransitionPolicy` / `InterviewRuntimeState` 만 사용 |
| 보안 | 해당 없음 | 인증 / 권한 / 입력 / SSRF 영역 외 |
| 관찰성 | INFO 로그 1종 신규 (cap 발동) | `[PlaygroundHandler] hard cap 도달: interviewId={}, turnCount={}, threshold={}` 패턴. dev / prod docker log grep 가능 |
| 롤백 | config 무중단 + 코드 revert 2-tier | 옵션 1 = `playground-max-turns` 9999 설정으로 사실상 비활성. 옵션 2 = 단일 PR revert |
| 검증 | Domain Unit + Service Integration | testing.md 카테고리: 정책 메서드 = Domain Unit, orchestrator 흐름 = Service Integration (외부 LLM Mock) |

## Verification

- [ ] 단위 (Domain Unit, `DomainUnitSupport`):
  - `ResumeModeTransitionPolicyTest`:
    - `should_returnTrue_when_turnCountAtThreshold` (turnCount=3, threshold=3 → true)
    - `should_returnTrue_when_turnCountAboveThreshold` (4 → true)
    - `should_returnFalse_when_turnCountBelowThreshold` (2 → false)
  - 임계값 주입 = `ReflectionTestUtils.setField(policy, "playgroundMaxTurns", 3)`. (Domain Unit = Spring 컨텍스트 없음)
- [ ] 통합 (Service Integration, `ServiceIntegrationSupport`, OpenAI / Resume LLM 어댑터 Mock):
  - `ResumeInterviewOrchestratorIntegrationTest` (신규):
    - `transitionsToInterrogation_when_playgroundTurnsReachHardCap` — opener + 답변 2회 진행 후 3번째 답변 처리 → mode INTERROGATION 전이 + INTERROGATION 첫 질문 노출 검증. cap 턴에 `playground_responder` adapter `verify(times(0))`, `chain_interrogator` adapter `verify(times(1))`.
    - `keepsLlmDrivenTransition_when_responderSignalsSwitchEarly` — 1턴째 LLM 응답 `shouldSwitchToInterrogation=true` → 기존 흐름 그대로 INTERROGATION 전이.
    - `staysInPlayground_when_belowCapAndNoSwitchSignal` — 1턴째 LLM 응답 신호 없음 → playground 유지 + LLM playground 질문 응답.
- [ ] 빌드: `./gradlew build` 통과 (테스트 + 컴파일).
- [ ] 관찰 (운영 메트릭):
  - dev EC2 docker log grep: `[PlaygroundHandler] hard cap 도달` 라인 검색 → cap 발동 카운트 / 인터뷰 ID 추출.
  - 운영 첫 2주 모니터링 = 신규 RESUME 세션 분모 / cap 발동 분자 비율 추적. 비율 / 잔존 결함 (LLM 신호 미발생 + cap 미도달) 측정 → product-spec Approach 의 phase 2 트리거 데이터로 활용.
- [ ] 회귀: `./gradlew test --tests "com.rehearse.api.domain.resume.*"` 전체 통과. 인접 영역 (`InterrogationChainTest`, `ResumeIngestionServiceTest` 등) 무영향.

## Pre / Post State

### Pre

- `PlaygroundModeHandler.handle` = LLM 호출 → `evaluateSwitchConditions` (LLM 자가 신호 only)
- `ResumeModeTransitionPolicy` = `isHardTimeoutExceeded` 단일 메서드, `hard-timeout-min` 단일 config
- `application.yml: playground-max-turns: 3` = orphan (코드 미참조)
- 결과: LLM 신호 없으면 무한 playground

### Post

- `PlaygroundModeHandler.handle` = entry 직후 cap 체크 → 도달 시 early return (LLM skip + switched=true). 미도달 시 기존 흐름.
- `ResumeModeTransitionPolicy` = `isHardTimeoutExceeded` + `isPlaygroundHardCapReached(int turnCount)` 2개 메서드. `playground-max-turns` `@Value` 주입.
- `application.yml: playground-max-turns: 3` = wired (정책 메서드 참조)
- 결과: opener 포함 누적 3턴 도달 시 LLM 신호와 무관하게 INTERROGATION 진입 보장

### Diff 요약

```
M backend/.../resume/service/ResumeModeTransitionPolicy.java
  + @Value("${rehearse.resume-track.playground-max-turns:3}")
  + private int playgroundMaxTurns;
  + public boolean isPlaygroundHardCapReached(int turnCount) {
  +     return turnCount >= playgroundMaxTurns;
  + }

M backend/.../resume/service/PlaygroundModeHandler.java
  + RequiredArgsConstructor 의존성에 ResumeModeTransitionPolicy 추가
  + handle() 진입부 cap 체크 + early return 분기 (LLM skip)
  + cap 발동 INFO 로그 (interviewId, turnCount, threshold)

A backend/src/test/java/.../resume/service/ResumeModeTransitionPolicyTest.java
A backend/src/test/java/.../resume/service/ResumeInterviewOrchestratorIntegrationTest.java
```

## 위험 / 마이그레이션 / 롤백

- 위험:
  - cap 임계값 너무 작음 → 정상 워밍업 흐름 단절. 완화: 기본 3 = 사용자 합의값. config 외부화로 운영 중 조정 가능.
  - cap 발동 턴에 `interrogationHandler.handle(null userAnswer, null analysis, ...)` 호출 안전성. 완화: 현재 LLM 자가 전환 케이스에서도 동일 호출 (orchestrator line 173) — 기존 동작 검증된 경로 재사용.
  - race 시 cap 우회 (read-then-act) → backstop 의도상 1회 추가 호출 허용. 완화: 동일 인터뷰 동시 follow-up 은 FE / runtime guard 가 차단 가정.
- 마이그레이션: DB / 데이터 변경 없음. 신규 세션부터 즉시 적용.
- 롤백:
  - 옵션 1: `application.yml: rehearse.resume-track.playground-max-turns` 매우 큰 값 (예: 9999) → 사실상 비활성. 무중단.
  - 옵션 2: 코드 revert (단일 PR) — 의존 작업 없음.

## 분기 결정

- [x] 단일 영역 (BE) → `implement.md` 1개
- [ ] BE+FE 동시
- [ ] BE 선행 강제
