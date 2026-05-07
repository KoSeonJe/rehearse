# Tech Spec — Resume 트랙 WRAP_UP 모드 제거 (FSM 2단계 단순화)

> **작성자**: 구현 agent (backend / frontend)
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## ⚠️ 2026-05-07 Amendment — V46 / V47 폐기

본 spec 초기 버전은 V46 (chk_question_track_meta_v2 재정의 — RESUME_WRAP_UP 제외) + V47 (롤백용 — RESUME_WRAP_UP 허용 복원) 두 Flyway 마이그레이션을 동반하기로 설계됨. 그러나 **V42 (`drop_question_resume_meta`) 가 이미 `chk_question_track_meta_v2` constraint + `chain_id` / `chain_step_type` / `project_id` 컬럼 일괄 DROP** 한 사실 확인. 즉 RESUME_WRAP_UP 차단을 위한 row-pattern CHECK 자체가 런타임에 부재. 따라서:

- **V46 / V47 폐기** — 신규 Flyway 마이그레이션 0건. application enum 차단만으로 충분.
- **운영 SQL cleanup** — V46 prerequisite 가 아닌 단순 데이터 위생 작업으로 격하 (별도 ops PR / 본 PR 비스코프).
- **롤백** — V47 (constraint 복원) 경로 무효. 코드 revert 만으로 충분 (revert 시 V35/V41/V42 SQL 파일은 immutable past-V 룰에 따라 그대로 보존).
- **Repository 테스트 (V46 prerequisite 검증)** — 폐기. constraint 부재 → CHECK 검증 불가.

본 amendment 이후 본문의 "Flyway V46 / V47" / "Repository 테스트" / "운영 SQL = V46 prerequisite" 표현은 무효. 아래 본문은 amendment 반영 상태로 유지.

---

## Why → Goal (1줄 미러)

회고 단계 (WRAP_UP) 기여도 낮음 → FSM 2단계 + 회고 LLM 호출 0 + 종료 시점이 사용자 답변 액션과 일치 + 회고 단계 데이터 잔존 0.

## Evidence

- 현재 구조
  - 진입: `FollowUpService.delegateToResumeOrchestrator` (`backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java:201-214`)
  - FSM: `ResumeInterviewOrchestrator.processUserTurn` (`backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java:51-120, 150-169`) — `advanceToWrapUpIfDue` 호출 + dispatch + `turnEventPublisher.publish` (line 116-117)
  - 전이: `ResumeModeTransitionPolicy.advanceToWrapUpIfDue` (line 24-32) — `remainingMinutes ≤ wrapUpThresholdMin(2)` 시 WRAP_UP / `isHardTimeoutExceeded` (line 34-37)
  - 핸들러: `WrapUpModeHandler` — LLM 호출 + question INSERT (RESUME_WRAP_UP)
  - 산물: `ResumeWrapUpPromptBuilder`, `ResumeQuestionResultGenerator.generateWrapUp(line 103-120)`, `FocusHints.ResumeWrapUpHints(line 50)`, `FocusLayer.CAP_RESUME_WRAP_UP / buildResumeWrapUp(line 32, 95)`, `SkeletonCallType.RESUME_WRAP_UP(line 71)`, `ResumeFallbackQuestions.WRAP_UP`, `ResumeFallbackModelAnswers.WRAP_UP`
  - enum: `ResumeMode.WRAP_UP`, `QuestionType.RESUME_WRAP_UP`
  - DB: V35 `chk_question_track_meta` 도입 → V41 `chk_question_track_meta_v2` 강화 → **V42 에서 일괄 DROP** (constraint + `chain_id` / `chain_step_type` / `project_id` 컬럼). 따라서 본 plan 시점 런타임에는 RESUME_WRAP_UP 를 막는 DB 레벨 제약 없음. application enum 만 신뢰.
  - 설정: `application.yml:91` `wrap-up-threshold-min: 2`
  - DTO: `FollowUpRequest` (`backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpRequest.java`) — 종료 신호 필드 없음 / `FollowUpResponse` — `followUpExhausted` 보유
  - FE: `frontend/src/types/interview.ts:55` QuestionType 유니언 / `frontend/src/hooks/use-interview-session.ts:183` Resume 분기
