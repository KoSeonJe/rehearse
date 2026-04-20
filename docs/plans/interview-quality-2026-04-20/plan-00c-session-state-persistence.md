# Plan 00c: Session State Persistence Design (Phase 0) `[parallel:00b]`

> 상태: Draft
> 작성일: 2026-04-20
> 주차: W2 초반 (2-3일)
> 해결 RC: RC2(영속화 경계 미정), Missing(동시성, 메모리 풋프린트)

## Why

plan-05/06/08/09가 만드는 신규 상태 — `ResumeSkeleton`, `InterviewPlan`, `RubricScore[]`, `SessionFeedback` — 의 **저장소가 결정되지 않은 채** 각 plan이 "세션 캐시"로 뭉뚱그렸다. 이로 인해:
- 서버 재시작 시 진행중 세션 상태 유실
- 기존 `Interview` aggregate와 신규 상태의 트랜잭션 경계 불명
- 동시 답변 시 race condition 무방어
- Flyway 마이그레이션 V24~ 계획 부재 → 프로덕션 배포 불가

**근본 해결**: 상태를 4계층으로 분류 → 각 계층의 저장소/TTL/consistency를 **먼저 결정** → 후속 plan은 이 결정만 소비.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `docs/plans/interview-quality-2026-04-20/STATE_DESIGN.md` | 신규. 4계층 분류 + 저장소 결정 + 동시성 정책 |
| `backend/src/main/resources/db/migration/V24__create_resume_skeleton.sql` | 신규 |
| `backend/src/main/resources/db/migration/V25__create_interview_plan.sql` | 신규 |
| `backend/src/main/resources/db/migration/V26__create_rubric_score.sql` | 신규 |
| `backend/src/main/resources/db/migration/V27__create_session_feedback.sql` | 신규 |
| `backend/src/main/resources/db/migration/rollback/V24__rollback.sql` ~ `V27__rollback.sql` | 신규 롤백 스크립트 |
| `backend/src/main/java/com/rehearse/api/domain/interview/runtime/InterviewRuntimeState.java` | 신규. request-scoped 런타임 상태 (covered_claims, active_chain, analysis_cache) |
| `backend/src/main/java/com/rehearse/api/domain/interview/runtime/InterviewRuntimeStateStore.java` | 신규. Caffeine 기반 in-memory store (세션 ID → RuntimeState, 2h TTL) |
| `backend/src/main/java/com/rehearse/api/domain/interview/lock/InterviewLockService.java` | 신규. interview.id 단위 pessimistic lock (동시 답변 방지) |
| `backend/build.gradle.kts` | `com.github.ben-manes.caffeine:caffeine` 의존성 추가 확인 (이미 있으면 스킵) |

## 상세

### 4계층 상태 분류

| 계층 | 예시 | 저장소 | 이유 | TTL |
|------|------|--------|------|------|
| **L1. 영속 핵심 도메인** | `Interview`, `Question`, `Feedback` (기존) | MySQL (JPA) | 감사/통계/리플레이 필요 | 영구 |
| **L2. 영속 보조 도메인 (신규)** | `ResumeSkeleton`, `InterviewPlan`, `RubricScore`, `SessionFeedback` | MySQL (JPA) | 세션 종료 후에도 조회/재생성 가능해야 함. 피드백은 사용자가 나중에 다시 봄 | 영구 or 90일 |
| **L3. 런타임 상태** | `covered_claims`, `active_chain`, `current_level`, `playground_turns`, `turn analysis cache` | **Caffeine in-memory (RuntimeStateStore)** | 초단위 업데이트, 세션 종료 후 불필요. DB 라운드트립 비용 회피 | 세션 idle 2h |
| **L4. 계산 캐시** | `ResumeSkeleton` (세션 동안만) | Caffeine | plan-05에서 추출된 Skeleton을 세션 내 재사용. 영구 저장은 사용자 동의 시에만 | 2h |

### 주요 결정
1. **Redis 도입 안 함**: 단일 인스턴스 배포 + Caffeine로 충분. 향후 multi-node 확장 시점에 Redis 이관 검토.
2. **`InterviewSession` 엔티티 신설 안 함**: 기존 `Interview` aggregate가 L1/L2를 담당하고, 런타임 상태는 `InterviewRuntimeState` POJO로 분리.
3. **ResumeSkeleton은 L2(영속) + L4(캐시) 이중**: 사용자가 저장 동의한 경우만 L2에 저장, 기본값은 L4 세션 캐시. 추출 1회 + 세션 동안만 메모리.
4. **RubricScore는 턴마다 즉시 영속화**(L2): 세션 중 서버 재시작되어도 plan-09 Synthesizer가 DB에서 읽어 복구 가능.

### Flyway 마이그레이션 스키마

#### V24: resume_skeleton
```sql
CREATE TABLE resume_skeleton (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  interview_id BIGINT NOT NULL,
  file_hash VARCHAR(64) NOT NULL,
  candidate_level VARCHAR(16) NOT NULL,
  target_domain VARCHAR(32),
  skeleton_json JSON NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_interview (interview_id),
  INDEX idx_file_hash (file_hash),
  CONSTRAINT fk_resume_skeleton_interview FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE
);
```

