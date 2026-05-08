# Implement — RESUME 트랙 PLAYGROUND→INTERROGATION 자동 전환 hard cap

> **작성자**: 메인 세션 (Staff Engineer 페르소나)
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: 단일 영역 (BE only)
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 PR | 의존 |
|-------|------|--------------|--------|------|
| 1 | `ResumeModeTransitionPolicy` cap 정책 + Domain Unit 테스트 | `backend` | #N | - |
| 2 | `PlaygroundModeHandler` cap 분기 + Service Integration 테스트 | `backend` | #N (Phase 1과 단일 PR 가능) | Phase 1 |

> Task 합 2 / 본문 50줄 미만 → `tasks/` 분리 미적용. 단일 implement.md.

---

## Phase 1: `ResumeModeTransitionPolicy` cap 정책 + Domain Unit 테스트

- **구현**: `backend` — 전환 정책 도메인 서비스에 hard cap 메서드 + config 주입 추가, Domain Unit 테스트 작성

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeModeTransitionPolicy.java` — 수정. `playground-max-turns` `@Value` 주입 + `isPlaygroundHardCapReached(int turnCount)` 메서드 추가. 기존 `hard-timeout-min` 패턴과 동일.
- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumeModeTransitionPolicyTest.java` — 신규. Domain Unit (`DomainUnitSupport` 적용 또는 단순 unit, Spring 컨텍스트 없음). `ReflectionTestUtils.setField` 로 `playgroundMaxTurns` 주입.

### 핵심 로직 / 변경 요약

```java
// ResumeModeTransitionPolicy.java
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeModeTransitionPolicy {

    @Value("${rehearse.resume-track.hard-timeout-min:10}")
    private long hardTimeoutMin;

    @Value("${rehearse.resume-track.playground-max-turns:3}")
    private int playgroundMaxTurns;

    public boolean isHardTimeoutExceeded(int durationMinutes, long remainingMinutes) { ... }

    public boolean isPlaygroundHardCapReached(int turnCount) {
        return turnCount >= playgroundMaxTurns;
    }
}
```

테스트 시나리오 (`@DisplayName` 한국어 + `@Nested`):
- 누적 턴 수가 임계값과 같으면 cap 도달로 판정한다 (3 == 3 → true)
- 누적 턴 수가 임계값보다 크면 cap 도달로 판정한다 (4 > 3 → true)
- 누적 턴 수가 임계값 미만이면 cap 미도달로 판정한다 (2 < 3 → false)

### 의존
- 선행 phase: 없음
- 외부 의존: 없음 (config `rehearse.resume-track.playground-max-turns` 이미 `application.yml:84` 정의 — 본 작업이 wiring)

### Verification Hook
- 명령: `./gradlew test --tests "ResumeModeTransitionPolicyTest"`
- 통과 기준: 모든 테스트 green
- 관찰 가능 동작: 단위 테스트 3 케이스 모두 통과

### 커밋 메시지 (예상)
```
feat(BE): ResumeModeTransitionPolicy 에 playground hard cap 메서드 추가
```

---

## Phase 2: `PlaygroundModeHandler` cap 분기 + Service Integration 테스트

- **구현**: `backend` — handle() entry 직후 cap 체크 → 도달 시 LLM skip + early return. 통합 테스트로 3 시나리오 검증

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandler.java` — 수정. `RequiredArgsConstructor` 의존성에 `ResumeModeTransitionPolicy` 추가. `handle()` 진입 직후 cap 체크 + early return 분기. cap 발동 INFO 로그.
- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestratorIntegrationTest.java` — 신규. `ServiceIntegrationSupport`. Resume LLM 어댑터 (`playground_responder`, `chain_interrogator` 호출 포트) Mock. 외부 API 만 Mock 원칙 준수.

### 핵심 로직 / 변경 요약

