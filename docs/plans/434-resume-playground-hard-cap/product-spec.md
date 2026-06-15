# Product Spec — RESUME 트랙 PLAYGROUND→INTERROGATION 자동 전환 hard cap

> **작성자**: 사용자 (PM 분석 보조)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → backend agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

- 현재 상태 (정상 동작 / 기존 흐름):
  - RESUME 트랙은 PLAYGROUND (워밍업) → INTERROGATION (기술 심문) 2단계 FSM.
  - PLAYGROUND→INTERROGATION 전환은 워밍업 응답 LLM 이 자가 신호 (전환 여부 / 4개 보조 조건 중 2개 met) 를 보낼 때만 발생.
  - INTERROGATION 진입 후 비로소 RESUME_INTERROGATION 질문 + TECHNICAL perspective 채점 (technical_depth, factual_consistency, chain_depth 등) 수행.

- 발생 증상 (재현 절차 / 빈도 / 영향 범위):
  - dev 서버 interview id=26 (RESUME_BASED, 2026-05-07): opener + playground 5턴 연속 / INTERROGATION 진입 0회 / 사용자 강제 종료.
  - 약 4분간 워밍업 질문만 반복. 사용자 perception "playground 를 벗어날 수 없다".
  - LLM 이 전환 신호를 안 보내면 영원히 워밍업에 머무름 (상한 부재).

- 사용자·운영 인지 채널:
  - 운영자가 dev EC2 docker 로그에서 워밍업 단계 LLM 호출 5회 / 심문 단계 LLM 호출 0회 패턴 발견.
  - 사용자가 임의 종료 (status=COMPLETED) 한 trace 와 교차 확인.

## 왜 해야 하는가 (Why)

- 사용자 임팩트:
  - INTERROGATION 미진입 = RESUME 트랙의 핵심 가치 (꼬리질문 / 기술 심문) 미수행. 사용자가 "워밍업만 하다 끝났다" 인지.
  - 사용자가 cap 부재를 임의 종료로 우회 → 인터뷰 가치 손상.

- 운영 / 시스템 임팩트:
  - TECHNICAL perspective 채점 0건. 심문 단계용 rubric dimension 미적용 → 피드백 리포트가 워밍업 채점만 포함.
  - LLM 자가 전환 정밀도 변동 시 본 결함 빈도 예측 불가 (현재 모니터링 부재).

- 외부 압력:
  - dev 단계에서 이미 발생. 동일 패턴 prod 발생 시 사용자 이탈 위험.

## 해결 방향 (Approach)

- 핵심 접근: LLM 자가 전환에 의존하던 PLAYGROUND→INTERROGATION 전이에 운영 기준 hard cap (backstop) 도입. 워밍업 단계 누적 턴 수가 운영 임계값에 도달하면 LLM 응답 신호와 무관하게 강제로 심문 단계로 전환한다. 임계값은 운영자가 코드 변경 없이 조정 가능하도록 외부화하며, 이미 운영자 의도로 정의되었으나 실제 동작에 연결되지 않은 기존 설정을 정합성 있게 재활용한다.
- 대안 비교 (간략):
  - 누적 턴 수 단일 기준 (채택) — 단순 / 측정 명확 / 로그 추적 쉬움. 운영 데이터 부재 상태에서 가장 빠른 안전망 확보.
  - 누적 답변 길이 / 경과 시간 조합 — 운영 데이터 축적 부재 시 임계값 결정 부담. 1차 스코프 외.
- 단계 분리: phase 1 = 누적 턴 수 cap 만. phase 2 진입 트리거 = "신규 RESUME 세션 일정 기간 (예: 2주) 운영 데이터 축적 + 1차 cap 발동율 / 잔존 결함 측정 후 재검토" — 데이터 부재 시 phase 2 보류.

## Evidence

- 코드 추적:
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandler.java:100-114` — 워밍업 단계 LLM 자가 전환 신호만 평가. hard cap 부재.
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeModeTransitionPolicy.java:13-19` — 현재 backstop 은 인터뷰 전체 hard timeout 만. PLAYGROUND→INTERROGATION 전용 없음.
  - `backend/src/main/java/com/rehearse/api/domain/interview/entity/InterviewRuntimeState.java:34,49,84` — 워밍업 단계 누적 턴 수 / 누적 답변 길이를 이미 들고 있는 인터뷰 상태 객체 존재. 본 작업이 추가 자료구조를 도입할 필요 없음.
  - `backend/src/main/resources/application.yml:84` — 운영자 의도로 정의된 워밍업 상한 설정 항목이 존재하나 실제 코드에서 참조되지 않는 orphan 상태. 본 작업이 wiring 해소 겸함.
