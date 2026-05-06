# Tech Spec — Resume 4-layer 컨텍스트 결함 (P0 + P1)

> **작성자**: backend agent (Staff Engineer)
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★
> **관련 plan**: `docs/plans/410-resume-context-defects/product-spec.md`
> **관련 Issue**: #410

---

## Why → Goal (1줄 미러)

Resume 4-layer 컨텍스트 P0 (5xx 직접 노출 2건) + P1 (LLM 품질 / 운영 가시성 / 동시성 3건) 결함 5건 제거. 5xx 0건 + 컨텍스트 누락 0% + 운영자 식별 가능 메트릭 + Interrogation lock 점유 = chain state 변경 구간만.

## Evidence

### 현재 구조

- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java` — L4. sealed FocusHints pattern. callType별 cap 9종. `render()` cap 초과 throw, `handleEmpty()` 미등록 callType throw
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/DialogueHistoryLayer.java` — L3. `buildWithCompaction()` 분기. runtimeState null 또는 압축 in-flight 시 olderTurns raw fallback 부재
- `backend/src/main/java/com/rehearse/api/infra/ai/context/InterviewContextBuilder.java:47-50` — L1+L2+L3+L4 합산 > `maxContextTokens` 시 WARN 만, 그대로 LLM 호출
- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java:38-79` — `tracker.withLock` 안에서 LLM 호출 + DB persist 수행
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java:161-168` — 기존 `hardTimeoutResponse()` graceful 종료 패턴 (followUpExhausted=true, skip=true, presentToUser=false, type="RESUME_HARD_TIMEOUT")
- `backend/src/main/java/com/rehearse/api/global/config/ContextEngineeringProperties.java` — maxContextTokens 8000 (yml 외부화)
- `backend/src/main/java/com/rehearse/api/infra/ai/context/metrics/ContextEngineeringMetrics.java` — Micrometer 기반. 기존 `rehearse.ai.context.tokens / cache_hit_ratio / compaction_count` 등록됨
- `backend/src/main/java/com/rehearse/api/infra/ai/context/token/TokenEstimator.java` — chars/4 휴리스틱 (CHARS_PER_TOKEN = 4). cap = 토큰 추정치

### 외부 레퍼런스

- 유사 spec: `docs/plans/404-interview-domain-findings/tech-spec.md` (BE-only / 메트릭 패턴 참고)
- `backend/.claude/rules/conventions.md` — `BusinessException` + `ErrorCode`, 트랜잭션, 로깅 룰
- `backend/.claude/rules/testing.md` — Domain Unit (FocusLayer / DialogueHistoryLayer 단순 단위 가능) + Service Integration (InterviewContextBuilder + Orchestrator)

### 사용자 발화 (특정 결정 근거)

- "면접을 종료시켜야하지 않을까?" → 전체 cap 초과 = graceful 종료 (200 + signal). 5xx 차단
- "P0 + P1 (5건, 권장)" — 본 spec scope 확정
- "P1 5건 전부 보강" — P1 review 발견 사항 모두 반영

### 추정 / 미확인 가정

- L4 cap 초과 절단 = USER_ANSWER 본문만 head/middle 영역 절단, 지시문 (fragment 마지막 "위 chain 상태에서 ... JSON 한 객체로만 응답하세요" 등) 은 보존. 지시문 절단 시 LLM JSON 응답 형식 미준수 → AI_005 (PARSE_FAILED) 5xx 전이 위험 회피 (도메인 추정)
- TokenEstimator chars/4 휴리스틱 = 한글 / 마크업 (`<<<...>>>`) 비중에 따라 ±20% 오차 가능. 절단 후 재추정 + 안전 마진 (cap × 0.9) 적용 (추정)
- Interrogation 동시 turn 시나리오는 "차단 X" 가 목표. race 발생 시 두 번째 turn 이 첫 번째 변경을 못 보고 같은 currentLevel 로 진행 가능성 — product-spec AC 범위 외 (별도 결함)
- 절단 / 폴백 발생 빈도 = 운영 메트릭 부재. 본 spec 으로 메트릭 도입 후 1주 운영 데이터로 차후 cap 튜닝 검토 (별도 작업)
- InterviewContextBuilder 호출부 19곳 중 본 plan scope = Resume FollowUp 트랙 (ResumeInterviewOrchestrator 진입점) 만. 다른 트랙 (일반 면접 follow-up / 단독 IntentClassifier / Clarify / GiveUp 호출 등) cap 초과 시 = 본 throw 도입으로 5xx 회귀 가능. 본 plan 외 별도 후속 작업 (각 트랙 진입점 catch 추가) 예정. 운영 모니터링 = `tokens.exceeded` 메트릭 callType 태그로 비-Resume 트랙 발생 빈도 추적

