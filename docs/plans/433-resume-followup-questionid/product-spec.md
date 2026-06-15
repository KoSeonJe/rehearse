# Product Spec — RESUME 트랙 FollowUpResponse.questionId 누락 정상화

> **작성자**: 사용자 (PM 초안 by Claude)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

RESUME 트랙 인터뷰에서 OPENER 이후 모든 turn 의 답변이 OPENER 질문에 잘못 매핑되어, 피드백·모범답변·기술 피드백 페이지가 일괄 깨진다.

- 현재 상태 (정상 동작 / 기존 흐름):
  - 채점은 turn 별로 정상 진행됨 — turn 별 자기 질문에 점수가 결합되어 있다.
  - 클라이언트는 서버 응답에 turn 의 자기 질문 식별자가 있으면 그 값으로 turn 을 기록하고, 없으면 OPENER 질문 식별자로 폴백한다.
- 발생 증상 (재현 절차 / 빈도 / 영향 범위):
  - dev 서버 interview id=26 (RESUME_BASED, 2026-05-07): OPENER 1건 + PLAYGROUND 5건. 모든 turn 기록이 OPENER 한 건에 묶임.
  - 결과 3개 화면 동시 깨짐:
    1. 피드백 페이지 — OPENER 1건만 노출, PLAYGROUND/INTERROGATION 답변 사라짐.
    2. 모범답변 페이지 — OPENER 외 안 보임.
    3. 기술 피드백 영역 — "준비 중" 표시 (실제 채점은 정상).
  - 운영 RESUME 트랙 인터뷰 전반에 동일 패턴 가능성 — 미검증 (추측).
- 사용자·운영 인지 채널:
  - 운영 dev 환경에서 직접 발견. 사용자 신고 채널 미확인 (추측).

## 왜 해야 하는가 (Why)

- 사용자 임팩트:
  - PLAYGROUND/INTERROGATION 답변에 대한 피드백·모범답변·기술 피드백을 영영 못 봄. RESUME 트랙 핵심 가치(꼬리 질문 코칭) 실질 불능.
- 운영 / 시스템 임팩트:
  - turn 기록이 한 질문에 집중되어 분석·운영 통계 왜곡.
  - 채점은 정상 진행되어 사일런트 결함. 로그·예외로 식별 불가.
- 외부 압력:
  - RESUME 트랙은 핵심 차별 가치 (영상 타임스탬프 ↔ AI 피드백 동기화) 의 진입 도메인. 본 결함은 핵심 가치 직격.

## 해결 방향 (Approach)

PM 수준 high-level 방향. 구현 디테일 = tech-spec.

- 핵심 접근: RESUME 트랙 turn 답변이 자기 질문으로 정확히 매핑되도록 정상화 + 동일 결함이 사일런트로 재발하지 않도록 운영 관측 가능성 확보.
- 대안 비교:
  - 클라이언트 폴백 제거로 우회 — 거부: 책임 도치, 정합성 단일 소스 위반.
  - 운영 데이터 보정 우선 — 거부: 사용자 결정으로 미수정. 신규 인터뷰부터 정상화.
- 단계 분리: 단일 phase. WRAP_UP turn 동일 결함은 본 작업에서 다루지 않음 — Issue #424 (WRAP_UP 모드 제거, 2026-05-07 시점 OPEN) 가 WRAP_UP 처리 자체를 제거하므로 자동 해소.

## Evidence

