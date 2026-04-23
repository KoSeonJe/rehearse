# Plan 04: Context Engineering 4-layer Builder `[blocking]`

> 상태: Draft
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
