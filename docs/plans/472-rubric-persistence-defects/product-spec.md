# Product Spec — rubric 채점 결과 적재 결함 정합화 (resume-v1 / nonverbal-v1 / OPENER UX / verbal 명확화)

> **작성자**: PM 페르소나 초안
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

dev DB 조사 (interview 23~27, question_score 8건) 결과 rubric 채점 결과가 의도된 대로 적재되지 않고 있음. 사용자 화면에는 피드백이 누락되거나 잘못된 라벨로 노출됨.

- 현재 상태:
  - `resume-rubric.yaml` 은 INTERROGATION 단계에서 4차원 (`technical_depth`, `reasoning_communication`, `factual_consistency`, `chain_depth`) 채점을 정의.
  - `NonverbalScorePersister` 는 `QuestionSetFeedbackPersister.persist()` 마지막에 호출됨 (호출 흐름 자체는 존재).
  - FE `content-tab.tsx` 는 rubric 결과가 비어 있을 때 "해당 턴은 평가 대상이 아닙니다" 메시지를 노출.
- 발생 증상:
  - INTERROGATION turn 의 question_score 8건 전부 `experience_concreteness` 1차원만 적재됨 — PLAYGROUND 모드 룰로 채점된 결과. 4차원 row 0건.
  - `rubric_id='nonverbal-v1'` row 0건. timestamp_feedback (Lambda raw) 만 정상 적재.
  - RESUME_OPENER qid 8건 모두 question_score null. FE 화면에는 "평가 대상 아님" fallback이 노출되어 사용자가 "기술 피드백이 막혀 있다" 로 오해 가능.
  - verbal 영역은 rubric / scorer 정의 자체가 코드베이스에 부재. Lambda 비언어 분석과 일관 정합이 잡히지 않음.
- 사용자·운영 인지 채널: dev DB 직접 조회 (2026-05-10 단서). 사용자 화면 검증은 PLAYGROUND turn 에서만 정상, INTERROGATION turn 미검증.

## 왜 해야 하는가 (Why)

- 사용자 임팩트: rubric 점수 기반 피드백이 인터뷰 핵심 가치. INTERROGATION 단계 4차원 누락 = 심문 모드 코칭이 1차원으로 축소되어 사용자에게 의미 있는 피드백 전달 불가. OPENER 화면 fallback 문구는 "기술 피드백이 막혀 있다" 로 오해되어 제품 신뢰 저하.
- 운영 / 시스템 임팩트: 비언어 row 0건 = 비언어 분석 체인의 적재 단계가 사실상 작동 안 함. 현재 상태로는 어느 분기에서 누락되는지 운영자가 식별 불가 (silent skip). 신규 인터뷰 누적될수록 데이터 자산 손실 누적.
- 외부 압력: P1 (이번 주). rubric 결과는 후속 통계·회차 비교·강약점 분석 모든 후속 작업의 입력이라 미해결 시 piling up.

## 해결 방향 (Approach)

PM 수준 high-level. 구현 디테일 = `tech-spec.md`.

- 핵심 접근: **결함 위치 확정 → 적재 누락 경로 보강 → 화면 메시지 정합 → verbal 영역 결정**. Phase 4단.
  - Phase 1 (BE): INTERROGATION 모드 채점이 4차원으로 적재되도록 모드 전이와 채점 시점의 정합을 회복한다.
  - Phase 2 (BE): 비언어 채점 결과가 question_score 에 적재되지 않는 원인을 진단한 뒤 결함 분기만 수정한다. 결함성 skip 과 정상 skip 을 운영자가 구분 가능하도록 가시성을 보강한다.
  - Phase 3 (BE+FE): OPENER turn 이 의도된 대로 채점 대상에서 제외된다는 사실을 도메인 결정으로 명시하고, 사용자에게 노출되는 메시지를 "기술 피드백 대상 아님" → "이 단계는 채점 대상이 아님" 같이 도메인 정확한 표현으로 교정한다.
  - Phase 4 (BE 결정): verbal rubric 적용 여부·범위·Lambda 비언어 분석과의 정합 정책을 결정 문서로 확정한다. 적용 결정 시 적재 동작도 본 Epic 안에서 검증한다.