- 코드 추적:
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandler.java:132` — turn 응답 빌더에 자기 질문 식별자 미주입.
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java:125` — 동일 누락.
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java:125` — 세션 시작 시 OPENER 재사용 응답에 식별자 미주입.
  - `backend/src/main/java/com/rehearse/api/domain/feedback/dto/QuestionSetFeedbackResponse.java:52` — 잘못 매핑된 turn 의 질문으로 점수 조회 → null → 기술 피드백 미노출.
  - `frontend/src/hooks/use-answer-flow.ts:294` — 서버 응답에 turn 의 자기 식별자가 없으면 OPENER 식별자로 폴백.
- 운영 로그 / 메트릭:
  - dev EC2 interview id=26 (2026-05-07) turn 기록 5건 모두 OPENER 질문에 적재됨. 채점 로그는 turn 별 정상.
  - dev id=26 외 인터뷰의 동일 패턴 여부 미확인 (추측).
- 사용자 발화 / 인접 plan:
  - Issue #433 본문 분석 메모.
  - 인접: Issue #424 (WRAP_UP 모드 제거, 2026-05-07 시점 OPEN, 작업 진행 중).

## Goal

- [ ] RESUME 트랙 신규 인터뷰 종료 후 OPENER 외 모든 PLAYGROUND/INTERROGATION turn 의 피드백·모범답변·기술 피드백이 사용자에게 노출된다.
- [ ] RESUME 트랙 신규 인터뷰의 turn 기록이 OPENER 단일 질문으로 집중되지 않고 각 turn 의 자기 질문으로 분산된다 (운영 데이터로 검증 가능).
- [ ] 동일 결함 (turn 답변이 OPENER 에 매핑) 이 머지 전 자동 검출되며, 운영에서 재발 시 운영자가 식별 가능하다.

## Non-Goals

- AI 답변 / 모범답변 품질 개선 — 본 작업은 매핑 정합성 단일 우선.
- 인터뷰 진행 latency 단축 — 정합성 우선, 성능 최적화는 본 작업 가치 아님.

## 수용 기준 (Acceptance Criteria)

신규 RESUME 트랙 인터뷰 시나리오 기준. 운영 기존 데이터는 영향 받지 않음 (Non-Goals 외 비스코프).

- [ ] OPENER 1건 + PLAYGROUND 5턴 시나리오: 인터뷰 종료 후 피드백 페이지에 정확히 6건 (중복·누락 없음) 노출, 각 turn 이 자기 답변과 매칭된다.
- [ ] OPENER 1건 + PLAYGROUND 3턴 + INTERROGATION 2턴 시나리오: 인터뷰 종료 후 피드백 페이지에 정확히 6건 노출, INTERROGATION turn 도 자기 답변과 매칭된다.
- [ ] 모든 turn 의 기술 피드백 영역에 turn 별 채점 결과가 노출된다 (점수 / 항목별 평가). "준비 중" 표시 발생하지 않는다.
- [ ] 모든 turn 의 모범답변 영역에 turn 별 모범답변이 노출된다.
- [ ] 세션 재진입(브라우저 새로고침 후 재접속) 으로 OPENER 재사용 시에도 OPENER turn 이 자기 답변과 매칭된다.
- [ ] turn 답변이 OPENER 로 잘못 매핑되는 회귀가 다시 발생하면 머지 전 자동 검출된다.
- [ ] 운영에서 동일 회귀가 재발할 경우 운영자가 로그·모니터링으로 즉시 식별할 수 있다 (사일런트 차단).

## 비스코프 (Don't)

- 운영 데이터 보정 (interview id≠26 등 기존 결함 인터뷰 재매핑) — 사용자 결정으로 미수정. 향후 별도 운영 SQL 가능.
- WRAP_UP turn 의 동일 매핑 결함 정상화 — Issue #424 (WRAP_UP 모드 제거) 가 처리 자체를 제거하므로 자동 해소. 본 작업에서 별도 fix 하지 않음. #424 머지 전까지 WRAP_UP turn 도달 시 결함 잔존 — 도달 조건 (인터뷰 hard timeout 또는 remainingMin ≤ wrap-up-threshold) 빈도 낮아 운영 임팩트 수용.
- 클라이언트 폴백 로직 변경 — 서버 응답 정상화로 폴백 발동 안 함. 클라이언트 변경 불필요.
- STANDARD 트랙 동일 점검 — 본 결함은 RESUME 트랙 한정. STANDARD 점검은 별도 plan.
- 모듈 구조 리팩토링 (#430 등) — 별도 부채.

## 참고

- 관련 Issue: #433
- 관련 Issue: #424 (WRAP_UP 모드 제거 — 의존, 2026-05-07 시점 OPEN)
- 관련 plan: docs/plans/410-resume-context-defects/, docs/plans/421-resume-playground-opener-tone/
- 외부 자료 / 디자인: 없음