## Trade-offs

### Trade-off 1: L4 cap 초과 처리 (P0-1)

**Option A (채택)**: 본문 절단 (USER_ANSWER 등 가변 본문만 head/middle 절단) + 지시문 보존 + log.warn
- 장점: 5xx 차단 / 사용자 경험 손상 없음 / 지시문 보존 = LLM JSON 형식 응답 유지 (PARSE_FAILED 5xx 전이 회피) / 절단 발생 시 log.warn (callType / estimated / cap) 으로 운영자 식별
- 단점: 절단된 fragment 일부 의미 손실 가능 → LLM 응답 품질 미세 저하. fragment 별 본문 위치 식별 로직 추가 필요
- 사유: P0 5xx 0건 = 최우선. 단순 꼬리 절단 = 지시문 절단 위험 (fragment 마지막 = 지시문) → 본문 영역 식별 후 절단. 품질 미세 저하는 운영 로그 집계로 추적 후 차후 cap 재튜닝 가능. 절단 후 토큰 재추정 + 안전 마진 (cap × 0.9) 적용으로 chars/4 휴리스틱 ±20% 오차 흡수

**Option B (폐기)**: 우선순위 절단 (FocusHints 필드별 가중치)
- 장점: 의미 보존 더 정교
- 폐기 사유: 9종 callType × 필드별 가중치 = 복잡도 증가, 본 plan scope 초과. 본문 영역 식별 + 안전 마진으로 충분

### Trade-off 2: L3 raw fallback 위치 (P1-1)

**Option A (채택)**: `triggerCompactionIfPossible` 분기에서 olderTurns raw 포함 (압축 시작 + raw 동시 반환). runtimeState null 시 raw 포함
- 장점: 압축 미완료 / cache miss 시 옛 turn 컨텍스트 누락 차단. 압축 완료 시 자동 요약 사용
- 단점: 압축 in-flight 동안 raw + 다음 turn raw 이중 토큰 일시 증가 (압축 완료 시까지)
- 사유: 옛 turn 누락 = LLM 응답 일관성 손상. 일시적 토큰 증가 < 컨텍스트 누락 영향

**Option B (폐기)**: sync fallback (압축 완료까지 대기)
- 장점: 토큰 증가 없음
- 폐기 사유: LLM latency 사용자 직접 대기 누적 → UX 저하. `DialogueCompactor.java:65` TODO 흔적도 deferred

### Trade-off 3: 전체 cap 초과 처리 (P1-2)

**Option A (채택)**: Graceful 종료 (200 + signal). `BusinessException(CONTEXT_BUDGET_EXCEEDED)` → Orchestrator 가 catch → graceful response (followUpExhausted=true, skip=true, presentToUser=false, type="CONTEXT_BUDGET_EXCEEDED")
- 장점: 5xx 차단 / 기존 hardTimeoutResponse 패턴 재사용 / FE 분기 추가 X (followUpExhausted 처리 기존 존재)
- 단점: 면접 중단. 사용자 입장에서는 정상 종료 화면 노출
- 사유: cap 초과 = 비정상 신호 (압축 실패 / fragment 폭주). 그대로 호출 = 비용 + 품질 저하. 종료 + 메트릭으로 운영자가 원인 추적 후 cap / 압축 정책 조정

**Option B (폐기)**: 일부 layer 절단 (L3 가장 옛 turn drop)
- 장점: 면접 진행
- 폐기 사유: 절단 정책 명문화 복잡 (어떤 layer 우선? 어디까지?), 본 plan scope 초과. cap 초과 자체가 비정상 신호 = 종료 신호로 충분

### Trade-off 4: Interrogation lock 경계 (P1-3)

**Option A (채택)**: 3-phase 분리. Phase 1 lock (chain 활성 체크 + initChain + 스냅샷) → Phase 2 lock 밖 (LLM 호출 + DB persist) → Phase 3 lock (applyDecision)
- 장점: lock 점유 = chain state 변경 구간만 (초 단위 LLM latency 제거). 동시 turn 차단 X
- 단점: phase 분리 보일러플레이트. 동시 turn 시 두 번째가 첫 번째 applyDecision 못 보고 진입 가능 (별도 결함, scope 외)
- 사유: product-spec AC 충족. race 자체는 본 plan scope 외 (클라이언트 재시도 별도)

**Option B (폐기)**: lock 제거 (낙관적 접근)
- 장점: 단순
- 폐기 사유: chain state transition (initChain / levelUp / markChainComplete) = race condition 발생 가능. 최소 직렬화 필요

## Architecture

### 변경 범위

