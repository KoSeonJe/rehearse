# Interview Quality 2026-04-20 — 진행 상황

## 태스크 상태

### Phase 0 (W1-W2) — Critic Remediation 선행 인프라

| # | 태스크 | 주차 | 상태 | 의존성 | 비고 |
|---|--------|------|------|--------|------|
| 00a | Codebase Inventory `[blocking]` | W1 초 | Completed | — | 실제 클래스/테스트/영향도 맵 — INVENTORY/TEST_BASELINE/IMPACT_MAP 머지 (S1, 2026-04-20) |
| 00b | AiClient Generalization `[blocking]` | W1 후 | Completed | 00a | C1+C3+M5+Missing(JSON 폴백, @RefreshScope) 근본 해결 (S2, 2026-04-20) |
| 00c | Session State Persistence `[parallel:00b]` | W2 초 | Draft | 00a | C2+Missing(동시성, 메모리) 해결. Flyway V24~V27 |
| 00d | Observability + Eval Smoke `[parallel:00c]` | W2 후 | Draft | 00a | M2+Missing(APM) 해결 |
| 00e | Feedback Migration Strategy `[parallel:00d]` | W2 후 | Draft | 00a | M6 해결. 결정 문서만 |

### Phase 1~4 (W3-W7) — 기존 플랜 (전제 인프라 위에서)

| # | 태스크 | 주차 | 상태 | 의존성 | 비고 |
|---|--------|------|------|--------|------|
| 01 | Intent Classifier (M2 축소판) | W3 | Draft | 00a,00b,00d | REMEDIATION 수정 지시 반영 필요 (M3/M4) |
| 02 | Answer Analyzer (M1 Step A) `[parallel:03]` | W4 | Draft | 01, 00c | P0. 꼬리질문 전제 |
| 03 | Follow-up Generator v3 (M1 Step B) `[parallel:02]` | W4 | Draft | 02 계약 | P0. v2 프롬프트 재활용 |
| 04 | Context Engineering 4-layer `[blocking]` | W5 | Draft | 00b,00c | Resume Track 전제. Fallback 캐시 정책 명시 필요 |
| 05 | Resume Extractor (Phase 1) `[parallel:06]` | W5 | Draft | 04, 00b | GPT-4o 호출은 00b의 modelOverride 사용 |
| 06 | Resume Interview Planner (Phase 2) `[parallel:05]` | W5 | Draft | 04, 00c | InterviewPlan 영속화는 V25 |
| 07 | Resume Orchestrator (Phase 3) | W6 | Draft | 04,05,06 | fact_check_flag 삭제 + 실제 진입점 명시 필요 |
| 08 | Rubric Family Scorer (10차원 × 7 rubric) | W7 | Draft | 02, 00c | **TODO 03 개정반영 — 전면 재작성**. `_dimensions.yaml` 마스터 + `_mapping.yaml` + 7개 rubric YAML. 작업량 1주 → 1~1.5주 |
| 09 | Feedback Synthesizer (M3 세션 종합) | W7 | Draft | 08, 00e | FEEDBACK_DOMAIN.md 결정 소비 |
| 10 | Eval Harness (M4 Full) `[parallel:09]` | W7 | Draft | 01~09 | smoke는 00d에서 이미 확보 |
| 11 | Nonverbal Rubric (D11~D14 결정론 매퍼) `[parallel:08]` | W7 | Draft | 00a,00c,00e,08 | TODO 09 반영 추가. Lambda Python mapper + backend context_weights. plan-09 선행 |

## 진행 로그

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
- [ ] C2 DB 영속화/Flyway (00c)
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
- [ ] Missing 동시성 제어 (00c InterviewLockService)
- [x] Missing JSON 파싱 폴백 (00b) — AiResponseParser.parseWithRetry() 추가 (S2)
- [ ] Minor plan-10 수동 라벨 = 골든셋 부분집합 (plan-10 edit)
- [ ] Addendum 비언어 루브릭 (plan-11) — TODO 09 반영. D11~D14 결정론 매퍼 + context_weights + V28
