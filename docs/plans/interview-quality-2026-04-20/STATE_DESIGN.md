# State Design — Session State Persistence

> 작성일: 2026-04-21
> Plan 참조: plan-00c-session-state-persistence.md
> 상태: Confirmed

## 4계층 상태 분류

후속 plan(05/06/08/09)의 모든 "어디에 저장하는가" 결정은 이 문서를 근거로 삼는다.

| 계층 | 대상 | 저장소 | TTL | 이유 |
|------|------|--------|-----|------|
| **L1. 영속 핵심 도메인** | `Interview`, `Question`, `Feedback` (기존) | MySQL (JPA) | 영구 | 감사·통계·리플레이 필요 |
| **L2. 영속 보조 도메인** | `ResumeSkeleton`, `InterviewPlan`, `RubricScore`, `SessionFeedback` | MySQL (JPA, V24~V27) | 영구 or 90일 | 세션 종료 후 조회·재생성 가능해야 함. 피드백은 사용자가 나중에 다시 봄 |
| **L3. 턴 워킹 메모리** | `coveredClaims`, `activeChain`, `currentLevel`, `playgroundTurns`, `turnAnalysisCache` | Caffeine in-memory (`InterviewRuntimeStateStore`) | 세션 상한 8h (write 기준) | **LLM 재호출 비용 회피가 일차 목적**. 턴 hot path 에서 DB/LLM 재조회 회피. SoT 필드 포함 → 세션 중 축출 금지 |
| **L4. 계산 캐시** | `ResumeSkeleton` (세션 동안만) | Caffeine (`InterviewRuntimeStateStore` 내 필드) | L3 공유 | plan-05 Skeleton을 세션 내 재사용. 영구 저장은 사용자 동의 시에만 |

## 주요 결정사항

### D1. Redis 도입 안 함
단일 인스턴스 배포 + Caffeine으로 충분. 향후 multi-node 확장 시점에 Redis 이관 검토.
Rejected: Redis — 운영 복잡도 증가, 현 트래픽 규모에서 오버엔지니어링.

### D2. `InterviewSession` 엔티티 신설 안 함
기존 `Interview` aggregate가 L1/L2를 담당하고, 런타임 상태는 `InterviewRuntimeState` POJO로 분리.
Rejected: `InterviewSession` 엔티티 — `Interview`와 1:1이면서 동일 lifecycle. aggregate 분열 비용 대비 이득 없음.

### D3. ResumeSkeleton 이중 저장 (L2 + L4)
사용자가 저장 동의한 경우만 L2에 영속화. 기본값은 L4 세션 캐시로 처리.
→ plan-05 구현 시 동의 플래그를 확인 후 L2 저장 여부 결정.

### D4. RubricScore 턴마다 즉시 영속화 (L2)
세션 중 서버 재시작되어도 plan-09 Synthesizer가 DB에서 읽어 복구 가능.
Rejected: 세션 종료 시 일괄 저장 — 서버 재시작 시 점수 유실 위험.

### D5. 동시성 제어: Caffeine.asMap().compute()
별도 lock 서비스를 두지 않는다. `InterviewRuntimeStateStore.update()` 는
`cache.asMap().compute(interviewId, (id, existing) -> { mutator.accept(state); return state; })`
패턴으로 구현. CHM 수준에서 **동일 key 에 대한 read-modify-write 가 직렬화**되므로 외부 락 불필요.
다른 interviewId 는 독립 병렬 실행.
Rejected: StripedLock 자체 구현 — 내부 필드가 이미 concurrent/volatile/atomic 이고 `asMap().compute()` 로 동등 효과 무료 획득. 인프라 불필요.
Rejected: Guava Striped — 동일 이유.

### D6. L4 계산 캐시는 L3 POJO 인라인 필드로 관리

- 독립 Caffeine 인스턴스를 추가로 생성하지 않는다.
- `InterviewRuntimeState` 의 필드 (`resumeSkeletonCache` 등) 에 인라인.
- 사용자 저장 동의 `false` 일 때만 L4 활성화 (true 일 때는 L2 영속 저장).
- **이유**: L3/L4 가 동일 TTL (2h idle) + 동일 세션 scope + 동일 저장소(Caffeine) 를 공유. 별도 캐시는 메모리 + 관리 오버헤드만 증가.
- **제약**: multi-node 확장 시 L3/L4 동시 이관 (동일 lifecycle).

### Update Contract

후속 plan(05/06/08/09) 이 `store.update()` 를 호출할 때:

1. **mutator 안에서 DB/IO 금지**: `asMap().compute()` 의 매핑 함수는 동일 key 에 대한 다른 `compute` 호출을 블록한다. LLM 호출·DB 쓰기 등 장시간 작업은 mutator 밖에서 수행한 뒤 결과만 mutator 로 주입.
2. **DB 트랜잭션 경계는 상위 서비스 책임**: Store 는 in-memory 상태만 담당. DB 쓰기는 `@Transactional` 메서드에서 별도 처리.
3. **중복 요청 방어는 상위에서**: 동일 interviewId 에 대한 답변 중복 전송은 컨트롤러 레벨 idempotency (request dedup) 로 막는다. Store 의 직렬화는 state 일관성만 보장하며 중복 턴 생성을 막지 않는다.

## L2 테이블 스키마 (V24~V27)

### V24: resume_skeleton
- `interview_id` FK → `interview.id` CASCADE DELETE
- `skeleton_json` JSON — plan-05가 추출한 후보 클레임 구조체
- `file_hash` — 동일 이력서 재사용 판단용 인덱스

