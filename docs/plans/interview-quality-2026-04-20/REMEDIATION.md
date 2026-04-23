ㅑㅜ# Remediation — Critic 리뷰 대응 (근본 원인 기반 재설계)

> 상태: Active
> 작성일: 2026-04-20
> 트리거: `critic` 에이전트의 조건부 승인 (Critical 3 + Major 6 + Missing 7)

## Why (이 문서가 왜 필요한가)

최초 플랜(requirements.md + plan-01~10)은 "무엇을 만들지"는 정의했으나 **"현재 시스템이 그걸 받아들일 수 있는지"** 를 검증하지 않았다. 결과적으로 critic이 지적한 문제들이 같은 뿌리에서 반복적으로 터졌다:

- Critical 3개(C1/C2/C3) = "인프라 전제 검증 부재"
- Major 6개(M1~M6) = "현재 코드베이스와의 통합 지점 부실"
- Missing 7개 = "운영/관측/회귀방어 계층 누락"

이 문서는 **개별 이슈별 패치가 아닌 근본 원인별 묶음 해결**을 제시한다. Phase 0(인프라 전제)을 W1-W2에 선행 배치하고, 기존 plan-01~10을 그 전제 위에서 재조정한다.

---

## 근본 원인 매핑 (Root Cause Analysis)

### RC1. "AiClient 인터페이스가 도메인 결합되어 있다"
- **증상**: C1(3-메서드 고정), C3(모델 선택 불가), M5(Fallback 캐시 콜드 미스 무대응)
- **진단**: 현재 `AiClient`는 `generateQuestions / generateFollowUpQuestion / generateFollowUpWithAudio` 3개 도메인 메서드. 신규 호출 패턴(Intent, Answer Analyzer, Compactor, Rubric Scorer, Synthesizer, Resume Extractor, Chain Interrogator 등 **최소 9개 신규 호출**)은 전부 다른 스키마 → 인터페이스를 9번 더 찢거나, 범용화하거나 양자택일.
- **근본 해결**: `AiClient` 인터페이스를 **범용 chat 호출 + 호출 메타(model, temperature, maxTokens, cachePolicy, responseFormat) 파라미터화**로 재설계. 기존 3개 도메인 메서드는 이 위에 얇은 어댑터로 보존(회귀 방지).
- **플랜**: **plan-00b** 신설

### RC2. "세션 상태 영속화 경계가 정의되지 않았다"
- **증상**: C2(Flyway 마이그레이션 누락), M6(기존 `TimestampFeedback`/`QuestionSetFeedback`과 관계 불명), Missing(동시성, 메모리 풋프린트, FE 미연동 피드백 유실)
- **진단**: plan-05/06/08/09가 만드는 새 상태(`ResumeSkeleton`, `InterviewPlan`, `RubricScore[]`, `SessionFeedback`)의 **저장소(Redis vs DB vs In-memory) 결정이 없음**. 또한 기존 `Interview` aggregate와의 관계(같은 트랜잭션? 이벤트? 별도 트랜잭션?)도 미정.
- **근본 해결**: 상태 카테고리를 **4계층으로 분류**하고 각각의 저장소/TTL/consistency를 먼저 결정 → 그 다음 plan-05/06/08/09가 그 결정을 소비. Flyway V24~ 마이그레이션 스키마를 Phase 0에 확정.
- **플랜**: **plan-00c** 신설

### RC3. "현재 코드베이스 지도 없이 변경 계획을 썼다"
- **증상**: M4(`InterviewTurnService` 실재 않음, 실제로는 `FollowUpService` + `InterviewService`), Missing(`PdfTextExtractor` 이미 존재), 각 plan의 "기존 클래스 (or equivalent)" 모호성
- **진단**: 작성자가 실제 파일 트리를 검증 않고 플랜을 썼다. 실행자(sub-agent 또는 사람)는 매 plan마다 "이 클래스 어디 있지?" 탐색으로 시간을 잃는다.
- **근본 해결**: **현재 코드 인벤토리 문서**를 작성해 모든 후속 plan이 참조. 인벤토리는 (a) 실재 클래스/메서드 맵, (b) 기존 테스트 커버리지 현황, (c) 각 plan이 건드릴 위치를 실제 경로로 고정.
- **플랜**: **plan-00a** 신설

### RC4. "관측/회귀방어 계층이 W4까지 없다"
- **증상**: M2(W1-W3 회귀 감지 불가), Missing(APM 메트릭, 턴당 LLM 호출 수 증가 관측)
- **진단**: 회귀 발생 시 사용자 컴플레인으로만 감지되는 리스크. 프로덕션 LLM 호출 수/지연/실패율도 측정 포인트 없음.
- **근본 해결 (2026-04-23 개정)**: (a) 기존 통합테스트 커버리지 파악 + 최소 보호선 설정(plan-00a), (b) 신규 LLM 호출 경로에 `Micrometer` 태그 표준(`ai.call.type`, `ai.model`, `ai.cache_hit`) 도입 (plan-00d), (c) 각 plan PR 머지 전 `MANUAL_AB_PROTOCOL.md` 수동 비교 3~5건 실행. Judge/골든셋 기반 smoke eval 은 폐기.
- **플랜**: **plan-00d** 신설