| 영역 | 변경 |
|------|------|
| L1 FixedContextLayer | 변경 X (회귀 범위만) |
| L2 SessionStateLayer | 변경 X (회귀 범위만) |
| L3 DialogueHistoryLayer | runtimeState null + 압축 in-flight 시 olderTurns raw fallback 추가 |
| L4 FocusLayer | render() cap 초과 → truncate + log.warn. handleEmpty() 미등록 callType → empty fragment + log.warn |
| InterviewContextBuilder | total > maxContextTokens → 무조건 BusinessException(CONTEXT_BUDGET_EXCEEDED) throw + log.warn + 메트릭 (`tokens.exceeded`) |
| ResumeInterviewOrchestrator | 단일 catch 지점. BusinessException errorCode enum 비교 (CONTEXT_BUDGET_EXCEEDED) → contextBudgetExceededResponse() (graceful 200 + signal). 그 외 BusinessException = 재throw |
| InterrogationModeHandler | 3-phase lock 분리. Phase 3 에 persist + applyDecision 원자적 포함 |
| ContextEngineeringMetrics | 신규 카운터 1종 추가 (`tokens.exceeded`). 다른 3건은 log.warn 대체 |
| `BusinessException` | `errorCode` 필드 추가 — 신규 catch 사이트 enum 비교 가능. 기존 사용처 영향 X (필드 nullable). 마이그레이션 별도 작업 |

### 시퀀스 — 정상 (변경 없음)

```
[Orchestrator] → [InterviewContextBuilder.build]
                  → [L1] [L2] [L3] [L4]
                → total <= cap → LLM 호출 → 응답
```

### 시퀀스 — L4 cap 초과 (P0-1, 변경)

```
[L4 render(fragment, cap, callType)]
  estimated > cap
  → 변경 전: throw IllegalStateException → 5xx
  → 변경 후:
     1. fragment 내 본문 영역 식별 (USER_ANSWER / CURRENT_CHAIN 등 가변 marker 사이)
     2. 본문만 char 단위 절단 (지시문 끝부분 보존)
     3. 절단 후 토큰 재추정 (안전 마진 cap × 0.9 적용)
     4. log.warn (callType / estimated / cap key=value)
     5. ChatMessage 정상 반환 → LLM 호출 → 200
```

### 시퀀스 — L4 미등록 callType (P0-2, 변경)

```
[L4 handleEmpty(callType)]
  callType = "compaction_summarizer" → return List.of() (회귀, 정상 흐름)
  callType = 그 외 (FocusHints.EmptyHints + l4JustInTime=true 인 신규 callType 추가 누락 케이스 등)
    → 변경 전: throw IllegalStateException → 5xx
    → 변경 후: log.warn (한국어, callType key=value) + return List.of()
              → L4 빈 fragment → L1+L2+L3 으로 LLM 호출 → 200

진입 경로: ContextBuildRequest.focusHints = FocusHints.EmptyHints 인데 callType
이 미등록 (예: 신규 callType 추가 시 FocusHints 매칭 누락). InterviewContextBuilder
의 l4JustInTime=true 분기에서 호출되므로 "L4 진입 전 NPE" 시나리오는 해당 없음.
```

### 시퀀스 — L3 압축 race / runtimeState null (P1-1, 변경)

```
[L3 buildWithCompaction]
  runtimeState == null
    → 변경 전: 압축 / raw 둘 다 X (옛 turn 누락)
    → 변경 후: olderTurns raw 추가 + log.warn (reason=null_runtime_state)
  runtimeState != null, 요약 부재
    → 변경 전: 압축 trigger only (옛 turn 누락)
    → 변경 후: 압축 trigger + olderTurns raw 추가 + log.warn (reason=compaction_in_flight | summary_absent)
              (압축 완료 시 자동 요약 전환)
```

### 시퀀스 — 전체 cap 초과 (P1-2, 변경)

```
[InterviewContextBuilder.build]
  total > maxContextTokens
  → 변경 전: WARN log only → LLM 호출 → 200 (품질 저하 응답)
  → 변경 후: 메트릭 increment + throw new BusinessException(CONTEXT_BUDGET_EXCEEDED)
            ↓
[Orchestrator.processFollowUp] catch BusinessException(CONTEXT_BUDGET_EXCEEDED)
  → contextBudgetExceededResponse() (followUpExhausted=true, skip=true, presentToUser=false, type="CONTEXT_BUDGET_EXCEEDED")
  → 200 응답 + FE 가 종료 화면 노출
```

### 시퀀스 — Interrogation lock 경계 (P1-3, 변경)

