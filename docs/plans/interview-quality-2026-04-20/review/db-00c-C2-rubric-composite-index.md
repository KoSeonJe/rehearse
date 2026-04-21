---
id: db-00c-C2-rubric-composite-index
severity: critical
category: database
plan: plan-00c-session-state-persistence
reviewer: database-optimization
raised_at: 2026-04-21
resolved_at: 2026-04-21
status: resolved
---

# `rubric_score` 복합 인덱스 누락 — rubric_id 별 통계 쿼리 풀스캔 위험

## 문제

`idx_rubric (rubric_id)` 단독 인덱스로는 `WHERE rubric_id = ? GROUP BY interview_id` 형태 통계 쿼리에서 `interview_id` 접근 시 row lookup 이 필요하거나 인덱스 머지가 발생한다.

## 원인

V26 의 현재 인덱스:
- `idx_interview_turn (interview_id, turn_id)` — 세션 내 턴별 조회 커버
- `idx_rubric (rubric_id)` — rubric 단독 조회만 커버

후속 `plan-10` (Eval Harness) 또는 운영 분석이 `"어느 rubric 에서 어느 interview 가 낮은 점수를 받았는가"` 를 쿼리하려면 `(rubric_id, interview_id)` covering index 가 이상적.

## 발생 상황

- **언제**: `plan-10` Eval Harness 가 rubric 별 품질 분석 쿼리를 돌릴 때, 또는 운영팀이 ad-hoc 분석 시
- **누가**: 평가 시스템 운영자, 데이터 분석가
- **파장**: 데이터가 누적될수록 (10k+ rubric_score 행) 쿼리 지연 선형 증가, 최악 시 풀스캔

## 해결 방법

`idx_rubric (rubric_id)` 를 `idx_rubric_interview (rubric_id, interview_id)` 로 교체. leftmost prefix 로 `rubric_id` 단독 쿼리도 여전히 커버 — 기능 유실 없음.

```sql
-- Before
INDEX idx_rubric (rubric_id)

-- After
INDEX idx_rubric_interview (rubric_id, interview_id)
```

**대안(기각)**: 인덱스 추가 유지 (두 개 모두). `idx_rubric` 은 `idx_rubric_interview` 의 prefix 이므로 중복 — MySQL optimizer 가 알아서 장 긴 인덱스 선택. 중복 인덱스는 쓰기 비용 증가.

## 결과

- 수정: `backend/src/main/resources/db/migration/V26__create_rubric_score.sql`
- 인덱스 이름 변경: `idx_rubric` → `idx_rubric_interview`
- 기존 운영 DB 는 아직 미배포 (V26 이 첫 배포) 이므로 ALTER 불필요 — 파일만 수정
- 테스트: Flyway 마이그레이션 validate + `SHOW INDEX FROM rubric_score` 로 확인
