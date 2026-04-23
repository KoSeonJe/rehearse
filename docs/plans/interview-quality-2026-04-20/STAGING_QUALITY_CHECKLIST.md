# Staging Quality Checklist — Rubric Scorer Automated Validation

> 작성일: 2026-04-22
> 수정일: 2026-04-23 (A/B 측정 인프라 축소 — G4/G5 및 Judge 기반 라벨링 절차 제거)
> 적용 시점: plan-08 + plan-09 스테이징 배포 완료 후, plan-13 ECR cut-over 직전
> 참조: `plan-08-rubric-scorer.md`, `plan-09-feedback-synthesizer.md`, `plan-13-lambda-content-removal.md`, `MANUAL_AB_PROTOCOL.md`

plan-13 "신규 ECR 이미지 배포 + Lambda 함수 버전 업데이트" cut-over 전 수동 비교 품질 검수의 구체 정의.
정성적 비교는 `MANUAL_AB_PROTOCOL.md` 프로토콜(3~5건 수동 diff)을 따르고,
정량 자동 검증은 하기 G1~G3 게이트를 모두 통과해야 cut-over PR 진입한다.

---

## 통과 기준 (3개 Gate 모두 pass 시 승인)

| Gate | 측정 대상 | 기준 |
|------|-----------|------|
| G1 차원 누락률 | RubricScorer 결과에서 `DIMENSIONS_TO_SCORE` 지정 차원 중 score=null 비율 (CLARIFY/의도상 null 제외) | **0%** |
| G2 evidenceQuote 포함률 | score != null 차원 중 `evidenceQuote` 가 실제 user_answer 에 존재 (정규식 매칭) | **≥ 95%** |
| G3 매핑 정확도 | 샘플 케이스가 `_mapping.yaml` 규칙에 따라 기대 rubric_id 로 귀결 | **100%** |

G1~G3 는 plan-08 `RubricLoaderTest` / `RubricScorerTest` 단위·통합 테스트로 자동 검증한다. Judge LLM 없이 정규식/assert 기반으로 실행되므로 CI 파이프라인에서 반복 실행 가능.

---

## 실패 시 재조정 — 허용/금지 매트릭스

| 실패 Gate | 허용 액션 | 금지 액션 |
|-----------|----------|----------|
| G1 차원 누락 | `turn-rubric-scorer.txt` 프롬프트 수정, `DIMENSIONS_TO_SCORE` 주입 로직 디버그 | YAML `per_turn_rules` 변경으로 차원 누락 은폐 |
| G2 인용 부재 | 프롬프트에 evidenceQuote hard-requirement 강화, parser 검증 보강 | 정규식 완화로 거짓 pass 만들기 |
| G3 매핑 오류 | `_mapping.yaml` 규칙 순서·조건 수정 | 매핑 자체 우회 |

### 공통 규칙
- 허용 범위 총괄: `_dimensions.yaml`, 6개 rubric YAML (`weight` / `per_turn_rules` / `level_expectations`), `turn-rubric-scorer.txt` 프롬프트
- 재조정 후 **동일 테스트 전수 재실행** 필수
- 실패 라운드 3회 초과 시 retrospective 필수 (원인 분석 + 설계 재검토)

---

## 정성 비교 (MANUAL_AB_PROTOCOL.md 연계)

G1~G3 자동 검증 통과 후, 수동 비교는 `MANUAL_AB_PROTOCOL.md` 에 정의된 ECR 2개 병렬 기동 → 3~5건 세션 투입 → JSON diff 방식으로 수행한다.

- 꼬리질문 판정: 신버전이 구버전 대비 "사용자 답변 claim 에 더 정확히 꽂힌다" 가 과반 이상
- 피드백 판정: 신버전이 "관찰 인용 포함 + 다음 액션 구체" 에서 구버전 대비 과반 이상 우세
- 결과 기록: `eval/manual-ab/{YYYY-MM-DD}-{plan-id}.md`

---

## 통과 시 기록

- `progress.md` 진행 로그에 다음 형식으로 기록:
  ```
  YYYY-MM-DD  staging-quality-gate PASSED (rubric-family v1)
  - G1~G3 결과: all pass
  - 수동 비교: eval/manual-ab/{YYYY-MM-DD}-plan-13.md
  - 재조정 라운드: N회
  ```
- plan-13 cut-over PR description 에 본 체크리스트 링크 + MANUAL_AB_PROTOCOL 결과 요약 첨부 의무화

---

## Out of Scope

- Production 품질 모니터링 (Grafana `rehearse_ai_call_duration_seconds` p95 로 운영 중 확인)
- 과거 인터뷰 소급 Rubric 적용 품질 (plan-13 Out of Scope)
- Nonverbal D11~D14 품질 검수 (plan-11 별도 gate)
- Rubric Scorer 자체 유닛 테스트 (plan-08 `./gradlew test --tests "Rubric*Test"`)
- Judge LLM 기반 자동 평가 (Judge/골든셋 방식은 이 스프린트에서 도입하지 않음)

---

## CHANGELOG
- 2026-04-22 v1 초안 작성. G1~G5 5개 게이트.
- 2026-04-23 v2 개정. A/B 측정 인프라 축소 결정에 따라 G4(레벨 밴드) / G5(Judge 일치율) 및 Judge 기반 10건 라벨링 절차 전면 삭제. G1~G3 자동 검증만 유지. MANUAL_AB_PROTOCOL.md 와 연계 명시. 제목 "Rubric Scorer Cut-over Gate" → "Rubric Scorer Automated Validation" 변경.