- 컨벤션
  - Flyway DDL only / 과거 V 파일 immutable / DTO Request `@Getter @NoArgsConstructor` 패턴 / 트랜잭션 = App Service / 로깅 placeholder
  - testing.md: 외부 API 만 Mock / Service Integration / TRUNCATE / Repository = Testcontainers
  - frontend conventions: any 금지 / TanStack Query / arbitrary Tailwind 금지
- 인접 plan: `docs/plans/423-intent-classifier-removal/` — 같은 `processUserTurn` 본문 재작성. **사용자 결정 = 423 선행 머지 → 424 rebase**.
- 추정: WRAP_UP 가치 정량 지표 미수집 — Issue #424 운영 판단에 의존 (product-spec Evidence 명시).

## Trade-offs

### Option A (채택): `FollowUpRequest.terminate` boolean 추가 + 종료 응답 = 기존 `followUpExhausted=true` 재사용
- 장점:
  - 1턴 1호출 정합 (답변 처리 + 종료 신호 단일 트랜잭션 경계)
  - 응답 contract 변경 0 (신규 type / 신규 boolean 0)
  - FE 분기 단순 — 기존 `followUpExhausted=true` 처리 경로 활용
- 단점:
  - Request DTO 1필드 확장
  - 사용자 신호 종료 vs hard timeout backstop 구분은 BE 로그 / 메트릭 채널에 의존
- 채택 사유: 사용자 결정 — type 신설 / 응답 boolean 추가 회피. `followUpExhausted` 가 정확히 "다음 호출 가능 여부" 의미.

### Option B (폐기): 별도 endpoint `POST /interviews/{id}/terminate`
- 장점: 답변 처리와 종료 책임 분리
- 폐기 사유: 1턴 2호출 / 동시성 / 순서 보장 복잡 / FE 호출 코드 2배

### Option C (폐기): 응답 신규 boolean `terminatedByUser` 추가
- 장점: 사용자 신호 종료 / hard timeout 응답 schema 구분
- 폐기 사유: 사용자 결정 — `followUpExhausted=true` 의미 중복. 관찰성 = BE 로그 / 메트릭.

### Option D (폐기): BE 능동 종료 — 잔여 시간 임계 도달 시 자동 종료
- 장점: FE 시계 신뢰 의존도 0 / 단일 종료 출처
- 폐기 사유: product-spec 결정. 인위 종료 = WRAP_UP 단점과 동형. 사용자 행동 (답변 제출) 시점에 자연 종료가 UX 자연스러움.

### Option E (폐기 — Amendment): V46 (chk_question_track_meta_v2 재정의 — RESUME_WRAP_UP 제외) + V47 (롤백용 복원)
- 장점: DB CHECK 로 RESUME_WRAP_UP INSERT 차단 이중 방어 / 롤백 시 constraint 복원 자동화
- 폐기 사유: V42 가 이미 constraint + 관련 컬럼 일괄 DROP. 재정의 대상 부재. application enum 차단으로 충분.

## Architecture

### 시퀀스 (정상 / 종료 신호 / hard timeout backstop)

```
[FE] 사용자 답변 제출
  ├─ 잔여 시간 모니터링 (인터뷰 시작 시각 기준 client-side; 시작 시각 = interview-store 보유)
  ├─ 답변 입력 도중 잔여 ≤ 0 도달 → 끊지 않음 (답변 완료까지 대기)
  ├─ 답변 완료 시점 + 잔여 ≤ 0 → terminate=true 동봉
  └─ POST /api/v1/interviews/{id}/follow-up { ..., terminate: true|false }

[InterviewController.followUp(@Valid)] (본인 인터뷰 인가 검증 — 기존 follow-up 경로 그대로 적용)
  → [FollowUpService.generateFollowUp] → Resume 인터뷰 판정
  → [FollowUpService.delegateToResumeOrchestrator]
  → [ResumeInterviewOrchestrator.processUserTurn]

processUserTurn 분기 (변경 후):
  1. clockWatcher.markStart
  2. turnAnalysisPipeline.analyze → TurnAnalysisResult
  3. non-answer intent 분기 (기존 — 423 머지 후 baseline 변동 가능 — 본 spec 은 423 선행 가정)
  4. AnswerAnalysis 추출 + InterviewRuntimeState 조회
  5. clockWatcher.remainingMinutes 산출
  6. modeTransitionPolicy.isHardTimeoutExceeded → true 면 hardTimeoutResponse() (기존)
     - log.warn("[ResumeOrchestrator] hard timeout backstop: interviewId={}")
  7. **종료 신호 분기**: request.terminate==true 면
     - 직전 답변의 turnAnalysisPipeline.analyze 결과는 본 분기 진입 전 4단계에서 이미 산출됨 → 분석 자체는 수행 (사용자 결정).
     - 신규 question INSERT 는 skip (확정).
     - **turnEventPublisher.publish 도 skip** (Decision B — implement-be.md 결정 로그 참조). 사유: rubric scoring listener 가 questionId 기준으로 score 적재. publish 호출 시 questionId null payload → listener NPE 가능. 분석 결과 = 메모리 산출만, score 누락 결정.
     - terminateResponse() 반환: followUpExhausted=true / skip=true / presentToUser=false / question INSERT 0
     - log.info("[ResumeOrchestrator] FE-signaled terminate: interviewId={}, lastQuestionAnalyzed=true")
  8. dispatchByMode (PLAYGROUND | INTERROGATION) — WRAP_UP 분기 제거
  9. validateQuestionId / turnEventPublisher.publish (기존)
  10. return

[종료 응답 수신] FE
  └─ followUpExhausted=true → 종료 페이즈 진입 (기존 hard timeout 동일 처리 경로)
```

