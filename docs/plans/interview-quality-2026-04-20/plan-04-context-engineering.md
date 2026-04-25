# Plan 04: Context Engineering 4-layer Builder `[blocking]`

> 상태: In Progress (S8, 2026-04-26 — `feat/plan-04-context-engineering`)
> 작성일: 2026-04-20
> 주차: W3 (Resume Track 선행 필수)
> 원본: `docs/todo/2026-04-20/07-context-engineering.md`

## Why

LLM이 stateless이기 때문에 매 턴 전체 컨텍스트를 다시 주입해야 한다. 현재는 "최근 N턴 전체 append" 방식이라 10턴 세션에서 누적 입력 토큰이 100k+. 이력서 10k tokens가 매 턴 희석되면 중요 claim을 "잊는" 현상까지 발생.

Anthropic의 Context Engineering 원칙(L1 고정/캐시 + L2 상태 + L3 compaction + L4 JIT focus)을 적용하면 **15배 압축**(100k → 5~8k)이 가능하고, Claude의 `cache_control: ephemeral`로 L1의 90%를 캐시에서 가져와 코스트/latency를 동시에 절감. Resume Track이 이 인프라 없이는 동작 품질이 보장되지 않으므로 선행 `[blocking]` 태스크.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/infra/ai/context/InterviewContextBuilder.java` | 신규. 4-layer 조립 |
| `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FixedContextLayer.java` | 신규. L1: System + Skeleton, 캐시 마킹 |
| `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/SessionStateLayer.java` | 신규. L2: 200-500 tokens JSON |
| `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/DialogueHistoryLayer.java` | 신규. L3: 슬라이딩 윈도우 5턴 + compaction |
| `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java` | 신규. L4: JIT |
| `backend/src/main/java/com/rehearse/api/infra/ai/context/compaction/DialogueCompactor.java` | 신규 |
| `backend/src/main/resources/prompts/template/compaction-summarizer.txt` | 신규. 요약 프롬프트 |
| `backend/src/main/java/com/rehearse/api/infra/ai/context/cache/OpenAiCacheAdapter.java` | 신규. 자동 캐싱(순서 보장) |
| `backend/src/main/java/com/rehearse/api/infra/ai/context/cache/ClaudeCacheAdapter.java` | 신규. `cache_control: ephemeral` 마킹 |
| `backend/src/main/java/com/rehearse/api/infra/ai/ResilientAiClient.java` | **수정**. 요청 조립 시 ContextBuilder 결과 사용. 단일 경로 — runtime toggle 없음 (배포 단위로 전환) |

## 상세

### 4-layer 크기 목표

| Layer | 빈도 | 목표 크기 | 최적화 |
|---|---|---|---|
| L1 Fixed | 세션 불변 | 3000-5000 tokens | Prompt Caching (Claude 90% / OpenAI 50% off) |
| L2 State | 매 턴 | 200-500 tokens | 구조화 JSON |
| L3 Dialogue | 슬라이딩 | 1500-3000 tokens | 5턴 넘으면 compaction |
| L4 Focus | 매 턴 | 500-1500 tokens | JIT 렌더링 |
| **합계** | | **5000-8000** | 기존 대비 15배 압축 |

### Compaction 트리거
- `DialogueHistoryLayer`가 총 턴 수 > 5 이면 오래된 부분을 `DialogueCompactor.compact(olderTurns)`로 축약
- 요약 JSON 스키마: `covered_topics / user_claims_made / chain_progress_history / perspectives_asked / notable_moments`

### 멀티 프로바이더 캐싱 — Gemini 제외
- OpenAI: 자동(고정 블록을 프롬프트 맨 앞에 배치)
- Claude: `cache_control: {type: "ephemeral"}` 명시
- Gemini: context cache API **제외** (32,768 tokens 최소 크기 → Rehearse 쓰임새에 부적합. Verbal/Vision은 이미 별개 파이프라인)

### Fallback 캐시 콜드 미스 정책 (critic M5)
Primary(OpenAI) 정상 시 Claude는 호출 안 되므로 Claude 캐시 항상 콜드. Fallback 발동 시 첫 호출은 필연적 캐시 미스 → latency 증가 수용.
- plan-00b 의 `CachePolicy.allowMiss=true` 를 fallback 경로에 자동 설정
- `max-context-tokens: 8000` 상한은 fallback에서도 동일 적용 → L3 compaction이 더 적극 발동
- 모니터링: `rehearse.ai.call.duration_seconds{fallback="true"}` p95 임계치를 정상(Primary)보다 +3s 허용

### DialogueCompactor 비용 산정 (critic Missing)
Compactor 자체가 LLM 호출 → 추가 비용/지연:
- 트리거 조건: 총 턴 > 5. 10턴 세션에서 Compactor 호출 1~2회
- 비용: GPT-4o-mini 호출 1회 ≈ $0.001 (input 2k + output 500 tokens)
- 지연: Compactor는 **비동기 백그라운드 실행** — 다음 턴 전에 완료되지 않으면 해당 턴은 이전 compaction 결과 사용(latency 영향 0). 단 max 3턴까지만 지연 허용, 초과 시 동기 전환.

### 적용 범위
기존 plan-01/02/03 + 앞으로의 plan-05~09 LLM 호출 전부 `InterviewContextBuilder.build(session)` 경유.

### 설정 (application.yml 상수)
```yaml
rehearse:
  context-engineering:
    l1-caching: true
    l3-compaction-threshold: 5
    l3-recent-window: 5
    l4-just-in-time: true
    max-context-tokens: 8000