```
[InterrogationModeHandler.handle]
  Phase 1 (tracker.withLock):
    if !hasActiveChain
      Optional<ChainReference> nextChain = tracker.resolveNextChain(...)
      if nextChain.isEmpty() → return ExhaustedResult (lock 안 종료)
      tracker.initChain(...)
    snapshot = ChainSnapshot(chainTopic, currentLevel, consecutiveStay,
                             currentProjectId, orderIndex, answerQuality)
    return snapshot

  Phase 2 (lock 밖):
    InterrogationResult result = promptBuilder.build(... snapshot ...)   // LLM (수 초)
    // 응답 검증 = lock 밖에서 즉시 (lock 안 throw 시 tracker state 불일치 방지)
    if (result.question() == null || result.question().isBlank())
        throw new BusinessException(AiErrorCode.RESPONSE_INVALID)
    if (ResumeFallbackQuestions.INTERROGATION.equals(result.question()))
        log.warn("[InterrogationHandler] 안전 폴백 사용 감지: ...")

  Phase 3 (tracker.withLock):
    Long questionId = questionPersister.persist(...)                     // DB I/O (persist + applyDecision 원자적)
    applyDecision(tracker, result, snapshot.answerQuality(), snapshot.currentLevel())
    return InterrogationTurnResult(
        buildResponse(result, tracker.getCurrentLevel()), questionId)
```

**ChainSnapshot record 필드 (6개)**: `chainTopic`, `currentLevel`, `consecutiveStay`, `currentProjectId`, `orderIndex`, `answerQuality`. orderIndex = Phase 1 lock 안에서 `state.nextResumeOrderIndex()` 호출 (race 방지).

## Data Model

스키마 변경 없음. 모든 결함 = 코드 / 메트릭 레벨 변경.

## API Contract

신규 endpoint 없음. 기존 `POST /api/v1/interviews/{id}/follow-up` 응답에 신규 type 1종 추가:

### Response (200) — 신규 케이스

```json
{
  "followUpExhausted": true,
  "skip": true,
  "presentToUser": false,
  "type": "CONTEXT_BUDGET_EXCEEDED",
  "question": null
}
```

기존 `RESUME_HARD_TIMEOUT` / `RESUME_INTERROGATION_EXHAUSTED` 응답 형태와 동형. FE 는 `followUpExhausted=true` 처리 기존 분기 재사용 (type 별도 분기 불필요).

### Error

신규 5xx 없음. P0 throw 제거가 핵심.

본 변경 회귀 범위 = `BusinessException(AiErrorCode.RESPONSE_INVALID)` (502 Bad Gateway) 그대로 유지. 사유: LLM 자체 결함 (응답 빈/null) = 정당한 5xx, 본 plan scope 외 (LLM 응답 품질 영역). product-spec AC "5xx 0건" = 컨텍스트 도메인 결함 (P0-1 / P0-2) 한정.

비-Resume 트랙 (일반 면접 follow-up / 단독 IntentClassifier / Clarify / GiveUp 호출 등) cap 초과 시 = `BusinessException(AiErrorCode.CONTEXT_BUDGET_EXCEEDED)` throw → catch 부재 → 5xx 가능. 본 plan = Resume FollowUp 트랙만 catch. 다른 트랙 = 별도 후속 작업 (메트릭 `tokens.exceeded` 의 callType 태그로 발생 빈도 추적).

## NF 결정 (11개)

| NF | 결정 | confidence | 근거 |
|----|------|-----------|------|
| 영향 범위 | BE only | 확신 | FE 응답 type 추가만, 기존 followUpExhausted 분기 재사용 |
| 정합성 | 트랜잭션적 (DB persist) + 이벤트적 (메트릭) | 확신 | persist 는 기존 @Transactional 유지 |
| 실시간성 | 사용자 직접 대기 (follow-up turn) | 확신 | P95 < 5s (LLM latency 의존, 본 변경으로 악화 X) |
| 부하 | 기존 follow-up turn 부하 동일 | 추정 | 절단 / 폴백 / 메트릭 = O(1) 이론적. 운영 데이터 부재 = 메트릭 도입 후 1주 재평가 |
| 동시성 | Interrogation = 3-phase lock 분리 | 확신 | 위 trade-off 4. Lock 안 = chain state 변경만 |
| 마이그레이션 | 없음 | 확신 | 코드 변경만 |
| 외부 의존 | 변경 없음 (OpenAI / Claude SDK 그대로) | 확신 | - |
| 보안 | 영향 없음 | 확신 | 입력 검증 / 인증 영역 무관 |
| 관찰성 | 메트릭 1종 신규 (`tokens.exceeded`) + log.warn 4종 | 추정 | 아래 메트릭 섹션. AC 4 만 메트릭, 나머지 3건 = log 집계로 충분 |
| 롤백 | 코드 revert (feature flag 미도입) | 추정 | 단순 변경 + 회귀 테스트 통과 = 안전 추정. flag 도입 = scope 초과 |
| 검증 | Domain Unit (L3/L4) + Service Mockist (Builder / Orchestrator / InterrogationHandler) + 회귀 | 확신 | testing.md 매핑. 외부 의존 mock 으로 충분 (Spring context 불필요) |