> **답변 적재 정책 (Decision B 확정)**: terminate=true 시 turnAnalysisPipeline.analyze 수행 / 신규 question INSERT skip / turnEventPublisher.publish skip. hard timeout 우선순위 = `terminate=true && hardTimeout` 동시 → hard timeout 응답 (RESUME_HARD_TIMEOUT).

### 변경 영역

| 영역 | 변경 |
|------|------|
| `ResumeMode` enum | WRAP_UP 제거 → 2종 |
| `ResumeInterviewOrchestrator` | `wrapUpHandler` 의존 제거 / `advanceToWrapUpIfDue` 호출 제거 / WRAP_UP case 제거. `terminate` 분기 + `terminateResponse()` 추가. javadoc 갱신 |
| `ResumeModeTransitionPolicy` | `advanceToWrapUpIfDue` 메서드 + `wrapUpThresholdMin` 필드 + `@Value` 제거. `isHardTimeoutExceeded` 유지 |
| `WrapUpModeHandler` | 파일 삭제 |
| `ResumeQuestionResultGenerator` | `generateWrapUp` + `wrapUpPromptBuilder` 의존 + `MODE_WRAP_UP` 상수 제거 |
| `ResumeWrapUpPromptBuilder` | 파일 삭제 (record `WrapUpResult` 포함) |
| `FocusHints` | `ResumeWrapUpHints` record + sealed permits 항목 제거 |
| `FocusLayer` | `CAP_RESUME_WRAP_UP` / `buildResumeWrapUp` / case 매핑 제거 |
| `SkeletonCallType` | `RESUME_WRAP_UP` enum 값 + 프롬프트 텍스트 제거 |
| `ResumeFallbackQuestions` | `WRAP_UP` 상수 제거 |
| `ResumeFallbackModelAnswers` | `WRAP_UP` 상수 제거 |
| `QuestionType` | `RESUME_WRAP_UP` enum 값 제거 + factory `Question.resume(...)` 분기 정리 |
| `application.yml` | `rehearse.resume-track.wrap-up-threshold-min` 키 삭제 |
| `FollowUpRequest` | `private boolean terminate` 필드 (기본값 false) — 기존 `@Getter @NoArgsConstructor` 패턴 그대로. `@JsonProperty` 불필요 (Jackson 이 `is*` getter / field reflection 으로 매핑). Bean Validation 불필요 |
| Flyway | **신규 마이그레이션 0건** (Amendment — V42 가 이미 chk_question_track_meta_v2 + chain_*/project_id DROP 완료) |
| FE `types/interview.ts` | QuestionType 유니언에서 `'RESUME_WRAP_UP'` 제거 |
| FE follow-up 호출 hook | 잔여 시간 산출 (인터뷰 시작 시각 = `interview-store` 또는 마운트 시 서버 응답) → 잔여 ≤ 0 + 답변 완료 시점에 request payload 에 `terminate: true` 포함 |
| FE 종료 처리 | 기존 `followUpExhausted=true` 분기 그대로 활용 |
| docs | `docs/domain/resume/glossary.md` / `schema.md` / `api/process-user-turn.md` / `docs/domain/question/glossary.md` / `schema.md` / `api/generate-questions.md` / `docs/domain/interview/api/follow-up.md` / `docs/domain/interview/runtime-state-and-context-layers.md` / `docs/domain/feedback/rubric-score-reflection.md` / `docs/domain/feedback/api/score-turn.md` 의 RESUME_WRAP_UP / WRAP_UP / wrap-up-threshold-min 항목 제거 |
| 운영 SQL (별도 ops PR — 본 PR 비스코프) | dev / prod `question` 테이블 `question_type='RESUME_WRAP_UP'` row 카운트 → 자식 FK 정리 → row 삭제. constraint prerequisite 가 아닌 단순 데이터 위생 |