```

Feature Flag runtime toggle은 사용하지 않는다. 기본 활성화 경로로 단일화. 전면 전환으로 배포.

### Async Compaction 실행 모델 (S8 보강, 2026-04-26)

- 채택: Spring `@Async("compactionExecutor")` + `ThreadPoolTaskExecutor` (core 2 / max 4 / queue 50, `RejectedExecutionHandler.CallerRunsPolicy`)
- 트리거: `DialogueHistoryLayer.build()` 가 `exchanges.size() > 5 AND compactedSummary.absent` 일 때 비동기 작업 enqueue. **즉시 동기 폴백 없음** — 첫 트리거 턴은 raw recent-window 만 사용
- 캐시 키: `(interviewId, windowEndIdx)` — 이미 압축된 윈도우는 재호출 안 함
- 결과 저장: `InterviewRuntimeState.compactedDialogueSummary: ConcurrentHashMap<Long, CompactedSummary>` (신규 필드)
- 동기 폴백 임계: 직전 비동기 작업이 3턴 동안 미완료(`compactionStartedAt + 3턴 timeout`) → 다음 트리거는 동기 호출. `AiCallMetrics` 의 `mode=sync_fallback` 카운터 증가
- 비용 산정 (재확인): GPT-4o-mini 1회 ≈ $0.001 (input 2k + output 500), 10턴 세션당 1~2회

### L4 FocusLayer 트리거 룰 (S8 보강, 2026-04-26)

`callType` 별 fragment 매핑:

| callType | Fragment | 토큰 상한 |
|---|---|---|
| `intent_classifier` | 직전 turn Q + A 1쌍 | 300 |
| `answer_analyzer` | 현재 question + 현재 answer + persona depth hint | 800 |
| `follow_up_generator_v3` | ANSWER_ANALYSIS JSON + asked_perspectives | 1000 |
| `clarify_response` | 직전 turn Q + persona greeting | 400 |
| `giveup_response` | 직전 turn Q + persona greeting | 400 |
| `compaction_summarizer` | 압축 대상 turn 청크 | 1500 (L4 미적용 — Compactor 가 자체 입력) |

- 합산 상한 1500 tokens. 초과 시 `IllegalStateException` (defense in depth)
- 검증: `TokenEstimator.estimate(fragment) <= 상한` assert

### L2 SessionStateLayer JSON 스키마 (S8 보강, 2026-04-26)

`InterviewRuntimeState` → JSON 매핑:

```json
{
  "level": "MID",
  "current_turn": 4,
  "covered_claims_recent": ["...", "..."],
  "active_chain": [101, 102],
  "asked_perspectives": ["TRADEOFF", "RELIABILITY"]
}
```

매핑 규칙:
- `level` ← `currentLevel` (그대로)
- `current_turn` ← `playgroundTurns.get()`
- `covered_claims_recent` ← `coveredClaims` 최근 50개로 트림 (100 초과 시)
- `active_chain` ← `activeChain` 그대로 (Resume Track 만 의미. 그 외 빈 배열)
- `asked_perspectives` ← `turnAnalysisCache` 의 `selectedPerspective` 의 distinct (관대 파싱)

상한: 200~500 tokens. `covered_claims_recent` 트림으로 강제. `resumeSkeletonCache` 는 L1 (FixedContextLayer) 에 흡수 — L2 에서 제외.

### eval/context/measure_tokens.py (S8 보강, 2026-04-26)

위치: `eval/context/measure_tokens.py`
의존: `tiktoken==0.7.0` (Python 3.10+, 별도 venv 권장. backend Java 와 무관)

실행:
```
python eval/context/measure_tokens.py \
  --sessions eval/context/fixtures/*.json \
  --encoding cl100k_base
```

입력 fixture (`eval/context/fixtures/session-{1..5}.json`):
```json
{
  "interview_id": 1001,
  "level": "MID",
  "track": "STANDARD",
  "exchanges": [{"q": "...", "a": "...", "selectedPerspective": "..."}]
}
```

출력 (stdout):
```
session-1.json: total=6800 (L1=4200, L2=420, L3=1500, L4=680)
session-2.json: total=7100 ...
---
avg=6900, max=7300, min=6500 (10턴 5세션)
PASS (≤ 8000)
```

Exit code: 0 (PASS) / 1 (avg > 8000 또는 max > 9000).

## 담당 에이전트

- Implement: `backend-architect` — 4-layer 레이어링, 추상화 경계, `ResilientAiClient` 리팩토링
- Review: `architect-reviewer` — 레이어 책임 분리(SRP), 멀티 프로바이더 어댑터 패턴

## 검증

1. 10턴 세션 평균 입력 토큰 ≤ 8,000 (측정 스크립트 `eval/context/measure_tokens.py`)
2. L1 캐시 히트율 ≥ 95% (Claude `cache_read_input_tokens` 메타데이터 기준)
3. 기존 plan-01/02/03 호출이 새 ContextBuilder로 전환된 뒤 회귀 없음 (`./gradlew test`)
4. OpenAI vs Claude 동일 세션에서 출력 유사도 수동 비교 — 3~5건 세션 정성 확인 (MANUAL_AB_PROTOCOL.md 참조)
5. Compaction 결과가 핵심 claim(covered_topics) 누락 없는지 5개 세션 수동 리뷰
6. `progress.md` 04 → Completed