### RC5. "시간 압축이 계획 질을 훼손했다"
- **증상**: M1(4주는 비현실)
- **근본 해결**: 7주 로드맵으로 재산정. Phase 0(W1-W2)을 추가해도 병렬화로 버퍼 흡수 가능.
- **플랜**: requirements.md 로드맵 갱신

### RC6. "축소 결정의 엣지 케이스 분석이 얕았다"
- **증상**: M3(META/OFF_TOPIC ANSWER 병합이 역효과), plan-07 fact_check_flag 코드-Out of scope 모순
- **근본 해결**: (a) 3-intent는 유지하되 **ANSWER fallback 경로에 가드 로직** 명시(`answer_quality ≤ 1 AND 질문과 무관` → 재설명 시도), (b) plan-07 프롬프트/JSON 스키마에서 `fact_check_flag` 관련 필드를 명시적으로 삭제하고 "이 필드는 본 스프린트 범위 아님. TODO 원본 06 참조" 주석.
- **플랜**: plan-01/plan-07 문서 수정

### RC7. "Feature flag runtime 변경 메커니즘 없음" — 2026-04-23 재결정
- **증상**: Missing(Spring Cloud Config 없는데 '즉시 롤백' 주장)
- **2026-04-23 결정**: runtime flag 메커니즘 자체 폐기. ECR 이미지 태그 재배포 + 세션 스토어 캐시 퍼지로 롤백. S2 에서 구현된 `@RefreshScope` / `AiFeatureProperties` / `/actuator/refresh` / `spring-cloud-context:4.1.4` 의존성은 PR B 에서 철거. `ChatRequest.modelOverride` 는 모델 선택 자체 가치로 유지.
- **플랜**: 본 RC 는 "의도적 축소" 로 종결. plan-00b 내 Feature Flag 섹션 삭제됨.

---

## Phase 0 태스크 (W1-W2 선행, 5개 신규)

| Plan | 해결하는 RC | 주차 | 병렬화 |
|---|---|---|---|
| **plan-00a** Codebase Inventory | RC3, RC4(부분) | W1 초반 | `[blocking]` 모든 후속 plan의 선결 조건 |
| **plan-00b** AiClient Generalization | RC1, RC7 | W1 후반 | `[blocking]` plan-01~10 전체 |
| **plan-00c** Session State Persistence Design | RC2 | W2 초반 | `[parallel:00b]` 인터페이스 설계만 — 00b와 계약 교차검증 |
| **plan-00d** Observability + Eval Smoke | RC4 | W2 후반 | `[parallel:00c]` |
| **plan-00e** Feedback Migration Strategy | RC2(M6) | W2 후반 | `[parallel:00d]` 기존 `TimestampFeedback`/`QuestionSetFeedback` ↔ 새 `SessionFeedback` 관계 결정 |

각 플랜 상세는 `plan-00a-codebase-inventory.md` 등 별도 파일. Phase 0 완료 후에야 plan-01 착수 가능.

---

## 기존 plan-01~10 수정 지시 (이 문서 승인 후 일괄 적용)

