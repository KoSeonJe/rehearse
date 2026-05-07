# Product Spec — Resume 트랙 WRAP_UP 모드 제거 (FSM 2단계 단순화)

> **작성자**: 사용자
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

- 현재 상태: Resume 트랙 FSM 3단계 (PLAYGROUND → INTERROGATION → WRAP_UP). WRAP_UP 단계에서 BE 가 잔여 시간 임계 도달 시 자동 진입 → 회고 질문 1회 LLM 생성 후 종료.
- 발생 증상: 회고 단계가 사용자 답변 흐름과 단절된 인위 종료 — 추가 가치 낮은 LLM 호출 1회 + 회고 전용 산출물 (FSM 분기 / 프롬프트 / 컨텍스트 빌더 / DB 제약 / 환경설정) 분산 유지.
- 인지 채널: Issue #424 운영 검토.

## 왜 해야 하는가 (Why)

- 사용자: 면접 흐름 자연스럽게 마무리 (인위 회고 질문 제거 → 사용자 행동 시점에 종료 정합).
- 운영: 회고 단계 LLM 호출 + 회고 전용 산출물 (코드 / 컨텍스트 빌더 / DB 제약 / 환경설정) 유지 비용 절감.
- 코드: FSM 단순화 → 분기 / 테스트 / 컨텍스트 적층 감소.

## 해결 방향 (Approach)

- FSM 2단계 (PLAYGROUND → INTERROGATION → 종료) 로 단순화.
- 종료 시점 판단을 FE 로 이관: FE 가 잔여 시간 모니터링 → 사용자 답변 처리 시점에 종료 의사 BE 에 전달 → BE 는 추가 질문 없이 종료 응답 반환.
- BE side backstop = 기존 hard timeout (duration + hard-timeout-min 초과 시 강제 종료) 유지.
- 회고 단계 관련 코드 산물 / 컨텍스트 빌더 / DB 제약 / 환경설정 항목 전면 제거.
- 기존 DB 의 RESUME_WRAP_UP question row = prod 부재 (dev 한정) 확인 — 운영 cleanup 불필요. application enum 차단으로 충분.

### 대안 비교

- (대안 A — Issue 본문 옵션 A) BE 가 잔여 시간 임계 도달 시 자동 종료. 단점: 회고 단계와 동일하게 사용자 행동 무관 인위 종료. 채택 X.
- (대안 B — Issue 본문 옵션 B) INTERROGATION 계속 + hard timeout 만 종료. 단점: duration 의미 상실 — 사용자가 예상 시간 초과 인지 시점이 없음. 채택 X.
- (채택) FE 시간 모니터링 + 답변 처리 시점 종료 신호. 사용자 행동과 종료 시점 정합 + BE 책임 단순화 (신호 수용 + backstop). hard timeout 은 신호 누락 대비 backstop.

## Evidence

- 코드 추적
  - `backend/src/main/java/com/rehearse/api/domain/resume/entity/ResumeMode.java:6` — enum 3단계
  - `ResumeModeTransitionPolicy.java:24-32` — 잔여 시간 기반 자동 전이
  - `ResumeInterviewOrchestrator.java:32, 92-98, 163-167` — WRAP_UP 분기 / hard timeout
  - `WrapUpModeHandler.java`, `ResumeWrapUpPromptBuilder.java`, `ResumeQuestionResultGenerator.java:103-120`
  - `FocusHints.java:50`, `FocusLayer.java:32,57,95`, `SkeletonCallType.java:71`
  - `QuestionType.java:9`, `V35/V41/V44` CHECK constraint, `application.yml:91`
  - `frontend/src/types/interview.ts:55` — RESUME_WRAP_UP 타입 유니언 잔존
- 정성 근거: Issue #424 운영 검토 결정 — "회고 단계 사용성 / 품질 기여 낮음".
- 정량 근거: (추정) 회고 단계 가치 정량 지표 미수집. Issue 운영 판단에 의존.

## Goal

- Resume 트랙 종료 경로에서 회고 단계 LLM 호출 0회 (outcome).
- 종료 시점이 사용자 답변 액션 또는 backstop hard timeout 과 일치 (outcome).
- BE/FE contract 변경 경계 (FE 종료 신호) 가 spec 단계에서 명시 — tech-spec 단계 BE/FE 동시 진행 가능 (outcome).
- 기존 DB 의 회고 단계 데이터 (RESUME_WRAP_UP) 가 신규 인터뷰 처리 / 조회 / 리포트에서 잔존 0 (outcome).

## Non-Goals

- INTERROGATION 단계 자체 품질 개선 (분기 / 꼬리질문 깊이 등) — 사유: 별도 plan (interview-quality 시리즈) 영역, 본 refactor 와 가치 축 다름.
- 종료 후 신규 UX (요약 / 점수 페이지 / 리포트 변경) — 사유: feedback 도메인 영역 / 본 작업의 가치는 FSM 단순화에 한정.

## 수용 기준 (Acceptance Criteria)

- [ ] Resume 인터뷰 정상 종료 / 신호 종료 / hard timeout 종료 모든 경로에서 회고 단계 질문이 사용자에게 노출되지 않음.
- [ ] FE 가 종료 의사를 BE 에 전달한 턴에서 BE 응답에 후속 질문 페이로드가 포함되지 않음 (응답 schema 관찰).
- [ ] FE 종료 신호 부재 + duration + hard-timeout-min 초과 상황에서 BE 가 hard timeout 종료 응답 반환 (backstop 검증).
- [ ] BE/FE 종료 신호 contract (요청 / 응답 페이로드 변경 사항) 가 tech-spec 에 명시됨 — BE/FE 동시 진행 게이트 충족.
- [ ] RESUME_WRAP_UP row prod 부재 확인 완료 — 운영 cleanup 불필요. application enum 차단으로 신규 INSERT 방지.
- [ ] 기존 DB 의 RESUME_WRAP_UP 데이터가 신규 인터뷰 처리 / 조회 / 리포트에서 관찰되지 않음.
- [ ] Resume 트랙 시작 → 다중 턴 → 종료 통합 시나리오 회귀 없이 통과.
- [ ] FSM 다이어그램 / 도메인 문서에 WRAP_UP 잔존 0.
- [ ] 기존 환경설정 키 `wrap-up-threshold-min` 미사용 (잔존 시 부팅 영향 없음 검증).

## 비스코프 (Don't)

- Standard 트랙 FSM — 사유: Resume 한정 refactor.
- BE 능동 종료 정책 (서버 시간 기반 자동 종료) 강화 — 사유: FE 신호 전환으로 대체.
- Feedback / 리포트 측 변경 — 사유: 기존 question row cleanup 으로 영향 차단.
- FE 시간 측정 정확도 보강 (clock drift / skew 대응) — 사유: 별도 plan (Issue 미생성 — 발견 시 신규 Issue).

## 참고

- 관련 Issue: #424
- 인접 plan:
  - `docs/plans/423-intent-classifier-removal/` (Resume 도메인 동시 진행 — 통합 회귀 점검 대상)
  - `docs/plans/421-resume-playground-opener-tone/`
  - `docs/plans/410-resume-context-defects/`
