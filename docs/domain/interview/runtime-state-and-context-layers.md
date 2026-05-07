# InterviewRuntimeState & 4-Layer Context 구조

> 본 문서는 **interview 도메인의 메모리 상태 / 캐시 / AI 컨텍스트 조립** 메커니즘을 정리한다. 매 턴마다 LLM 호출에 들어가는 프롬프트가 어떻게 4개 레이어로 조립되는지, 그 입력이 되는 메모리 상태가 어디에 어떻게 저장되는지를 한 곳에서 본다.
>
> 대상 코드:
> - [`InterviewRuntimeState`](../../../backend/src/main/java/com/rehearse/api/domain/interview/entity/InterviewRuntimeState.java)
> - [`InterviewRuntimeStateCache`](../../../backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewRuntimeStateCache.java)
> - [`RuntimeCacheConfig`](../../../backend/src/main/java/com/rehearse/api/global/config/RuntimeCacheConfig.java)
> - [`InterviewContextBuilder`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/InterviewContextBuilder.java)
> - [`FixedContextLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FixedContextLayer.java) / [`SessionStateLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/SessionStateLayer.java) / [`DialogueHistoryLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/DialogueHistoryLayer.java) / [`FocusLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java)
> - [`DialogueCompactor`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/compaction/DialogueCompactor.java)

---

## 1. 큰 그림

면접 1건 = JVM 메모리 위에 살아있는 **세션 상태 객체 1개** ([`InterviewRuntimeState`](../../../backend/src/main/java/com/rehearse/api/domain/interview/entity/InterviewRuntimeState.java)) + 그 객체를 보관하는 **Caffeine 캐시 1개** ([`RuntimeCacheConfig`](../../../backend/src/main/java/com/rehearse/api/global/config/RuntimeCacheConfig.java)). 매 턴 LLM 호출 시 이 상태에서 데이터를 꺼내 4개의 컨텍스트 레이어 (L1~L4) 로 조립한 뒤 모델에 던진다.

```
┌─────────────────────────────────────────────────────────────────┐
│ Spring Bean: Cache<Long, InterviewRuntimeState>  (Caffeine)     │
│   - TTL 8h / max 10,000 / Micrometer 통계                        │
│   key = interviewId                                              │
│   value = InterviewRuntimeState (mutable POJO, 면접당 1개)        │
└──────────────────────────┬──────────────────────────────────────┘
                           │ getOrInit / get / update / evict
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ InterviewRuntimeStateCache  (Caffeine wrapper)                   │
│   - computeIfAbsent atomic 진입                                  │
│   - update = ConcurrentHashMap.compute (atomic mutate)           │
└──┬──────────────────────────────────────────────────────────────┘
   │ 면접 ID 1개당 1 인스턴스
   ▼
┌─────────────────────────────────────────────────────────────────┐
│ InterviewRuntimeState                                            │
│   ├─ L1 입력: resumeSkeletonCache, interviewPlanCache           │
│   ├─ L2 입력: currentLevel, coveredClaims, activeChain ...      │
│   ├─ L3 입력: compactedDialogueSummaries, compactionInFlight    │
│   ├─ L4 입력: turnAnalysisCache (turnId → AnswerAnalysis)        │
│   └─ 도메인 상태: ChainStateTracker, resumeMode, startedAt ...   │
└─────────────────────────────────────────────────────────────────┘
                           │ build(ContextBuildRequest)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ InterviewContextBuilder                                          │
│   L1 FixedContextLayer       (system, cache_control=true)        │
│   L2 SessionStateLayer       (system, JSON snapshot)             │
│   L3 DialogueHistoryLayer    (alternating user/assistant + 요약) │
│   L4 FocusLayer              (user, JIT per-callType)            │
│   → BuiltContext { messages, totalTokens, perLayerTokens }       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 4-Layer Context

[`ContextEngineeringProperties`](../../../backend/src/main/java/com/rehearse/api/global/config/ContextEngineeringProperties.java) (`rehearse.context-engineering.*`) 로 동작 조절 (default: [`application.yml`](../../../backend/src/main/resources/application.yml) 75~80 라인). 조립 본체 = [`InterviewContextBuilder`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/InterviewContextBuilder.java).

| Layer | 클래스 | Role | 캐싱 | 가변성 | 토큰 캡 / 정책 |
|-------|-------|------|------|--------|--------------|
| **L1** | [`FixedContextLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FixedContextLayer.java) | SYSTEM | `cache_control=true` (Anthropic ephemeral + OpenAI prompt cache) | **세션 내 불변** | callType 별 템플릿 1개. global core (보안 룰 / 출력 룰) + skeleton |
| **L2** | [`SessionStateLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/SessionStateLayer.java) | SYSTEM | 미캐싱 (매 턴 직렬화) | RuntimeState 변하면 자동 반영 | 500 token. 초과 시 `coveredClaimsRecent` 이진 절반씩 trim |
| **L3** | [`DialogueHistoryLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/DialogueHistoryLayer.java) | USER/ASSISTANT alternating + SYSTEM(요약) | 요약은 `compactedDialogueSummaries` 캐시 | 매 턴 추가 | recent window = 5턴. 초과 시 older 턴 → async 요약 → 캐시 hit 시 재사용 |
| **L4** | [`FocusLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java) | USER | 미캐싱 (JIT per-call) | callType + FocusHints sealed type 매칭 | callType 별 cap 다름 (300~1200). 초과 시 marker 블록 단위 절단 |

### L1 — [`FixedContextLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FixedContextLayer.java)
- **목적**: 모델 prompt cache 에 박힐 불변 시스템 블록.
- **구성**: `GLOBAL_CORE` (구분자/보안/출력 규칙) + callType 별 skeleton 템플릿 ([`prompts/template/`](../../../backend/src/main/resources/prompts/template/)).
- **로딩**: `@PostConstruct` 시점 일괄 로드 → `Map<String, String>` 메모리 상주.
- **주의**: `cache_control=true` 메시지 1개로 반환. callType 마다 별도 prompt cache prefix.

