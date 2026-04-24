# Interview Quality 2026-04-20 — 진행 상황

## 태스크 상태

### Phase 0 (W1-W2) — Critic Remediation 선행 인프라

| # | 태스크 | 주차 | 상태 | 의존성 | 비고 |
|---|--------|------|------|--------|------|
| 00a | Codebase Inventory `[blocking]` | W1 초 | Completed | — | 실제 클래스/테스트/영향도 맵 — INVENTORY/TEST_BASELINE/IMPACT_MAP 머지 (S1, 2026-04-20) |
| 00b | AiClient Generalization `[blocking]` | W1 후 | Completed | 00a | C1+C3+M5+Missing(JSON 폴백) 근본 해결 (S2, 2026-04-20). `@RefreshScope`/`AiFeatureProperties`는 2026-04-23 철거 예정 (PR B) |
| 00c | Session State Persistence `[parallel:00b]` | W2 초 | Completed | 00a | C2+Missing(동시성, 메모리) 해결. Flyway V24~V27 (S3, 2026-04-21) |
| 00d | Observability `[parallel:00c]` | W2 후 | Completed | 00a | M2+Missing(APM) 해결. `OBSERVABILITY.md` + Timer 6 태그 + Counter 4 종(input/output/cached.read/cached.write) + `micrometer-registry-prometheus` 의존성 (S3c, 2026-04-24). 코드 기반은 S2(#336)+S3(#338) 선행, S3c(#347) 로 완결 |
| 00e | Feedback Migration Strategy `[parallel:00d]` | W2 후 | Draft | 00a | M6 해결. 결정 문서만 |
| 00f | Interview Turn Policy Abstraction `[parallel:00c]` | W2 | Draft | 00a | **신규 (2026-04-21)**. `MAX_FOLLOWUP_ROUNDS=2` 하드코딩 제거 → `InterviewTurnPolicy` Strategy. plan-07 선행 blocker |

### Phase 1~4 (W3-W7) — 기존 플랜 (전제 인프라 위에서)

| # | 태스크 | 주차 | 상태 | 의존성 | 비고 |
|---|--------|------|------|--------|------|
| 01 | Intent Classifier (M2 축소판) | W3 | Draft | 00a,00b,00d | REMEDIATION 수정 지시 반영 필요 (M3/M4) |
| 02 | Answer Analyzer (M1 Step A) `[parallel:03]` | W4 | Draft | 01, 00c | P0. 꼬리질문 전제 |
| 03 | Follow-up Generator v3 (M1 Step B) `[parallel:02]` | W4 | Draft | 02 계약 | P0. v2 프롬프트 재활용 |
| 04 | Context Engineering 4-layer `[blocking]` | W5 | Draft | 00b,00c | Resume Track 전제. Fallback 캐시 정책 명시 필요 |
| 05 | Resume Extractor (Phase 1) `[parallel:06]` | W5 | Draft | 04, 00b | GPT-4o 호출은 00b의 modelOverride 사용. Dynamic Pacing: duration 무관 최대 추출 (2026-04-22) |
| 06 | Resume Interview Planner (Phase 2) `[parallel:05]` | W5 | Draft | 04, 00c | InterviewPlan 영속화는 V25. **Dynamic Pacing 재설계 (2026-04-22)**: duration 스케일링 폐기, priority 랭킹만 |
| 07 | Resume Orchestrator (Phase 3) | W6 | Draft | 04,05,06,00f | fact_check_flag 삭제 + 실제 진입점 명시 필요. `ResumeTrackPolicy` 에 `ChainStateTracker` 주입(00f skeleton 활용). **WRAP_UP 모드 + ClockWatcher + Resume Exclusivity Rule 추가 (2026-04-22)** |
| 08 | Rubric Family Scorer (10차원 × 7 rubric) | W7 | Draft | 02, 00c | **TODO 03 개정반영 — 전면 재작성**. `_dimensions.yaml` 마스터 + `_mapping.yaml` + 7개 rubric YAML. 작업량 1주 → 1~1.5주 |
| 09 | Feedback Synthesizer (M3 세션 종합) | W7 | Draft | 08, 00e | FEEDBACK_DOMAIN.md 결정 소비 |
| 11a | Lambda Nonverbal Schema Prerequisite `[blocking:11]` | W7 초 | Draft | 00a | **신규 (2026-04-21 VERIFICATION_REPORT §D3 대응)**. Gemini 프롬프트 3개 수치 필드 확장(`speed_variance` / `gaze_on_camera_ratio` / `posture_unstable_count`). plan-11 착수 전 필수 |
| 11 | Nonverbal Rubric (D11~D14 결정론 매퍼) `[parallel:08]` | W7 후 | Draft | 11a, 00a, 00c, 00e, 08 | TODO 09 반영 추가. Lambda Python mapper + backend context_weights. plan-09 선행 |
| 13 | Lambda Content Removal `[blocking:08,09]` | W7 후 | Draft | 08, 09 배포 + STAGING G1~G3 + MANUAL_AB_PROTOCOL 3~5건 통과 | **신규 (2026-04-22)**. Lambda `verbal`/`technical` 블록 제거, `TimestampFeedback` 컬럼 4개 drop (V29 — plan-11 V28 이후 순서), Rubric/Synthesizer를 Content 유일 소스로 확정. Content/Delivery 책임 경계 확정. 2026-04-23 flag-on 대신 ECR 단일 cut-over 로 갱신 |

## 진행 로그

### 2026-04-24 (S3c — plan-00d 완결 — Counter 4 종 + prometheus registry)

- **시그니처 확정**: plan 본문 초안의 4-arg `recordChat(callType, model, provider, Supplier)` → 실구현 2-arg `recordChat(String, Callable<ChatResponse>)` 로 **실구현 정답** 채택. 근거: `ResilientAiClient` fallback 시 provider/model 은 응답 수신 후 확정 → 호출 전 pre-tagging 불가. `ChatResponse.provider()`/`model()` 후추출이 정확.
- **토큰 Counter 4 종 구현** (`AiCallMetrics.recordChat()` finally 블록):
  - `rehearse.ai.call.tokens.input` (Usage.inputTokens)
  - `rehearse.ai.call.tokens.output` (Usage.outputTokens)
  - `rehearse.ai.call.tokens.cached.read` (Usage.cacheReadTokens — OpenAI `prompt_tokens_details.cached_tokens` / Claude `cache_read_input_tokens`)
  - `rehearse.ai.call.tokens.cached.write` (Usage.cacheWriteTokens — Claude `cache_creation_input_tokens`)
  - 태그: `call.type` / `provider` / `model` (Timer 와 동일 키 → 쿼리 간소화)
  - 0 토큰은 미등록 — 무의미 시리즈 Prometheus 누적 방지
  - 예외 경로(outcome=failure)에서는 Counter 미기록 (Timer 만)
- **의존성 추가**: `backend/build.gradle.kts` 에 `runtimeOnly("io.micrometer:micrometer-registry-prometheus")`. Spring Boot 3.x 는 `spring-boot-starter-actuator` 만으로는 `/actuator/prometheus` 엔드포인트 노출 안 함 → 현재 설정 그대로면 Grafana scraping 전면 실패. **관측 인프라 실작동을 위한 필수 의존성**.
- **테스트 추가**: `AiCallMetricsTest` 에 5 케이스 추가 — input/output Counter 증가 / cached read·write 분리 / 0 토큰 미등록 / 예외 경로 Counter 미기록 / 복수 호출 누적.
- **문서 갱신**: `OBSERVABILITY.md` Counter 표 4 종 + PromQL 쿼리 6 종 (provider 별 비용, 캐시 절감률, cache_write 추이) + 의존성 메모. `plan-00d-observability.md` 시그니처·파일 목록·검증 항목 교정.
- **라이브 검증**: `./gradlew bootRun --args='--spring.profiles.active=local --spring.sql.init.mode=never'` 로 실제 기동 → `/actuator/prometheus` HTTP 200 확인. Caffeine 캐시 메트릭 6 종 (`cache_gets_total` / `cache_evictions_total` / `cache_eviction_weight_total` / `cache_puts_total` / `cache_size`) 노출 확인.
- **문서 오류 수정**: Caffeine 메트릭 실제 이름을 확인해 OBSERVABILITY.md 의 `rehearse_runtime_state_cache_*` 쿼리를 `cache_*{cache="rehearse.runtime.state"}` 로 전면 교체. Micrometer `CaffeineCacheMetrics.monitor()` 의 세 번째 인자는 metric prefix 가 아니라 `cache` 태그 값 — 플랜 문서 초안 가정이 잘못됐음을 실측으로 확정.
- **AI 메트릭 노출 검증 보류**: 로컬 실 AI 호출(API 키) 없이는 `rehearse_ai_call_*` Lazy 등록 안 됨 → 스테이징 배포 후 실 호출 1 회로 검증 예정 (`OBSERVABILITY.md §검증 스냅샷` 부록 업데이트).
- **머지 순서 권장**: #346 (00e) → #348 (00f) → #347 (00d S3c).

### 2026-04-24 (S3b — plan-00d Observability 1차 — docs 초안)

- `OBSERVABILITY.md` 신규. AI 호출 Timer(`rehearse.ai.call.duration` + 태그 6 종) + Caffeine 캐시 메트릭(`rehearse.runtime.state.*` 5 종) 계약 정의
- Grafana/PromQL 쿼리 레퍼런스 7 종 (p95/fallback/캐시/토큰/실패율/Runtime State 히트율·eviction)
- Alert 임계치 5 종 가이드 (실제 Alertmanager 설정은 인프라 별건)
- 배포 중 회귀 감지 체크리스트 3 단계 (10 분/1 시간/1 일) — plan-01~ 롤아웃 시 공식 레퍼런스
- **실측 확인**: `application.yml` management 설정 이미 `prometheus` 노출 중. 추가 수정 0
- **권한 제한**: 로컬 `bootRun` 실행 불가 → 라이브 스냅샷 캡처는 스테이징 배포 후 부록으로 추가 예정 (문서 §검증 스냅샷)
- **이월**: Counter 3 종(`tokens.input/output/cached`) 실제 구현은 plan-04 Context Engineering PR 에서 `ChatResponse.Usage` 파싱과 함께 권장



플랜 본체 착수 전, 측정·롤백 인프라가 본체(LLM 품질 개선)보다 복잡해지는 위험을 검토하고 다음 결정 적용.

- **plan-10 Eval Harness Lite 전체 삭제**: 골든셋 30 + Judge 3(J1/J2/J3) + 수동 라벨 20건 + Cohen's κ 검증 인프라를 구축 비용 대비 효용 낮아 폐기
- **plan-12 Feature Flag Cleanup 전체 삭제**: flag 자체를 없애므로 cleanup 대상도 소멸
- **plan-00d Smoke Eval 부분 삭제**: `eval/golden-sets/smoke/`, `eval/judges/j1-*`, `eval/scripts/smoke.py` 등 5개 golden + J1 초안 폐기. Micrometer `AiCallMetrics` + `OBSERVABILITY.md` + Runtime State 캐시 메트릭만 유지 → `plan-00d-observability.md` 로 개명
- **STAGING_QUALITY_CHECKLIST v2 개정**: G4(레벨 밴드) / G5(Judge 일치율) / 10건 라벨링 절차 / J3 교차 검증 전부 삭제. G1~G3 자동 게이트만 유지. 제목 "Cut-over Gate" → "Automated Validation". 정성 비교는 `MANUAL_AB_PROTOCOL.md` 에 위임
- **`MANUAL_AB_PROTOCOL.md` 신규**: ECR 이미지 2개(before/after) 를 2개 포트(8081/8082) 에 병렬 기동 → 동일 녹화본 3~5건 투입 → JSON diff 수동 비교. 각 plan PR 머지 전 실행
- **Feature Flag runtime toggle 전면 제거**: plan-01/03/04/05/07/08/09/11/13 전반의 `rehearse.features.*` 언급 삭제. S2(plan-00b) 에서 이미 머지된 `AiFeatureProperties` / `@RefreshScope` / `/actuator/refresh` / `spring-cloud-context` 의존성은 **PR B (`[BE] refactor(ai): Feature Flag runtime toggle 철거`)** 에서 별도 철거. `ChatRequest.modelOverride` 는 모델 선택 자체 가치로 유지. 롤백 수단은 ECR 이미지 태그 + 세션 스토어 캐시 퍼지로 일원화
- **requirements.md Goal 표 개정**: Judge 기반 지표(J1/J2/J3, Intent Accuracy 등) 를 "수동 비교 판정" 으로 교체. 객관 측정 가능 지표(토큰, 캐시 히트율, 매핑 정확도) 는 유지
- **이유**: "신버전이 구버전보다 진짜 나아졌는가" 판정은 ECR 2개 병렬 기동 + 수동 diff 3~5건 으로 충분하며, 자동 Judge / runtime flag 는 본체 복잡도 상승 대비 이득이 부족. Grafana APM 은 회귀의 "정량" 감지에 유지

### 2026-04-22 (plan-13 신규 — Lambda Content Removal / Content·Delivery 책임 경계 확정)

`lambda/analysis/analyzers/gemini_analyzer.py` 가 단일 프롬프트로 `verbal`(답변 구조), `technical`(정확성/코칭), `vocal`, `attitude`, `overall` 5개 블록을 생성 중이며, FE `content-tab.tsx` 가 이를 가공 없이 렌더 중임을 확인. plan-08 Rubric Scorer 도입 시 **같은 답변을 Gemini + Rubric 이 이중 LLM 호출로 평가**하는 구조가 굳어질 위험 확인.

- **구조적 문제**: Gemini 는 `questionSetCategory` / `intentType` / `resumeMode` / `currentChainLevel` / resume 체인 컨텍스트를 받지 않아 레벨·의도 기준 정확성 판정 원천 불가. Rubric D1~D10 중 D2/D3/D4/D6 4차원이 Lambda `verbal`+`technical` 과 중복. Lambda `verbal` 블록 6개 축(용어 정확, 수치 구체, 논리 구조, 주제 이탈, 분량, 전달 명확성) 전부 D3/D4/D6 로 흡수됨 → 고유 가치 없음.
- **결정**: Lambda = Delivery Analyzer (AV-grounded only, `transcript`+`vocal`+`attitude`+`vision`+`overall_delivery`), Rubric = Content Analyzer 단독. 이중 평가 금지.
- **사용자 결정**: (1) 빠른 전환 (dual-read 단계 생략, plan-08/09 flag-off 배포 + 스테이징 품질 검수 후 flag-on 과 동시에 Lambda content 제거), (2) `verbal` 블록 완전 제거 (D3가 흡수), (3) DB 컬럼 바로 drop (V28, 과거 인터뷰 Content 탭은 "데이터 없음" 허용).
- **신규 plan-13 Lambda Content Removal 생성** — cut-over 시 Lambda 프롬프트·handler 정리, Backend DTO/Entity/Mapper 제거, FE content-tab 재설계, V28 migration 드롭, 플래그 on 동시 적용.
- **plan-08 개정**: Why 에 "Content 평가 유일 소스" 명시 + plan-13 연계 섹션 추가. 본 plan 범위를 "기술 내용 루브릭(D1~D10) 만" 으로 명시.
- **plan-09 개정**: 입력 스키마 `VERBAL_ANALYSIS` → `DELIVERY_ANALYSIS` 개명, `TURN_SCORES[].status: OK|FAILED` 필드 추가, `overall.coverage` 출력 필드 추가 (Rubric 실패 투명성), 작문 원칙에 "Content/Delivery 소스 엄격 분리" 강제 추가 (정규식 검증), Delivery 섹션은 delivery_analysis 에서만 / Content 섹션은 turn_scores 에서만 인용. cross-modal signal 은 `overall.narrative` 연성 관찰로만 허용 (차원 점수 수정 금지).
- **plan-11a 개정**: Out of Scope 에 "verbal/technical 블록 제거는 plan-13" 명시.
- **plan-11 개정**: 연계 섹션에 plan-13 / plan-08 경계 명시 (기술 D1~D10 vs 비언어 D11~D14 섞지 말 것).

이유: 이중 LLM 호출 구조가 굳기 전에 경계 확정. Rubric 품질 실패 시 Lambda fallback 이 존재하면 품질 드리프트가 은폐됨 → coverage 투명성과 fallback 금지로 품질 회복 루프 확보. 코드 수정 0건 — plan-13 구현 PR 에서 소화.

### 2026-04-22 (Resume Track 스펙 보완 — Dynamic Pacing + Exclusivity)

구현 착수 전 plan-05/06/07 Draft 허점 3개 차단:

- **plan-06 Dynamic Pacing 재설계**: duration 별 스케일링 테이블 폐기. Planner 는 모든 chain 을 priority 랭킹만 수행. `allocated_time_min` / `max_turns` / `estimated_duration_min` 필드 제거, `duration_hint_min` 만 남김 — opener 톤 조정에만 사용
- **plan-07 WRAP_UP 모드 추가**: `PLAYGROUND → INTERROGATION → WRAP_UP` 3단계 FSM. `ClockWatcher` 로 `remaining_time ≤ 2분` 전이. 새 chain 시작 금지, 현재 chain 완결 허용, 회고 질문 pool. `rehearse.features.resume-track.wrap-up-threshold-min` flag
- **plan-07 Resume Exclusivity Rule**: `resumeFile != null` 이면 `interviewTypes = {RESUME_BASED}` 강제. 위반 시 BE 400 + `RESUME_EXCLUSIVITY_VIOLATION`. FE 는 RESUME_BASED 선택 시 다른 카드 disabled + 자동 해제. Defense in depth
- **plan-05 최대 추출 원칙 명시**: Extractor 는 duration 무관 전체 Skeleton 추출 (이력서당 1회 비용 고정)
- **STATE_DESIGN.md `currentLevel` 의미 확정**: 사용자 레벨(junior/mid/senior), Chain level(L1~L4) 과 무관. Chain level 은 항상 `activeChain.size()` 로 도출. 혼동 금지 note 추가. `startedAt: Instant` 필드 추가 (ClockWatcher 용)

이유: 사용자 페이스 적응 불가 + 심층 체인 연속성 보호 + `currentLevel`/Chain level 오해석 위험을 구현 이전에 차단. 코드 수정 0건 — 각 plan 구현 PR 에서 소화.

### 2026-04-21 (S3 — plan-00c Session State Persistence 완료)

- Flyway V24~V27 마이그레이션 + rollback SQL 작성 (resume_skeleton / interview_plan / rubric_score / session_feedback)
- `InterviewRuntimeState` POJO — thread-safe 컬렉션(ConcurrentHashMap, CopyOnWriteArrayList, AtomicInteger) 기반 L3 런타임 상태 POJO
- `InterviewRuntimeStateStore` — Caffeine 2h idle TTL / max 10,000 / recordStats / Micrometer 메트릭(hits/misses/evictions) 노출
- `InterviewLockService` — 자체 구현 StripedLock(256 stripe ReentrantLock 배열). Guava 의존성 없음
- `build.gradle.kts` — `com.github.ben-manes.caffeine:caffeine` 의존성 추가
- `STATE_DESIGN.md` — 4계층 분류 + 결정사항(D1~D5) + 후속 plan 소비 방법 정식 문서화
- 신규 테스트 12개 추가: `InterviewRuntimeStateStoreTest`(7) + `InterviewLockServiceTest`(5)
- 결정: H2 호환 불필요 — test 프로파일은 Flyway disabled + create-drop. JSON 컬럼은 MySQL 전용
- 결정: StripedLock 자체 구현 채택 — Guava Striped 대비 외부 의존성 0, 구현 20줄 이내
- `./gradlew test` 전체 통과 확인 (baseline 606 + 신규 12 = 618 예상)

### 2026-04-21 (plan-00f 추가 — Interview Turn Policy Abstraction)

- `FollowUpTransactionHandler.MAX_FOLLOWUP_ROUNDS=2` 하드코딩이 plan-07 Resume 트랙(최대 7턴) 을 차단하는 설계 결함 발견
- `InterviewTurnPolicy` Strategy 도입 plan 작성: `StandardFollowUpPolicy` (CS/Language, 행위 무변경) + `ResumeTrackPolicy` skeleton + `InterviewTurnPolicyResolver`
- plan-07 의존성에 plan-00f 추가, `ChainStateTracker` 주입은 plan-07 에서 완성
- plan-00c 와 병렬 실행 가능 (W2)

### 2026-04-21 (검증 리포트 반영 — 문서 교정)

VERIFICATION_REPORT.md 작성 후 Critical/Major 문서 교정 적용:

- **A-F1 모델 드리프트 제거** (`plan-05`): `modelOverride="gpt-4o"` 하드코딩 → `application.yml` 기본 `gpt-4o-mini` 유지. flag 경유 선택적 업그레이드로 변경
- **B-F1 Aggregate Latency SLA 추가** (`plan-01/02/03/08`): 턴 파이프라인 aggregate p95 ≤ 4000ms 규약. plan-01 에 canonical 섹션 정의, plan-02/03 은 참조. Rubric Scorer(plan-08) 는 `@TransactionalEventListener(AFTER_COMMIT)` 비동기 post-turn 으로 명시 → SLA 제외
- **C-F1/F2/F3 실체 불일치 교정**: plan-03 `GeneratedFollowUp` 경로 확정, plan-06 `InterviewRuntimeState` 로 전환, plan-07 `FollowUpService`/`FollowUpServiceTest` 로 교정
- **D-F2 plan-11a 신규 생성**: Lambda Gemini 프롬프트 3개 수치 필드 확장(`speed_variance` / `gaze_on_camera_ratio` / `posture_unstable_count`). plan-11 의 `[blocking]` 선행 조건으로 편입. plan-11 본문에서 Lambda 프롬프트 확장 항목 → plan-11a 로 이관
- **D-F4 Lambda Error Handling** (`plan-09`): failure_reason → 처리 매트릭스 + admin 재시도 엔드포인트 + 사용자 "일시 오류" 배너 정책. 모든 실패는 `deliveryRetryable=true` 기본 (영구 실패 금지)

### 2026-04-20 (S1 — plan-00a Codebase Inventory 완료)
- `INVENTORY.md` (380L), `TEST_BASELINE.md` (249L), `IMPACT_MAP.md` (364L) 생성
- `./gradlew test` baseline: **606 tests / 0 failures / 0 ignored / 56s** (JaCoCo 미설정 — 추후 추가 권장)
- 주요 교정: `InterviewSession`/`InterviewTurnService` 실존 X → `Interview` entity + `FollowUpService.generateFollowUp(Long,Long,FollowUpRequest,MultipartFile):31` / `PdfTextExtractor` 기존 확장 / plan-07 `fact_check_flag` 삭제 대상 확정
- IMPACT_MAP 15개 plan (00b~11) 각각 신규/수정 파일 절대 경로 확정
- 다음 세션: S2 — plan-00b AiClient Generalization (`[BE] feat(ai): AiClient.chat() 범용 메서드 + @RefreshScope + JSON 파싱 재시도`)

### 2026-04-20 (초기 플래닝)
- `docs/todo/2026-04-20/` 7개 TODO 문서 분석 완료
- `docs/plans/interview-quality-2026-04-20/` 스펙 디렉토리 생성
- `requirements.md` 작성: Why/Goal/Evidence/Trade-offs + 4주 로드맵 + Out of scope 명시
- plan-01~10 (Phase 1~4) 초안 작성 완료
- **Critic 에이전트 리뷰** 실시 → 조건부 승인 판정 (Critical 3 + Major 6 + Missing 7)
- `REMEDIATION.md` 작성: 근본 원인별(RC1~RC7) 해결 전략
- **Phase 0 추가**: plan-00a~00e 5개 신규 (W1-W2 선행 배치)
- 로드맵 4주 → **7주 재산정** (critic M1 반영)
- `requirements.md` 로드맵 섹션 갱신
- 의존성 그래프 재정리

### 해결 체크리스트 (REMEDIATION.md 동기)
- [x] C1 AiClient 범용화 (00b) — chat(ChatRequest) 추가, 3개 도메인 메서드 어댑터 보존 (S2)
- [x] C2 DB 영속화/Flyway (00c) — V24~V27 + InterviewRuntimeStateStore + InterviewLockService (S3)
- [x] C3 호출별 모델 선택 (00b) — ChatRequest.modelOverride 지원 (S2)
- [ ] M1 7주 재산정 (이 문서)
- [x] M2 W1-W3 회귀 방어 (00d) — `OBSERVABILITY.md` 작성 (S3b, 2026-04-24). Grafana/PromQL 쿼리 7 종 + Alert 임계치 5 종 + 배포 회귀 감지 체크리스트
- [ ] M3 META/OFF_TOPIC 가드 (plan-01 edit)
- [x] M4 실제 클래스명 정정 — plan-00a 인벤토리 완료 (S1). plan-01/07/08 본문 edit은 각 plan 실행 직전 해당 PR에 포함 (IMPACT_MAP 교정 사항 참조)
- [x] M5 Fallback 캐시 정책 (00b) — ResilientAiClient.fallbackChat() allowMiss=true 자동 적용 (S2)
- [ ] M6 Feedback 관계 (00e)
- [x] Missing PdfTextExtractor 재사용 — 기존 클래스 확인 (infra/ai/PdfTextExtractor.java, `extract(MultipartFile)`). IMPACT_MAP plan-05 수정 항목으로 기록
- [x] Missing APM 메트릭 표준 (00d) — Micrometer 태그 6 종(`call.type` / `model` / `provider` / `cache.hit` / `fallback` / `outcome`) + Caffeine 캐시 메트릭 5 종 문서화. 구현은 S2/S3 머지 완료
- [x] Missing Feature flag runtime — **의도적 제거 (2026-04-23)**. S2 에서 @RefreshScope/AiFeatureProperties 구현 완료됐으나 ECR 이미지 롤백으로 대체 결정 → PR B 에서 철거 예정
- [x] Missing 동시성 제어 (00c InterviewLockService) — StripedLock 256 자체 구현 (S3)
- [x] Missing JSON 파싱 폴백 (00b) — AiResponseParser.parseWithRetry() 추가 (S2)
- [x] ~~Minor plan-10 수동 라벨~~ — plan-10 전체 삭제 (2026-04-23)
- [ ] Addendum 비언어 루브릭 (plan-11) — TODO 09 반영. D11~D14 결정론 매퍼 + context_weights + V28
- [x] ~~Flag Cleanup (plan-12)~~ — flag 자체 제거로 plan-12 폐기 (2026-04-23)