## Data Model

### Flyway 마이그레이션 — 0건 (Amendment)

V42 가 이미 `chk_question_track_meta_v2` constraint + `chain_id` / `chain_step_type` / `project_id` 컬럼 일괄 DROP. RESUME_WRAP_UP row-pattern 차단 대상 부재 → 신규 V 파일 불필요.

과거 V 파일 (V35 / V41 / V44) 은 immutable 원칙에 따라 RESUME_WRAP_UP 문자열 잔존 (DDL 히스토리). 신규 마이그레이션 / 롤백 마이그레이션 0건.

### 운영 SQL (별도 ops PR — 본 PR 비스코프)

```sql
-- ============================================================
-- 파일: ops/424-resume-wrap-up-cleanup.sql (별도 PR / ops 채널 관리)
-- 실행 시점: 본 BE PR 머지 후 임의 시점 (constraint prerequisite 아님 — 데이터 위생).
-- 실행 환경: dev → prod 순서. 각 환경 1회.
-- 실행자: 백엔드 담당자 + ops 검수자 2인 페어 (PR 리뷰 + 실행 로그 캡처).
-- 사전 검증: 하기 1) SELECT 결과 = (예상 row 수) 보고 + 0 이면 skip 결정.
-- 사후 검증: 하기 3) SELECT = 0 확인.
-- 트랜잭션: START TRANSACTION ~ COMMIT 한 단위. 실패 시 ROLLBACK.
-- 백업: 운영 DB 표준 백업 정책 준수 (실행 전 RDS 스냅샷 1회 트리거).
-- 롤백: 본 SQL 비가역 (DELETE). RDS 스냅샷 복원 외 자동 롤백 없음.
-- ============================================================
-- 자식 FK ON DELETE CASCADE 아님 → 자식 우선 삭제.

-- 1) 카운트 확인
SELECT COUNT(*) FROM question WHERE question_type = 'RESUME_WRAP_UP';

-- 2) 자식 정리 → 본 row 삭제 (트랜잭션)
START TRANSACTION;
CREATE TEMPORARY TABLE _wrap_up_q AS
  SELECT id FROM question WHERE question_type = 'RESUME_WRAP_UP';

DELETE FROM timestamp_feedback WHERE question_id IN (SELECT id FROM _wrap_up_q);
DELETE FROM question_score     WHERE question_id IN (SELECT id FROM _wrap_up_q);
DELETE FROM question_answer    WHERE question_id IN (SELECT id FROM _wrap_up_q);
DELETE FROM question           WHERE id          IN (SELECT id FROM _wrap_up_q);

DROP TEMPORARY TABLE _wrap_up_q;
COMMIT;

-- 3) 카운트 0 검증
SELECT COUNT(*) FROM question WHERE question_type = 'RESUME_WRAP_UP';
```

### Entity

`QuestionType` enum 값 1개 제거. 기존 V35 / V41 / V44 SQL 파일은 보존 (룰: 과거 V 파일 immutable). DB 차단 layer 자체 부재 (V42 DROP) → application enum 차단만 유효.

## API Contract

### Endpoint

`POST /api/v1/interviews/{id}/follow-up` (기존 변경 없음 — multipart, request part `request` (JSON) + `audioFile`)

### 인가

- 기존 follow-up 경로의 본인 인터뷰 검증 그대로 적용 (`InterviewController` + `FollowUpService.loadFollowUpContext` 가 `userId` 비교).
- `terminate=true` 신호도 동일 인가 통과 후에만 처리. 추가 검증 없음.

### Request (변경)

```json
{
  "questionSetId": 12,
  "questionContent": "프로젝트에서 가장 어려웠던 점은?",
  "answerText": "...",
  "nonVerbalSummary": "...",
  "previousExchanges": [],
  "terminate": false
}
```

