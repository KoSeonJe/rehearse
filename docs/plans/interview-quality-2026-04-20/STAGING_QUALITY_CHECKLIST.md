# Staging Quality Checklist — Rubric Scorer Cut-over Gate

> 작성일: 2026-04-22
> 적용 시점: plan-08 + plan-09 스테이징 배포 완료 후, plan-13 flag-on cut-over 직전
> 참조: `plan-08-rubric-scorer.md`, `plan-09-feedback-synthesizer.md`, `plan-10-eval-harness-lite.md`, `plan-13-lambda-content-removal.md`

플랜-13 L35 "스테이징 품질 검수 통과" 의 구체 정의. 하기 5개 절을 모두 수행·통과해야 cut-over PR 진입.

---

## 1. 샘플 10건 — 하이브리드 구성

### 소스
- plan-10 `eval/golden-sets/v1/` 골든셋에서 **7건 선정** (카테고리 균형):
  - CS_FUNDAMENTAL × 2 (happy_path 1 + shallow 1)
  - FeedbackPerspective=EXPERIENCE × 2 (happy_path 1 + give_up 1)
  - RESUME_BASED × 2 (PLAYGROUND 모드 1 + INTERROGATION 모드 1)
  - BEHAVIORAL × 1
- **스테이징 신규 녹화 3건** 보강 (골든셋 미커버 시나리오):
  - LANGUAGE_FRAMEWORK (frontend React) × happy_path
  - LANGUAGE_FRAMEWORK (backend Spring) × clarify 의도 전환
  - RESUME_BASED × WRAP_UP 모드 (시간 소진 시나리오)

### 저장
- 경로: `eval/staging-samples/v1/sample-{01..10}.yaml`
- 스키마: plan-10 골든셋 스키마 재사용 (`case_id`, `category`, `coverage_class`, `initial_question`, `user_turns`)
- 선정 근거(골든셋 case_id + 신규 녹화 3건 메타): 본 문서 말미 "샘플 선정 로그" 섹션에 PR 시점 기록

---

## 2. 라벨링 — 1명 + J3 Judge 교차 검증

### 라벨링
- 라벨러: 개발자 본인 단독
- 대상: 10건 × 평균 4차원 ≈ 40 점수 기입
- 기준: `_dimensions.yaml` 각 차원의 `scoring.{1,2,3}.observable` 리스트를 근거로 점수 부여. null 허용 (의미 없는 차원)
- 결과 저장: `eval/staging-samples/v1/labels.yaml` (`{sample_id, dimension_id, score, rationale, evidence_quote}`)

### 교차 검증
- plan-10 J3 Feedback Rubric Adherence Judge 를 동일 샘플에 돌림
- **일치율 ±1점 내 ≥ 80%** 확인. 80% 미만이면 Judge 프롬프트 또는 라벨 기준 재검토 (plan-10 신뢰성 검증 프로토콜과 동일)
- Judge 결과도 `eval/staging-samples/v1/labels.yaml` 에 함께 기록

---

## 3. 통과 기준 (5개 Gate 모두 pass 시 승인)

| Gate | 측정 대상 | 기준 |
|------|-----------|------|
| G1 차원 누락률 | RubricScorer 결과에서 `DIMENSIONS_TO_SCORE` 지정 차원 중 score=null 비율 (CLARIFY/의도상 null 제외) | **0%** |
| G2 evidenceQuote 포함률 | score != null 차원 중 `evidenceQuote` 가 실제 user_answer 에 존재 (정규식 매칭) | **≥ 95%** |
| G3 매핑 정확도 | 샘플 10건이 `_mapping.yaml` 규칙에 따라 기대 rubric_id 로 귀결 | **100%** |
| G4 레벨 평균 점수 밴드 | 차원별 레벨 평균 점수 — **10건 heuristic, v1 한정** | junior 1.8~2.2 / mid 2.1~2.5 / senior 2.4~2.8 내. 밴드 이탈 차원이 전체의 **20% 이하** |
| G5 라벨-Judge 일치율 | 개발자 라벨 vs J3 Judge ±1점 일치율 | **≥ 80%** |

G4 의 밴드는 10건 샘플 기준의 heuristic 이며 v1 에 한정. 향후 샘플 수가 늘거나 운영 데이터가 축적되면 본 문서 말미 "CHANGELOG" 섹션에 근거와 함께 갱신.

---

## 4. 실패 시 재조정 — 허용/금지 매트릭스

| 실패 Gate | 허용 액션 | 금지 액션 |
|-----------|----------|----------|
| G1 차원 누락 | `turn-rubric-scorer.txt` 프롬프트 수정, `DIMENSIONS_TO_SCORE` 주입 로직 디버그 | YAML `per_turn_rules` 변경으로 차원 누락 은폐 |
| G2 인용 부재 | 프롬프트에 evidenceQuote hard-requirement 강화, parser 검증 보강 | 정규식 완화로 거짓 pass 만들기 |
| G3 매핑 오류 | `_mapping.yaml` 규칙 순서·조건 수정 | 매핑 자체 우회 |
| G4 분포 이상 | `_dimensions.yaml` scoring observable 세분화, rubric `level_expectations` 조정 | 샘플 재선정 (확증편향) |
| G5 Judge 불일치 | plan-10 J3 프롬프트 튜닝, 라벨러 기준 재정의 | 일치율 기준 80% → 60% 완화 |

### 공통 규칙
- 허용 범위 총괄: `_dimensions.yaml`, 6개 rubric YAML (`weight` / `per_turn_rules` / `level_expectations`), `turn-rubric-scorer.txt` 프롬프트
- 재조정 후 **동일 10건 전수 재실행** 필수 (부분 재실행 금지 — 샘플 오염 방지)
- 실패 라운드 3회 초과 시 retrospective 필수 (원인 분석 + 설계 재검토)

---

## 5. 통과 시 기록

- `progress.md` 진행 로그에 다음 형식으로 기록:
  ```
  YYYY-MM-DD  staging-quality-gate PASSED (rubric-family v1)
  - 샘플: eval/staging-samples/v1/ (10건)
  - G1~G5 결과: …
  - 재조정 라운드: N회
  ```
- plan-13 cut-over PR description 에 본 체크리스트 링크 + 결과 요약 첨부 의무화
- `eval/staging-samples/v1/` 디렉토리 전체를 PR 에 포함 (라벨 + Judge 결과 포함)

---

## Out of Scope

- Production 품질 모니터링 (plan-12 Feature Flag Cleanup 단계에서 수행)
- 과거 인터뷰 소급 Rubric 적용 품질 (plan-13 Out of Scope)
- Nonverbal D11~D14 품질 검수 (plan-11 별도 gate)
- Rubric Scorer 자체 유닛 테스트 (plan-08 `./gradlew test --tests "Rubric*Test"`)

---

## 샘플 선정 로그
> PR 시점 기록. v1 기준.

(pending — plan-13 cut-over PR 작성 시 채움)

## CHANGELOG
- 2026-04-22 v1 초안 작성. G4 레벨 밴드는 10건 heuristic.
