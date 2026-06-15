# Product Spec — question_score / question_score_dimension 데이터 미적재

> **작성자**: 사용자 (PM 페르소나 초안)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

- 현재 상태: 인터뷰 1턴 완료 시 (질문 → 답변 → follow-up / skip) `question_score` 1행 + `question_score_dimension` N행 (차원별) INSERT 가 기대 동작.
- 발생 증상: dev DB 조회 결과 두 테이블 모두 행 미생성. Resume 트랙 / 일반 인터뷰 (TECH 등) 양쪽 모두 발생 (Issue 보고).
- 사용자·운영 인지 채널: 운영 모니터링 단서 — score 기반 후속 피드백·분석 동작 불가. P0 라벨로 트리아지됨.

## 왜 해야 하는가 (Why)

- 사용자 임팩트: rubric coaching (점수 기반 피드백) 이 인터뷰 핵심 가치. score 미적재 = 점수 시각화 / 강약점 분석 / 회차 비교 등 후속 UX 전부 무효화.
- 운영 / 시스템 임팩트: 진단 인프라 부재 — silent skip 분기 ≥5개 (questionId null / questionSetId null / score.isEmpty / Question 미존재 / generic catch) 가 모두 `log.debug` 또는 `log.warn` 만 기록. 메트릭 카운터 부재 → 운영에서 어느 분기로 빠지는지 식별 불가.
- 외부 압력: P0 (즉시 / 장애·보안). 핵심 데이터 자산 누락 — 미해결 시 후속 piling up.

## 해결 방향 (Approach)

PM 수준 high-level. 구현 디테일 = `tech-spec.md`.

- 핵심 접근: **결함 위치 확정 → 적재 누락 경로 보강 → 회귀 방지 검증**. 단일 phase.
  - 의심 분기 다수 중 코드 추적으로 결함성 vs 정상 동작 분류. 결함분만 수정.
  - 운영 가시성 보강 — 정상 skip 과 결함성 skip 을 운영 모니터링에서 구분 가능하게.
  - 회귀 방지 테스트로 신규 턴 적재율 정상화 검증.
- 대안 비교:
  - "진단 메트릭 우선 phase" — 미채택. 코드 추적으로 결함 분류 충분. 메트릭은 별도 후속.
  - "의심 분기 일괄 수정 (분류 무관)" — 미채택. 변경 범위 부풀리고 회귀 표면 ↑. 정상 동작 분기 수정 = 회귀 위험.

## Evidence

- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScoringEventListener.java:43-83` — 5개 silent skip / return 분기. `score.isEmpty()` (CLARIFY 등) 정상 skip 과 결함성 skip 구분 불가.
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java:143-164` — `publishTurnCompletedEvent` 만 `generateAndSaveFollowUp` 성공 경로에서 호출. `analyzer_skip` / `step_b_skip` 경로 = event 미발행 → 턴 완료해도 score 적재 안 됨.
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeTurnEventPublisher.java:30-44` — RESUME_BASED 별도 publish. 호출 시점 검증 필요. `questionSetRepository.findByInterviewIdAndCategory(... RESUME_BASED)` orElse(null) → questionSetId null payload 가능.
- `backend/src/main/java/com/rehearse/api/domain/feedback/score/service/QuestionScorePersister.java:27-30` — `(questionId, rubricId)` idempotent skip. 동일 question 에 후속 턴 적재 차단 가능성.
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalScorePersister.java:30-33` — 비언어 트랙 별도 persist. 본 plan 비스코프.
- 진단 메트릭 (`AiCallMetrics`) 존재. `incrementRubricFailure("persist_failed")` 1개만 사용 — 분기별 세분화 부재.

## Goal

- [ ] **신규 턴 적재율 100%** — 결함분 fix 후 dev 환경 신규 인터뷰에서 적재 시도 N건 = `question_score` row N건 매칭. 측정: dev DB 직접 쿼리 (인터뷰 ID 별 row count) + Service Integration assert.
- [ ] **4 시나리오 회귀 보호 통과** — 일반 / RESUME_BASED / follow-up 미생성 턴 / 정상 skip (CLARIFY 등). 측정: Service Integration 100% 통과.
- [ ] **결함성 skip 0회 발생** — dev 신규 1턴 운영에서 정상 skip 만 허용, 결함성 skip 발생 시 운영 로그로 즉시 식별 가능. 측정: dev 1턴 검증 시 로그 grep.

## Non-Goals

- **rubric 출력 품질 / 정확도 향상** — 사유: 적재 자체 결함. rubric 응답 동일해도 row 들어가면 본 plan 목표 충족.
- **AI 응답 latency 단축** — 사유: async listener 경로. 사용자 체감 latency 무관.

## 수용 기준 (Acceptance Criteria)

- [ ] 일반 인터뷰 (TECH 카테고리, intent=ANSWER) 1턴 → `question_score` 1행 + dimension N행 적재.
- [ ] RESUME_BASED 트랙 1턴 → 동일.
- [ ] follow-up question 미생성 턴 (분석기 / writer 단계 skip) 도 score 적재됨.
- [ ] Resume 트랙에서 결함성 skip 발생 시 운영자가 어떤 분기로 빠졌는지 로그로 식별 가능 (정상 skip 과 구분 가능).
- [ ] 회귀 방지 테스트 — 정상 / CLARIFY 정상 skip / follow-up 미생성 턴 / Resume 결함성 skip 4 시나리오 통과.

## 비스코프 (Don't)

- **silent skip / return 분기 메트릭 카운터** — `AiCallMetrics` 확장 또는 신규 namespace. 별도 후속 작업. (사용자 결정)
- **운영 적재율 baseline / 알람 임계** — 메트릭 도입 후속.
- **비언어 score 적재 (`NonverbalScorePersister`)** — 별도 호출 경로. 미적재 시 별도 Issue.
- **점수 결과 사용자 노출 UI / 차트 / 비교** — FE 별도 plan.
- **rubric 정의 / dimension 추가 / 채점 기준 변경** — 적재 경로와 분리.
- **과거 미적재 row backfill** — 운영 SQL 분리.
- **`@Version` 낙관락 도입 / 동시성 제어** — `docs/plans/404-interview-domain-findings/` (#404 항목 #6) 영역.

## 참고

- 관련 Issue: #409
- 인접 plan: `docs/plans/404-interview-domain-findings/` (interview 도메인 보안 / 안정성 — listener 경로 일부 영향 가능)
- 코드 진입점:
  - `RubricScoringEventListener` (수신)
  - `FollowUpService.publishTurnCompletedEvent` (일반 트랙 발행)
  - `ResumeTurnEventPublisher` (Resume 트랙 발행)
  - `QuestionScorePersister.saveRubric` (적재)
- 메트릭: `AiCallMetrics` (분기 카운터 추가 대상)
