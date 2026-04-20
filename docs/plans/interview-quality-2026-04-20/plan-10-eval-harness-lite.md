# Plan 10: Eval Harness Lite (M4 축소판) `[parallel:09]`

> 상태: Draft
> 작성일: 2026-04-20
> 주차: W4
> 원본: `docs/todo/2026-04-20/04-m4-evaluation-harness.md` (축소 채택)

## Why

plan-01~09를 만들어도 **측정 없이는 개선됐는지 확신할 수 없다**("감에 기반한 감 탈출" 자기모순). TODO 원본의 풀 버전(4개 Judge + 80 골든셋 + Promptfoo + GitHub Actions CI + Multi-provider 리그레션 리포트)은 2-3주 투자라 이번 스프린트 범위 초과 → **Lite 버전**으로 축소:

- Judge 3개만: J1(Follow-up Relevance) / J2(Intent Handling) / J3(Feedback Rubric Adherence)
- 골든셋 30개 (TODO 원본 80개의 약 40%)
- 실행 스크립트는 **자체 Python 스크립트**(Promptfoo 마이그레이션은 추후)
- CI 자동화 **없음** (로컬 수동 실행, PR 코멘트 봇도 없음)
- **제외**: J4 Consistency / J5 Resume Flow / J6 Context Fidelity, Multi-provider 동시 실행, 리그레션 머지 차단

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `eval/golden-sets/v1/concept/backend/*.yaml` | 신규 10개 (happy_path 5 + clarify 2 + give_up 2 + shallow 1) |
| `eval/golden-sets/v1/experience/backend/*.yaml` | 신규 10개 (happy_path 4 + clarify 2 + give_up 2 + shallow 2) |
| `eval/golden-sets/v1/resume/backend/*.yaml` | 신규 10개 (Resume Track 전용 시나리오) |
| `eval/judges/j1-followup-relevance.txt` | 신규 Judge 프롬프트 |
| `eval/judges/j2-intent-handling.txt` | 신규 |
| `eval/judges/j3-feedback-rubric-adherence.txt` | 신규 |
| `eval/scripts/run_eval.py` | 신규. 골든셋 실행 + Judge 호출 + 리포트 생성 |
| `eval/scripts/measure_judge_reliability.py` | 신규. 수동 라벨 vs Judge 일치율 측정 |
| `eval/reports/.gitkeep` | 신규. 실행 결과 저장 디렉토리 |
| `eval/requirements.txt` | 신규. openai, anthropic, pyyaml |
| `eval/README.md` | 신규. 사용법 (로컬 수동 실행) |

## 상세

### 골든셋 스키마 (요약)
```yaml
case_id: gs_001
category: concept|experience|resume
coverage_class: happy_path|clarify|give_up|shallow
initial_question: {...}
user_turns:
  - turn: 1
    user_utterance: "..."
    expected_intent: ANSWER
    expected_analysis: {answer_quality_min: 2, should_detect_missing: [...]}
    expected_behavior:
      follow_up_should: [...]
      follow_up_should_not: [...]
```

### Judge 3종

**J1 Follow-up Relevance** (3차원 × 1-5점):
- `relevance` / `depth_appropriateness` / `conversational_flow`
- 출력: `{relevance, depth_appropriateness, conversational_flow, overall}`

**J2 Intent Handling** (2차원):
- `intent_accuracy` (Y/N, golden vs 시스템 분류 일치)
- `response_quality` (1-5, 분류된 의도에 맞는 응답인지)

**J3 Feedback Rubric Adherence** (5차원 × 1-5점, plan-08 Rubric Family 반영):
- `has_observations` / `is_concrete` / `level_calibration` / `delivery_separation`
- **`category_dimension_fit`** (신규): 적용된 rubric_id의 `uses_dimensions` 외 차원이 strength/gap에 인용됐는지 탐지. 예: `concept-cs-fundamental-rubric` 세션 strength에 "Experience Concreteness(D6)" 언급되면 감점(카테고리-차원 부정합).
- **`cross_category_pattern`** (신규, 선택): 세션에 2개 이상 카테고리 섞였을 때 Synthesizer가 `overall.narrative` 에 교차 패턴 1회 이상 언급했는지.

### Judge 신뢰성 검증 (필수 선행)
1. 본인이 **골든셋 30개 중 부분집합 20개** 를 수동 라벨링 (각 차원별 정답 점수). 별도 케이스 만들지 않고 기존 골든셋 재사용 — 라벨 작성 부담 최소화.
   - 선정 기준: happy_path 8 / clarify 4 / give_up 4 / shallow 2 / resume 2 (커버리지 클래스 균형)
2. Judge 동일 케이스에 돌림
3. 일치율 계산 (±1점 내 일치율 + Cohen's kappa)
4. **80% 미만이면 Judge 프롬프트 튜닝 → 재측정**
5. 80% 이상 달성 후 본격 사용

### 리그레션 리포트 (간소화)
```
=== Rehearse Eval Report (Lite) ===
Golden Set: v1 (30 cases)
Run: 2026-04-25

J1 Follow-up Relevance    avg 4.12
J2 Intent Accuracy        93%  (28/30)
J3 Feedback Adherence     avg 3.88

BY COVERAGE CLASS
  happy_path (13):     J1 4.25
  clarify (6):         J2 100%
  give_up (6):         J2 92%
  resume (10):         J1 4.00

FAILED CASES
  - gs_017: J1 3.1 (target_claim_idx 오판)
  - gs_023: J2 Intent misclassified (CLARIFY → ANSWER)
```

CI 통합 **없음** — 사용자가 `python eval/scripts/run_eval.py` 로컬 실행.

### Multi-provider
본 Lite 버전은 **기본값 1개 provider만** 실행 (OpenAI). Fallback(Claude) 평가는 수동 옵션(`--provider=claude`)으로 제공, 기본 리포트 제외.

## 담당 에이전트

- Implement: `test-engineer` — 골든셋 스키마, Judge 프롬프트, 실행 스크립트
- Review: `critic` — Judge 신뢰성 검증 엄정성, 골든셋 커버리지 편향

## 검증

1. 3개 Judge 모두 수동 라벨 20개 대비 일치율 ≥ 80%
2. 골든셋 30개 실행 시간 ≤ 10분 (로컬)
3. `run_eval.py` 실행 후 `eval/reports/{timestamp}.md` 자동 생성
4. plan-01~09 배포 전후 비교 리포트가 실제 개선 수치를 보여줌 (J1 Δ≥+0.3, J2 0%→≥90%, J3 Δ≥+0.3)
5. 리포트 포맷이 읽기 쉬운지 본인 리뷰
6. `eval/README.md`만 보고 제3자가 실행 가능한지 확인
7. `progress.md` 10 → Completed