- 대안 비교:
  - "Phase 4 별도 Issue 분리" — 미채택. 사용자 결정으로 본 Epic 포함. 다만 신규 dimension 정의 자체는 본 Epic 비스코프 (Phase 4 는 결정·명확화 한정).
  - "Phase 1+2 만 본 Epic, Phase 3 + 4 별도" — 미채택. OPENER UX 결함은 채점 결정과 1:1 묶임이라 같은 결정 단위.
- 단계 분리: 위 4 phase 는 PR 분리 가능. Phase 1·2 = BE 단독, Phase 3 = BE 도메인 결정 + FE 1줄 수정, Phase 4 = 결정 문서 + (적용 시) BE 적재. 본 spec 의 Phase 1~4 = Issue #472 본문 "후속 Phase 분리" 1~4 와 1:1 매핑.

## Evidence

- 코드 추적:
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java:94-114` — runtime mode (`currentMode`) 가 핸들러 진입 전에 캡처되고, 모드 전환이 핸들러 내부에서 일어나도 publish 시점은 캡처된 currentMode 그대로. 결과: PLAYGROUND→INTERROGATION 전환 turn 의 채점이 PLAYGROUND 룰로 적재됨.
  - `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalScorePersister.java:42-75` — `nonverbalRubricScorer.score()` 결과가 `hasAnyScore()=false` 면 silent skip. 호출은 존재 (`QuestionSetFeedbackPersister.java:31`).
  - `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalRubricScorer.java:21-36` — `score == null` 이면 `NonverbalTurnScore.empty()` 반환. payload 단계 누락 가능성 (추정).
  - `backend/src/main/resources/rubric/resume-rubric.yaml:26-28` — INTERROGATION 4차원 정의.
  - `frontend/src/components/feedback/content-tab.tsx:17-20` — `FALLBACK_COPY` 문구 "해당 턴은 평가 대상이 아닙니다".
- DB (dev, 2026-05-10):
  - question_score 8건 / 모두 `resume-v1` / 모두 `experience_concreteness`.
  - `nonverbal-v1` row 0건.
  - `RESUME_OPENER` qid 8건 모두 question_score null.
- 인접 plan / Issue:
  - `docs/plans/409-question-score-missing/` — 일반 트랙 적재 누락 결함. 본 Epic 과는 도메인 다름 (resume-v1 / nonverbal-v1 / RESUME 트랙 한정).
  - Issue #472 본문 = PM 분석 단계까지 정리된 상태.

## Goal

- [ ] Goal-1: dev 환경에서 신규 RESUME 인터뷰 1회 실행 시 INTERROGATION turn 마다 `question_score` 1행 + `question_score_dimension` 4행 (`technical_depth` / `reasoning_communication` / `factual_consistency` / `chain_depth`) 이 적재되어 DB 직접 쿼리로 확인 가능.
- [ ] Goal-2: dev 환경에서 신규 RESUME 인터뷰 1회 실행 시 비언어 분석 결과가 도착한 turn 마다 `rubric_id='nonverbal-v1'` row 가 적재되어 DB 직접 쿼리로 확인 가능. 비언어 payload 가 비어 도착해 정상 skip 된 경우와 결함성 skip 을 운영자가 인터뷰 ID 로 1회 로그 조회 시 분류 (정상 / 결함성) 단위로 구분 가능.
- [ ] Goal-3: RESUME_OPENER turn 의 화면 메시지가 "채점 대상이 아님" 의미로 사용자에게 정확히 전달되어 "피드백이 막혀 있다" 오해를 유발하지 않음.
- [ ] Goal-4: verbal rubric 적용 정책 (적용 / 미적용 / 범위 / Lambda 비언어와의 분리 또는 통합 방식) 이 결정되어 plan 폴더 안에 결정 문서로 추적 가능.

## Non-Goals

- 항목 A — 사유: rubric 정의 자체 (차원 추가·가중치 변경·level_expectations 수정) 는 추구하지 않는다. 본 Epic 은 "정의된 rubric 이 의도대로 적재" 까지가 가치.
- 항목 B — 사유: latency 단축은 목표 아님. 적재 정합화 과정에서 latency 증가가 작아도 정합 우선.
- 항목 C — 사유: 점수 결과 노출 UI 개편 (점수 차트 / 회차 비교 / 강약점 시각화) 은 별도 가치. 본 Epic 은 OPENER fallback 문구 1점 외 UI 개편을 추구하지 않음.

## 수용 기준 (Acceptance Criteria)

- [ ] AC-1 (Phase 1): RESUME 인터뷰의 INTERROGATION 첫 turn 에서 `question_score` row 1건 + `question_score_dimension` 4행 (resume-rubric.yaml `on_interrogation_mode` 와 일치) 이 적재된다. PLAYGROUND→INTERROGATION 전환 turn 도 4차원 적재 대상.
- [ ] AC-2 (Phase 1): PLAYGROUND turn 은 기존대로 `experience_concreteness` 1차원만 적재된다 (회귀 보호).
- [ ] AC-3 (Phase 2): 비언어 분석 payload 가 도착한 turn 마다 `rubric_id='nonverbal-v1'` row 가 적재된다. 검증 절차 = 정상 payload 1건 + 빈 payload 1건 (정상 skip) + 결함성 skip 1건을 발생시킨 동일 인터뷰 로그를 인터뷰 ID 로 조회 시 세 케이스가 각각 다른 분류로 식별된다.
- [ ] AC-4 (Phase 3): RESUME_OPENER turn 의 화면 메시지가 "이 단계는 채점 대상이 아닙니다" 의미로 노출되어 채점 결과 부재가 의도된 도메인 결정임을 사용자가 인지할 수 있다 (구체 문구는 tech-spec 에서 결정).
- [ ] AC-5 (Phase 4): verbal rubric 정책 결정이 본 plan 폴더 안의 문서 (tech-spec 또는 별도 결정 노트) 로 남아 후속 세션에서 추적 가능. "적용" 결정 시 결정 범위에 해당하는 turn 에서 verbal 적재가 검증 가능.
- [ ] AC-6 (회귀 보호): 위 시나리오 5종 (정상 4차원 적재 / PLAYGROUND 1차원 적재 / 비언어 정상 적재 / 비언어 정상 skip / OPENER 채점 미수행) 이 자동화 회귀 테스트로 보호된다. 테스트 레이어 선택 = tech-spec 위임.

## 비스코프 (Don't)

- 항목 A — 사유: Production 데이터 backfill (과거 누락 question_score / nonverbal-v1 / OPENER row 보정) — 신규 인터뷰부터 적용. 운영 SQL 분리.
- 항목 B — 사유: rubric dimension 정의 변경 (resume-rubric.yaml 차원 추가 / 가중치 변경) — 본 Epic 은 적재 정합화. dimension 변경은 별도 spec.
- 항목 B-1 (Phase 4 부피 천장) — 사유: Phase 4 가 "verbal rubric 적용" 으로 결정되더라도 신규 dimension 정의 / Lambda 비언어 분석과의 통합 재설계 / Lambda payload 스키마 변경은 본 Epic 비스코프. Phase 4 의 본 Epic 작업 한계 = 결정 문서 + (적용 시) 기존 인프라로 가능한 적재 동작 검증까지.
- 항목 C — 사유: LLM 출력 의미 자동 평가 (rubric 결과의 정확성 / 일관성 평가) — 본 Epic 과 직교.
- 항목 D — 사유: 점수 결과 사용자 노출 UI 개편 (차트 / 회차 비교 등) — 별도 FE plan.
- 항목 E — 사유: silent skip 분기 메트릭 카운터 / 운영 알람 임계 — Phase 2 가시성 보강은 로그 수준까지. 메트릭 namespace 확장은 별도 후속.
- 항목 F — 사유: 일반 (TECH 등) 트랙 적재 누락 — `docs/plans/409-question-score-missing/` 영역.

## 참고

- 관련 Issue: #472
- 관련 plan: `docs/plans/409-question-score-missing/` (일반 트랙 적재 누락)
- 코드 진입점:
  - `ResumeInterviewOrchestrator` (Resume 트랙 모드 전이 + publish)
  - `NonverbalScorePersister` / `NonverbalRubricScorer` (비언어 적재)
  - `QuestionSetFeedbackPersister` (피드백 저장 진입점)
  - `resume-rubric.yaml` (mode-aware 차원 정의)
  - `frontend/src/components/feedback/content-tab.tsx` (OPENER fallback)
