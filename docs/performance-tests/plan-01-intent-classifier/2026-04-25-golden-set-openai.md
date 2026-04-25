# plan-01 Intent Classifier 성능 테스트 결과 — 2026-04-25 (OpenAI)

> 실행자: Claude (auto mode)
> 환경: local (`@SpringBootTest` H2 인메모리, `spring.sql.init.mode=never`)
> 커밋: feat/plan-01-intent-classifier (uncommitted local)
> 브랜치: `feat/plan-01-intent-classifier`
> 소요 시간: 38.78s (Spring 부팅 10.3s + 25 호출 ~28s)

## 목적

plan-01 Phase A 구현 (4-intent classifier) 의 정확도 게이트 검증. OpenAI primary provider 단독 측정. Claude 측정은 modelOverride 분기 추가 후 별도 진행 예정.

## 대상 모델·파라미터

| 항목 | 값 |
|------|------|
| Provider | OpenAI |
| Model | gpt-4o-mini (`application-local.yml` 기본값) |
| Temperature | 0.1 |
| Max tokens | 200 |
| Response format | JSON_OBJECT |
| Prompt caching | auto (시스템 프롬프트 정적) |
| Fallback threshold | confidence < 0.7 → forceAnswer |

## 골든셋

| 분류 | 건수 |
|------|------|
| ANSWER | 10 |
| CLARIFY_REQUEST | 5 |
| GIVE_UP | 5 |
| OFF_TOPIC | 5 |
| **합계** | **25** |

소스: `backend/src/test/java/com/rehearse/api/domain/interview/service/IntentClassifierGoldenSetLiveTest.java#goldenSet()`

## 결과 — 정확도

| 분류 | 맞춤 | 전체 | 정확도 | 합격 기준 | 판정 |
|------|------|------|--------|--------|------|
| **전체** | **25** | **25** | **100.0%** | ≥ 90% | **PASS** |
| ANSWER | 10 | 10 | 100% | — | PASS |
| CLARIFY_REQUEST | 5 | 5 | 100% | FP ≤ 3% (≤1/25) | PASS (FP=0) |
| GIVE_UP | 5 | 5 | 100% | — | PASS |
| OFF_TOPIC | 5 | 5 | 100% | ≥ 80% | PASS |

오답 0건 — `[MISS]` 로그 없음.

## 결과 — 지연 (대략치)

전체 38.78s 중 Spring 부팅 ~10.3s 제외 → 25 호출 약 28.5s → **호출당 평균 ~1.14s**.

> 주의: 단일 머신 + 직렬 호출 + 콜드 캐시. 실 트래픽에서는 prompt caching hit 으로 평균 600~900ms 추정.
>
> 정밀 p50/p95 는 스테이징 배포 후 Grafana `rehearse.ai.call.duration_seconds{call.type="intent_classifier"}` 에서 측정 예정.

| 지표 | 측정 | 비고 |
|------|------|------|
| 평균 latency (cold) | ~1140ms | Spring 부팅 제외, 콜드 cache, sequential |
| p50 latency | (N/A) | Grafana 후속 측정 |
| p95 latency | (N/A) | Grafana 후속 측정 |
| Fallback 발동 | 0/25 | confidence ≥ 0.7 + 파싱 성공 |
| 평균 input 토큰 | (N/A) | Micrometer counter 후속 캡처 |
| 평균 output 토큰 | (N/A) | 동일 |

## 오답 상세

없음 (0/25). 골든셋 내 모든 케이스가 한 번에 정분류. ANSWER 경계 케이스("정확하진 않을 수 있는데..." / "잘 모르지만 ~ 같아요") 도 ANSWER 로 정분류 — few-shot 설계 효과 확인.

## 제공자 비교

OpenAI 단독. Claude 측정은 다음 절차:
1. `IntentClassifierGoldenSetLiveTest` 에 `modelOverride="claude-haiku-4-5-20251001"` 옵션 추가하거나
2. 별도 profile 로 primary/fallback 스왑 후 동일 골든셋 실행

Phase B 진입 전 Claude 비교 측정 1회 수행 권장 (제공자 차이 ≤ 5%p 게이트 검증).

## 결론

- **합격 여부**: PASS — 정확도 게이트 (≥90%, OFF_TOPIC ≥80%, CLARIFY FP ≤3%) 모두 달성
- **이월 이슈**:
  - Claude 제공자 측정 미수행 (Phase B 전 권장)
  - p50/p95 latency 정량 측정 (스테이징 배포 후 Grafana)
  - 토큰·비용 카운터 캡처 (스테이징 배포 후 Prometheus)
- **후속 조치**: Phase A commit + PR 진행
- **다음 측정 트리거**:
  - 프롬프트 변경 PR
  - 스테이징 배포 후 1차 Grafana 캡처
  - 1주일 운영 후 회귀 점검

## 부록 — 실행 명령

```bash
cd backend
OPENAI_API_KEY=$(grep '^OPENAI_API_KEY=' .env | cut -d= -f2-) \
CLAUDE_API_KEY=$(grep '^CLAUDE_API_KEY=' .env | cut -d= -f2-) \
LIVE_TEST=true ./gradlew test --tests "IntentClassifierGoldenSetLiveTest"
```

테스트 리포트: `backend/build/reports/tests/test/classes/com.rehearse.api.domain.interview.service.IntentClassifierGoldenSetLiveTest.html`