- 신규 필드: `terminate` (`boolean`, 선택, default `false`)
- DTO 패턴: `private boolean terminate` + `@Getter @NoArgsConstructor` (기존 동일). Jackson reflection deserialize 정상 (`isTerminate()` getter / field 매칭).

### Response (200) — 변경 없음

기존 `FollowUpResponse` schema. 종료 케이스:

```json
{
  "questionId": null,
  "question": null,
  "ttsQuestion": null,
  "reason": null,
  "type": null,
  "skip": true,
  "presentToUser": false,
  "followUpExhausted": true
}
```

- `followUpExhausted=true` = FE 종료 분기 트리거 (기존 hard timeout / context budget exceeded 와 동일 경로)
- 사용자 신호 종료 / hard timeout backstop 구분 = BE 로그 / 메트릭 채널.

### Error

기존 매핑 변경 없음.

### FE 호출 정책

- 잔여 시간 산출 = 인터뷰 시작 시각 (FE `interview-store` 보유) 기준 `Date.now()` 차이.
- 잔여 시간 ≤ 0 도달 + 사용자 답변 완료 (제출 시점) 에 한해 `terminate: true` 전송.
- 답변 입력 도중 시간 초과 → 끊지 않음. 사용자 답변 완료까지 대기 후 다음 제출 시 신호.
- clock skew / drift 보강은 비스코프 (별도 plan).

## NF 결정 (11개)

| NF | 결정 | 근거 / Confidence |
|----|------|-------------------|
| 영향 범위 | BE + FE + docs (Flyway 신규 0건) | 확신 — Evidence 매핑 + Amendment |
| 정합성 | 답변 분석 = 수행 / 신규 question INSERT = skip / turnEventPublisher.publish = skip (Decision B) | 확신 |
| 실시간성 | 회고 LLM 1회 제거 → latency 개선 부산물. 사용자 직접 대기 변경 없음 | 확신 |
| 부하 | LLM 호출 1회 / 회고 컨텍스트 빌드 1회 감소. 부하 측면 net positive | 확신 |
| 동시성 | `InterviewRuntimeStateCache` 단일 인터뷰 1 사용자 = 변경 없음. WRAP_UP 전이 race 가능성 자체 제거 | 확신 |
| 마이그레이션 | BE 코드 배포 → FE 코드 배포 (2단계). Flyway 신규 0건 → DB 단계 부재 | 확신 (Amendment 반영) |
| 외부 의존 | OpenAI / Claude (회고 prompt) 호출 1경로 제거. 신규 의존 0. **신뢰 경계: terminate 신호 = 클라이언트 시계 신뢰 모델** | 확신 |
| 보안 | A01 (인가) = 기존 follow-up 본인 검증 그대로. A03/A04 = boolean primitive 검증 단순. terminate 임의 송신 = 본인 인터뷰 조기 종료 (사용자 자기 자원 한정 → 위협 모델 외) | **추정 (위협 모델 한정 단정 — 운영 데이터 없음)** |
| 관찰성 | BE 로그 분리 (`FE-signaled terminate` / `hard timeout backstop`). 메트릭 = 본 plan 비스코프 (후속 plan) | **추정 (메트릭 부재로 사용자 신호 종료 비율 파악 불가 — 후속 plan 결정)** |
| 롤백 | 코드 revert 만으로 충분. V46/V47 폐기 → constraint 복원 절차 부재. enum 삭제는 비가역 → revert 시 enum 복구 함께 | 확신 (Amendment 반영) |
| 검증 | Service Integration + Domain Unit + DTO Unit + FE Integration + 정적 grep | 확신 |

## Verification (완료 판정)

### 통합 테스트 (Service Integration — `ServiceIntegrationSupport` + Testcontainers)