### L2 — [`SessionStateLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/SessionStateLayer.java)
- **목적**: 면접 진행 상태 스냅샷을 LLM 에 매 턴 알려줌.
- **입력**: `runtimeState.toSessionStateSnapshot()` →
  ```json
  {
    "level": "JUNIOR",
    "current_turn": 7,
    "covered_claims_recent": ["...", "..."],
    "active_chain": [123, 124, 125],
    "asked_perspectives": []
  }
  ```
- **trim 정책**: 500 token 초과 시 `coveredClaimsRecent` 만 절반씩 줄임 (level / activeChain 은 보존).
- **주의**: `asked_perspectives` 는 RuntimeState 에서 못 만든다 (selectedPerspective 는 FollowUpRequest 에만 있음). L3/L4 가 필요하면 `previousExchanges` 에서 직접 derive.

### L3 — [`DialogueHistoryLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/DialogueHistoryLayer.java)
- **목적**: 이전 턴 (질문/답변) 을 모델에 user/assistant 메시지로 재현.
- **분기**:
  - `exchanges.size() ≤ recentWindow(5)` → 전부 alternating 으로 그대로 렌더.
  - 초과 → `windowEnd = exchanges.size() - 5`. older 턴 = async 요약, recent 5턴 = alternating.
- **요약 캐시 키 = `windowEnd`** (정수). 같은 windowEnd 한 번 요약되면 재사용.
- **요약 트리거**:
  1. `runtimeState.getCompactedSummary(windowEnd)` 조회 → present 면 즉시 SYSTEM 메시지로 prepend.
  2. absent 면 `dialogueCompactor.compactAsync(...)` 발화. **현재 턴은 요약 없이 진행** (다음 턴부터 hit).
  3. `compactionInFlight.add(windowEnd)` atomic 으로 중복 LLM 콜 차단.
