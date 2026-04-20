# Interview Quality 2026-04-20 — 요구사항 정의

> 상태: Draft
> 작성일: 2026-04-20
> 원본 TODO: `docs/todo/2026-04-20/` (00~07)

## Why

### 문제
Rehearse의 핵심 LLM 파이프라인(질문 생성 / 꼬리질문 생성 / 피드백 생성)이 "한 번의 LLM 호출에 다수 결정을 묶어놓은" 구조라 **어느 단계에서 품질이 떨어졌는지 관측 불가능**하다. 5대 아픈 지점:
1. 꼬리질문이 피상적(사용자 답변 실제 갭과 무관한 기계적 관점 전환)
2. 피드백이 추상적("소통이 좋습니다" 수준, 다음 액션 불명확)
3. 같은 답변에 다른 평가(일관성 부재)
4. 사용자 이해 실패를 감지 못함 ("질문이 이해 안 가요"도 답변으로 처리)
5. 취조 톤(대화가 아니라 심문처럼 느껴짐)

또한 **이력서 기반 면접**(CS와 게임의 룰 자체가 다른 "사실 검증 + 의사결정 심문")이 현재 `follow-up-experience.txt` 한 장으로 처리되어 실제 면접관 수준의 깊이 있는 파고들기가 불가능하다.

### Goal

| 지표 | 현재 (추정) | 목표 | 측정 방법 |
|------|-----------|------|---------|
| Follow-up Relevance (J1) | ? | ≥ 4.0 / 5.0 | Judge (plan-10) |
| Intent Detection Accuracy | 0% (미구현) | ≥ 90% | Judge (plan-10) |
| Feedback Rubric Adherence | ? | ≥ 4.0 / 5.0 | Judge (plan-10) |
| 관찰 인용 포함률 (피드백 gap/strength) | ? | 100% | 정규식 + Judge |
| 카테고리-차원 적합도 (적용 rubric의 `uses_dimensions` 외 차원 강제 채점 없음) | — | 100% | 자동 체크 (plan-08 Rubric Family) |
| 카테고리-루브릭 매핑 정확도 | — | ≥ 98% | `RubricLoaderTest` 전수 |
| 10턴 세션 평균 입력 토큰 | ~100k (누적) | ≤ 8k | APM 토큰 카운터 |
| L1 Prompt 캐시 히트율 | — | ≥ 95% | Provider 메타데이터 |
| Resume Extraction claim 커버리지 | — | ≥ 90% | 수동 라벨 vs 추출 결과 |

