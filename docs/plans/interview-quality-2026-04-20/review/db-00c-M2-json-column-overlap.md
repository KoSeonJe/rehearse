---
id: db-00c-M2-json-column-overlap
severity: major
category: database
plan: plan-00c-session-state-persistence
reviewer: database-optimization
raised_at: 2026-04-21
resolved_at: 2026-04-21
status: resolved
---

# `scored_dimensions` / `scores_json` 역할 중복

## 문제

V26 `rubric_score` 가 두 JSON 컬럼을 갖는다:
- `scored_dimensions JSON` — 평가된 차원 ID 배열 (예: `["D2","D3","D4"]`)
- `scores_json JSON` — 차원별 상세 (예: `{"D2": {score, observation, evidence_quote}, ...}`)

`scored_dimensions` 는 `scores_json.keys()` 와 정확히 동일한 정보. 중복 저장은 스토리지 낭비 + 쓰기 시 일관성 부담 + JSON 파싱 비용 중복.

## 원인

plan 명세가 "인덱싱/필터용 차원 배열" 과 "상세 점수" 를 따로 두는 패턴으로 기술됨. 실제로는 MySQL 8.0 JSON 함수 (`JSON_KEYS`, `JSON_CONTAINS`) 로 `scores_json` 에서 직접 추출 가능 — 별도 컬럼 불필요.

## 발생 상황

- **언제**: plan-08 (RubricScorer) 가 점수 INSERT 할 때, plan-10 (Eval Harness) 가 차원별 통계 쿼리할 때
- **누가**: plan-08 구현자 — 두 컬럼 모두 채워야 함. 누락 시 silent 데이터 불일치
- **파장**: 두 컬럼이 어긋나면 어느 쪽이 진실인지 모호. 변경 시 양쪽 동기화 필요

## 해결 방법

`scored_dimensions` 컬럼 삭제. 차원 배열이 필요한 쿼리는 `JSON_KEYS(scores_json)` 사용.

```sql
-- Before (V26)
scored_dimensions JSON NOT NULL,
scores_json JSON NOT NULL,

-- After
scores_json JSON NOT NULL,
```

차원 별 필터 쿼리 예:
```sql
-- 특정 차원 D2 가 평가된 행 조회
SELECT * FROM rubric_score WHERE JSON_CONTAINS_PATH(scores_json, 'one', '$.D2');
```

**대안(기각)**:
1. 컬럼 유지 + 주석으로 "인덱싱 전용" 명시 — 동기화 버그 여지 잔존
2. Generated column (`scored_dimensions AS JSON_KEYS(scores_json)`) — MySQL functional index 필요 시에만 재도입

## 결과

- 수정: `backend/src/main/resources/db/migration/V26__create_rubric_score.sql` — `scored_dimensions` 컬럼 제거
- plan-00c 명세 본문의 `scored_dimensions` 설명 삭제
- plan-08 구현자는 `scores_json` 한 컬럼만 작성하면 됨
- 향후 차원별 인덱싱 필요 시 functional index 또는 generated column 으로 **재도입 가능** — 지금 빼는 것이 쉬움