| 플랜 | 수정 내용 | 근거 |
|---|---|---|
| **requirements.md** | 로드맵 4주 → 7주로 갱신, Phase 0 추가 | M1, RC5 |
| **progress.md** | 14개 태스크(Phase 0 5개 + Phase 1~4 10개)로 갱신 | 위와 동일 |
| **plan-01** | (a) 수정 대상 `InterviewTurnService` → **`FollowUpService.generateFollowUp()` 앞단** 으로 변경. (b) ANSWER fallback에 META/OFF_TOPIC 가드 로직 추가 | M3, M4 |
| **plan-02** | `InterviewSession` 언급을 실제 aggregate인 **`Interview` 엔티티 + `InterviewRuntimeState`(신규 상태 객체, plan-00c)** 로 변경 | RC3 |
| **plan-03** | 기존 `FollowUpPromptBuilder` 위치 확인됨. v3 프롬프트 교체 + Step A 분석 주입 로직 구체화 | (경미) |
| **plan-04** | (a) plan-00b 완료 전제로 재작성. (b) Fallback 캐시 콜드 미스 수용 정책 명시(`max-context-tokens` 상한은 fallback에도 동일 적용). (c) `DialogueCompactor` LLM 호출 코스트를 total 예산에 산정 | M5, Missing(코스트) |
| **plan-05** | GPT-4o 호출은 plan-00b의 모델 오버라이드 메커니즘 경유. 기존 `PdfTextExtractor` 확장으로 Phase 0 Ingestion 구성(신규 클래스 최소화) | C3, Missing(PDF) |
| **plan-06** | `InterviewPlan` 영속화를 plan-00c의 결정에 위임. Flyway V24 마이그레이션 파일명 확정 | C2 |
| **plan-07** | (a) `InterviewTurnService` → 실제 진입점 명시. (b) `resume-chain-interrogator.txt` JSON 스키마에서 `fact_check_flag`/`fact_check_note` 삭제 + "Out of scope" 주석. (c) 동시성: `ChainStateTracker`는 request-scoped 또는 `Interview.id` 단위 lock | M4, RC6, Missing(동시성) |
| **plan-08** | `RubricScore` 영속화를 plan-00c 결정에 위임. DTO 분리(Entity 직접 반환 금지 CLAUDE.md 원칙) 명시 | C2, 규약 |
| **plan-09** | (a) plan-00e(Feedback Migration) 결정 소비. (b) Verbal/Vision 비동기 완료 대기 전략(polling + timeout → partial feedback 생성). (c) 기존 `FeedbackService` 확장 vs 신규 `SessionFeedbackService` 분리 결정 | M6 |
| ~~**plan-10**~~ | ~~Judge-Human 일치율 검증~~ — **plan-10 전체 폐기 (2026-04-23)**. `MANUAL_AB_PROTOCOL.md` 수동 비교로 대체 | — |

---

## 관측 포인트 표준 (RC4 운영 차원)

모든 신규 LLM 호출에 `Micrometer` 태그 필수:

| 태그 | 값 예시 | 용도 |
|---|---|---|
| `ai.call.type` | `intent_classifier / answer_analyzer / follow_up_v3 / compactor / resume_extractor / chain_interrogator / rubric_scorer / feedback_synthesizer` | 호출 분류 |
| `ai.model` | `gpt-4o-mini / claude-haiku / gpt-4o / claude-sonnet` | 모델별 비용 |
| `ai.provider` | `openai / anthropic` | Fallback 발동률 |
| `ai.cache_hit` | `true / false / n/a` | 캐시 효율 |
| `ai.fallback` | `true / false` | Resilience 상태 |

메트릭: `rehearse.ai.call.duration`, `rehearse.ai.call.tokens.input`, `rehearse.ai.call.tokens.output`, `rehearse.ai.call.tokens.cached`.

---

## 의존성 그래프 (재조정 후)

```
Phase 0 (W1-W2):
  00a (Inventory) ──┬──> 00b (AiClient)
                    └──> 00c (State) ──> 00e (Feedback Migration)
                         00d (Observability+Eval Smoke)

Phase 1 (W3):      01 (Intent) [requires 00a, 00b, 00d]
Phase 2 (W4):      02 + 03 (Answer Analyzer || Follow-up v3) [requires 01]
Phase 3 (W5-W6):   04 (Context Eng) ──> 05 + 06 (parallel) ──> 07 (Orchestrator)
Phase 4 (W7):      08 (Rubric) ──> 09 (Synthesizer) + 10 (Eval Full) [parallel]
```

조기 종료 허용 지점:
- W2: Phase 0 완료만으로도 **AiClient 범용화 + 상태 영속화 + 관측** 인프라 개선 단독 가치
- W4: Phase 0~2 → 대화 자연스러움 + 꼬리질문 품질 개선 배포
- W6: Phase 0~3 → 이력서 면접 파이프라인까지
- W7: 전체

---

## 검증 (이 Remediation이 잘 작동하는지)

1. Phase 0 완료 시점에 `AiClient.chat(request)` 범용 메서드가 **최소 1개 신규 호출(Intent Classifier smoke)** 로 동작 증명
2. Flyway V24(신규) 마이그레이션이 `./gradlew flywayMigrate` 로컬 통과, prod-like 환경에서 롤백 스크립트 V24__rollback.sql 존재
3. `./gradlew test` 현재 통과율을 Phase 0 진입 전후로 비교 — **회귀 0건** (기존 수치 유지)
4. 관측 대시보드에서 `rehearse.ai.call.*` 메트릭이 Intent Classifier 호출 시 노출됨
5. Eval smoke(골든셋 5 + J1 1개)가 `npm`/`python` 없이 `./gradlew evalSmoke` 또는 `eval/scripts/smoke.sh` 로 실행
6. critic이 지적한 Critical 3 + Major 6 중 해결된 항목을 `REMEDIATION.md` 하단 체크리스트에 기록 (아래)

### 해결 체크리스트