### V25: interview_plan
- `interview_id` UNIQUE FK — 면접당 1개 플랜
- `plan_json` JSON — plan-06이 생성하는 질문 전략 구조체

### V26: rubric_score
- `(interview_id, turn_id)` 복합 인덱스 — 턴별 조회
- `rubric_id` 인덱스 — 카테고리별 품질 분석 쿼리 고속화
- `scored_dimensions` JSON — 해당 턴에서 평가된 차원 ID 배열 (mode/intent에 따라 가변)
- `scores_json` JSON — DimensionScore 맵 {score 1~3 또는 null, observation, evidence_quote}
- `level_flag` — 레벨 기대치 미달 시 선택적 플래그

### V27: session_feedback
- `interview_id` UNIQUE FK — 면접당 1개 최종 피드백
- 구조별 JSON 컬럼 분리 (overall/strengths/gaps/delivery/week_plan) — plan-09 Synthesizer 출력 그대로 매핑

## L3 런타임 상태 필드

`InterviewRuntimeState` POJO (Caffeine value):

| 필드 | 타입 | 관리 주체 | 의미 |
|------|------|----------|------|
| `coveredClaims` | `Set<String>` | plan-05 ClaimTracker | Skeleton 에서 세션 중 다룬 claim_id 집합. Playground → Interrogation 전환 조건(60% 커버) 판정 근거 |
| `activeChain` | `List<Long>` | plan-06 ChainManager | 현재 interrogation chain 의 질문 ID 흐름. **Chain level = `activeChain.size()`** 로 도출 (1=L1, 2=L2, ...) |
| `currentLevel` | `String` (volatile) | plan-08 RubricScorer | **사용자 측정 레벨** (`junior`/`mid`/`senior`). 세션 중 rubric 채점 누적으로 업데이트 가능. **Chain level (L1~L4) 과 무관** |
| `playgroundTurns` | `int` (AtomicInteger) | plan-07 PlaygroundModeHandler | Playground 모드에서 누적 턴 수. max_turns 하드리밋 체크용 |
| `turnAnalysisCache` | `Map<Long, TurnAnalysis>` | plan-08 | question_id → AnswerAnalyzer 결과 캐시. 같은 턴 분석 재호출 방지 (LLM 재호출 비용 회피) |
| `resumeSkeletonCache` | `CachedResumeSkeleton` | plan-05 (L4 캐시 겸용) | Skeleton 전체를 세션 내 1회 추출 후 재사용 |
| `startedAt` | `Instant` (volatile) | plan-07 ClockWatcher | **신규 (2026-04-22)**. 세션 시작 시각. `remainingMinutes()` 계산 + WRAP_UP 전이 판정용 |

### ⚠️ `currentLevel` vs Chain level 혼동 금지

- **`currentLevel`** (사용자 측정 레벨, junior/mid/senior) → plan-08 `level_expectations` (`must_reach_2: all`, `must_reach_3: [D2, D4]`) 적용 기준
- **Chain level** (L1 WHAT / L2 HOW / L3 WHY_MECH / L4 TRADEOFF) → `activeChain.size()` 로 **항상** 도출. 별도 필드로 저장하지 않음
- plan-08 D10 Chain Depth 채점 시 `currentChainLevel` 파라미터로 전달되는 값은 **`activeChain.size()`**, **`currentLevel` 아님**

두 값의 생명주기도 다름:
- `currentLevel`: 세션 중 1~수회 업데이트 (레벨 재측정)
- Chain level: 매 턴 변경 (LEVEL_UP, LEVEL_STAY, CHAIN_SWITCH 시 activeChain 변화)

## 메모리 풋프린트 추정

| 상태 | 세션당 | 100 동시세션 | 500 동시세션 |
|------|--------|------------|------------|
| `InterviewRuntimeState` (L3) | ~5 KB | 500 KB | 2.5 MB |
| `resumeSkeletonCache` (L4) | ~30 KB | 3 MB | 15 MB |
| 합계 | ~35 KB | 3.5 MB | 17.5 MB |

단일 t3.small (2 GB RAM) 기준 500 동시세션 여유. OOM 리스크 낮음.

## Micrometer 메트릭

`InterviewRuntimeStateStore`가 `rehearse.runtime.state.cache.*` 접두사로 노출:

| 메트릭 | 설명 |
|--------|------|
| `rehearse.runtime.state.cache.hits` | 캐시 히트 수 |
| `rehearse.runtime.state.cache.misses` | 캐시 미스 수 |
| `rehearse.runtime.state.cache.evictions` | 만료/강제 제거 수 |

## 후속 plan 소비 방법

| Plan | 소비 방법 |
|------|----------|
| plan-05 (ResumeSkeleton) | `InterviewRuntimeStateStore.getOrInit()` → `resumeSkeletonCache` 필드 읽기/쓰기. 저장 동의 시 L2 `resume_skeleton` 테이블 저장 |
| plan-06 (InterviewPlan) | 플랜 생성 후 L2 `interview_plan` 테이블에 즉시 영속화 + `activeChain` 필드를 L3에 캐시 |
| plan-08 (RubricScorer) | 턴마다 LLM 채점 실행 → L2 `rubric_score` 즉시 INSERT → `store.update()` 로 `turnAnalysisCache` L3 갱신 (LLM 호출은 update() 바깥) |
| plan-09 (Synthesizer) | 세션 종료 시 `rubric_score` 전체 조회 → 종합 → L2 `session_feedback` 저장 → `InterviewRuntimeStateStore.evict()` 호출 |