- [ ] Resume 정상 다중 턴: PLAYGROUND → INTERROGATION 전이 / 회고 질문 미발생 / `question` 테이블 RESUME_WRAP_UP INSERT 0
- [ ] FE 종료 신호: `terminate=true` 요청 → 응답 `followUpExhausted=true && skip=true && presentToUser=false` / 신규 question INSERT 0 / 답변 분석 (turnAnalysisPipeline) 수행 검증 / turnEventPublisher.publish 미호출 검증
- [ ] hard timeout backstop: `terminate=false` + 시간 초과 → 종료 응답 / question INSERT 0
- [ ] **경계 — terminate=true && hardTimeout 동시**: hard timeout 우선 → `RESUME_HARD_TIMEOUT` 응답 (Decision B 명시 우선순위)
- [ ] **경계 — 멱등성**: terminate=true 후 동일 interviewId 로 재요청 시 동작 (이미 종료 인터뷰 응답 정책 — 기존 follow-up 정책 그대로 echo: hard timeout 응답 또는 normal flow 진입. 기존 동작 회귀 확인)
- [ ] **경계 — 첫 턴 terminate (Playground opener 답변 케이스)**:
  - 시나리오: opener 질문 INSERT 직후 사용자 첫 답변에 `terminate=true` 동봉
  - 검증: turnAnalysisPipeline.analyze 호출 1회 + 신규 question INSERT 0 + 응답 followUpExhausted=true / skip=true / presentToUser=false
  - 예외 미발생: opener answer 만 존재하는 history 에서 분석 파이프라인 정상 통과
- [ ] 회귀: PLAYGROUND opener / Playground → Interrogation 전이 / Interrogation 다중 턴

### 단위 테스트 (Domain Unit / DTO Unit)

- [ ] `ResumeMode` enum 2종 (PLAYGROUND, INTERROGATION) 정합 검증 / WRAP_UP valueOf IllegalArgumentException
- [ ] `QuestionType` enum 에 `RESUME_WRAP_UP` 부재 검증 / 3종 RESUME_* 잔존 (RESUME_OPENER, RESUME_PLAYGROUND, RESUME_INTERROGATION) / 매핑 (referenceType + feedbackPerspective) 정합
- [ ] `FollowUpRequest` Jackson 역직렬화 — terminate 미포함 → false / true / false 명시 3케이스

### Repository 테스트 — Amendment 로 폐기

V42 가 chk_question_track_meta_v2 constraint DROP 한 상태 → CHECK 검증 대상 부재. 별도 Repository 테스트 추가 0건.

### FE Integration (RTL + msw)

- [ ] 인터뷰 진행 중 잔여 시간 ≤ 0 도달 + 답변 제출 시 request payload 에 `terminate: true` 포함 (msw 핸들러로 request body assert)
- [ ] `followUpExhausted=true` 응답 수신 시 종료 UI 페이즈 진입 (기존 동작 회귀)
- [ ] 잔여 ≤ 0 + 답변 입력 중 → terminate 미전송 / 답변 완료 후 제출 시점에만 전송

### 정적 검증 (grep)

- [ ] `grep -rEn "RESUME_WRAP_UP|WrapUp|wrap-up-threshold-min|wrapUpThresholdMin|ResumeWrapUp" backend/src` → 과거 V 파일 (V35 / V41 / V44) + 본 PR 의 enum 부재 검증 테스트만 잔존. 그 외 0건.
- [ ] `grep -rEn "RESUME_WRAP_UP|wrap_up" frontend/src` → 0건
- [ ] `grep -rEn "WRAP_UP|RESUME_WRAP_UP|wrap-up-threshold-min" docs/domain` → 0건

### 빌드 / 린트

- [ ] `./gradlew build`
- [ ] `npm run lint && npm run build`

### 관찰 가능 동작

- [ ] BE 로그: `[ResumeOrchestrator] FE-signaled terminate: interviewId=...` / `[ResumeOrchestrator] hard timeout backstop: interviewId=...` 분리 출력
- [ ] dev 배포 후 E2E 1회: Resume 시작 → 다중 턴 → duration 초과 후 답변 제출 → 종료 응답 → FE 종료 페이즈

### 회귀 체크

- [ ] `./gradlew test --tests "com.rehearse.api.domain.resume.*"`
- [ ] `./gradlew test --tests "com.rehearse.api.domain.interview.service.FollowUpServiceTest"`
- [ ] `./gradlew test --tests "com.rehearse.api.infra.ai.context.*"` (FocusLayer / SkeletonCallType 영향 영역)
- [ ] FE Resume 관련 hooks/page 테스트

## Pre / Post State

### Pre (현재)

