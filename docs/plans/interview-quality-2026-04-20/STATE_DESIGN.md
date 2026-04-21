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
| **L3. 런타임 상태** | `covered_claims`, `active_chain`, `current_level`, `playground_turns`, `turn analysis cache` | Caffeine in-memory (`InterviewRuntimeStateStore`) | idle 2h | 초단위 업데이트, 세션 종료 후 불필요. DB 라운드트립 비용 회피 |
| **L4. 계산 캐시** | `ResumeSkeleton` (세션 동안만) | Caffeine (`InterviewRuntimeStateStore` 내 필드) | 2h | plan-05 Skeleton을 세션 내 재사용. 영구 저장은 사용자 동의 시에만 |

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

### D5. 동시성 제어: StripedLock 자체 구현
Guava `Striped<Lock>` 대신 `ReentrantLock[]` 256개 배열 자체 구현.
이유: 외부 의존성(Guava) 추가 없이 동일 효과 달성 가능. 구현 코드 20줄 이내.
Rejected: Guava Striped — 추가 의존성, 현재 프로젝트에 Guava 미사용.

### D6. L4 계산 캐시는 L3 POJO 인라인 필드로 관리

- 독립 Caffeine 인스턴스를 추가로 생성하지 않는다.
- `InterviewRuntimeState` 의 필드 (`resumeSkeletonCache` 등) 에 인라인.
- 사용자 저장 동의 `false` 일 때만 L4 활성화 (true 일 때는 L2 영속 저장).
- **이유**: L3/L4 가 동일 TTL (2h idle) + 동일 세션 scope + 동일 저장소(Caffeine) 를 공유. 별도 캐시는 메모리 + 관리 오버헤드만 증가.
- **제약**: multi-node 확장 시 L3/L4 동시 이관 (동일 lifecycle).

### Lock Acquisition Contract

후속 plan(05/06/08/09) 이 `withLock` + `@Transactional` 을 조합할 때 반드시 아래 순서를 지킨다.

1. **lock outer → txn inner**: `withLock` / `tryLock` 은 `@Transactional` 메서드 **바깥**에서 획득한다. 트랜잭션 커밋 전에 락이 해제되면 후속 스레드가 미완료 상태를 읽는다.
2. **단일 interviewId 원칙**: 락 블록 안에서는 동일 `interviewId` 의 DB 작업만 수행한다. 복수 interview 업데이트는 DB 트랜잭션 isolation 에 의존한다.
3. **무한 블로킹 방지**: 운영 환경에서는 `withLock` 대신 `tryLock(interviewId, timeout, action)` 사용을 권장한다. timeout 초과 시 `LockAcquisitionException` 발생.
4. **재진입 허용**: `ReentrantLock` 기반이므로 동일 스레드에서 중첩 호출 가능 — 데드락 없음.

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

| 필드 | 타입 | 관리 주체 |
|------|------|----------|
| `coveredClaims` | `Set<String>` | plan-05 ClaimTracker |
| `activeChain` | `List<Long>` | plan-06 ChainManager |
| `currentLevel` | `String` | plan-08 RubricScorer |
| `playgroundTurns` | `int` | plan-09 Synthesizer |
| `turnAnalysisCache` | `Map<Long, Object>` | plan-08 |
| `resumeSkeletonCache` | `Object` | plan-05 (L4 캐시 겸용) |

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
| plan-08 (RubricScorer) | 턴마다 `InterviewLockService.withLock()` 안에서 채점 → L2 `rubric_score` 즉시 INSERT + `turnAnalysisCache` L3 갱신 |
| plan-09 (Synthesizer) | 세션 종료 시 `rubric_score` 전체 조회 → 종합 → L2 `session_feedback` 저장 → `InterviewRuntimeStateStore.evict()` 호출 |
