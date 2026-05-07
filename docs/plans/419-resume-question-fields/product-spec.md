# Product Spec — RESUME 트랙 Question 4개 필드 적재 정상화

> **작성자**: 사용자 (PM 초안)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

RESUME 트랙 (`RESUME_OPENER`, `RESUME_PLAYGROUND`, `RESUME_INTERROGATION`, `RESUME_WRAP_UP`) 으로 생성된 Question 의 학습/운영 메타가 결손. 두 종류로 분류:

**(A) 사용자 직접 가치 메타 — DB 적재 결손**
- `tts_text` — 음성 출력용 본문
- `model_answer` — 피드백 페이지 모범답안

**(B) 도메인 고정 분류 메타 — 매핑 표면화 부재**
- `referenceType` (모범답안 / 가이드 구분)
- `feedbackPerspective` (TECHNICAL / BEHAVIORAL / EXPERIENCE)
- RESUME 트랙은 모드별 분류 결정성 있음. 그러나 코드에 매핑 부재 → 다운스트림 (rubric / 응답 DTO) 이 분류값 인지 불가.

- **현재 상태**:
  - (A) `ResumeQuestionPersister.persist():25-34` 시그니처에 ttsText / modelAnswer 인자 부재. `Question.resume()` 팩토리 (`Question.java:70-83`) 도 미설정. 호출처 4곳 (`PlaygroundModeHandler:53,98`, `InterrogationModeHandler:65`, `WrapUpModeHandler:51`) 이 prompt 결과의 `ttsQuestion` 만 받아두고 persist 인자에 전달 안 함. prompt result record 에 modelAnswer 필드 자체 부재.
  - (B) `QuestionType` enum 에 referenceType / feedbackPerspective 속성 부재. 매핑 단일 출처 없음.
  - STANDARD 트랙은 `QuestionSetAssembler` 경로로 4개 필드 정상 적재 (LLM 결정).
- **발생 증상**:
  - (A) RESUME 트랙 인터뷰 → DB `tts_text` / `model_answer` 컬럼 NULL. 상시 발생, 100% 재현.
  - (B) RESUME 트랙 모드별 분류값을 코드에서 조회 불가 → 응답 DTO / rubric 분기에서 분류 인지 불가.
- **사용자·운영 인지 채널**: DB 직접 조회 (`SELECT ... FROM question WHERE question_type LIKE 'RESUME_%'`). 피드백 페이지 모범답안 비표시. TTS 재생 누락.

## 왜 해야 하는가 (Why)

- **사용자 임팩트** (그룹 A):
  - 피드백 페이지에서 RESUME 트랙 질문에 모범답안 표시 불가 → 학습 가치 저하
  - TTS 클라이언트가 `tts_text` 사용 시 음성 출력 누락
- **운영 / 시스템 임팩트** (그룹 B):
  - 모드별 referenceType / feedbackPerspective 매핑이 코드 단일 출처 부재 → 다운스트림이 분류값 인지하려면 자체 분기 필요 (응집도 저하)
  - 향후 다운스트림 (예: 응답 DTO 노출, perspective 기반 분기) 활용 시 매번 재작성 위험