- `ResumeMode` enum 3종
- `processUserTurn` — `advanceToWrapUpIfDue` 호출 + WRAP_UP case dispatch
- WrapUpModeHandler / ResumeWrapUpPromptBuilder / generateWrapUp / FocusHints.ResumeWrapUpHints / CAP_RESUME_WRAP_UP / buildResumeWrapUp / SkeletonCallType.RESUME_WRAP_UP / ResumeFallbackQuestions.WRAP_UP / ResumeFallbackModelAnswers.WRAP_UP 존재
- `QuestionType.RESUME_WRAP_UP` 존재 / DB constraint 자체 부재 (V42 DROP)
- `application.yml` `wrap-up-threshold-min: 2`
- `FollowUpRequest` 종료 신호 필드 없음
- FE `QuestionType` 유니언 6종 / 종료 의사 신호 코드 없음

### Post (구현 후)

- `ResumeMode` 2종
- `processUserTurn` — terminate 분기 + hard timeout backstop. WRAP_UP 분기 0
- 회고 산출물 (Handler / PromptBuilder / generateWrapUp / FocusHints.ResumeWrapUpHints / CAP_RESUME_WRAP_UP / buildResumeWrapUp / SkeletonCallType.RESUME_WRAP_UP / ResumeFallbackQuestions.WRAP_UP / ResumeFallbackModelAnswers.WRAP_UP) 0
- `QuestionType.RESUME_WRAP_UP` 부재 (Flyway 신규 0건 — application enum 차단만 유효)
- `application.yml` `wrap-up-threshold-min` 키 부재
- `FollowUpRequest.terminate: boolean`
- FE `QuestionType` 5종 / `terminate` 신호 전송 로직 / 종료 처리 = 기존 followUpExhausted 경로
- docs FSM 다이어그램 / 회고 단계 항목 0

## 위험 / 마이그레이션 / 롤백

### 위험

- **인접 plan 423 동시 진행**: `ResumeInterviewOrchestrator.processUserTurn` 본문 conflict 확정. mitigation = **423 선행 머지 후 424 rebase** (사용자 결정).
- **신뢰 경계 (FE 시계)**: terminate 신호 = 클라이언트 시계 신뢰 모델. 시계 조작 / 오차로 사용자가 임의 시점에 인터뷰 조기 종료 가능. 위협 모델 = 사용자 자기 자원 한정 → 보안 영향 외. backstop 미커버. mitigation = product-spec 비스코프 (별도 plan).
- **Sealed permits 누락**: `FocusHints` sealed interface 의 permits 절 정리 필요. 누락 시 컴파일 실패 — 빌드 단계 즉시 검출.
- **DB 차단 layer 부재 (Amendment)**: V42 DROP 이후 RESUME_WRAP_UP INSERT 차단은 application enum 만 유효. 직접 SQL INSERT 또는 enum 우회 코드 등장 시 차단 불가. mitigation = 정적 grep + 코드 리뷰.

### 마이그레이션 전략

1. **BE 머지 + 배포**: 코드 산출 제거 / `FollowUpRequest.terminate` 추가. Flyway 신규 0건 → DB 무관. FE 미배포 상태에서도 BE backstop 정상 동작.
2. **FE 머지 + 배포**: terminate 신호 전송 / QuestionType 유니언 정리.
3. **운영 SQL cleanup (별도 ops PR — 본 PR 비스코프)**: dev / prod RESUME_WRAP_UP row 자식 + 본체 삭제 → 카운트 0. 데이터 위생 작업.
4. 양방향 호환: BE only (FE 미배포) → terminate 미수신 + backstop 정상. FE only (BE 미배포) → request 의 terminate 필드 BE 무시 (Jackson default) → 기존 동작.

### 롤백 시나리오

- **BE 배포 후 결함 발견**: `git revert` BE PR → 코드 산출 / enum 복구. Flyway 마이그레이션 0건 → DB 롤백 절차 부재. enum 복구 후 정상 동작.
- **FE 배포 후 결함**: FE PR revert → BE backstop 정상 동작 유지.
- **roll forward 우선**: 가능 시 fix-forward. revert 는 마지막 수단.

## 분기 결정

- [ ] 단일 영역 → `implement.md` 1개
- [ ] BE+FE 동시 → `implement-be.md` + `implement-fe.md` (API contract 합의 후 병렬)
- [x] **BE 선행 (강결합 완화) → `implement-be.md` 머지 후 `implement-fe.md`**

사유:
- 423 선행 머지 가정 + 424 BE 가 처음 rebase 대상 → BE 안정화 후 FE 진입
- BE only (FE 미배포 상태) 에서도 hard timeout backstop 정상 동작 → 호환 윈도우 확보
- Flyway 신규 0건 → DB 단계 부재 (Amendment)