### 신규 메트릭 (Micrometer Counter) — 1종

| 메트릭 | 태그 | 트리거 | 운영자 활용 |
|--------|------|--------|------------|
| `rehearse.ai.context.tokens.exceeded` | callType | 전체 cap 초과 → 종료 응답 | callType 별 빈도 추적 → 비-Resume 트랙 진입 시 별도 후속 작업 트리거 / cap 재튜닝 후보. product-spec AC 4 충족 |

**메트릭 단일 사유**: product-spec AC 4 ("전체 컨텍스트 토큰 cap 초과 호출이 운영자가 식별 가능한 메트릭 신호 1개 이상 노출") 만 Micrometer counter 필수. 다른 3건 (L3 raw fallback / L4 본문 절단 / L4 미등록 callType) = 운영자 식별 = 구조화 log.warn (한국어 + key=value) 으로 대체 — AC 1/2/3 "운영 로그로 식별" 표현 부합. 향후 dashboard 활용 필요 시 메트릭 추가 = 별도 후속 작업.

**Logging 룰**: 본 변경에서 추가하는 모든 로그 = 한국어 + placeholder + key=value (conventions.md). 발견 시점별 로그 메시지:

| 발견 | 로그 형식 |
|------|---------|
| L3 raw fallback (P1-1) | `WARN [DialogueHistoryLayer] L3 raw fallback 발동: interviewId={}, windowEnd={}, reason={null_runtime_state\|compaction_in_flight\|summary_absent}` |
| L4 본문 절단 (P0-1) | `WARN [FocusLayer] L4 cap 초과 → 본문 절단: callType={}, estimated={}, cap={}` |
| L4 미등록 callType (P0-2) | `WARN [FocusLayer] L4 미등록 callType 진입: callType={}` |
| 전체 cap 초과 (P1-2) | `WARN [InterviewContextBuilder] 전체 cap 초과 → graceful 종료: callType={}, total={}, max={}` (메트릭 incrementTokensExceeded 동시 호출) |

기존 영문 로그 (`compaction triggered for windowEnd={}`) 는 회귀 범위로만 (별도 통일 작업 외).

## Verification (완료 판정)

구현 완료 = 아래 모두 통과.

### Domain Unit (testing.md `Domain Unit Support`)

- [ ] `FocusLayerTest`
  - L4 fragment cap 미만 → 정상 ChatMessage 반환
  - L4 fragment cap 초과 → 본문 절단 + log.warn 출력 + 정상 반환 (throw X) + 절단 후 토큰 cap × 0.9 이내 검증
  - 미등록 callType (예: "unknown_caller") → empty List + log.warn (throw X)
  - 등록된 모든 9종 callType + EmptyHints+compaction_summarizer 회귀
- [ ] `DialogueHistoryLayerTest`
  - exchanges <= recentWindow → 압축 분기 미진입 (회귀)
  - runtimeState null + exchanges > recentWindow → olderTurns raw 포함 + log.warn (reason=null_runtime_state)
  - runtimeState 존재, 요약 부재, 압축 in-flight → olderTurns raw 포함 + log.warn (reason=compaction_in_flight)
  - runtimeState 존재, 요약 부재, 압축 미시작 → olderTurns raw + 압축 trigger + log.warn (reason=summary_absent)
  - runtimeState 존재, 요약 존재 → 요약 사용 (회귀, raw 미포함)

### Service Mockist (testing.md `Domain Unit Support` 확장 — 외부 의존 mock 으로 충분)

본 plan 검증 영역 (lock 경계 / 메트릭 호출 / 응답 type 변환 / 예외 재throw) 은 외부 의존 (`ResilientAiClient`, `PromptBuilder`, `questionPersister`, `ContextEngineeringMetrics`) mock 만으로 충분 커버. 실제 Spring context / Testcontainers MySQL 기반 통합 테스트는 본 plan scope 외 (검증 가치 대비 비용 부담). 추후 회귀 위험 발견 시 별도 작업.

- [ ] `InterviewContextBuilderTest` (기존 unit test 보강)
  - total <= maxContextTokens → 정상 반환 (회귀)
  - total > maxContextTokens → `BusinessException(CONTEXT_BUDGET_EXCEEDED)` throw + `incrementTokensExceeded(callType)` 메트릭 호출 mock verify
