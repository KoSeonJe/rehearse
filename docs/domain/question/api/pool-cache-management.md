# API: 질문 풀 캐시 관리 (`QuestionPoolService` + `CacheableQuestionProvider`)

> Endpoint: **HTTP 엔드포인트 없음.** 내부 도메인 서비스. `generate-questions` 흐름이 호출.
> Action: `cache_key` 단위로 묶인 질문 풀 (`question_pool`) 의 충분성 판정 / 선택 / AI 생성 결과 저장 (with soft cap) / stampede protection.
> 관련 테이블: `question_pool` (read/write) / `question` (read — 사용자 사용 추적)
> 관련 외부 의존: `AiClient` (`CacheableQuestionProvider` 가 풀 부족 시 호출)

> ⚠️ 사용자 인스트럭션의 `CsTopicQuestionPoolService` 는 실코드 부재. 현 코드의 풀 관리 = `QuestionPoolService` (`domain/question/service/`). CS 세부 주제 (운영체제 / 네트워크 등) 는 별도 테이블 분리 X — 단일 `question_pool` 의 `cache_key = {level}:CS_FUNDAMENTAL` + `category` (CsSubTopic.categoryName) 필터로 처리.

---

## 입력 (서비스 메서드 시그니처)

| 메서드 | 시그니처 | 책임 |
|--------|---------|------|
| `isPoolSufficient(criteria)` | `PoolSelectionCriteria → boolean` | 풀이 요구 개수의 multiplier 배만큼 있는지 |
| `selectFromPool(criteria)` | `PoolSelectionCriteria → List<QuestionPool>` | 후보 → usedPoolIds 필터 → 카테고리 분포 선택 |
| `selectWithCategoryDistribution(candidates, requiredCount)` | `List<QuestionPool>, int → List<QuestionPool>` | 카테고리별 round-robin + shuffle. candidates ≤ required 면 그대로 반환 |
| `shouldSaveToPool(cacheKey)` | `String → boolean` | 활성 풀 < `POOL_SOFT_CAP(=200)` 인지 |
| `convertAndCacheIfEligible(cacheKey, generated)` | `String, List<GeneratedQuestion> → List<QuestionPool>` | AI 결과 → `QuestionPool.create` 매핑 + 조건부 영속 (REQUIRES_NEW) |

`PoolSelectionCriteria` (record):

| 필드 | 타입 | 의미 |
|------|------|------|
| `cacheKey` | String | `QuestionCacheKeyGenerator` 산출 |
| `requiredCount` | int | 필요 질문 수 |
| `categoryFilter` | List<String> | 카테고리 화이트리스트 (CS_FUNDAMENTAL 시 `CsSubTopic.categoryName` 리스트). 없으면 모든 카테고리 |
| `usedPoolIds` | Set<Long> | 사용자가 이전 면접에서 사용한 풀 ID (제외 대상) |

---

## 출력

직접 응답 없음. 호출자 (`CacheableQuestionProvider`) 가 결과 받아 `QuestionSetAssembler` 로 `QuestionSet` 변환 후 `generate-questions` 흐름 합류.

---

## 흐름

### 1. 충분성 판정 (`isPoolSufficient`)

분기 (criteria 상태별):

| 케이스 | 판정식 |
|--------|--------|
| `usedPoolIds` 보유 | 후보 fetch (`getCandidates`) → 미사용 필터 → `availableCount ≥ ceil(requiredCount × USER_SUFFICIENCY_MULTIPLIER(=2.0))` |
| `categoryFilter` 만 보유 | `countByCacheKeyAndIsActiveTrueAndCategoryIn(cacheKey, categoryFilter) ≥ requiredCount × POOL_SUFFICIENCY_MULTIPLIER(=3)` |
| 둘 다 없음 | `countByCacheKeyAndIsActiveTrue(cacheKey) ≥ requiredCount × 3` |

> 사용자 분기는 후보 fetch (전체 row 가져오기) 하므로 비용 ↑. 표준 풀 충분 판정은 COUNT 만으로 끝나 가벼움.

### 2. 선택 (`selectFromPool` → `selectWithCategoryDistribution`)

1. `getCandidates(cacheKey, categoryFilter)` — `categoryFilter` 비면 `findByCacheKeyAndIsActiveTrue`, 있으면 `findByCacheKeyAndIsActiveTrueAndCategoryIn`
2. `usedPoolIds` 있으면 stream filter
3. `selectWithCategoryDistribution(candidates, requiredCount)`:
   - `candidates.size() ≤ requiredCount` → 그대로 반환 (모자라도 fallback 없음)
   - 그렇지 않으면:
     - `groupAndShuffleByCategory(candidates)` — 카테고리별 LinkedList Queue, 각 큐 내부 shuffle
     - `categories` 리스트 shuffle
     - while `result.size() < requiredCount`:
       - 라운드 로빈으로 카테고리 순회 → 큐 poll → result 추가
       - 빈 큐 카테고리는 categories 에서 제거
4. 결과: 카테고리 균등 + 동일 카테고리 내 무작위

### 3. AI 결과 저장 (`convertAndCacheIfEligible`)