### Evidence
- `docs/todo/2026-04-20/00-overview.md` ~ `07-context-engineering.md` 7개 문서: 이 설계의 상세 근거
- `docs/plans/prompt-improvement-2026-04/` 완료된 audit 결과: Lane 1-2에서 v2 프롬프트 확정, 본 플랜은 그 다음 단계(구조 재설계) 담당
- `docs/plans/prompt-redesign/background/` 2차원 페르소나 시스템 (Base × Overlay) — Resume Track에서 재사용
- Anthropic 공식: [Effective context engineering](https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents), [Multi-agent research system](https://www.anthropic.com/engineering/multi-agent-research-system)
- 실무: Booking.com LLM Evaluation, PromptLayer Eval Framework, LeadDev 루브릭 가이드
- 학술: arxiv 2601.22025 (When "Better" Prompts Hurt), ResumeFlow (2402.06221), AI Hiring with LLMs (2504.02870)

### Trade-offs
- **채택** (사용자 요청 "질문 생성 / 꼬리질문 / 피드백 생성 강화" 직결):
  - M2 Intent Classifier (3-intent 축소판)
  - M1 Step A Answer Analyzer + Step B Follow-up Generator v3
  - Context Engineering 4-layer
  - Resume Track Phase 1-3 (Ingestion → Extraction → Plan → Orchestrator)
  - M3 Rubric Scorer + Feedback Synthesizer
  - M4 Eval Harness Lite (Judge 3개 + 골든셋 30)
- **제외** (프로젝트 방향과 안 맞음 / MVP 범위 초과):
  - GDPR 스타일 저장 동의 UI 플로우 → 세션 스코프 캐시만
  - Promptfoo + GitHub Actions CI 통합 → 로컬 수동 실행으로 시작
  - J4 Consistency Meter, J5 Resume Flow Judge, J6 Context Fidelity Judge → Judge 3개(J1/J2/J3)만
  - 사실 검증 flag (이력서 vs 답변 모순 탐지) → 정밀도 관리 부담, MVP 이후
  - LoRA 파인튜닝 준비 → 6개월 후 데이터 축적 뒤 재평가
  - Verbal/Vision(Gemini) 구조 변경 → `prompt-improvement-2026-04` Lane 3-5에서 별도 진행 중
  - META/OFF_TOPIC intent 세분화 → ANSWER로 병합하여 fallback 단순화
  - 5차원 루브릭 중 `recovery_from_gaps` → intent=GIVE_UP 턴에만 조건부

## 아키텍처 / 설계

### 통합 파이프라인

```
[User Utterance]
   ↓
[L1: Intent Classifier (plan-01)]  ← ANSWER/CLARIFY/GIVE_UP
   ↓ ANSWER인 경우
[L2: Answer Analyzer (plan-02, M1 Step A)]
   ↓ claims/depth/missing_perspectives
[L3: Follow-up Generator v3 (plan-03, M1 Step B)]  ← Resume Track은 Chain Interrogator로 교체
   ↓ question + target_claim
[L4: Rubric Scorer (plan-08, 턴마다)]
   ↓ 세션 누적
[L5: Feedback Synthesizer (plan-09, 세션 종료 시 1회)]
```

### Cross-cutting
- **Context Engineering (plan-04)**: 모든 LLM 호출이 4-layer(L1 고정/캐시 · L2 세션 상태 · L3 대화 compaction · L4 JIT focus) 구조 공유
- **Resume Track (plan-05/06/07)**: 이력서 Ingestion → Skeleton 추출 → Interview Plan → Playground/Interrogation 모드
- **Eval Harness Lite (plan-10)**: 골든셋 30 + Judge 3(J1/J2/J3)로 리그레션 감지

### 재사용 자산
- `ResilientAiClient` (OpenAI Primary + Claude Fallback 이중화) — 신규 LLM 호출 전부 경유
- `docs/plans/prompt-improvement-2026-04/proposals/follow-up.v2.txt` — plan-03 Step B의 시작점
- `docs/plans/prompt-improvement-2026-04/proposals/question-generation.v2.txt` — 그대로 유지(수정 대상 아님)
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/` — 신규 Builder들이 이 패키지로 들어감

## Scope

### In
- 질문 생성 강화: Resume Track의 Extractor(plan-05)가 이력서 기반 질문 파이프라인을 구축
- 꼬리질문 생성 강화: Intent 분류(plan-01) + Answer Analyzer(plan-02) + Follow-up v3(plan-03) + Chain Interrogator(plan-07)
- 피드백 생성 강화: **Rubric Family** (10차원 D1~D10 마스터 + 7개 카테고리별 Rubric YAML, plan-08) + Turn Scorer(plan-08) + Session Synthesizer(plan-09, 카테고리 크로스-비교)
- Context 효율화: 4-layer Builder(plan-04)로 모든 호출 전환
- 측정 루프: Eval Harness Lite(plan-10)로 J1/J2/J3 + 골든셋 30

### Out
- Verbal/Vision(Gemini) 프롬프트 개편 — `prompt-improvement-2026-04`에서 처리
- 프론트엔드 피드백 UI 리디자인 — 별도 트랙 (`feedback-v3`, `feedback-panel-redesign`)
- 결제 / 히스토리 대시보드 / 피어 리뷰 등 MVP DON'T 항목
- 이력서 저장 동의 UI / GDPR / 영구 저장
- Promptfoo + GitHub Actions CI 자동화 (수동 실행으로 시작)
- 사실 검증 flag, 파인튜닝 데이터셋 준비

## 7주 로드맵 (Critic 리뷰 후 재조정 — `REMEDIATION.md` 참조)

| 주차 | 목표 | Plan | Phase |
|---|---|---|---|
| W1 | 코드 인벤토리 + AiClient 범용화 | 00a Inventory → 00b AiClient Generalization | **Phase 0** |
| W2 | 상태 영속화 설계 + 관측 인프라 + 피드백 마이그레이션 전략 | 00c State Persistence `[parallel]` 00d Observability+Smoke Eval `[parallel]` 00e Feedback Migration | **Phase 0** |
| W3 | 대화 자연스러움 | 01 Intent Classifier | Phase 1 |
| W4 | 꼬리질문 품질 | 02 Answer Analyzer + 03 Follow-up v3 `[parallel]` | Phase 2 |
| W5 | Context Engineering + 이력서 파이프라인 (1/2) | 04 Context Eng → 05 Extractor + 06 Planner `[parallel]` | Phase 3 |
| W6 | 이력서 파이프라인 (2/2) | 07 Resume Orchestrator | Phase 3 |
| W7 | 피드백 품질 + 측정 루프 | 08 Rubric Scorer + 11 Nonverbal Rubric `[parallel]` → 09 Synthesizer + 10 Eval Full `[parallel]` | Phase 4 |

조기 종료 허용:
- **W2 종료**: Phase 0 완료만으로도 AiClient 범용화 / 상태 영속화 / 관측 인프라의 단독 가치
- **W4 종료**: Phase 0~2 → 대화 자연스러움 + 꼬리질문 품질 배포
- **W6 종료**: Phase 0~3 → 이력서 면접 파이프라인까지
- **W7 종료**: 전체

Critic 리뷰에서 제기된 Critical 3 + Major 6 + Missing 7 이슈를 근본 원인별로 묶어 `REMEDIATION.md`에 정리. Phase 0(W1-W2)은 모든 issue의 구조적 선행 조건.

## 제약조건 / 환경

- **모델 구성** (CLAUDE.md 참조, 변경 없음): Backend는 GPT-4o-mini primary + Claude fallback, Lambda(Verbal/Vision)는 Gemini 주력
- **모든 신규 프롬프트는 모델 중립**: OpenAI/Claude 양쪽에서 동일 JSON 스키마 출력 보장
- **Prompt Caching**: Claude `cache_control: ephemeral`, OpenAI 자동 캐싱. Layer 1은 ≥ 1024 tokens 보장
- **Feature Flag**: 모든 신규 기능은 `rehearse.features.*` 하위 toggle로 즉시 롤백 가능
- **Spec-Driven Development** (CLAUDE.md 강제): 구현 전 반드시 각 `plan-NN-*.md`를 선행 작성/리뷰
- **BE/FE PR 분리**: 본 플랜은 전부 Backend. FE 연동은 별도 PR

## 관련 문서

- 원본 아이디어: `docs/todo/2026-04-20/00-overview.md` ~ `07-context-engineering.md`
- 직전 스프린트: `docs/plans/prompt-improvement-2026-04/` (audit + v2 프롬프트)
- 진행 추적: `./progress.md`
- **Critic 리뷰 대응 (Phase 0)**: `./REMEDIATION.md`
- Phase 0 산출물: `./INVENTORY.md`, `./TEST_BASELINE.md`, `./IMPACT_MAP.md`, `./STATE_DESIGN.md`, `./FEEDBACK_DOMAIN.md`, `./OBSERVABILITY.md`
