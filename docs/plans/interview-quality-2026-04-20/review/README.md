# plan-00c Review Index

`plan-00c` (Session State Persistence) 구현 후 `architect-reviewer` + `database-optimization` 병렬 리뷰에서 식별된 이슈 기록.

각 문서는 `REVIEW_TEMPLATE.md` 를 따른다 — 문제 / 원인 / 발생 상황 / 해결 방법 / 결과.

## 상태 테이블

### Architecture (리뷰어: `architect-reviewer`)
| ID | 심각도 | 제목 | 상태 |
|----|--------|------|------|
| [arch-00c-C1](arch-00c-C1-update-atomicity.md) | Critical | `update()` atomicity 계약 미명시 | resolved |
| [arch-00c-C2](arch-00c-C2-lock-transaction-ordering.md) | Critical | Lock / Transaction 순서 규약 부재 | resolved |
| [arch-00c-M1](arch-00c-M1-store-srp-violation.md) | Major | Store SRP 위반 (캐시 + 메트릭 + 생성) | resolved |
| [arch-00c-M2](arch-00c-M2-cache-metrics-gauge.md) | Major | Caffeine 메트릭이 gauge 로 등록 | resolved |
| [arch-00c-M3](arch-00c-M3-raw-object-type-leak.md) | Major | RuntimeState Object 타입 노출 | resolved |
| [arch-00c-M4](arch-00c-M4-l3-l4-boundary-blur.md) | Major | L3 / L4 경계 결정 미문서화 | resolved |

### Database (리뷰어: `database-optimization`)
| ID | 심각도 | 제목 | 상태 |
|----|--------|------|------|
| [db-00c-C1](db-00c-C1-rubric-turn-fk-missing.md) | Critical | V26 `turn_id` FK 누락 | deferred |
| [db-00c-C2](db-00c-C2-rubric-composite-index.md) | Critical | `rubric_score` 복합 인덱스 누락 | resolved |
| [db-00c-M1](db-00c-M1-datetime-to-timestamp.md) | Major | `DATETIME` → `TIMESTAMP` (UTC 보장) | resolved |
| [db-00c-M2](db-00c-M2-json-column-overlap.md) | Major | `scored_dimensions` / `scores_json` 역할 중복 | resolved |

## Deferred 사유

- **db-00c-C1**: `interview_turn` 테이블이 본 리포지토리에 존재하지 않음. 턴 테이블 설계는 `plan-02` (Answer Analyzer) 에서 다룬다. FK 추가는 해당 plan 구현 PR 에서 함께 처리.

## 규약 (knowledge base 용)

- 리뷰 판정이 `REQUEST_CHANGES` 또는 `BLOCK` 인 경우 **이슈 1건당 문서 1개** 를 작성한다
- 문서는 plan 별 디렉토리 (`review/`) 에 모아 관리
- 심각도는 원 리뷰어 판정을 따른다 (Critical / Major / Minor)
- 해결 후 `resolved_at` 과 `status: resolved` 기록, 상태 테이블 갱신
