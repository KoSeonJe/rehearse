# Task 14 — 운영 SQL 스크립트 (dev only)

> **위치**: `tasks/p2-be-14-dev-sql.md`
> **답하는 질문**: dev DB 레거시 row 어떻게 정리?

---

## 목적

`scripts/dev-cleanup-resume-legacy.sql` 신규 — P2 머지 직전 수동 실행. Flyway 자동 실행 X (DDL 전용 룰 준수). 레거시 `RESUME_PLAYGROUND` / `RESUME_INTERROGATION` row + 이력서 트랙 기존 채점 결과 truncate + skeleton JSON `interrogationPriorityMap` 키 정리.

## 에이전트

- **구현**: `backend` — SQL 스크립트 신규 + 실행 절차 문서화 + 테이블/컬럼명 grep 확인
- **리뷰**: `code-reviewer-backend` — FK 순서 / DML 안전성 / 실행 절차

## 변경 파일

- `scripts/dev-cleanup-resume-legacy.sql` — 신규 (루트 `scripts/` — AGENTS.md "Placement Rule" 정합)
- `docs/plans/481-resume-track-simplification/RUNBOOK.md` (옵션) — 실행 절차 (P2 머지 직전 dev 운영자 수동 실행)

## 핵심 로직

```sql
-- dev only. P2 머지 직전 수동 실행 (Flyway 자동 실행 X)

-- 1) 레거시 question_type row 삭제 (FK 순서 준수)
DELETE FROM question_score
 WHERE question_id IN (SELECT id FROM questions WHERE question_type IN ('RESUME_PLAYGROUND', 'RESUME_INTERROGATION'));
DELETE FROM questions WHERE question_type IN ('RESUME_PLAYGROUND', 'RESUME_INTERROGATION');

-- 2) 이력서 트랙 기존 채점 결과 truncate (Rubric.selectDimensions 변경 → 신규 dimension 축 재해석 vs 폐기)
--    Rubric YAML mode key (on_playground_mode / on_interrogation_mode) 제거로 기존 score row 가 신규 dimension 축과 불일치
--    dev only → 폐기 선택
DELETE FROM question_score
 WHERE question_id IN (
   SELECT q.id FROM questions q
   JOIN question_set qs ON q.question_set_id = qs.id
   WHERE qs.category = 'RESUME_BASED'
 );

-- 3) resume_skeleton JSON 의 interrogation_priority_map key 정리 (역직렬화 호환 강화 목적)
--    @JsonIgnoreProperties(ignoreUnknown = true) 적용되어 있어 미실행 시에도 안전. 정리는 보강 차원
UPDATE resume_skeleton SET payload = JSON_REMOVE(payload, '$.interrogationPriorityMap')
 WHERE JSON_EXTRACT(payload, '$.interrogationPriorityMap') IS NOT NULL;
```

테이블 / 컬럼명 (`question_score` / `question_set.category` / `resume_skeleton.payload`) = implement 단 dev DB grep 확인 후 정정.

## 의존
- 선행 Task: 없음 (스크립트 독립)
- 외부: dev DB MySQL 접근 권한 (운영자 수동 실행)

## 테스트 케이스
- [ ] dev DB 에 SQL 실행 → 레거시 row 0 확인 (`SELECT COUNT(*) FROM questions WHERE question_type IN ('RESUME_PLAYGROUND', 'RESUME_INTERROGATION')`)
- [ ] FK constraint 위배 없이 실행 (question_score 선 삭제 → questions 삭제 순서)
- [ ] `interrogationPriorityMap` JSON key 제거 후 record 역직렬화 정상

## 완료 기준
- [ ] SQL 파일 신규 작성 (DDL 0, DML 만)
- [ ] 테이블 / 컬럼명 dev DB grep 확인 정정 완료
- [ ] 실행 절차 RUNBOOK 또는 PR 본문 문서화
- [ ] code-reviewer-backend 실행

## 커밋 메시지

```
chore(BE): dev-cleanup-resume-legacy.sql 운영 스크립트 추가
```

## 비고

- Flyway 분리 = `backend/.claude/rules/conventions.md` §Flyway "DDL 전용, DML 금지" 준수
- prod 환경 N/A — dev only (product-spec Non-Goals "prod 마이그레이션 전략")
- 실행 시점 = P2 BE PR 머지 직전 (운영자 책임)
