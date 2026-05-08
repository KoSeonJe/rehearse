# Product Spec — RESUME 트랙 채점 perspective 별 라벨/영역 라우팅 정상화

> **작성자**: 사용자 (PM 초안 by Claude)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

RESUME 트랙 PLAYGROUND mode 채점 결과 (경험 구체성 평가) 가 사용자 피드백 페이지의 "기술 피드백" 라벨 영역에 노출된다. 라벨과 평가 perspective 가 일치하지 않아 사용자에게 "기술 평가" 로 오인된다.

- 현재 상태 (정상 동작 / 기존 흐름):
  - RESUME 트랙 채점은 turn 별 정상 진행. PLAYGROUND turn = `experience_concreteness` (EXPERIENCE perspective) / INTERROGATION turn = `technical_depth` 외 (TECHNICAL perspective) 가 DB 에 적재됨.
  - perspective 정보 = `QuestionType.feedbackPerspective()` enum 메서드로 단일 진실 원천 이전 (#447 V46 머지 후, `question_score.feedback_perspective` 컬럼 + entity 필드 모두 제거. AnswerResponse / RubricLoader / RubricFamily 3곳에서 호출 중).
  - "기술 피드백" 영역은 STANDARD 트랙 TECH_MAIN/FOLLOWUP turn 에서는 의도대로 동작.
- 발생 증상 (재현 절차 / 빈도 / 영향 범위):
  - dev 서버 interview id=26 (RESUME_BASED, 2026-05-07): OPENER 1건 + PLAYGROUND 5건 시나리오. PLAYGROUND turn (q148~152) 의 `experience_concreteness` 점수가 "기술 피드백" 라벨 영역에 그대로 노출.
  - RESUME 트랙 PLAYGROUND turn 보유 인터뷰 전반에 동일 패턴 (재현 일관 — 코드 분기 부재 기인).
  - INTERROGATION 도입 시 동일 인터뷰 안에서 PLAYGROUND turn (EXPERIENCE) 와 INTERROGATION turn (TECHNICAL) 채점 결과가 라벨 구분 없이 혼재.
- 사용자·운영 인지 채널:
  - 운영 dev 환경에서 직접 발견 (Issue #436 작성). 사용자 신고 채널 미확인 (추측).

## 왜 해야 하는가 (Why)

- 사용자 임팩트:
  - PLAYGROUND turn 의 경험 평가가 "기술 피드백" 라벨로 노출 → 사용자가 기술 평가로 오인 (운영 발견 기반 추정 — 사용자 신고 미확인).
  - INTERROGATION 도입 후 한 인터뷰 안에 두 perspective 평가가 라벨 구분 없이 섞임 → 결과 해석 불능.
- 운영 / 시스템 임팩트:
  - RESUME PLAYGROUND vs INTERROGATION 채점 결과 시각 구분 불가 → 운영 분석/QA 비용 상승.
  - 채점 perspective 데이터는 정상 적재되나 사용자 노출 단계에서 무시됨 — 채점 메타데이터 활용 누락.
- 외부 압력:
  - RESUME 트랙은 핵심 차별 가치 (영상 타임스탬프 ↔ AI 피드백 동기화) 진입 도메인. 본 결함은 라벨 정합성 직격.

## 해결 방향 (Approach)

PM 수준 high-level 방향. 구현 디테일 = tech-spec.

- 핵심 접근: 채점 결과 perspective 가 사용자 화면 라벨/영역에 그대로 반영되도록 응답 구조 + UI 노출 정상화.
- 대안 비교:
  - 응답 DTO 무변경 + FE 가 dimension 식별자로 추론 — 거부: 책임 도치, 채점 메타 단일 소스 위반.
  - PLAYGROUND 채점 자체 비활성화 — 거부: 채점 데이터 손실, 사용자 가치 저하.
  - perspective 별 별도 응답 필드 신설 (technical / experience 독립 분기) — 거부: 화면 단일 라벨 영역 재사용 가능, 신규 perspective 추가마다 응답 구조 확장 필요로 복잡도 ↑. 채택안 = 기존 단일 필드에 perspective 메타 추가 (구조 변경 최소).
- 단계 분리: 단일 phase. EXPERIENCE / TECHNICAL 두 perspective 한정. BEHAVIORAL 별도 plan.

## Evidence

- 코드 추적:
  - `backend/src/main/java/com/rehearse/api/domain/feedback/dto/TimestampFeedbackResponse.java:147-168` — `toTechnicalFeedback` perspective 분기 없음. 모든 dimension 을 `TechnicalFeedback` 으로 직조.
  - `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionType.java:6-27` — RESUME_OPENER/PLAYGROUND=EXPERIENCE, RESUME_INTERROGATION=TECHNICAL, TECH_MAIN/FOLLOWUP=TECHNICAL. `feedbackPerspective()` 메서드로 perspective 노출 (단일 진실 원천).
  - `backend/src/main/java/com/rehearse/api/domain/feedback/score/entity/QuestionScore.java` — `feedback_perspective` 컬럼 + entity 필드 제거 완료 (#447 V46). `rubricId`, `levelFlag` 만 보유.
  - `backend/src/main/resources/rubric/resume-rubric.yaml:26-28` — PLAYGROUND=`experience_concreteness` / INTERROGATION=`technical_depth, reasoning_communication, factual_consistency, chain_depth`.
  - `frontend/src/components/feedback/content-tab.tsx:13-29` — "기술 피드백" 라벨 고정. perspective 분기 없음.
- 운영 로그 / 메트릭:
  - dev EC2 interview id=26 (2026-05-07) PLAYGROUND turn 5건 모두 `experience_concreteness` 평가 → "기술 피드백" 라벨 노출 (사용자 첨부 화면).
- 사용자 발화 / 인접 plan:
  - Issue #436 본문 분석 메모.
  - 인접: docs/plans/433-resume-followup-questionid/ (#433, #445 머지 14af0a5) — 본 결함 가시화 선행.

## Goal

- [ ] RESUME 트랙 PLAYGROUND turn 의 채점 결과가 사용자 화면에서 "경험 평가" (또는 동등) 라벨 영역에 노출되며 "기술 피드백" 라벨 영역에는 노출되지 않는다.
- [ ] RESUME 트랙 INTERROGATION turn 의 채점 결과가 "기술 피드백" 라벨 영역에 노출된다.
- [ ] 한 인터뷰 안에 두 perspective turn 이 공존할 때 turn 별 자기 perspective 라벨/영역에 정확히 노출된다.
- [ ] 동일 회귀 (perspective 라벨 오노출) 가 머지 전 자동 검출된다.

## Non-Goals

- 채점 정확도 향상 — 본 작업은 perspective 라벨 정합성 우선. dimension 점수 산정 로직은 변경 없음.
- 운영 기존 NULL perspective row 가시화 — 신규 데이터 정상화 우선. 기존 row 는 안 보여도 수용.
- 사용자 채점 결과 시각 임팩트 강화 — 본 작업은 라벨 매칭 정상화 한정. 시각 강조는 별도 가치.

## 수용 기준 (Acceptance Criteria)

신규 RESUME 트랙 인터뷰 시나리오 기준. 운영 기존 NULL perspective row 는 영향 받지 않음 (Non-Goals).

- [ ] RESUME OPENER turn (채점 없음): 화면에 채점 영역 비어 있음 표시 (현행 유지, 회귀 없음).
- [ ] RESUME PLAYGROUND turn: "경험 평가" 라벨 영역에 `experience_concreteness` 점수·관찰·근거가 노출된다. 동일 turn 의 "기술 피드백" 영역에는 평가 표시되지 않는다.
- [ ] RESUME INTERROGATION turn: "기술 피드백" 라벨 영역에 `technical_depth, reasoning_communication, factual_consistency, chain_depth` 평가가 노출된다.
- [ ] PLAYGROUND + INTERROGATION 혼합 인터뷰: 각 turn 클릭 시 자기 perspective 의 라벨 영역에만 채점이 노출되고 다른 perspective 영역은 비어 있음 표시된다.
- [ ] STANDARD 트랙 TECH_MAIN / TECH_FOLLOWUP turn: 기존과 동일하게 "기술 피드백" 영역에 채점 노출 (회귀 없음).
- [ ] RESUME PLAYGROUND turn 응답이 EXPERIENCE perspective 로 라우팅되어 "경험 평가" 영역에 노출되는 통합 테스트 1건 + STANDARD TECH_MAIN turn 이 TECHNICAL perspective 로 "기술 피드백" 영역에 노출되는 회귀 테스트 1건 존재 (테스트 형태 = tech-spec 결정).

## 비스코프 (Don't)

- 운영 기존 NULL perspective row 백필 — 사용자 결정으로 미수행. 신규 인터뷰부터 정상화.
- BEHAVIORAL perspective 라벨 분기 — 현재 BEHAVIORAL_MAIN/FOLLOWUP 평가 활성 여부 / 사용 빈도 미확인. 본 작업 EXPERIENCE / TECHNICAL 한정. 별도 plan.
- 기술 피드백 영역 디자인 개편 — 라벨/영역 분기 한정.
- 모듈 구조 리팩토링 — 별도 부채.

## 참고

- 관련 Issue: #436
- 관련 Issue: #433 (#445 머지 14af0a5 — 본 결함 가시화 선행)
- 관련 plan: docs/plans/433-resume-followup-questionid/
- 외부 자료 / 디자인: 없음 (영역 분리 형태 자율 결정)
