---
id: db-00c-C1-rubric-turn-fk-missing
severity: critical
category: database
plan: plan-00c-session-state-persistence
reviewer: database-optimization
raised_at: 2026-04-21
resolved_at: null
status: deferred
---

# V26 `rubric_score.turn_id` FK 누락

## 문제

`rubric_score` 는 `turn_id BIGINT NOT NULL` 컬럼을 갖지만 참조 테이블에 대한 FK 가 없다. 턴이 삭제될 때 고아 rubric_score 행이 남을 수 있고, 무결성 검증이 애플리케이션 레이어 책임으로 미뤄진다.

## 원인

plan-00c 구현 시점에 `interview_turn` (또는 동등) 테이블이 본 리포지토리에 **존재하지 않음** — 턴 테이블 설계는 `plan-02` (Answer Analyzer) 의 책임 범위이며 plan-00c 구현 시에는 의존 대상이 없어 FK 를 선언할 수 없었다.

확인 (2026-04-21):
```
grep -r "interview_turn\|CREATE TABLE.*turn" backend/src/main/resources/db/migration/
# → V26 자기 참조만 발견
find backend/src/main/java -name "*Turn*.java"
# → 없음
```

## 발생 상황

- **언제**: plan-02 구현 완료 후 `interview_turn` 테이블이 등장하면서 `rubric_score` 와의 무결성 계약 필요
- **누가**: plan-02 / plan-08 구현자, 향후 데이터 정합성 문제 발생 시 운영팀
- **파장**: 삭제된 턴에 대한 rubric_score 고아 행 → 피드백 조회 시 dangling reference, 분석 쿼리 왜곡

## 해결 방법

**Deferred** — plan-02 (Answer Analyzer) 구현 시 `interview_turn` 테이블 도입 PR 에서 함께 처리한다.

```sql
-- plan-02 PR 내 future migration (예: V29__*)
ALTER TABLE rubric_score
  ADD CONSTRAINT fk_rubric_score_turn
  FOREIGN KEY (turn_id) REFERENCES interview_turn(id) ON DELETE CASCADE;
```

**대안(기각)**: 지금 `interview_turn` 을 선제 설계. plan-02 에서 다시 정정될 가능성 + 소유권 위반 (plan-02 가 해당 도메인을 리드).

## 결과

- V26 수정 없음 (이번 PR 에서는)
- plan-02 spec 에 "`rubric_score.turn_id` FK 추가" 항목 추가 필요 (plan-02 착수 시점에 반영)
- 임시 보완: 애플리케이션 레이어에서 `RubricScoreService.save()` 시 `interviewRepository.existsTurn(turnId)` 체크 (plan-08 구현 시 함께)