1. `@Transactional(propagation = REQUIRES_NEW)` — 호출자 트랜잭션과 분리
2. `GeneratedQuestion → QuestionPool.create(cacheKey, content, ttsContent, category, modelAnswer, referenceType)` 매핑
3. **soft cap 가드** — `shouldSaveToPool(cacheKey)`:
   - `countByCacheKeyAndIsActiveTrue(cacheKey) < POOL_SOFT_CAP(=200)` 이면 `saveAll(pools)` + INFO 로그 (`[POOL] 저장 완료`)
   - else: 저장 생략 + INFO 로그 (`[POOL] soft cap 도달, DB 저장 생략`). 호출자는 어쨌든 pools 받아 즉시 사용
4. 반환은 항상 변환된 `pools` (영속 여부 무관)

### 4. Stampede protection (`CacheableQuestionProvider.generateWithStampedeProtection`)

본 서비스 책임 외이지만 풀 캐시 무결성에 필수:

1. `QuestionGenerationLock.acquire(cacheKey)` — `ConcurrentHashMap<String, ReentrantLock>` 에서 동일 cacheKey lock 획득 (`computeIfAbsent` + `lock.lock()`)
2. 락 보유 동안 `usedPoolIds` 재조회 + `isPoolSufficient` 재판정 (락 대기 중 다른 호출이 풀 채웠을 가능성)
3. 여전히 부족 → AI 호출 → `convertAndCacheIfEligible`
4. categoryFilter 매칭 시 generated → 카테고리별 분포 선택
5. finally `lock.release()` (`unlock()`)