- [ ] `ResumeInterviewOrchestratorTest` (기존 unit test 보강)
  - cap 초과 시 `contextBudgetExceededResponse` 200 응답 (followUpExhausted=true, type="CONTEXT_BUDGET_EXCEEDED")
  - 다른 BusinessException (RESPONSE_INVALID 등) 재throw 검증
- [ ] `InterrogationModeHandlerTest` (기존 unit test 보강)
  - 단일 turn 정상 흐름 (회귀)
  - chain 소진 시 buildExhaustedResponse (회귀)
  - Phase 2 (LLM) 동안 lock 미점유 검증 — `ResilientAiClient` / `PromptBuilder` mock + CountDownLatch 로 LLM 응답 지연 시뮬. Phase 2 동안 다른 thread `tracker.withLock` 1초 이내 진입 가능 (assertion: `latch.await(1, SECONDS)` true)
  - `result.question() == null/blank` → BusinessException(RESPONSE_INVALID) Phase 2 lock 밖에서 throw 검증 (tracker state 변경 없음 회귀)
  - persist 가 Phase 3 (lock 안) 에서 호출됨 (Q1 fix 반영 — persist + applyDecision 원자적)

### 빌드 / 린트

- [ ] `./gradlew compileJava` 통과
- [ ] `./gradlew test` 전체 통과
- [ ] `./gradlew test --tests "com.rehearse.api.infra.ai.context.*"` 도메인 통과
- [ ] `./gradlew test --tests "com.rehearse.api.domain.resume.*"` 도메인 통과

### 관찰 가능 동작

- [ ] dev 환경 docker log: `rehearse.ai.context.tokens.exceeded` 메트릭 노출 (Actuator `/actuator/metrics` 또는 micrometer registry 확인)
- [ ] dev 환경 docker log: L4 본문 절단 시 `WARN [FocusLayer] L4 cap 초과 → 본문 절단: callType=..., estimated=..., cap=...` 로그
- [ ] dev 환경 docker log: 미등록 callType 진입 시 `WARN [FocusLayer] L4 미등록 callType 진입: callType=...` 로그
- [ ] dev 환경 docker log: L3 raw fallback 발동 시 `WARN [DialogueHistoryLayer] L3 raw fallback 발동: interviewId=..., windowEnd=..., reason=...` 로그
- [ ] dev 환경 docker log: 전체 cap 초과 시 `WARN [InterviewContextBuilder] 전체 cap 초과 → graceful 종료: callType=..., total=..., max=8000` 로그 + 메트릭 increment

### 회귀 체크

- [ ] 기존 정상 케이스 (L1/L2 / 압축 미트리거 / cap 미초과 / 단일 turn) 회귀 통과
- [ ] `RESUME_HARD_TIMEOUT` 응답 타입 회귀 통과 (Orchestrator 기존 분기)
- [ ] FocusLayer 9종 callType + EmptyHints+compaction_summarizer 회귀 통과

## Pre / Post State

### Pre (현재)

```java
// FocusLayer.render
if (estimated > cap) {
    throw new IllegalStateException(...);    // 5xx
}

// FocusLayer.handleEmpty
throw new IllegalStateException("L4 unregistered callType: " + callType);    // 5xx

// DialogueHistoryLayer.buildWithCompaction (runtimeState null 분기 부재)
if (runtimeState != null) {
    runtimeState.getCompactedSummary(windowEnd).ifPresentOrElse(
        summary -> result.add(...),
        () -> triggerCompactionIfPossible(...)    // raw fallback 부재
    );
}
// runtimeState == null → result 에 옛 turn 미포함

// InterviewContextBuilder.build
if (total > properties.maxContextTokens()) {
    log.warn(...);    // WARN only, 그대로 진행
}

// InterrogationModeHandler.handle (실제 코드 순서)
return tracker.withLock(() -> {
    if (!tracker.hasActiveChain()) {
        Optional<ChainReference> nextChain = tracker.resolveNextChain(...)
        if (nextChain.isEmpty()) return ExhaustedResult
        tracker.initChain(...)
    }
    int answerQuality = ...; int currentLevel = ...; ...
    InterrogationResult result = promptBuilder.build(...);    // LLM lock 안 (수 초)
    applyDecision(tracker, result, answerQuality, currentLevel);    // tracker 변경 lock 안
    if (result.question() == null/blank) throw BusinessException(RESPONSE_INVALID);
    if (fallback question 사용) log.warn(...);
    Long questionId = questionPersister.persist(...);    // DB lock 안
    return InterrogationTurnResult(buildResponse(result, tracker.getCurrentLevel()), questionId);
});
// 결과: lock 안 = LLM latency (수 초) + DB I/O 모두 점유
```

### Post (구현 후)

