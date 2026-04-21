# Interview Quality 2026-04-20 — 진행 상황

## 태스크 상태

### Phase 0 (W1-W2) — Critic Remediation 선행 인프라

| # | 태스크 | 주차 | 상태 | 의존성 | 비고 |
|---|--------|------|------|--------|------|
| 00a | Codebase Inventory `[blocking]` | W1 초 | Completed | — | 실제 클래스/테스트/영향도 맵 — INVENTORY/TEST_BASELINE/IMPACT_MAP 머지 (S1, 2026-04-20) |
| 00b | AiClient Generalization `[blocking]` | W1 후 | Completed | 00a | C1+C3+M5+Missing(JSON 폴백, @RefreshScope) 근본 해결 (S2, 2026-04-20) |
| 00c | Session State Persistence `[parallel:00b]` | W2 초 | Completed | 00a | C2+Missing(동시성, 메모리) 해결. Flyway V24~V27 (S3, 2026-04-21) |
| 00d | Observability + Eval Smoke `[parallel:00c]` | W2 후 | Draft | 00a | M2+Missing(APM) 해결 |
| 00e | Feedback Migration Strategy `[parallel:00d]` | W2 후 | Draft | 00a | M6 해결. 결정 문서만 |
| 00f | Interview Turn Policy Abstraction `[parallel:00c]` | W2 | Draft | 00a | **신규 (2026-04-21)**. `MAX_FOLLOWUP_ROUNDS=2` 하드코딩 제거 → `InterviewTurnPolicy` Strategy. plan-07 선행 blocker |

### Phase 1~4 (W3-W7) — 기존 플랜 (전제 인프라 위에서)

| # | 태스크 | 주차 | 상태 | 의존성 | 비고 |
|---|--------|------|------|--------|------|
| 01 | Intent Classifier (M2 축소판) | W3 | Draft | 00a,00b,00d | REMEDIATION 수정 지시 반영 필요 (M3/M4) |
| 02 | Answer Analyzer (M1 Step A) `[parallel:03]` | W4 | Draft | 01, 00c | P0. 꼬리질문 전제 |
| 03 | Follow-up Generator v3 (M1 Step B) `[parallel:02]` | W4 | Draft | 02 계약 | P0. v2 프롬프트 재활용 |
| 04 | Context Engineering 4-layer `[blocking]` | W5 | Draft | 00b,00c | Resume Track 전제. Fallback 캐시 정책 명시 필요 |
| 05 | Resume Extractor (Phase 1) `[parallel:06]` | W5 | Draft | 04, 00b | GPT-4o 호출은 00b의 modelOverride 사용 |
| 06 | Resume Interview Planner (Phase 2) `[parallel:05]` | W5 | Draft | 04, 00c | InterviewPlan 영속화는 V25 |
| 07 | Resume Orchestrator (Phase 3) | W6 | Draft | 04,05,06,00f | fact_check_flag 삭제 + 실제 진입점 명시 필요. `ResumeTrackPolicy` 에 `ChainStateTracker` 주입(00f skeleton 활용) |
| 08 | Rubric Family Scorer (10차원 × 7 rubric) | W7 | Draft | 02, 00c | **TODO 03 개정반영 — 전면 재작성**. `_dimensions.yaml` 마스터 + `_mapping.yaml` + 7개 rubric YAML. 작업량 1주 → 1~1.5주 |
| 09 | Feedback Synthesizer (M3 세션 종합) | W7 | Draft | 08, 00e | FEEDBACK_DOMAIN.md 결정 소비 |
| 10 | Eval Harness (M4 Full) `[parallel:09]` | W7 | Draft | 01~09 | smoke는 00d에서 이미 확보 |
| 11a | Lambda Nonverbal Schema Prerequisite `[blocking:11]` | W7 초 | Draft | 00a | **신규 (2026-04-21 VERIFICATION_REPORT §D3 대응)**. Gemini 프롬프트 3개 수치 필드 확장(`speed_variance` / `gaze_on_camera_ratio` / `posture_unstable_count`). plan-11 착수 전 필수 |
| 11 | Nonverbal Rubric (D11~D14 결정론 매퍼) `[parallel:08]` | W7 후 | Draft | 11a, 00a, 00c, 00e, 08 | TODO 09 반영 추가. Lambda Python mapper + backend context_weights. plan-09 선행 |
| 12 | Feature Flag Cleanup `[post-rollout]` | W8+ | Draft | 01,03,04,07 전면 롤아웃 + 2주 안정 | 5개 release flag + v2 구버전 코드 제거. Flag Debt 방지. 각 flag 별 독립 PR |

## 진행 로그

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
- [ ] M2 W1-W3 회귀 방어 (00d)
- [ ] M3 META/OFF_TOPIC 가드 (plan-01 edit)
- [x] M4 실제 클래스명 정정 — plan-00a 인벤토리 완료 (S1). plan-01/07/08 본문 edit은 각 plan 실행 직전 해당 PR에 포함 (IMPACT_MAP 교정 사항 참조)
- [x] M5 Fallback 캐시 정책 (00b) — ResilientAiClient.fallbackChat() allowMiss=true 자동 적용 (S2)
- [ ] M6 Feedback 관계 (00e)
- [x] Missing PdfTextExtractor 재사용 — 기존 클래스 확인 (infra/ai/PdfTextExtractor.java, `extract(MultipartFile)`). IMPACT_MAP plan-05 수정 항목으로 기록
- [ ] Missing APM 메트릭 표준 (00d + REMEDIATION)
- [x] Missing Feature flag runtime (00b) — AiFeatureProperties @RefreshScope + /actuator/refresh (S2)
- [x] Missing 동시성 제어 (00c InterviewLockService) — StripedLock 256 자체 구현 (S3)
- [x] Missing JSON 파싱 폴백 (00b) — AiResponseParser.parseWithRetry() 추가 (S2)
- [ ] Minor plan-10 수동 라벨 = 골든셋 부분집합 (plan-10 edit)
- [ ] Addendum 비언어 루브릭 (plan-11) — TODO 09 반영. D11~D14 결정론 매퍼 + context_weights + V28
- [ ] Flag Cleanup (plan-12) — 5개 release flag + v2 코드 제거. 스프린트 종료 후 트리거
