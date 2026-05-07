# Handoff — 427-standard-track-classification-enum

> **수명**: 단명 (plan 종료 시 제거)
> **작성 시점**: 세션 종료 / PR1 머지 완료
> **다음 세션**: plan 폴더 진입 시 **이 파일 먼저 읽음**

---

## 현재 상태

- 진행: `implement-be.md` Phase 1 완료 (PR1 머지). Phase 1.5 대기 중.
- 브랜치: `develop` (HEAD = `1af8d59`, PR #437 squash merge)
- 관련 PR: #437 (merged) — `[BE] refactor: 꼬리질문 분기 기준을 분류 컬럼에서 질문 타입으로 전환`
- 빌드: 통과
- 테스트: 통과 — RubricScoringEventListenerIntegrationTest 4종 + RubricLoaderTest sub-type 4종 포함

---

## 다음 세션 시작점

- **다음 작업**: `implement-be.md` Phase 1.5 — 운영 SQL 백필 수동 실행 (dev → prod) + 검증
- 참조: `implement-be.md#phase-15-운영-sql-백필-수동-실행--검증`
- 첫 명령:
  ```bash
  # dev EC2 SSH 접속 후 DB backfill-V46-pre.sql 실행
  ssh -i ~/.ssh/key.pem ec2-user@54.180.188.135
  # 또는 dev DB 직접 query (RDS 콘솔)
  ```
- 예상 변경 파일: 없음 (코드 변경 X, 운영 SQL 수동 실행만)

---

## 미해결 질문 / Blocker

- `docs/domain/feedback/rubric-score-reflection.md`, `docs/domain/resume/api/process-user-turn.md` — local 수정 미커밋 잔존. Phase 1.5 작업 scope 외. 별도 커밋 처리 필요 여부 사용자 판단 필요.

---

## 컨텍스트 메모

- **Phase 1.5 = 비코드 작업** (운영 SQL 1회성). Flyway 아님. dev → prod 순서 강제. 각 환경마다 `검증 SQL #2 (remain = 0)` + `검증 SQL #3 (RESUME_BASED 잔여 = 0)` 확인 후 다음 환경 진입.
- **Phase 1.6 = atomic 단일 commit 강제**. Phase 1.5 잔여 0 확인 + 회귀 관찰 1-2일 후 진입. sentinel(`MAIN`/`FOLLOWUP`) + fallback 헬퍼 + `Question.resume()` 가드 단순화 = 한 commit 으로 묶음 (부분 적용 시 컴파일 깨짐).
- **Phase 2 = V46 DDL DROP COLUMN 3종 + Seed 18종 + R__ + LLM schema 정리**. Phase 1.6 머지 + 회귀 관찰 1-2일 + 운영 백업 후 진입. 단일 PR / 단일 commit 강제.
- **Phase 3-BE = FE PR3 선행 머지 후 진입**. `QuestionDetailResponse.referenceType` 필드 제거. 역순 머지 = TS strict 빌드 오류 위험.
- **`ResumeTrackPolicy.assertCanContinue` no-op override 보존 확정** — interface contract 명시 목적. interface default 이동 안 함. Phase 1 PR 에 포함됨.
- **P1-6 (Mockist → Service Integration 전환) 별도 follow-up 보류** — `FollowUpTransactionHandlerTest` Mockist 패턴 유지. PR1 미포함. 추후 별도 결정.
- **P2-1 / P2-2 미수정** (`DisplayName "Phase 1.6 제거 예정"` 표현 / import 순서) — Phase 1.6 머지 시 일괄 정리.
- **잘못 적재된 legacy row** (예: BEHAVIORAL category 인데 `reference_type=MODEL_ANSWER`) = 의도된 회귀 정정 대상. backfill SQL 핵심은 이 row 식별 + 정정.
- **Flyway DROP COLUMN (V46) = Phase 1 미포함 = 의도**. Phase 2 작업. Phase 1 PR 에서 미적용.

---

## 참고 명령

```bash
# dev EC2 접속
ssh -i ~/.ssh/key.pem ec2-user@54.180.188.135

# 컨테이너 내부 접속 (MySQL)
docker exec -it rehearse-backend bash

# Phase 1.5 검증 SQL (implement-be.md Phase 1.5 섹션 참조)
# SELECT COUNT(*) AS remain FROM question WHERE question_type IN ('MAIN', 'FOLLOWUP');
# 기대: 0

# Phase 1.6 진입 전 회귀 확인
cd /Users/koseonje/dev/devlens/backend
./gradlew test --tests "QuestionType*"
./gradlew test --tests "RubricScoringEventListenerIntegrationTest"

# 전체 빌드
./gradlew build
```

---

업데이트: 2026-05-07 (세션 종료 / PR1 머지 완료)