```java
// PlaygroundModeHandler.java
private final ResumeQuestionResultGenerator resultGenerator;
private final ResumeQuestionPersister questionPersister;
private final ResumeModeTransitionPolicy modeTransitionPolicy;  // 신규

public PlaygroundTurnResult handle(...) {
    int turnCount = state.getPlaygroundTurns().get();

    if (modeTransitionPolicy.isPlaygroundHardCapReached(turnCount)) {
        log.info("[PlaygroundHandler] hard cap 도달 → INTERROGATION 강제 전환: interviewId={}, turnCount={}, threshold={}",
                interviewId, turnCount, /* policy 가 노출하는 임계값 또는 turnCount 비교 결과 */);
        FollowUpResponse response = buildResponse(null, null, null, true, null);
        return new PlaygroundTurnResult(response, true, null);
    }

    // 기존 흐름 그대로 — LLM 호출 → evaluateSwitchConditions → 결과
    ProjectPlan currentPlan = resolveCurrentPlan(plan);
    ...
}
```

주의:
- cap 분기 안에서 `state.getPlaygroundTurns().incrementAndGet()` **호출 안 함**. cap 도달 후 PLAYGROUND 재진입 없음 → 불필요 side-effect 회피 (tech-spec Architecture 명시).
- `accumulateLength` 도 호출 안 함 (PLAYGROUND 길이 누적 의미 없음).
- orchestrator 의 `validateQuestionId` 는 dispatchByMode 결과에 적용되며, cap 시 orchestrator 가 `interrogationHandler.handle` 결과로 응답 / questionId 덮어쓰므로 검증 통과.

테스트 시나리오 (`@DisplayName` 한국어 + `@Nested`):
- `누적 턴 수가 임계값에 도달하면 LLM 호출 없이 INTERROGATION 으로 강제 전환된다`
  - opener 호출 + handle() 2회 진행 (playgroundTurns=3 도달) 후 3번째 사용자 답변 처리
  - 검증: 응답 `type=RESUME_INTERROGATION` (또는 동등), `playground_responder` adapter `verify(times(0))`, `chain_interrogator` adapter `verify(times(1))`, runtime mode = `INTERROGATION`
- `LLM 이 임계값 이전에 자가 전환 신호를 보내면 기존 흐름대로 INTERROGATION 으로 전이한다`
  - 1턴째 LLM 응답 `shouldSwitchToInterrogation=true` Mock
  - 검증: `playground_responder` 호출 1회, runtime mode = `INTERROGATION`
- `누적 턴 수가 임계값 미만이고 LLM 신호가 없으면 워밍업 단계가 유지된다`
  - 1턴째 LLM 응답 신호 부재 (필드 모두 false / null) Mock
  - 검증: 응답 `type=RESUME_PLAYGROUND`, runtime mode = `PLAYGROUND`, playground 질문 노출

### 의존
- 선행 phase: Phase 1 (`ResumeModeTransitionPolicy.isPlaygroundHardCapReached` 필요)
- 외부 의존: 없음

### Verification Hook
- 명령: `./gradlew test --tests "ResumeInterviewOrchestratorIntegrationTest"` + `./gradlew test --tests "com.rehearse.api.domain.resume.*"` (회귀)
- 통과 기준: 모든 테스트 green. 인접 테스트 (`InterrogationChainTest`, `ResumeIngestionServiceTest` 등) 무영향.
- 관찰 가능 동작: 통합 테스트 3 시나리오 통과 + dev 배포 후 docker log grep `[PlaygroundHandler] hard cap 도달` 로 cap 발동 추적 가능

### 커밋 메시지 (예상)
```
fix(BE): RESUME playground 누적 3턴 도달 시 INTERROGATION 강제 전환
```

---

## 통합 Verification

- [ ] `tech-spec.md` Verification 항목 모두 통과 (단위 + 통합 + 빌드 + 관찰 + 회귀)
- [ ] 운영 메트릭 — dev 배포 후 docker log grep `[PlaygroundHandler] hard cap 도달` 라인 검색 가능 확인 (배포 후 1회)

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] 지정 리뷰어 실행 (구현 완료 직후 — 메인 세션 책임)
  - BE only → `code-reviewer-backend`
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `backend/.claude/rules/testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (`tech-spec.md` Diff 요약 섹션)
