# Product Spec — STANDARD 트랙 분류 메타 enum 단일 출처화 + 컬럼 정규화

> **작성자**: 사용자 (PM 페르소나 초안 — `/create-product-spec` 스킬)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성
> **관련 Issue**: #427 (Epic, P2)

---

## 문제 상황 (Problem)

- **현재 상태 (정상 동작 / 기존 흐름)**:
  - RESUME 트랙 (`RESUME_OPENER` / `RESUME_PLAYGROUND` / `RESUME_INTERROGATION` / `RESUME_WRAP_UP`) 4종은 `QuestionType` enum 안에 (referenceType, feedbackPerspective) 가 고정 매핑되어 단일 출처 (#419 PR #425).
  - STANDARD 트랙 (CS / Behavioral / SystemDesign / 기타 InterviewType) 신규 Question 은 `QuestionType.MAIN` 또는 `FOLLOWUP` sentinel 로 적재. 두 enum 값은 (null, null) 매핑이라 referenceType / feedbackPerspective 결정에 기여하지 않음.
- **발생 증상 (재현 절차 / 빈도 / 영향 범위)**:
  - STANDARD 트랙에서 referenceType 은 LLM 응답 (`question-generation.txt` 의 `reference_type` 필드 — `MODEL_ANSWER` / `GUIDE`) 으로 결정 → row 마다 column 적재.
  - feedbackPerspective 는 도메인 코드 (`QuestionSetAssembler.perspectiveOf`) 가 `QuestionSetCategory` 기반 계산 → row 마다 column 적재.
  - 두 출처 (LLM 결과 ↔ 도메인 정책) 가 같은 questionType 에서 충돌하거나 LLM hallucination 으로 잘못된 reference_type 이 적재될 위험 상시.
  - FE `ReferenceType` 타입 (`'RESUME'|'CS'|'TECH'|'BEHAVIORAL'|'SYSTEM_DESIGN'`, 5종 카테고리 의미) 과 BE enum (`MODEL_ANSWER`, `GUIDE`, 2종 답변 모드 의미) 이 **이름 동일·의미 충돌** — 선존재 결함. FE 사용처는 `use-answer-flow.ts:378` 의 `referenceType: 'CS'` 하드코딩 1건뿐.
  - `ResumeTrackPolicy.java:28-30` 의 `QuestionType.FOLLOWUP` 카운트 가드는 RESUME 트랙이 FOLLOWUP enum 을 사용하지 않아 항상 0 → HARD_TURN_CAP=7 가드 사실상 무력. RESUME 종료 제어는 ChainStateTracker / ResumeModeTransitionPolicy 가 별도 보장하므로 실해는 없으나 enum 재구성 시 표면화되는 dead code. 본 Epic enum 정리와 함께 일관 처리.
- **사용자·운영 인지 채널**:
  - 운영 데이터 로그 / 코드 리뷰 (#419 RESUME 트랙 enum 매핑 도입 시 STANDARD 트랙 미정합 잔여 식별).
  - hallucination 발현 시 면접 점수 / 피드백 산출 단계에서 부정확한 perspective 적용 위험 (현재 정량 측정 데이터는 부재 — Goal 의 정합성 보장이 우선).

## 왜 해야 하는가 (Why)

- **사용자 임팩트**: LLM hallucination 으로 잘못된 referenceType 적재 시 `model_answer` / 채점 문맥이 잘못 분기 → 사용자가 받는 피드백 신뢰도 하락. 트랙 간 정합 깨짐은 차후 분석 / 통계 / 추가 메타 도입 시마다 분기 비용 증가.
- **운영 / 시스템 임팩트**: DB 정규화 위반 (questionType 으로 환원 가능한 파생값을 row 마다 저장) — schema 무결성 약화 + LLM 응답 schema 변경 시 회귀 표면 확대.
- **외부 압력**: 인접 PR #425 (#419) 가 RESUME 트랙 단일 출처화를 완료. STANDARD 트랙 미정합은 동일 도메인 안의 일관성 차이 — 후속 작업 (예: 분류 메타 추가 / question 스키마 변경) 마다 두 출처 동시 갱신 부담 누적.

## 해결 방향 (Approach)

PM 수준 high-level 방향. 구현 디테일은 `tech-spec.md` 영역.

- **핵심 접근**: `QuestionType` enum 분할 → STANDARD 트랙도 enum 만으로 (referenceType, feedbackPerspective) 결정. Question entity 의 두 컬럼 DROP. LLM 응답 schema 에서 분류 메타 출력 제거. 동일 원칙으로 `question_pool` 의 `reference_type` 컬럼 + 시드 18개 SQL 도 일괄 정리 (pool 의 referenceType 은 category(InterviewType) 종속으로 환원 가능 → 컬럼 보존 이유 없음).
- **부가 정리**: `ResumeTrackPolicy` 의 dead `FOLLOWUP` 카운트 가드 정리 (구체 방향 — 클래스 채 제거 / RESUME_INTERROGATION 카운트로 교체 — 은 tech-spec 단계 결정). 사유: enum MAIN/FOLLOWUP 제거 시 컴파일 깨짐 + RESUME 종료 제어는 ChainStateTracker / ResumeModeTransitionPolicy 가 이미 보장.
- **단계 분리** (사용자 결정 채택 — phase 분리):
  - **Phase 1 (BE)**: 신규 enum sub-type 도입 + 기존 row 백필 + 코드 path 정합 (DROP 보류, 컬럼 유지).
  - **Phase 2 (BE)**: column DROP + LLM 응답 schema 에서 분류 메타 출력 제거.
  - **Phase 3 (FE / Lambda)**: Phase 1 머지 직후 병렬 진행 — FE `ReferenceType` 타입 / 하드코딩 정리, Lambda payload 출처 정합 검증.
- **enum 분할 granularity** (사용자 결정 — TECH/BEHAVIORAL 2계열):
  - 신규: `TECH_MAIN` / `TECH_FOLLOWUP` / `BEHAVIORAL_MAIN` / `BEHAVIORAL_FOLLOWUP` (RESUME_* 4종 유지). perspective(TECHNICAL/BEHAVIORAL) 기준 최소 분할.
  - SYSTEM_DESIGN / CLOUD / DATA_PIPELINE 등 InterviewType 세분은 enum 으로 표현하지 않음 — 분류 그라울러리티가 perspective 1차원으로 충분.
- **FE ReferenceType 처리** (사용자 결정 — type 제거):
  - FE `ReferenceType` 타입 + `QuestionDetail.referenceType` 필드 + 단일 하드코딩 사용처 동시 제거. BE enum 과 의미 충돌 근원 해소.
- **대안 비교**:
  - `MAIN/FOLLOWUP` sentinel 유지 + 컬럼 유지: 정합 깨짐 방치 — 채택 X.
  - InterviewType 세분 (12종 × 2 = 24종): 백필 / enum 폭증 비용 — 채택 X.
  - FE 용어 변경 (CategoryType 등): 컴포넌트 사용처 적어 저비용이나 손익비 작음 — 채택 X.

## Evidence

- 코드 추적:
  - `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionType.java:6-7` — `MAIN(null,null)` / `FOLLOWUP(null,null)` sentinel.
  - `backend/src/main/java/com/rehearse/api/domain/question/entity/Question.java:38-44` — `reference_type` / `feedback_perspective` 컬럼 적재.
  - `backend/src/main/java/com/rehearse/api/domain/question/service/QuestionSetAssembler.java:42-51` — assemble 시 questionType=`MAIN` 고정 + perspective 도메인 계산 + referenceType LLM 결과 그대로 전달 (이중 출처 핵심).
  - `backend/src/main/resources/prompts/template/question-generation.txt:37-39, 73` — LLM 이 `reference_type=MODEL_ANSWER|GUIDE` 출력하도록 schema 명시.
  - `backend/src/main/resources/db/migration/V4__add_question_set_and_file_metadata.sql` — `reference_type` 최초 컬럼 추가.
  - `backend/src/main/resources/db/migration/V16__feedback_redesign.sql` — `feedback_perspective` 컬럼 추가.
  - `frontend/src/types/interview.ts:57, 71` — FE `ReferenceType` 5종 카테고리 + `QuestionDetail.referenceType` 필드.
  - `frontend/src/hooks/use-answer-flow.ts:378` — `referenceType: 'CS'` 하드코딩 (단일 사용처).
  - `lambda/analysis/handler.py:205-218, 233-246` — `feedbackPerspective` payload 의존 (default `'TECHNICAL'`).
- 인접 plan / PR:
  - PR #425 (#419 — RESUME 트랙 4 필드 적재 정상화) — 본 Epic 의 RESUME 부분 선행.
  - 별도 Issue 없음 (이번 Epic 으로 STANDARD 트랙 단일 출처화 마무리).
- 추정 / 미확인 가정:
  - `question_pool` 테이블 (별도 시드 캐시 — `cs-fundamental-junior.sql` 등 18개) 의 `reference_type` 컬럼 처리는 본 Epic 범위 외 가능. tech-spec 단계에서 별도 결정 필요 (비스코프 / 후속 Issue 분기).
  - 운영 기존 `MAIN`/`FOLLOWUP` row 백필은 `question_set.category` (= `InterviewType.name()`) 기준이면 정확도 100% 매핑 가능 — 검증 필요 (tech-spec 단계).

## Goal

측정 가능한 결과.

- [ ] **G-1** STANDARD 트랙 신규 Question 의 referenceType / feedbackPerspective 가 `QuestionType` enum 1곳에서만 결정됨 (LLM 응답에서 분류 메타 출력 0건 — 응답 schema 검증).
- [ ] **G-2** question 테이블 `reference_type` / `feedback_perspective` 컬럼 부재 (information_schema 조회 부재 확인).
- [ ] **G-3** question_pool 테이블 `reference_type` 컬럼 부재 (시드 18개 SQL 도 컬럼 미사용 형태로 정리됨).
- [ ] **G-4** 운영 기존 row 100% 신규 enum sub-type 으로 매핑 (백필 후 잔여 `MAIN`/`FOLLOWUP` 0건).
- [ ] **G-5** `ResumeTrackPolicy` FOLLOWUP dead code 정리 — RESUME 트랙 follow-up cap 의미가 명확 (제거 또는 RESUME_INTERROGATION 환원).
- [ ] **G-6** BE / FE / Lambda 3 영역 회귀 0건 — 인터뷰 생성 → 답변 → 점수 산출 → 피드백 표시 흐름 통과.

## Non-Goals

이 작업이 **목표로 삼지 않는** 것 (혼동 방지).

- LLM 분류 정확도 향상 — 사유: 분류 자체 제거가 목표.
- FE UX / 화면 변경 — 사유: 정합화만, 화면 결과 동일.
- prompt 콘텐츠 (질문 톤 / 가이드 표현) 튜닝 — 사유: 응답 schema 만 변경.

## 수용 기준 (Acceptance Criteria)

외부에서 관찰 가능한 결과.

- [ ] **AC-1** 신규 인터뷰 (CS / Behavioral / SystemDesign / Resume 각 1건 이상) 생성 → 답변 → 꼬리질문 → 피드백 점수 정상 산출 (BE service integration 자동 + dev E2E 수동 통과).
- [ ] **AC-2** question 테이블 schema 에서 `reference_type` / `feedback_perspective` 컬럼이 제거되어 있음 (Phase 2 완료 조건 — information_schema 조회).
- [ ] **AC-3** question_pool 테이블 schema 에서 `reference_type` 컬럼이 제거되어 있고 시드 18개 SQL 이 컬럼 미사용 형태로 갱신됨 (Phase 2 완료 조건).
- [ ] **AC-4** LLM 응답에서 분류 메타 (reference_type) 가 더 이상 출력되지 않음 — 신규 questionType sub-type 만으로 도메인 분기 정상 동작 (Phase 2 완료 조건 — schema 검증 + Mock infra 테스트).
- [ ] **AC-5** 운영 기존 row 잔여 `MAIN` / `FOLLOWUP` enum 값 0건 (Phase 1 백필 완료 조건 — 검증 SQL `SELECT COUNT(*) ... 0` 확인).
- [ ] **AC-6** FE 답변 화면 / 꼬리질문 적재 / 피드백 화면이 변경 전과 동일하게 동작하며 FE 코드에 BE 와 의미 충돌하던 ReferenceType 정의가 잔존하지 않음.
- [ ] **AC-7** Lambda 분석 결과의 feedbackPerspective 출처 변경 후 동등 — 분석 산출물 (점수 / 코멘트) 회귀 0건.
- [ ] **AC-8** RESUME 트랙 follow-up 흐름 (PLAYGROUND → INTERROGATION → WRAP_UP) 회귀 0건 — `ResumeTrackPolicy` 정리 후에도 chain L1~L4 추궁 + chain switch + exhausted 종료가 변경 전과 동등.

## 비스코프 (Don't)

이번에 의도적으로 안 하는 것. 향후 별도 plan 또는 후속 Issue.

- 운영 통계 손실 / 분류 로그 보존 분석 — 사유: Issue 본문 명시. 별도 분석 Issue 로 분기.
- DTO 응답 shape 광범위 재설계 — 사유: 값 출처만 enum 환원, public API 시그니처 변경 최소화.
- LLM prompt 콘텐츠 튜닝 — 사유: schema 만 변경.
- (제거됨 — 본 Epic 포함으로 변경) ~~`question_pool` 시드 캐시 테이블 (18개 SQL) 의 `reference_type` 컬럼 처리~~ → Goal / AC 에 반영. 사유: pool 의 referenceType 도 category (InterviewType) 종속으로 환원 가능 — question entity 와 동일 정규화 원칙 일괄 적용으로 일관성 ↑.
- InterviewType 세분 enum (24종) — 사유: perspective 1차원 분류로 현재 분기 충분, YAGNI.

## 참고

- 관련 Issue: #427 (Epic), #419 (RESUME 트랙 단일 출처화 — 선행 PR #425 머지 완료)
- 관련 plan: `docs/plans/423-intent-classifier-removal/` (인접 question / interview 도메인 작업, 본 Epic 과 직접 의존 X)
- 외부 자료: `docs/domain/resume/api/process-user-turn.md`