- [ ] C1. AiClient 인터페이스 범용화 — plan-00b
- [ ] C2. DB 영속화/Flyway 설계 — plan-00c
- [ ] C3. 호출별 모델 선택 — plan-00b
- [ ] M1. 타임라인 7주로 재산정 — requirements.md
- [ ] M2. W1-W3 회귀 방어 — plan-00d
- [ ] M3. META/OFF_TOPIC 가드 — plan-01 edit
- [ ] M4. 실제 클래스명 정정 — plan-00a 인벤토리 + plan-01/07/08 edit
- [ ] M5. Fallback 캐시 콜드 미스 정책 — plan-04 edit + plan-00b
- [ ] M6. 기존 FeedbackService와의 관계 — plan-00e
- [ ] Missing: PdfTextExtractor 기존 존재 — plan-05 edit
- [ ] Missing: APM 메트릭 표준 — plan-00d + 본 문서 "관측 포인트 표준"
- [x] Missing: Feature flag runtime 변경 — **의도적 축소 (2026-04-23)**. ECR 이미지 롤백으로 대체, 관련 코드 PR B 에서 철거
- [ ] Missing: 동시성 제어 — plan-07 edit (request-scoped / per-interview lock)
- [ ] Missing: JSON 파싱 실패 폴백 — plan-00b (범용 파서 + retry 정책)
- [x] ~~Minor: plan-10 수동 라벨=골든셋 부분집합~~ — plan-10 전체 폐기 (2026-04-23)

---

## 다음 단계

1. 이 문서 승인 후 → plan-00a~00e 5개 문서를 `docs/plans/interview-quality-2026-04-20/` 아래 작성 (이미 초안 동반 제출)
2. `requirements.md` + `progress.md` 갱신 (본 커밋에 포함)
3. plan-01/04/05/06/07/08/09/10 의 "수정 지시" 적용은 각 plan 실행 직전 executor가 일괄 수행(하나의 PR에 묶음)

## Addendum (2026-04-20 저녁) — 비언어 루브릭 추가 (plan-11)

TODO 09(`docs/todo/2026-04-20/09-nonverbal-rubric.md`) 신규 반영. 기술 rubric(D1~D10, plan-08)과 **완전 분리된 형제 plan-11** 로 편입.

- **분리 근거**: 데이터 소스(verbal/vision analyzer 숫자 지표)와 처리 방식(결정론 threshold, LLM 0) 이 기술 rubric과 다름. 섞으면 복잡도 폭발.
- **차원**: D11 Fluency / D12 Confidence Tone / D13 Eye Contact & Posture / D14 Composure — 전역 공통, 카테고리 무관.
- **Mapper 위치**: Lambda(`lambda/analysis/analyzers/nonverbal_rubric_mapper.py`). 기존 `verbal_analyzer`/`vision_analyzer` 출력 소비. Backend는 `context_weights`만 적용.
- **선행 필드 확장**: `verbal_prompt_factory.py` 에 `speed_variance` 추가, `vision_analyzer.py` 에 `gaze_on_camera_ratio`/`posture_unstable_count` 확인·추가. 프롬프트 전면 개편은 Out of Scope(`prompt-improvement-2026-04` Lane 3-5 별건).
- **Flyway V28**: `nonverbal_score` 별도 테이블. plan-00c의 V24~V27 계보 확장.
- **mapping.yaml**: `always_apply: nonverbal-v1` 로 기술 rubric과 **병렬** 적용 규칙 추가. 턴마다 2쌍 점수.
- **plan-09 Synthesizer 연계**: Delivery 섹션을 차원별 구조화(D11~D14 점수 + 관찰 + 개선 액션) 로 상향. `nonverbal-improvement-actions.yaml` 참조.
- **progress.md** 태스크 11번 추가. 주차 W7, plan-08과 병렬, plan-09 선행.

---

## Addendum (2026-04-20 오후) — Rubric Family 재설계

TODO 03 개정판(`docs/todo/2026-04-20/03-m3-feedback-rubric.md`)이 단일 5차원 → **10차원 × 7루브릭 패밀리**로 변경됨. plan-08은 전면 재작성되어 `QuestionSetCategory` + `FeedbackPerspective` + Resume Track 조합을 `_mapping.yaml` 선언적 규칙으로 rubric_id에 매핑. 관련 문서(plan-00a, plan-00c V26, plan-07 D9/D10, plan-09 `SCORES_BY_CATEGORY`)도 연쇄 업데이트. 상세는 Plan Mode 승인본 Addendum 섹션 참조. (plan-10 J3 `category_dimension_fit` 항목은 2026-04-23 plan-10 폐기로 제거됨)

---

## 참조
- `critic` 에이전트 리뷰 리포트(세션 내): Critical 3 + Major 6 + Missing 7 + Minor 5
- TODO 원본: `docs/todo/2026-04-20/` (00~07)
- 최초 플랜: `docs/plans/interview-quality-2026-04-20/requirements.md` + `plan-01~10`