> ⚠️ **단일 JVM 한정** (Issue #407 추적). 멀티 인스턴스 / 컨테이너 배포 시 인스턴스별 동시 AI 호출 가능 — 분산 락 / 외부 캐시 도입 정책 미정.

### 5. 비활성화 (`QuestionPool.deactivate()`)

- 도메인 메서드 (`is_active = false`)
- 단방향 (재활성화 메서드 없음)
- 호출 경로 — 코드 스캔상 명시 호출처 확인 필요. 운영 SQL / 별도 정리 스크립트 가능성. ⚠️ **사용처 추적 필요** (Issue #407 비스코프).

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| 활성 풀 ≥ 200 + AI 호출 발생 (cap 초과 후 stampede 방어 실패 시나리오) | DB 저장 생략 + 호출자에게는 즉시 반환 (사용자에게 답변은 보장) |
| 풀 후보 < requiredCount | `selectWithCategoryDistribution` 가 가용분만 반환 (부족분 fallback 없음). 호출자가 fallback 필요 (현 코드는 그대로 사용) |
| 동일 cacheKey 동시 호출 | `QuestionGenerationLock` 직렬화. 첫 호출이 풀 채우면 후속 호출은 락 후 즉시 풀 히트 |
| `categoryFilter` 매칭 풀 0개 | candidates 빈 리스트 → `selectWithCategoryDistribution` 가 빈 후보 반환 → 호출자가 generated 전체에서 fallback (`CacheableQuestionProvider.generateWithStampedeProtection` 마지막 분기) |
| `usedPoolIds` size 가 풀 크기와 비등 | `availableCount` 부족 → AI 호출 트리거. 사용자 다양성 보장 |
| 동일 `(cacheKey, content)` 중복 INSERT | UNIQUE 부재 (위 schema.md 참조). 풀 비대화 가능성 — POOL_SOFT_CAP 만 방어 |
| `isPoolSufficient` 가 `categoryFilter` + `usedPoolIds` 동시 가질 때 | `usedPoolIds` 분기 (categoryFilter 가 그 분기 내 candidates fetch 에 적용됨 — `getCandidates(cacheKey, categoryFilter)`) |

---

## 캐시 키 정책 (`QuestionCacheKeyGenerator`)

| InterviewType | 키 형식 | 사유 |
|---------------|---------|------|
| `CS_FUNDAMENTAL` | `{level}:CS_FUNDAMENTAL` | 포지션·스택 무관 (CS 본질). category 컬럼 (`CsSubTopic.categoryName`) 으로 세부 주제 분류 |
| `BEHAVIORAL` | `{level}:BEHAVIORAL` | 포지션·스택 무관 (행동 면접 공통) |
| `SYSTEM_DESIGN` | `{level}:SYSTEM_DESIGN` | 포지션·스택 무관 (시스템 설계 공통) |
| 외 (LANGUAGE_FRAMEWORK / FULLSTACK_STACK / UI_FRAMEWORK / 등) | `{position}:{level}:{techStack}:{type}` | 포지션·스택 특화 |

→ 의도: 캐시 데이터 1벌로 전 포지션 커버 (POSITION_AGNOSTIC). 풀 hit ratio 극대화.

### TTL / Eviction 정책

- ❓TODO(사용자 확인) — Issue #407 비스코프 (정책 미결):
  - **TTL 부재** — `question_pool` row 는 `is_active = false` 까지 영속. `created_at` 기준 자동 만료 X
  - **Eviction 부재** — soft cap (200) 만 신규 INSERT 차단. 기존 row 회수 정책 부재
  - **모델 버전 변경 시 invalidate** — AI 모델 변경 / 프롬프트 개정 시 기존 풀 강제 `deactivate()` 정책 미정

---

## 관찰성

- **로그**:
  - `QuestionPoolService` — `[POOL] 저장 완료: cacheKey={}, count={}` / `[POOL] soft cap 도달, DB 저장 생략: cacheKey={}, count={}` (INFO)
  - `CacheableQuestionProvider` — `[CACHE] pool 히트: cacheKey={}, required={}, usedByUser={}` / `[CACHE] pool 부족, AI 호출: cacheKey={}, usedByUser={}` / `[CACHE] lock 후 pool 히트: cacheKey={}` / `[CACHE] AI 호출 실패: cacheKey={}` (INFO/ERROR)
- **메트릭**: 풀 자체 메트릭 미정 (❓TODO Issue #407). pool hit ratio / cache_key 별 size / soft cap 도달 횟수 등 미수집.
- **알람**: 미정 (❓TODO Issue #407). pool 비대화 / hit ratio 급락 / 멀티 인스턴스 stampede 등.

---

## 동시성 / 트랜잭션 노트

- 클래스 단위 `@Transactional(readOnly = true)` (count / find 메서드)
- `convertAndCacheIfEligible` = `@Transactional(propagation = REQUIRES_NEW)` — AI 호출 결과 저장 분리. 호출자 트랜잭션 길어지지 않음
- `QuestionGenerationLock` = JVM 인메모리 (`ConcurrentHashMap<String, ReentrantLock>`). cacheKey 종류 유한 → 락 객체 누수 미고려
- `selectWithCategoryDistribution` = 순수 메서드 (DB 호출 없음). 호출자 트랜잭션 영향 X
- ⚠️ **잠재 race** (Issue #407 추적):
  - 멀티 인스턴스 — `QuestionGenerationLock` 인스턴스간 무효 → 동일 cacheKey 동시 AI 호출 가능
  - `shouldSaveToPool` 검사와 `saveAll` 사이 race — soft cap 미세 초과 가능 (전체 일관성에는 영향 없음)
  - `findUsedQuestionPoolIdsByUserIdAndCacheKey` 와 selectFromPool 사이 race — 사용자 동시 면접 생성 시 같은 풀 row 받을 가능성. 단 풀 row 자체는 immutable 이라 데이터 무결성 OK

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.question.repository.QuestionPoolRepository` | count / find 쿼리 | calls |
| `com.rehearse.api.domain.question.repository.QuestionRepository` | `findUsedQuestionPoolIdsByUserIdAndCacheKey` (사용자 사용 풀 ID) | calls — `CacheableQuestionProvider.findUsedPoolIds` |
| `com.rehearse.api.domain.question.entity.QuestionPool` | 풀 entity | calls — `create()` 팩토리, `deactivate()` |
| `com.rehearse.api.domain.question.entity.CsSubTopic` | CS 세부 주제 enum (한글 라벨 매핑) | calls — `CacheableQuestionProvider.toCategoryFilter` |
| `com.rehearse.api.domain.question.service.QuestionGenerationLock` | cacheKey lock | calls — `CacheableQuestionProvider.generateWithStampedeProtection` |
| `com.rehearse.api.domain.question.service.QuestionCacheKeyGenerator` | 캐시 키 산출 | calls — `CacheableQuestionProvider.provide` |
| `com.rehearse.api.domain.question.service.PoolSelectionCriteria` | 선택 기준 record | calls |
| `com.rehearse.api.infra.ai.AiClient` | LLM 진입점 (풀 부족 시) | calls — `CacheableQuestionProvider.generateWithStampedeProtection` |
| `com.rehearse.api.infra.ai.dto.{GeneratedQuestion, QuestionGenerationRequest}` | AI 호출 DTO | calls |

---

## 정책 출처

- 비즈니스 룰: `docs/domain/question/schema.md` `question_pool` 섹션 (불변 / 정책)
- 임계값 (직접 인용):
  - `QuestionPoolService.POOL_SUFFICIENCY_MULTIPLIER = 3`
  - `QuestionPoolService.USER_SUFFICIENCY_MULTIPLIER = 2.0`
  - `QuestionPoolService.POOL_SOFT_CAP = 200`
  - `QuestionCacheKeyGenerator.POSITION_AGNOSTIC_TYPES = {CS_FUNDAMENTAL, BEHAVIORAL, SYSTEM_DESIGN}`
- ❓TODO(사용자 확인) — Issue #407 비스코프 (정책 미결):
  1. TTL / eviction 정책 (현재 deactivate() 만, 자동 만료 X)
  2. 모델 버전 변경 시 invalidate 정책
  3. 멀티 인스턴스 stampede 보호 (분산 락 / Redis 등)
  4. pool 메트릭 / 알람 (hit ratio / size / soft cap 도달)
  5. `QuestionPool.deactivate()` 호출 경로 추적 (운영 SQL 인지 자동 정리인지)
  6. `(cache_key, content)` UNIQUE / dedup 도입 (현재 중복 INSERT 가능, schema.md 참조)