#### V25: interview_plan
```sql
CREATE TABLE interview_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  interview_id BIGINT NOT NULL UNIQUE,
  plan_json JSON NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_interview_plan_interview FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE
);
```

#### V26: rubric_score (plan-08 Rubric Family 반영)
```sql
CREATE TABLE rubric_score (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  interview_id BIGINT NOT NULL,
  turn_id BIGINT NOT NULL,
  rubric_id VARCHAR(64) NOT NULL,         -- e.g., "concept-cs-fundamental-v1"
  scored_dimensions JSON NOT NULL,        -- e.g., ["D2","D3","D4","D8"]
  scores_json JSON NOT NULL,              -- {"D2": {score, observation, evidence_quote}, ...}
  level_flag VARCHAR(64),                 -- optional, e.g., "mid_expected_but_measured_junior_depth"
  created_at DATETIME NOT NULL,
  INDEX idx_interview_turn (interview_id, turn_id),
  INDEX idx_rubric (rubric_id),
  CONSTRAINT fk_rubric_score_interview FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE
);
```

- `rubric_id`: 어느 카테고리 rubric이 적용됐는지(인덱싱 → 카테고리별 품질 분석 쿼리 고속화)
- `scored_dimensions`: 해당 턴에서 평가된 차원 ID 배열 (mode/intent에 따라 가변)
- `level_flag`: 레벨 기대치 미달 시 (`junior_expected_but_below` 등) — 선택적
- `scores_json`: DimensionScore 맵 (score 1~3 또는 null, observation, evidence_quote)

#### V27: session_feedback
```sql
CREATE TABLE session_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  interview_id BIGINT NOT NULL UNIQUE,
  overall_json JSON NOT NULL,
  strengths_json JSON NOT NULL,
  gaps_json JSON NOT NULL,
  delivery_json JSON,
  week_plan_json JSON NOT NULL,
  synthesizer_model VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_session_feedback_interview FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE
);
```

각 V2X 마다 대응 rollback SQL(`DROP TABLE IF EXISTS ...`).

### 동시성 정책

사용자가 빠르게 연속 답변 전송 시 race condition 방지:
```java
@Service
public class InterviewLockService {
    private final StripedLock stripedLock = new StripedLock(256);

    public <T> T withLock(Long interviewId, Supplier<T> action) {
        Lock lock = stripedLock.get(interviewId);
        lock.lock();
        try { return action.get(); } finally { lock.unlock(); }
    }
}
```
`FollowUpService.generateFollowUp()` 진입부에서 `lockService.withLock(interview.getId(), () -> ...)` 감쌈.

### 메모리 풋프린트 추정

| 상태 | 세션당 크기 | 동시 세션 100개 | 동시 세션 500개 |
|---|---|---|---|
| `InterviewRuntimeState` (L3) | ~5 KB | 500 KB | 2.5 MB |
| `ResumeSkeleton` (L4) | ~30 KB | 3 MB | 15 MB |
| 합계 | ~35 KB | 3.5 MB | 17.5 MB |

**결론**: 단일 t3.small(2 GB RAM)에서도 500 동시세션 여유. OOM 리스크 낮음.

### InterviewRuntimeStateStore 설계
```java
@Component
public class InterviewRuntimeStateStore {
    private final Cache<Long, InterviewRuntimeState> cache = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofHours(2))
        .maximumSize(10_000)
        .recordStats()
        .build();

    public InterviewRuntimeState getOrInit(Long interviewId, Supplier<InterviewRuntimeState> init) { ... }
    public void update(Long interviewId, Consumer<InterviewRuntimeState> mutator) { ... }
    public void evict(Long interviewId) { ... }   // 세션 종료 시 호출
}
```

## 담당 에이전트

- Implement: `database-architect` + `backend-architect` — 스키마 + 런타임 상태 분리 설계
- Implement: `backend` — Flyway 파일 + Store/Lock 서비스
- Review: `database-optimization` — 인덱스 설계, JSON 컬럼 쿼리 패턴
- Review: `architect-reviewer` — L1~L4 경계 준수, 기존 `Interview` aggregate와 신규 테이블 관계

## 검증

1. `./gradlew flywayMigrate` 로컬(H2 아님, mysql-dev) 통과
2. 각 V2X rollback SQL로 정상 롤백 확인
3. `InterviewRuntimeStateStore` 동시성 테스트(JUnit + 100 스레드) — 동일 interviewId 업데이트 race 없음
4. `InterviewLockService.withLock()` 블로킹 동작 검증
5. Caffeine 메트릭(`rehearse.runtime.state.cache.{hits,misses,evictions}`)이 Actuator로 노출
6. STATE_DESIGN.md가 plan-05/06/08/09의 "영속화" 결정 근거로 참조됨
7. 기존 `./gradlew test` 회귀 0건
8. `progress.md` 00c → Completed