- 운영 로그 / 메트릭:
  - dev EC2, interview id=26 (2026-05-07): 워밍업 LLM 호출 5회 / 심문 LLM 호출 0회. 사용자 4분 후 종료.
  - 표본 1건. 빈도 측정 부재. 보수 가정 = LLM 자가 전환 정밀도에 의존하는 한 임의 빈도로 재현 가능.
- 사용자 발화 / 인접 plan:
  - Issue #434 본문 (재현 trace + 코드 인용 포함).
  - 인접 plan: `424-resume-wrap-up-removal` (wrap-up 단계 제거로 워밍업 비대 위험이 상대적으로 부각됨), `421-resume-playground-opener-tone`.

## Goal

- [ ] PLAYGROUND 누적 턴 수 (opener 발화 1턴 + 사용자 답변 응답 N턴 합산) 가 **3턴**에 도달하면 다음 사용자 답변 처리 시 INTERROGATION 으로 강제 전환된다.
- [ ] 신규 RESUME 트랙 세션 중 사용자가 3턴 이상 답변한 모집단의 INTERROGATION 진입율 100% (운영 로그 기준 워밍업 LLM 호출 / 심문 LLM 호출 / 인터뷰 status 교차 측정. 현재 dev 사례 = 0%).
- [ ] LLM 자가 전환 신호로 임계값 이전에 전이되는 기존 정상 케이스 회귀 0건.
- [ ] 임계값 (기본 3턴) 은 운영자가 코드 수정 없이 외부 설정으로 조정 가능하다.

## Non-Goals

- LLM 자가 전환 정밀도 / 워밍업 프롬프트 자체 개선 — 사유: 본 작업은 backstop 안전망 확보. 정밀도 / 프롬프트 품질은 별도 가치.
- 심문 단계 hard cap (chain depth / wrap-up) — 사유: 기존 정책이 별도로 존재. 본 작업의 목표는 워밍업 → 심문 진입 보장 단일.

## 수용 기준 (Acceptance Criteria)

- [ ] PLAYGROUND 누적 턴 수 (opener 포함) 가 3턴에 도달한 다음 사용자 답변 처리 시, LLM 응답 신호와 무관하게 INTERROGATION 모드로 전환되고 첫 INTERROGATION 질문이 사용자에게 노출된다.
- [ ] hard cap 발동 시 운영자가 로그로 발동 사유 (누적 턴 수 임계 도달) 와 인터뷰 식별자를 식별 가능하다.
- [ ] LLM 이 임계값 이전에 자가 전환 신호를 보낸 케이스에서는 기존 동작 그대로 유지된다 (회귀 없음).
- [ ] 운영자가 코드 변경 없이 외부 설정만으로 임계값 (기본 3) 을 조정할 수 있으며, 변경된 임계값이 다음 신규 세션부터 즉시 반영된다.
- [ ] 3턴 미만 + LLM 자가 전환 신호 부재 케이스에서는 워밍업이 그대로 유지되어, 강제 전환이 임계값 도달 전에 트리거되지 않는다.

## 비스코프 (Don't)

- 누적 답변 길이 / 경과 시간 기반 cap 추가 — 사유: 운영 데이터 부재. phase 2 후속 plan.
- 과거 dev / prod interview 데이터 백필 / 재처리 — 사유: 신규 세션부터 적용. 종료된 세션 보정 가치 낮음.
- FE 변경 (사용자 알림 UI 등) — 사유: 전환 자체는 기존 followUpExhausted / 다음 INTERROGATION 질문 노출 흐름으로 충분. 신규 UX 불필요.
- 워밍업 LLM 프롬프트 전환 조건 정밀화 — 사유: backstop 도입과 직교. 별도 Issue 권장.

## 참고

- 관련 Issue: #434
- 관련 plan:
  - `docs/plans/424-resume-wrap-up-removal/` (RESUME 트랙 단계 정리 — wrap-up 제거 후 워밍업 비대 위험 보강 맥락)
  - `docs/plans/421-resume-playground-opener-tone/` (워밍업 톤 개선)
- 외부 자료: 없음