```java
// FocusLayer.render(fragment, cap, callType)
if (estimated > cap) {
    log.warn("[FocusLayer] L4 cap 초과 → 본문 절단: callType={}, estimated={}, cap={}",
            callType, estimated, cap);
    // 본문 영역 식별 (USER_ANSWER / CURRENT_CHAIN 등 marker 사이) → 본문만 절단
    // 지시문 끝부분 (예: "위 ... JSON 한 객체로만 응답하세요") 보존
    // 안전 마진 cap × 0.9 적용 후 재추정 (chars/4 휴리스틱 ±20% 오차 흡수)
    fragment = truncateBodyWithSafetyMargin(fragment, cap, callType);
}
return List.of(ChatMessage.of(USER, fragment));

// FocusLayer.handleEmpty
if ("compaction_summarizer".equals(callType)) {
    return List.of();
}
log.warn("[FocusLayer] L4 미등록 callType 진입: callType={}", callType);
return List.of();    // 안전 폴백

// DialogueHistoryLayer.buildWithCompaction
if (runtimeState == null) {
    log.warn("[DialogueHistoryLayer] L3 raw fallback 발동: interviewId={}, windowEnd={}, reason=null_runtime_state",
            interviewId, windowEnd);
    result.addAll(renderAlternating(olderTurns));                 // raw fallback
} else {
    runtimeState.getCompactedSummary(windowEnd).ifPresentOrElse(
        summary -> result.add(...),
        () -> {
            String reason = runtimeState.hasCompactionInFlight(windowEnd)
                ? "compaction_in_flight" : "summary_absent";
            log.warn("[DialogueHistoryLayer] L3 raw fallback 발동: interviewId={}, windowEnd={}, reason={}",
                    interviewId, windowEnd, reason);
            result.addAll(renderAlternating(olderTurns));         // raw fallback
            triggerCompactionIfPossible(...);
        }
    );
}
result.addAll(renderAlternating(recentTurns));

// InterviewContextBuilder.build
if (total > properties.maxContextTokens()) {
    log.warn("[InterviewContextBuilder] 전체 cap 초과 → graceful 종료: callType={}, total={}, max={}",
            req.callType(), total, properties.maxContextTokens());
    contextMetrics.incrementTokensExceeded(req.callType());
    throw new BusinessException(AiErrorCode.CONTEXT_BUDGET_EXCEEDED);
}

// ResumeInterviewOrchestrator.processFollowUp
try {
    ... existing turn handler ...
} catch (BusinessException e) {
    if (e.getErrorCode() == AiErrorCode.CONTEXT_BUDGET_EXCEEDED) {
        return contextBudgetExceededResponse();
    }
    throw e;
}

// InterrogationModeHandler.handle (3-phase)
// Phase 1: chain state 진입 + 스냅샷 (lock 안)
Optional<ChainSnapshot> snapshotOpt = tracker.withLock(() -> {
    if (!tracker.hasActiveChain()) {
        Optional<ChainReference> nextChain = tracker.resolveNextChain(plan.projectPlans());
        if (nextChain.isEmpty()) return Optional.<ChainSnapshot>empty();    // exhausted
        tracker.initChain(nextChain.get().projectId(), nextChain.get().chainId());
    }
    int orderIndex = state.nextResumeOrderIndex();    // race 방지 = lock 안 캡처
    int answerQuality = analysis != null ? analysis.answerQuality() : 2;
    return Optional.of(new ChainSnapshot(
        tracker.getCurrentChainId(), tracker.getCurrentLevel(),
        tracker.getConsecutiveLevelStayCount(), tracker.getCurrentProjectId(),
        orderIndex, answerQuality
    ));
});
if (snapshotOpt.isEmpty()) {
    return new InterrogationTurnResult(buildExhaustedResponse(), null);
}
ChainSnapshot snapshot = snapshotOpt.get();

// Phase 2: LLM 호출 + 응답 검증 (lock 밖)
InterrogationResult result = promptBuilder.build(
    interviewId, state, previousExchanges,
    snapshot.chainTopic(), snapshot.currentLevel(),
    snapshot.answerQuality(), userAnswer, snapshot.consecutiveStay());
if (result.question() == null || result.question().isBlank()) {
    throw new BusinessException(AiErrorCode.RESPONSE_INVALID);    // lock 밖 throw = tracker 불변
}
if (ResumeFallbackQuestions.INTERROGATION.equals(result.question())) {
    log.warn("[InterrogationHandler] 안전 폴백 사용 감지: interviewId={}, chainId={}, level={}",
            interviewId, snapshot.chainTopic(), snapshot.currentLevel());
}

// Phase 3: persist + tracker 상태 변경 원자적 (lock 안)
return tracker.withLock(() -> {
    Long questionId = questionPersister.persist(
        interviewId, QuestionType.RESUME_INTERROGATION, result.question(), snapshot.orderIndex());
    applyDecision(tracker, result, snapshot.answerQuality(), snapshot.currentLevel());
    return new InterrogationTurnResult(
        buildResponse(result, tracker.getCurrentLevel()), questionId);
});

// ChainSnapshot record (6 필드)
record ChainSnapshot(
    String chainTopic, int currentLevel, int consecutiveStay,
    String currentProjectId, int orderIndex, int answerQuality
) {}
```

