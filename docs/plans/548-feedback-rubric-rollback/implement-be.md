# Implement (Backend + Lambda) — 피드백 루브릭 롤백

> **작성자**: backend agent (create-implement-plan)
> **답하는 질문**: BE/Lambda 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★
> **강결합**: BE 선행 강제 (V54 + contract REVERT + Lambda 출력형식 = FE 2탭 데이터 전제)
> **범위**: PR1 (Phase 1~6) = 답변별 롤백 / PR2 (Phase 7) = 세션 입력 재설계. PR1 머지·검증 후 PR2 착수.

---

## Phase 0: 착수 전 확인

- [ ] **develop 동기화** — `git checkout develop && git pull` (직전 머지 #547 반영). `git-manager` 위임.
- [ ] **API Contract 확인** — `tech-spec.md#api-contract` main `TimestampFeedbackResponse` 형태 합의됨.
  - 조회 `GET /api/v1/interviews/{interviewId}/question-sets/{questionSetId}/feedback` (`QuestionSetController`)
  - 저장 `POST /api/internal/interviews/{interviewId}/question-sets/{questionSetId}/feedback` (`FeedbackController`)
- [ ] **컨벤션 Read** — `backend/.claude/rules/conventions.md`, `testing.md`, `backend/AGENTS.md`.

미합의 → STOP. tech-spec 갱신 + 재승인.

---

## Phase / Step 개요

| Phase | 제목 | 구현 | PR | 의존 |
|-------|------|------|----|------|
| 1 | V54 마이그레이션 | `backend` | PR1 | Phase 0 |
| 2 | Entity/DTO/Mapper REVERT (코멘트형) | `backend` | PR1 | Phase 1 |
| 3 | FeedbackService/Persister/Batch ADAPT | `backend` | PR1 | Phase 2 |
| 4 | rubric/score/infra-ai 루브릭 DELETE + ArchUnit | `backend` | PR1 | Phase 2,3 |
| 5 | 세션 피드백 임시 중립화 | `backend` | PR1 | Phase 4 |
| 6 | Lambda 출력형식 복원 | `general-purpose` | PR1 | Phase 2 (저장계약) |
| 7 | 세션 입력 재설계 + 재활성 | `backend` | PR2 | PR1 머지 |

> PR1 = Phase 1~6 한 PR. Lambda(Phase 6)는 BE 저장계약(Phase 2~3)과 **같은 PR/머지**에 묶음 (위험 3 계약 불일치 회피).
> Phase 2·3·4 는 동일 영역 강결합 → 단일 backend 세션에서 순차. Phase 6(Lambda)만 general-purpose 병렬 가능 (단 머지는 같이).

---

## Phase 1: V54 마이그레이션

- **구현**: `backend` — 루브릭 점수 테이블 DROP + main 코멘트 컬럼 재추가.

### 변경 파일
- `backend/src/main/resources/db/migration/V54__rollback_score_system.sql` — 신규 (DDL only)

### 핵심 로직
- tech-spec.md §Data Model DDL 그대로.
- `DROP TABLE IF EXISTS question_score_dimension; DROP TABLE IF EXISTS question_score;` (child first)
- `ALTER TABLE timestamp_feedback ADD COLUMN ...` 14컬럼 (verbal_comment/accuracy_issues/coaching_*/nonverbal_comment/overall_comment/vocal_comment/attitude_comment/speech_pace/tone_confidence_level/emotion_label/eye_contact_level/posture_level/expression_label).
- `rubric_score`(V26)/`nonverbal_score`(V33) 건드리지 않음 (V38 에서 이미 DROP). CHECK 제약 재생성 안 함 (Staff 결정 — 앱계층 enum 검증).

### 의존
- 선행: Phase 0. 외부: Flyway.

### Verification
- `./gradlew test` Testcontainers — 마이그레이션 적용 후 `timestamp_feedback` 14컬럼 존재 + `question_score*` 테이블 부재.

### 커밋 메시지
```
feat(BE): V54 루브릭 점수 테이블 제거 + timestamp_feedback 코멘트 컬럼 재추가
```

---

## Phase 2: Entity/DTO/Mapper REVERT (코멘트형 복원)

- **구현**: `backend` — main 코멘트형 저장/응답 구조 복원. `bestAnswer`·`QuestionSet` import 유지.

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/feedback/entity/TimestampFeedback.java` — 14컬럼 + 빌더 복원
- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/TimestampFeedbackResponse.java` — `from(feedback)` → ContentFeedback/DeliveryFeedback/CommentBlock 구조. dimension/score/observation/evidenceQuote/status 필드 제거
- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/SaveFeedbackRequest.java` — CommentBlock 기반 TimestampFeedbackItem
- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/QuestionSetFeedbackResponse.java` — 단일 `from`
- `backend/src/main/java/com/rehearse/api/domain/feedback/mapper/TimestampFeedbackMapper.java` — 코멘트 직렬화/역직렬화

### 핵심 로직
- main 의 `TimestampFeedbackResponse` 형태 (tech-spec §API Contract Response). content{verbalComment/accuracyIssues/coaching} + delivery{nonverbal/vocal/attitudeComment} + overallComment.
- **유지(되돌리지 않음)**: `getBestAnswer()` (V47 리네임), `question.entity.QuestionSet` import (패키지 리네임).

### 의존
- 선행: Phase 1 (컬럼 존재).

### Verification
- Domain Unit: `TimestampFeedbackResponse.from(feedback)` content/delivery 반환 + dimension 필드 부재.

### 커밋 메시지
```
refactor(BE): TimestampFeedback 응답을 코멘트형 Content/Delivery 로 복원
```

---

## Phase 3: FeedbackService/Persister/Batch ADAPT

- **구현**: `backend` — 루브릭 기대 제거, 코멘트 저장 경로 복원.

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/feedback/service/FeedbackService.java` — 루브릭 기대 제거, 코멘트 저장
- `backend/src/main/java/com/rehearse/api/domain/feedback/service/QuestionSetFeedbackPersister.java` — 코멘트 컬럼 기록
- `backend/src/main/java/com/rehearse/api/domain/feedback/service/TimestampFeedbackBatch.java` — 코멘트 컬럼 batch insert

### 핵심 로직
- Lambda POST 페이로드(코멘트형) → `timestamp_feedback` 14컬럼 저장.
- 신규 컬럼 NULL 허용 (기존 row + Lambda 미전환 구간 대비, 위험 3).

### 의존
- 선행: Phase 2.

### Verification
- Service Integration (외부 API Mock): 코멘트 저장 후 조회 시 main 형태 응답.

### 커밋 메시지
```
refactor(BE): FeedbackService 코멘트 기반 저장 경로 복원
```

---

## Phase 4: rubric/score/infra-ai 루브릭 DELETE

- **구현**: `backend` — develop 신규 루브릭 채점 경로 전체 제거.

### 변경 파일 (삭제)
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/**` — RubricIds, entity 7종, models/service, `RubricScorer` 포트, service 6종 (15파일)
- `backend/src/main/java/com/rehearse/api/domain/feedback/score/**` — entity 3종, repository 2종, `QuestionScorePersister` (6파일)
- `backend/src/main/java/com/rehearse/api/infra/ai/` 루브릭 어댑터 (15파일): `adapter/{Claude,OpenAi,Resilient}RubricScorer`, `RubricScorerResponseValidator`, `RubricScoringPipeline`, `client/{Claude,OpenAi}RubricScorerClient`, `config/{Claude,OpenAi}RubricScorerRestClientConfig`, `properties/{Claude,OpenAi}RubricScorerProperties`, `schema/GeneratedRubricScoringSchema`, `prompt/RubricScorerPromptBuilder`, `dto/GeneratedRubricScoring`, `MockRubricScorer`
- `backend/src/main/resources/prompts/template/turn-rubric-scorer.txt`, `backend/src/main/resources/rubric/*.yaml`
- 관련 테스트 삭제/대체

### 핵심 로직
- **KEEP (절대 삭제 금지)**: 비루브릭 포트 전체 (answer-analyzer/follow-up/question-gen/resume/audio-turn/session-synth) + 공용 infra (ChatMessage/ChatRequest/AiCallMetrics/PromptTemplateLoader).
- `AnswerAnalysisCompletedEvent` **발행 유지** — 소비자(`RubricScoringEventListener`)만 삭제 (위험 4, cross-domain 변경 회피).

### 의존
- 선행: Phase 2,3 (FeedbackService 가 루브릭 의존 끊긴 후).

### Verification
- 컴파일 통과 + ArchUnit (계층/패키지 룰). `./gradlew build`.

### 커밋 메시지
```
refactor(BE): 루브릭 채점기 + 점수 테이블 도메인/어댑터 제거
```

---

## Phase 5: 세션 피드백 임시 중립화

- **구현**: `backend` — PR2 전까지 세션 피드백 생성 경로 비활성. 부팅/런타임 예외 방지.

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/feedback/session/SessionFeedbackEventListener.java` — 생성 경로 비활성 (early-return / no-op)
- (의존이 삭제된 `question_score*`·루브릭 클래스를 참조하는 `session/synthesis/*` 컴파일 차단 해소 — 최소 stub 또는 호출 차단)

### 핵심 로직
- `InterviewCompletedEvent` 수신해도 세션 피드백 생성 건너뜀 (예외 없이).
- `session_feedback` 테이블·기존 데이터 **보존** (DROP 안 함).
- FE 세션 모달/FAB 숨김은 implement-fe Phase 4 에서.

### 의존
- 선행: Phase 4 (루브릭 클래스 삭제 후 컴파일 깨짐 해소).

### Verification
- 부팅 Smoke + Service Integration: 세션 피드백 비활성 상태에서 인터뷰 완료 이벤트 예외 없이 처리. `session_feedback` 기존 데이터 보존.

### 커밋 메시지
```
refactor(BE): 세션 피드백 생성 임시 비활성 (PR2 재설계 전 중립화)
```

---

## Phase 6: Lambda 출력형식 복원

- **구현**: `general-purpose` — 비언어 출력을 main 평면 필드로 복원. SDK(#516) 유지.

### 변경 파일
- `lambda/analysis/analyzers/gemini_analyzer.py` — **수술적**: google.genai SDK 유지, 프롬프트·스키마·파싱만 main 평면 필드 형식으로 (차원점수 → eyeContactLevel/postureLevel/expressionLabel + vocalComment/speechPace/toneConfidenceLevel/emotionLabel + verbalComment/accuracyIssues/coaching*)
- `lambda/analysis/analyzers/vision_analyzer.py` — main 형식 revert
- `lambda/analysis/analyzers/dimension_validator.py` — **삭제** (PR#374 결정론 매퍼)
- `lambda/analysis/analyzers/verbal_analyzer.py` — main 에서 복원 (RESTORE)
- `lambda/analysis/handler.py` — 출력 매핑 main 형태
- `lambda/analysis/tests/` — `test_dimension_validator.py`/`test_gemini_analyzer_dimension_prompt.py`/`test_handler_new_fields.py` 제거·대체, 평면 필드 출력 검증 테스트

### 핵심 로직
- 출력 페이로드 = BE `SaveFeedbackRequest`(Phase 2 코멘트형)와 정합. POST `/api/internal/.../feedback`.
- SDK 교체(#516)는 출력형식과 무관 → 통째 revert 금지, 출력 매핑만 수정 (TO-1).

### 의존
- 선행: Phase 2 (BE 저장계약 코멘트형 확정). **같은 PR 머지** (위험 3).
- 외부: google.genai, OpenAI Vision/Whisper.

### Verification
- `cd lambda/analysis && pytest` — 비언어 평면 필드 출력 검증, dimension 테스트 제거 확인.

### 커밋 메시지
```
fix(lambda): 비언어 분석 출력을 main 평면 코멘트 형식으로 복원
```

---

## Phase 7: 세션 입력 재설계 + 재활성 (PR2)

- **구현**: `backend` — 세션 synthesizer 입력을 `timestamp_feedback` 코멘트 기반으로 재작성. 생성 재활성.
- **착수 조건**: PR1 머지·검증 완료 (timestamp_feedback 코멘트 컬럼 적재 확인).

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/feedback/session/synthesis/SessionFeedbackInputAssembler.java` — 입력 소스를 `question_score*`/RubricCatalog → `timestamp_feedback` 코멘트로 교체
- `backend/src/main/java/com/rehearse/api/domain/feedback/session/synthesis/SessionFeedbackInput.java` — 차원점수 필드 제거, 코멘트형 입력
- `backend/src/main/java/com/rehearse/api/domain/feedback/session/synthesis/TurnScoreView.java` — 코멘트형 뷰로 재정의
- `backend/src/main/java/com/rehearse/api/domain/feedback/session/synthesis/SessionFeedbackParser.java` — 차원점수 파싱 제거
- `backend/src/main/java/com/rehearse/api/domain/feedback/session/models/service/SessionFeedbackSynthesizer.java` — LLM 프롬프트 재작성 (차원점수 無, 코멘트 기반 서술)
- `backend/src/main/java/com/rehearse/api/domain/feedback/session/SessionFeedbackEventListener.java` — Phase 5 중립화 해제 (재활성)
- 세션 프롬프트 템플릿 (`resources/prompts/...session...`)

### 핵심 로직
- tech-spec §Architecture BE 세션 종합 피드백 Post 흐름.
- 입력 = `timestamp_feedback` 코멘트(verbal/nonverbal/coaching) → synthesizer → `session_feedback`(JSON).
- 응답에 루브릭 차원명/점수 텍스트 부재. 품질 고도화는 비스코프.
- `SessionFeedbackResponse` 의 `dimensionScores`/`dimension`/`levelGap` 필드는 직렬화 유지하되 코멘트 기반 값 (필드 제거는 비스코프).

### 의존
- 선행: PR1 전체 머지.

### Verification
- Service Integration: 인터뷰 완료 → `session_feedback` 코멘트 기반 적재, 응답에 차원명/점수 텍스트 부재.

### 커밋 메시지
```
refactor(BE): 세션 피드백 입력을 코멘트 기반으로 재설계 + 생성 재활성
```

---

## FE 와 통합 시점

- **BE 선행 강제** (강결합). PR1 BE+Lambda 머지 후 FE 통합 (implement-fe Phase).
- FE 는 main 기지 contract 라 mock 병렬 진행 가능. 단 위험 3 (Lambda↔BE 계약) 때문에 BE 머지 후 통합 권장.
- BE 머지 직후 FE 측 알림.

## 통합 Verification

- [ ] tech-spec.md §Verification PR1 (DB/BE/세션중립화/Lambda/빌드린트/회귀) 통과
- [ ] tech-spec.md §Verification PR2 (BE 세션 코멘트 기반 적재) 통과
- [ ] 회귀: 질문풀 어드민·이력서 트랙·인터뷰 생성/진행/follow-up E2E

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-backend` 실행 (구현 완료 직후 — 메인 세션 책임)
- [ ] FE 동시 작업 시 `code-reviewer-frontend` 와 **병렬** 호출 (단일 메시지 multiple tool_use)
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec §Pre/Post)