- **외부 압력**: 내부 발견 결함 (인접 issue #409 점검 중 발견).

## 해결 방향 (Approach)

PM 수준 high-level 방향. 구현 디테일은 tech-spec 영역.

- **핵심 접근**:
  - 4개 필드의 **결정 출처를 구분** — 일부는 LLM 결과 (사용자 맥락 의존), 일부는 도메인 규칙 (RESUME 트랙 정책 결정성)
  - 결정 출처에 따라 **저장 위치도 분리**:
    - LLM 결과 (사용자 맥락 의존) → 인스턴스별 다름 → DB 적재 (`tts_text`, `model_answer`)
    - 도메인 고정 매핑 (questionType 으로 환원 가능한 파생값) → 모든 인스턴스 동일 → 단일 출처로 표면화 (DB 적재 불필요)
  - LLM 결과 비어있을 때 폴백 정책 명시 (1회 재시도 후 정적 텍스트)
- **대안 비교**:
  - 4개 모두 LLM 결정: 유연성 ↑ but RESUME 트랙은 모드별 정책 결정성 있음 → 환각 / 회귀 비용 ↑. 채택 안 함
  - 4개 모두 도메인 고정: 모범답안 가치 제한적 (사용자 맥락 반영 X). 채택 안 함
  - 4개 모두 DB 적재 (LLM 2 + 도메인 2): 도메인 고정 항목 = questionType 으로 환원 가능한 파생값. 매 row 적재 = 정규화 위반 + 저장 비용. 채택 안 함
  - **혼합 + 정규화 (채택)**: modelAnswer / ttsText = LLM 결정 → DB 적재. referenceType / feedbackPerspective = 도메인 고정 → 단일 출처로 표면화. 다운스트림은 questionType 조회로 환원. 결정성 + 가치 균형 + 정규화 (구체 표면화 메커니즘은 tech-spec 영역)
- **단계 분리**: 단일 phase. RESUME 트랙 한정. 운영 백필 / DB 컬럼 정리는 비스코프.

## Evidence

- 코드 추적:
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeQuestionPersister.java:25-34` — persist 시그니처에 4개 필드 인자 부재
  - `backend/src/main/java/com/rehearse/api/domain/question/entity/Question.java:70-83` — `Question.resume()` 팩토리 4개 필드 미설정
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandler.java:53,98` — `result.ttsQuestion()` 보유 but persist 미전달
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java:65` — 동일
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/WrapUpModeHandler.java:51` — 동일
  - `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptBuilder.java:73-92` — result record 에 modelAnswer / referenceType / feedbackPerspective 부재
  - `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeChainInterrogatorPromptBuilder.java:51-69` — 동일
  - `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeWrapUpPromptBuilder.java:48-54` — 동일
  - `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricLoader.java:68-83` — `resumeTrack=true` 매핑이 perspective 보다 우선 → RESUME 트랙은 perspective NULL 이어도 rubric 라우팅 정상 (추정 — `RubricFamily#resolve` 내부 우선순위 코드 미확인. tech-spec 단계 검증 필요)
  - `backend/src/main/resources/rubric/_mapping.yaml` — `resumeTrack: true` 키가 perspective 매칭보다 상단 → 라우팅 무영향 (추정)
  - `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScoringEventListener.java:57` — perspective null 안전 처리 확인 → DB NULL 유지 안전
  - `backend/src/main/java/com/rehearse/api/domain/question/dto/QuestionDetailResponse.java:17-28` / `AnswerResponse.java:15-33` — modelAnswer / referenceType / feedbackPerspective 응답 노출. (Option X 적용 시 referenceType / feedbackPerspective 는 enum 속성에서 조회 가능 — 본 PR 비스코프)
- 운영 로그 / 메트릭: Issue body SQL 결과 — `id 142~144 RESUME_OPENER` 4개 컬럼 모두 빈값
- 인접 plan: #409 (question_score 미적재 — feedbackPerspective null 영향 의심, `docs/plans/409-question-score-missing/`), #412 (resume project 도메인, `docs/plans/412-resume-project-name/`) — 동일 RESUME 도메인 라인

## Goal

측정 가능한 결과.

- [ ] 신규 RESUME 트랙 Question 행 100%: `tts_text` 길이 ≥ 10자 + `question_text` 와 다른 문자열
- [ ] 신규 RESUME 트랙 Question 행 100%: `model_answer` not null / not blank (LLM 결과 또는 폴백 정적 텍스트)
- [ ] RESUME 트랙 4개 모드 (`RESUME_OPENER` / `RESUME_PLAYGROUND` / `RESUME_INTERROGATION` / `RESUME_WRAP_UP`) 각각에 대해 referenceType / feedbackPerspective 매핑이 코드 단일 출처에서 조회 가능 (다운스트림이 자체 분기 작성 불필요)
- [ ] STANDARD 트랙 4개 필드 적재 동작 회귀 0건 (DB 컬럼 동작 유지)

## Non-Goals

추구하지 않는 가치 (혼동 방지).

- **사용자 만족도 (UX 지표) 향상** — 본 작업의 가치는 데이터 정합성 + 매핑 단일 출처. 모범답안 표시로 인한 학습 효과 측정은 목표 아님 (부산물).
- **modelAnswer 콘텐츠 품질** — 본 작업은 적재 경로 정상화. LLM 응답 텍스트의 교육 효과 / 정확도 향상은 목표 아님.
- **다른 RESUME 결함 동시 해소** — 본 작업은 #419 한정. 다른 결함 해소는 별도 작업.

## 수용 기준 (Acceptance Criteria)

검증 가능 외부 관찰 결과.

- [ ] RESUME 트랙 4개 모드 (OPENER / PLAYGROUND / INTERROGATION / WRAP_UP) 각 1건 인터뷰 진행 → DB `question` 행 조회 → `tts_text` 길이 ≥ 10자 + `question_text` 와 다른 문자열
- [ ] 4개 모드 각 1건 → `model_answer` not null / not blank (LLM 응답 또는 폴백 텍스트)
- [ ] LLM 이 modelAnswer 비어있는 응답 반환 → 1회 재시도 → 여전히 비면 폴백 정적 텍스트 적재 (`model_answer` not blank 보장)
- [ ] RESUME 트랙 4개 모드 각각 단일 출처에서 (referenceType, feedbackPerspective) 조회 가능 + 정의된 값 반환 (도메인 정책 결정 — 모드별 매핑 자체는 tech-spec 확정)
- [ ] STANDARD 트랙 인터뷰 회귀: 기존 4개 필드 적재 동작 변경 0건
- [ ] 4개 모드별 회귀 테스트 통과 (테스트 카테고리 / Mock 전략은 tech-spec 결정)

## 비스코프 (Don't)

이번 PR 에 들어가지 않는 구체 작업.

- **운영 백필 SQL 작성·실행** — 별도 작업. Flyway DML 금지 룰 → 운영 SQL 분리.
- **RESUME 트랙 `reference_type` / `feedback_perspective` DB 컬럼 적재** — Option X 결정. 단일 출처 표면화로 대체. 컬럼 자체는 STANDARD 트랙 사용 중이라 유지.
- **응답 DTO 의 referenceType / feedbackPerspective 노출 경로 변경** — 현재 `QuestionDetailResponse` / `AnswerResponse` 가 entity 컬럼 직접 매핑. **본 PR 후 RESUME 트랙 응답에서 두 필드 NULL 유지 (현행 그대로)**. 다운스트림 클라이언트 영향 분석 / 노출 경로 변경 = 별도 plan.
- **#409 question_score 미적재 해소** — 별도 Issue / plan (`docs/plans/409-question-score-missing/`).
- **prompt 콘텐츠 튜닝** — 본 PR 은 schema 추가 + 적재 경로만. 텍스트 품질 개선은 별도.

## 참고

- 관련 Issue: #419
- 인접 Issue: #409 (question_score 미적재), #412 (resume project name)
- 관련 plan: docs/plans/412-resume-project-name/
- 도메인 코드 위치: `backend/src/main/java/com/rehearse/api/domain/resume/service/`, `backend/src/main/java/com/rehearse/api/infra/ai/prompt/Resume*PromptBuilder.java`