### 신규 / 수정 파일

| 파일 | 변경 |
|------|------|
| `infra/ai/context/layer/FocusLayer.java` | render / handleEmpty 변경 + log.warn 추가 |
| `infra/ai/context/layer/DialogueHistoryLayer.java` | buildWithCompaction raw fallback + log.warn 추가 |
| `infra/ai/context/InterviewContextBuilder.java` | cap 초과 throw + 메트릭 호출 |
| `infra/ai/context/metrics/ContextEngineeringMetrics.java` | 메서드 1종 신규 (`incrementTokensExceeded`). 다른 3건 = log.warn 대체 |
| `infra/ai/exception/AiErrorCode.java` | `CONTEXT_BUDGET_EXCEEDED` enum 값 추가 |
| `domain/resume/service/ResumeInterviewOrchestrator.java` | `contextBudgetExceededResponse()` + try-catch |
| `domain/resume/service/InterrogationModeHandler.java` | 3-phase lock 분리 + `ChainSnapshot` record |
| 테스트: `FocusLayerTest`, `DialogueHistoryLayerTest`, `InterviewContextBuilderTest`, `ResumeInterviewOrchestratorTest`, `InterrogationModeHandlerTest` | 신규 / 보강 (Mockist — 외부 의존 mock) |

## 위험 / 마이그레이션 / 롤백

- **위험 1**: L4 본문 절단으로 인한 LLM 응답 품질 미세 저하. 지시문 보존으로 PARSE_FAILED 5xx 전이 회피. log.warn 집계로 발생 빈도 추적, 1주 운영 후 cap 재튜닝 검토 (별도 작업).
- **위험 2**: L3 raw fallback 으로 압축 in-flight 동안 토큰 일시 증가 → 전체 cap 초과 가능성 증가. 동일 turn 에서 P1-2 graceful 종료 발동 가능 (compatible 동작, race 손실 X).
- **위험 3**: Interrogation 3-phase 분리 시 phase 2 (lock 밖) 에서 race 발생 가능 (동시 turn 두 개가 같은 currentLevel 진입). product-spec AC 범위 외 (별도 결함). 본 변경은 AC "차단 X" 만 보장.
- **위험 4**: 비-Resume 트랙 (일반 면접 follow-up / 단독 IntentClassifier 등) cap 초과 시 catch 부재 → 5xx 회귀 가능. 메트릭 `tokens.exceeded` callType 태그 운영 모니터링 후 별도 후속 작업 처리.
- **마이그레이션 전략**: 코드 변경만, 스키마 / 데이터 변경 없음. zero-downtime 배포 가능.
- **롤백 시나리오**: 운영 메트릭 / 로그에서 비정상 발견 시 `git revert` 후 재배포. feature flag 미도입 사유 = 단순 변경 + 회귀 테스트 충분.
- **TODO 부채 해소**: `DialogueCompactor.java:65 // TODO when sync fallback added (deferred from Task 3)` = 본 변경의 L3 raw fallback 도입으로 sync fallback 의 의도 (압축 미완료 시 옛 turn 컨텍스트 누락 차단) 가 raw fallback 으로 대체 충족 → TODO 주석 제거.

## 분기 결정

- [x] **단일 영역 (BE only) → `implement.md` 1개**
- [ ] BE+FE 동시
- [ ] BE 선행 강제

분리 사유 / PR 분할:
- 본 plan 5건 모두 BE only. 단일 implement.md.
- PR 분할 = 구현 단계 결정. 권장: PR 2개
  - PR #1: P0 2건 (L4 cap truncate + 미등록 callType graceful) + log.warn 2종 → 5xx 차단 우선 머지
  - PR #2: P1 3건 (L3 raw fallback / 전체 cap graceful 종료 / Interrogation 3-phase) + 메트릭 1종 (`tokens.exceeded`) + log.warn 2종
- 둘 다 회귀 테스트 통과 필수.
- PR #1 머지 후 PR #2 = 동일 파일 (`ContextEngineeringMetrics`, `FocusLayer`) 추가 변경 가능성 → develop rebase 정책: PR #2 = PR #1 머지 직후 develop 동기화 후 작업 시작. conflict 발생 시 PR #2 에서 해소.