- **raw 폴백 (PR #420)**: 요약 absent + runtimeState null / 압축 in-flight / summary 부재 3가지 reason 모두 raw 대화 alternating 으로 폴백. `log.warn` 발행. 5xx 차단 목적.
- **요약 LLM**: [`DialogueCompactor`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/compaction/DialogueCompactor.java). callType=`compaction_summarizer`. temperature 0.3, max_tokens 800, JSON 강제. `@Async` executor = `vtExecutor` (가상 스레드, PR #420 — 기존 ThreadPoolTaskExecutor core=2/max=4 제거).

### L4 — [`FocusLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java)
- **목적**: callType 별 1회용 USER 프래그먼트. "이번 턴 무엇에 집중하라" 를 마커 블록으로 주입.
- **JIT 의미**: 매 콜마다 새로 만든다. 캐시 X. callType ↔ `FocusHints` sealed type 1:1 컴파일타임 매칭.
- **callType 별 cap (token)**:
  | callType | cap |
  |---------|-----|
  | `intent_classifier` | 300 |
  | `clarify_response` / `giveup_response` | 400 |
  | `resume_playground_opener` / `resume_wrap_up` | 600 |
  | `answer_analyzer` | 800 |
  | `follow_up_generator_v3` / `resume_playground_responder` | 1000 |
  | `resume_chain_interrogator` | 1200 |
- **절단 알고리즘**: cap 초과 시 marker 블록 (`<<<TAG>>> ... <<<END_TAG>>>`) 단위로 head 부터 점진 truncate. 8회 미수렴 시 강제 head cut + tail 200자 보존.
- **OFF**: `rehearse.context-engineering.l4-just-in-time=false` 면 L4 스킵 (디버깅용).

---

## 3. [`InterviewRuntimeState`](../../../backend/src/main/java/com/rehearse/api/domain/interview/entity/InterviewRuntimeState.java) 안 자료구조

면접당 1개. **모든 자료구조의 동시성 보호 방식이 다른 이유 = 액세스 패턴이 다르기 때문**.

| 필드 | 타입 | 용도 | 동시성 / 가변성 | 어느 레이어가 쓰는가 |
|------|------|------|----------------|------------------|
| `currentLevel` | `String` (final) | 후보 레벨 (예: JUNIOR) | 생성 시 고정 | L2 (`level`) |
| `coveredClaims` | `ConcurrentLinkedDeque<String>` | 다룬 claim 시간 순 | recency trim 위함 | L2 (`covered_claims_recent` 최근 50) |
| `coveredClaimsSet` | `ConcurrentHashMap.newKeySet` | dedup | Deque + Set 짝 (Deque contains O(n) 회피) | (L2 입력 dedup 보조) |
| `activeChain` | `CopyOnWriteArrayList<Long>` | 현재 추적 중인 chain (followup question id 시퀀스) | read 多 / write 少 | L2 (`active_chain`) |
| `playgroundTurns` | `AtomicInteger` | playground 모드 턴 수 | 무락 카운터 | L2 (`current_turn`), L4 (`PLAYGROUND_TURN_COUNT`) |
| `playgroundCumulativeLength` | `AtomicInteger` | playground 누적 답변 길이 | 무락 카운터 | L4 (`CUMULATIVE_UTTERANCE_LENGTH`) |
| `resumeOrderCounter` | `AtomicInteger` | resume 트랙 turn 발급 카운터 | 무락 발급 | resume 도메인 내부 (orchestrator) |
| `turnAnalysisCache` | `ConcurrentHashMap<Long, TurnAnalysis>` | turnId → [`AnswerAnalysis`](../../../backend/src/main/java/com/rehearse/api/domain/interview/entity/AnswerAnalysis.java) (or 변종) | 매 턴 put | L4 (직전 턴 분석을 다음 턴 hint 로) |
| `resumeSkeletonCache` | `volatile` [`ResumeSkeleton`](../../../backend/src/main/java/com/rehearse/api/domain/resume/entity/ResumeSkeleton.java) | 이력서 골격 (record 불변) | reference swap 안전 | L1 (resume callType skeleton 입력), [`ResumeSkeletonRuntimeCache`](../../../backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeSkeletonRuntimeCache.java) 경유 read |
| `interviewPlanCache` | `volatile` [`InterviewPlan`](../../../backend/src/main/java/com/rehearse/api/domain/resume/entity/InterviewPlan.java) | 면접 plan (JPA Entity, plan_json 컬럼) | reference swap 안전 | L1 / [`InterviewPlanRuntimeCache`](../../../backend/src/main/java/com/rehearse/api/domain/resume/service/InterviewPlanRuntimeCache.java) 경유 read |
| `compactedDialogueSummaries` | `ConcurrentHashMap<Integer, String>` | windowEnd → 요약 텍스트 | 한 번 만들면 재사용 | L3 |
| `compactionInFlight` | `ConcurrentHashMap.newKeySet<Integer>` | async compaction 중복 발화 차단 | `Set.add` atomic = 진입 게이트 | L3 / [`DialogueCompactor`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/compaction/DialogueCompactor.java) |
| `startedAt` | `volatile Instant` | 면접 시작 시각 | 1회 set | 시계 기반 분기 (`ClockWatcher`) |
| `resumeMode` | `volatile ResumeMode` | PLAYGROUND / INTERROGATION / WRAPUP | 단순 swap | resume 트랙 분기 |
| `chainStateTracker` | [`ChainStateTracker`](../../../backend/src/main/java/com/rehearse/api/domain/resume/entity/ChainStateTracker.java) (객체) | chain 진행 상태 머신 | 자체 `ReentrantLock` 보유 | resume 트랙 chain 결정 |

### 3-1. [`ChainStateTracker`](../../../backend/src/main/java/com/rehearse/api/domain/resume/entity/ChainStateTracker.java) (state machine)

`InterviewRuntimeState.chainStateTracker` 안에 산다. 수명 = 세션 수명. 동시성 = 인스턴스당 `ReentrantLock` (`withLock(action)`).

| 필드 | 의미 |
|------|------|
| `currentProjectId` | 진행 중 resume 프로젝트 |
| `currentChainId` | 진행 중 claim chain |
| `currentLevel` | 1~4 (chain 깊이). 4 초과 = chain complete |
| `consecutiveLevelStayCount` | 같은 level 머문 횟수. 2 초과 시 `levelStay()` 가 true 반환 → 강제 levelUp 트리거 |
| `completedChainIds` | 완료된 chain id 목록 (`CopyOnWriteArrayList`) |

API: `initChain → levelUp / levelStay → markChainComplete`. `resolveNextChain(projectPlans)` 가 primary → backup 순으로 미완료 chain 탐색.

---

## 4. 캐시 / 메모리 저장소 계층

| 클래스 | 역할 | 위치 |
|--------|------|-----|
| [`RuntimeCacheConfig#interviewRuntimeStateCaffeineCache`](../../../backend/src/main/java/com/rehearse/api/global/config/RuntimeCacheConfig.java) | Caffeine `Cache<Long, InterviewRuntimeState>` Bean. TTL 8h, max 10,000, Micrometer stats | Spring 컨텍스트 |
| [`InterviewRuntimeStateCache`](../../../backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewRuntimeStateCache.java) | 위 캐시 wrapper. `getOrInit / get / update / evict` 4 API. atomic mutate 보장 | `domain/interview/service/` |
| [`InterviewPlanRuntimeCache`](../../../backend/src/main/java/com/rehearse/api/domain/resume/service/InterviewPlanRuntimeCache.java) | RuntimeState.interviewPlanCache 필드 read/write facade | `domain/resume/service/` |
| [`ResumeSkeletonRuntimeCache`](../../../backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeSkeletonRuntimeCache.java) | RuntimeState.resumeSkeletonCache 필드 read/write facade. `fileHash` 미스매치 시 null 반환 | `domain/resume/service/` |
| [`DialogueCompactor`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/compaction/DialogueCompactor.java) | L3 요약 LLM 발화. 결과를 `state.putCompactedSummary(windowEnd, summary)` 에 적재. `@Async` executor = `vtExecutor` (가상 스레드, PR #420) | `infra/ai/context/compaction/` |

> 핵심: **저장소는 Caffeine 1개 뿐**. Plan/Skeleton wrapper 는 별도 캐시가 아니라 RuntimeState 안 필드로 향하는 1 hop 추가다.

### 영속성 vs 휘발성

| 데이터 | 메모리 | DB | 재시작 시 |
|--------|-------|----|----------|
| [`InterviewPlan`](../../../backend/src/main/java/com/rehearse/api/domain/resume/entity/InterviewPlan.java) | `interviewPlanCache` (volatile) | `interview_plan.plan_json` | DB 에서 lazy reload ([`InterviewPlanPersister`](../../../backend/src/main/java/com/rehearse/api/domain/resume/service/InterviewPlanPersister.java)) |
| [`ResumeSkeleton`](../../../backend/src/main/java/com/rehearse/api/domain/resume/entity/ResumeSkeleton.java) | `resumeSkeletonCache` (volatile) | `resume_skeleton` | DB 에서 lazy reload ([`ResumeSkeletonPersister`](../../../backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeSkeletonPersister.java)) |
| [`AnswerAnalysis`](../../../backend/src/main/java/com/rehearse/api/domain/interview/entity/AnswerAnalysis.java) | `turnAnalysisCache` | **❌ 없음** | **L4 입력 손실** |
| Compaction summary | `compactedDialogueSummaries` | **❌ 없음** | 다음 턴 trigger 시 재요약 (LLM 재호출) |
| `coveredClaims` | Deque + Set | **❌ 없음** | 손실 (영향: L2 covered_claims_recent 일시 비어있음) |
| [`ChainStateTracker`](../../../backend/src/main/java/com/rehearse/api/domain/resume/entity/ChainStateTracker.java) | 객체 | **❌ 없음** | 손실 (영향: chain 처음부터 다시 진행) |
| `playground*Counter` | AtomicInteger | **❌ 없음** | 손실 |

> Caffeine 자체는 in-memory only. JVM 죽으면 면접 1만 건 상태 전부 휘발. **현재 구조는 stateful = 단일 인스턴스 가정**.

---

## 5. 흐름별 사용

### 5-1. 면접 생성 (POST `/api/v1/interviews`)

> 참조: [`InterviewService`](../../../backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewService.java), [`ResumeSkeletonPersister`](../../../backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeSkeletonPersister.java), [`InterviewPlanPersister`](../../../backend/src/main/java/com/rehearse/api/domain/resume/service/InterviewPlanPersister.java)

```
InterviewService.createInterview
  ├─ interview row INSERT
  ├─ (resume 트랙) ResumeSkeletonPersister → resume_skeleton row INSERT
  └─ (resume 트랙) InterviewPlanPersister → interview_plan row INSERT
```

이 시점엔 **RuntimeState 미생성**. Caffeine 비어있음.

### 5-2. 첫 follow-up 진입 (POST `/api/v1/interviews/{id}/follow-up`)

> 참조: [`FollowUpService`](../../../backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java) · 흐름 상세 = [`docs/domain/interview/api/follow-up.md`](api/follow-up.md)

```
FollowUpService.generateFollowUp (Async, NOT_SUPPORTED)
  ├─ loadFollowUpContext (readOnly 트랜잭션)
  ├─ InterviewRuntimeStateCache.getOrInit(interviewId, () -> seed())
  │   └─ Caffeine.computeIfAbsent
  │       └─ InterviewRuntimeState.seed(level, skeleton, plan)
  │           ├─ 모든 ConcurrentXxx 자료구조 초기화
  │           ├─ resumeSkeletonCache = (DB 에서 fetch 한) skeleton
  │           ├─ interviewPlanCache = (DB 에서 fetch 한) plan
  │           └─ chainStateTracker = new ChainStateTracker()
  └─ ...
```

> `getOrInit` 의 `computeIfAbsent` 가 atomic — 첫 follow-up 동시 호출 (FE 더블 클릭 등) 에도 supplier 1회만 실행.

### 5-3. 매 턴 follow-up 흐름 (단순화)

> 참조: [`AnswerAnalyzer`](../../../backend/src/main/java/com/rehearse/api/domain/interview/service/AnswerAnalyzer.java), [`AudioTurnAnalyzer`](../../../backend/src/main/java/com/rehearse/api/domain/interview/service/AudioTurnAnalyzer.java)

```
턴 N 진입
  │
  ├─ runtimeStateStore.get(interviewId) → state
  │
  ├─ Step A: AnswerAnalyzer (LLM 콜)
  │   └─ buildContext(callType="answer_analyzer", focusHints=AnswerAnalyzerHints)
  │       ├─ L1: answer_analyzer.txt skeleton (cache_control)
  │       ├─ L2: state.toSessionStateSnapshot() → JSON
  │       ├─ L3: previousExchanges (recent 5턴 alternating) + 요약 (있으면)
  │       └─ L4: AnswerAnalyzerHints (질문/답변/recent claims)
  │   → AnswerAnalysis 결과
  │   └─ runtimeStateStore.update(id, s -> s.recordAnalysis(turnId, analysis))
  │       (turnAnalysisCache.put)
  │
  ├─ IntentDispatcher 분기 (CLARIFY / GIVE_UP / OFF_TOPIC / ANSWER)
  │
  ├─ Step B: FollowUpQuestionWriter (ANSWER 인 경우, LLM 콜)
  │   └─ buildContext(callType="follow_up_generator_v3", focusHints=FollowUpGeneratorV3Hints)
  │       └─ L4 가 직전 턴 AnswerAnalysis 를 hint 로 사용
  │
  ├─ resume 트랙: ChainStateTracker.withLock(...) → levelUp / levelStay / markComplete
  ├─ state.addCoveredClaim(claim) (claims 집어넣기, dedup)
  └─ state.activeChain.add(newQuestionId)
```

### 5-4. L3 compaction 트리거 시점

> 참조: [`DialogueHistoryLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/DialogueHistoryLayer.java), [`DialogueCompactor`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/compaction/DialogueCompactor.java)

`exchanges.size() > 5` 인 턴부터:

```
DialogueHistoryLayer.build
  ├─ windowEnd = exchanges.size() - 5
  ├─ state.getCompactedSummary(windowEnd)
  │   ├─ present  → SYSTEM 메시지 ("## DIALOGUE SUMMARY (turns 1..N)\n...") prepend → 끝
  │   └─ absent  → triggerCompactionIfPossible
  │       ├─ state.hasCompactionInFlight(windowEnd) ? skip
  │       └─ dialogueCompactor.compactAsync(...)  ← @Async("compactionExecutor")
  │           ├─ state.tryStartCompaction(windowEnd) ? race loser 빠져나옴
  │           ├─ aiClient.chat(...) (LLM 콜, callType=compaction_summarizer)
  │           ├─ state.putCompactedSummary(windowEnd, summary)
  │           └─ state.markCompactionFinished(windowEnd)
  └─ recent 5턴 alternating
```

> 주의: 첫 트리거 턴은 **요약 없이 진행**. 비동기 완료된 다음 턴부터 캐시 hit. 사용자에게 노출되는 latency 영향 없음.

### 5-5. 면접 종료 (`/status` UPDATE → COMPLETED)

> 참조: [`InterviewCompletionService`](../../../backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewCompletionService.java) · 액션 = [`docs/domain/interview/api/update-status.md`](api/update-status.md)

```
InterviewCompletionService.complete(...)
  ├─ interview.updateStatus(COMPLETED)
  └─ runtimeStateStore.evict(interviewId)
      └─ Caffeine.invalidate(id)
          → 해당 InterviewRuntimeState 객체 GC
```

---

## 6. 동시성 모델

| 시나리오 | 보호 메커니즘 |
|---------|-------------|
| 첫 follow-up 동시 호출 | `Caffeine.asMap().computeIfAbsent` atomic |
| RuntimeState 필드 mutate | `Caffeine.asMap().compute(id, mutator)` atomic |
| coveredClaims add | `Set.add(claim)` atomic 후 Deque addLast |
| compaction 중복 발화 | `compactionInFlight.add(windowEnd)` atomic gate |
| ChainStateTracker 상태 전이 | `ReentrantLock.withLock(action)` |
| activeChain read-many | `CopyOnWriteArrayList` (snapshot 안전) |
| 카운터 증분 | `AtomicInteger.getAndIncrement` |
| 단순 reference swap | `volatile` (ResumeSkeleton/InterviewPlan/ResumeMode/Instant) |

> 모든 동시성 보호가 **인스턴스 단위 (interviewId)**. 다른 면접끼리는 보호 불필요 → Caffeine 키 분리만으로 충분.

---

## 7. 디버깅 / 운영 포인트

- **L1 캐시 미스**: `cache_control` 마킹은 메시지 내용이 byte-for-byte 동일해야 hit. callType skeleton 텍스트 변경 시 prompt cache 무효화. dev 에서 토큰 비용 폭증으로 감지.
- **L2 빈 출력**: `req.runtimeState().get("interviewRuntimeState") == null` → SessionStateLayer 빈 list 반환. RuntimeState seed 누락 시 발생. `IllegalStateException` 던지지 않으니 조용히 빠진다.
- **L3 요약 stale**: 요약 캐시 키 = `windowEnd`. `recentWindow` 프로퍼티 변경 시 이전 캐시 무효화 안 됨 (다른 키이므로 새로 만들고 둘 다 메모리 점유). 운영 변경 시 Caffeine evict 필요.
- **L4 cap 초과 경고**: `[FocusLayer] L4 cap 초과 → 본문 절단` 로그. callType 별 cap (위 표) 확인. 코드: [`FocusLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java).
- **컨텍스트 토큰 budget 초과**: total context tokens > `maxContextTokens` 초과 시 `BusinessException(CONTEXT_BUDGET_EXCEEDED)` throw (PR #420). `ResumeInterviewOrchestrator` 가 이를 catch → graceful 종료 (`followUpExhausted=true`, type=`"CONTEXT_BUDGET_EXCEEDED"`). `max-context-tokens` 임계값 = yml 관리 (`application-*.yml`). 에러 코드 `AiErrorCode.CONTEXT_BUDGET_EXCEEDED` (신규, PR #420).
- **재시작 후 L4 품질 저하**: `turnAnalysisCache` 휘발 → 첫 턴 hint 비어있음. 진행 중 면접 영향 직접 측정 불가 (DB 영속 없음).
- **Caffeine 통계**: `rehearse.runtime.state.*` Micrometer 메트릭. hit ratio / size / evictions 모니터링.

---

## 8. 자주 헷갈리는 포인트

1. **"[`InterviewPlanRuntimeCache`](../../../backend/src/main/java/com/rehearse/api/domain/resume/service/InterviewPlanRuntimeCache.java) / [`ResumeSkeletonRuntimeCache`](../../../backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeSkeletonRuntimeCache.java) 가 별도 캐시인 줄 알았다"** — 아니다. RuntimeState 안 필드 read/write facade 일 뿐. 진짜 저장소 = Caffeine 1개.
2. **"L4 가 매 턴 LLM 으로 만들어지는 줄 알았다"** — 아니다. [`FocusLayer`](../../../backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java) 는 `FocusHints` 데이터를 텍스트 fragment 로 렌더링만 한다. LLM 콜 X. (L3 의 compaction summary 만 LLM 콜.)
3. **"`turnAnalysisCache` 가 DB 백업되는 줄 알았다"** — 아니다. 휘발 only. 재시작 시 손실.
4. **"L2 의 `asked_perspectives` 가 RuntimeState 에서 나오는 줄 알았다"** — 아니다. 항상 빈 리스트로 직렬화됨. selectedPerspective 는 [`FollowUpRequest.FollowUpExchange`](../../../backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpRequest.java) 에만 있어 L3/L4 가 직접 derive 해야 한다.
5. **"`compactedDialogueSummaries` 키가 turn 번호인 줄 알았다"** — 아니다. `windowEnd = exchanges.size() - recentWindow` 정수. recentWindow 프로퍼티 변경 시 키 의미 변함.
6. **"Caffeine TTL 8h 가 면접당인 줄 알았다"** — write 후 8h. 매 턴 update 마다 expireAfterWrite 가 갱신되지 않으면 진행 중 면접도 evict 가능 (현재 `expireAfterWrite` 사용 중. update 시 자동 갱신되지 않음 — 장기 면접 시 주의). 코드: [`RuntimeCacheConfig`](../../../backend/src/main/java/com/rehearse/api/global/config/RuntimeCacheConfig.java).
