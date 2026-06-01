# Tech Spec — 피드백 시스템 롤백 (루브릭/차원 채점 제거, main 코멘트형 피드백 복원)

> **작성자**: Staff Engineer (create-tech-spec)
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement 진입 ★
> **관련**: Issue #548 / product-spec.md (동일 폴더)

---

## Why → Goal (1줄 미러)

develop 의 루브릭/차원 채점 피드백을 main 의 코멘트형 Content/Delivery 2탭으로 논리적 롤백한다. 세션 종합 피드백은 유지하되 입력을 코멘트형으로 재설계한다.

## Evidence

- **현재 구조 (핵심 교정)**: main 에는 BE 피드백 **생성 LLM 이 없다**. 답변별 코멘트(verbal/nonverbal/accuracy/coaching)는 **Lambda(Gemini/Vision/Whisper)가 생성 → BE 로 POST → `timestamp_feedback` 비정규화 컬럼 저장 → `TimestampFeedbackResponse.from(feedback)` 가 컬럼 직독**. 루브릭 채점기(`RubricScorer` LLM 포트 + `RubricScoringEventListener`)는 develop 이 **새로 추가한** BE LLM 경로다.
- **부활시킬 옛 BE 생성기 없음**: 롤백 = ① 코멘트형 저장/응답 복원 + ② 루브릭 채점기 삭제·이벤트 언와이어. (BE 에 새 LLM 추가 아님.)
- **DROP 된 컬럼**: `V34__drop_lambda_content_columns` + `V48__drop_timestamp_feedback_freetext_and_raw_columns` 가 main 엔티티가 읽던 14개 컬럼을 제거. Flyway forward-only → 재추가 마이그레이션 필요.
- **세션 피드백 입력 결합**: `feedback/session/synthesis/SessionFeedbackInputAssembler` 가 `QuestionScoreRepository`/`QuestionScoreDimensionRepository`/`RubricCatalog`/`NonverbalImprovementActionsLoader`/`RubricIds` 의존. 루브릭 제거 시 `timestamp_feedback` 코멘트에서 읽도록 재설계 필요.
- **Lambda SDK 엉킴**: google.generativeai → google.genai 교체(#516)가 출력매핑과 **같은 파일**(`lambda/analysis/analyzers/gemini_analyzer.py`)에 위치. 통째 revert 불가, 수술적 수정 필요.
- **추정/가정**: dev DB 만 루브릭 데이터 보유(사용자 확인). prod 무관. `infra/ai` 비루브릭 포트(answer-analyzer/follow-up/question-gen/resume/audio-turn/session-synth)는 인터뷰 파이프라인용 → 절대 revert 금지.
- **사용자 결정**: 세션 피드백 = 유지 + 입력 재설계 / Lambda SDK = 유지(수술적) / `bestAnswer` 필드명 = 유지(되돌리지 않음).

## Trade-offs

### TO-1. Lambda Gemini SDK 교체분 처리

**Option A (채택): SDK 유지 + 출력매핑만 수술적 수정**
- 장점: #516 독립 개선 보존. "무관 작업 유지" 원칙 부합.
- 단점: `gemini_analyzer.py` 통째 revert 불가 → 수동 편집(프롬프트·스키마·파싱만 main 형식으로). 구현 난도 ↑.
- 사유: SDK 교체는 출력형식과 논리적 무관(파일 동거일 뿐). 롤백은 출력형식 한정.

**Option B (폐기): `gemini_analyzer.py` 통째 main revert**
- 장점: 단순. clean checkout.
- 폐기 사유: #516 SDK 개선까지 롤백 → 무관 작업 손실. 사용자 원칙 위배.

### TO-2. `model_answer` / `best_answer` 필드명

**Option A (채택): develop `bestAnswer` 유지, 복원 컴포넌트를 `bestAnswer` 사용하게 적응**
- 장점: 인터뷰 플로우(동일 필드 사용)·BE contract 영향 0. 롤백 핵심(피드백 형태)에 집중.
- 단점: main 코드와 필드명 1개 불일치 → 복원 시 grep 치환 필요.
- 사유: V47 리네임은 인터뷰 도메인 전역. 되돌리면 번짐.

**Option B (폐기): `modelAnswer` 로 되돌림**
- 폐기 사유: 인터뷰 플로우 + BE 응답 contract 까지 롤백 번짐. 범위 폭발.

### TO-3. 세션 피드백 입력 재설계 (사용자 결정 반영)

**Option A (채택): 유지 + 입력을 `timestamp_feedback` 코멘트형으로 재설계**
- 장점: 세션 단위 종합 피드백 기능 유지.
- 단점: `SessionFeedbackInputAssembler`/`SessionFeedbackInput`/`TurnScoreView`/`SessionFeedbackParser`/`SessionFeedbackSynthesizer` + 프롬프트 재작성. 작업량 큼.
- 사유: 사용자 명시 결정(기능 유지). 품질 고도화는 비스코프.

**Option B (폐기): 세션 피드백 전체 삭제**
- 폐기 사유: 사용자가 유지 선택.

## Architecture

### BE — 답변별 피드백 (Pre/Post 흐름)

```
[Pre / develop]
Lambda → POST /api/internal/.../feedback → FeedbackService → QuestionSetFeedbackPersister/TimestampFeedbackBatch
                                                       └ (rubric-shaped 컬럼만 저장)
interview FollowUpService ──publish──> AnswerAnalysisCompletedEvent
                                          └> RubricScoringEventListener → RubricScoringService
                                               → RubricScorer(LLM 포트) → QuestionScorePersister
                                                  → question_score / question_score_dimension
조회: GET feedback → TimestampFeedbackResponse.from(feedback, questionScores, dimsByScoreId)  # 차원 점수 응답

[Post / 롤백]
Lambda(코멘트형 복원) → POST /api/internal/.../feedback → FeedbackService(코멘트 저장) → TimestampFeedbackBatch
                                                  → timestamp_feedback 비정규화 컬럼(14개 재추가)
RubricScoringEventListener / RubricScoringService / RubricScorer  ── 삭제
AnswerAnalysisCompletedEvent  ── 유지(발행은 둠, 소비자만 제거) ※ 분기 결정 참조
조회: GET feedback → TimestampFeedbackResponse.from(feedback)  # content/delivery 응답 (main 형태)
```

### BE — 세션 종합 피드백 (입력 재설계)

```
[Pre] InterviewCompletedEvent → SessionFeedbackEventListener → SessionFeedbackService
        → SessionFeedbackInputAssembler ─reads→ question_score / question_score_dimension / RubricCatalog
        → SessionFeedbackSynthesizer(LLM) → session_feedback(JSON)

[Post] InterviewCompletedEvent → SessionFeedbackEventListener → SessionFeedbackService
        → SessionFeedbackInputAssembler ─reads→ timestamp_feedback 코멘트(verbal/nonverbal/coaching)
        → SessionFeedbackSynthesizer(LLM, 프롬프트 재작성: 차원점수 無, 코멘트 기반 서술) → session_feedback(JSON)
```

### FE — 피드백 뷰어

```
[Pre] interview-feedback-page → feedback-panel(단일 ContentTab) → RubricDimensionCard (차원 점수)
                                  + SessionFeedbackModal/CoachNoteFab (차원점수 행 포함)

[Post] interview-feedback-page(세션모달 유지) → feedback-panel(2탭: Content/Delivery)
         ├ ContentTab: StructuredComment + AccuracyIssues + CoachingCard
         └ DeliveryTab: nonverbal(eye/posture/expression) + vocal(filler/pace/tone/emotion) + LevelBadge
       SessionFeedbackModal: 차원 점수 행 제거, 강점/약점/계획을 서술형 렌더
```

### Lambda — 비언어 출력형식

```
[Pre] gemini_analyzer / vision_analyzer → nonverbalScore{vocal/vision dimensions[]}  (fluency/confidence_tone/eye_contact_posture + score)
[Post] gemini_analyzer(SDK 유지) / vision_analyzer(revert) → 평면 필드 (eyeContactLevel/postureLevel/expressionLabel + vocalComment/speechPace/toneConfidenceLevel/emotionLabel + verbalComment/accuracyIssues/coaching*)
       dimension_validator.py(PR#374 결정론 매퍼) 삭제 / verbal_analyzer.py 복원
```

## Data Model

### `V54__rollback_score_system.sql` (DDL only — 컨벤션 §Flyway)

> 버전 근거: develop/로컬 최신 = `V53__drop_resume_skeleton.sql`. 다음 가용 번호 = **V54**.

```sql
-- 1. develop 루브릭 점수 테이블 제거 (child first)
DROP TABLE IF EXISTS question_score_dimension;
DROP TABLE IF EXISTS question_score;

-- 2. main Content/Delivery 가 읽는 timestamp_feedback 컬럼 재추가 (V34 + V48 이 드롭)
ALTER TABLE timestamp_feedback
    ADD COLUMN verbal_comment        TEXT,
    ADD COLUMN accuracy_issues       TEXT,
    ADD COLUMN coaching_structure    VARCHAR(500),
    ADD COLUMN coaching_improvement  VARCHAR(500),
    ADD COLUMN nonverbal_comment     TEXT,
    ADD COLUMN overall_comment       TEXT,
    ADD COLUMN vocal_comment         TEXT,
    ADD COLUMN attitude_comment      TEXT,
    ADD COLUMN speech_pace           VARCHAR(10),
    ADD COLUMN tone_confidence_level VARCHAR(20),
    ADD COLUMN emotion_label         VARCHAR(20),
    ADD COLUMN eye_contact_level     VARCHAR(20),
    ADD COLUMN posture_level         VARCHAR(20),
    ADD COLUMN expression_label      VARCHAR(50);
```

- `rubric_score`(V26)/`nonverbal_score`(V33)는 develop V38 에서 이미 DROP → **V54 에서 건드리지 않음**.
- `filler_words`(V13) 미드롭 → 재추가 불필요.
- **CHECK 제약 (V48 드롭분 `chk_eye_contact_level`/`chk_posture_level`/`chk_tone_confidence_level`) — 재생성 안 함 (Staff 결정)**. 사유: dev 전용 환경 + BE/Lambda 가 enum 검증 담당. DB CHECK 재생성 시 Lambda 출력 enum 어긋남에 저장 실패(운영 마찰) 대비 이득 적음. 무결성은 앱계층 단일화.
- `session_feedback`(V27/V32) = 범용 JSON 컬럼 → DROP 불필요. (복원 엔티티 충돌 시 BE 리뷰에서 재확인.)

### Entity / DTO

- **REVERT (코멘트형 복원)**: `feedback/entity/TimestampFeedback.java`(14컬럼+빌더), `feedback/dto/TimestampFeedbackResponse.java`(`from(feedback)` → ContentFeedback/DeliveryFeedback/CommentBlock), `feedback/dto/SaveFeedbackRequest.java`(CommentBlock 기반 TimestampFeedbackItem), `feedback/dto/QuestionSetFeedbackResponse.java`(단일 `from`), `feedback/mapper/TimestampFeedbackMapper.java`(코멘트 직렬화). 단 `question.entity.QuestionSet` import(패키지 리네임)·`getBestAnswer()`(V47) 유지.
- **DELETE**: `feedback/rubric/**`(RubricIds, entity 7종, models/service/RubricScorer, service 6종), `feedback/score/**`(entity 3종, repository 2종, service QuestionScorePersister).
- **ADAPT**: `feedback/service/FeedbackService.java`(루브릭 기대 제거, 코멘트 저장 유지), `QuestionSetFeedbackPersister`/`TimestampFeedbackBatch`(코멘트 컬럼 기록).
- **REWORK(세션)**: `feedback/session/synthesis/{SessionFeedbackInputAssembler,SessionFeedbackInput,TurnScoreView,SessionFeedbackParser}.java`, `feedback/session/models/service/SessionFeedbackSynthesizer.java` — 입력 소스를 `timestamp_feedback` 코멘트로 재설계.

### infra/ai DELETE 목록

`adapter/{ClaudeRubricScorer,OpenAiRubricScorer,ResilientRubricScorer,RubricScorerResponseValidator,RubricScoringPipeline}`, `client/{ClaudeRubricScorerClient,OpenAiRubricScorerClient}`, `config/{Claude,OpenAi}RubricScorerRestClientConfig`, `properties/{Claude,OpenAi}RubricScorerProperties`, `schema/GeneratedRubricScoringSchema`, `prompt/RubricScorerPromptBuilder`, `dto/GeneratedRubricScoring`, `MockRubricScorer`, `resources/prompts/template/turn-rubric-scorer.txt`, `resources/rubric/*.yaml`. → **KEEP**: 비루브릭 포트 전체 + 공용 infra(ChatMessage/ChatRequest/.../AiCallMetrics/PromptTemplateLoader).

## API Contract

> BE+FE 강결합. contract = main `TimestampFeedbackResponse` 형태 복원. **합의 = 승인 게이트.**

### Endpoint (불변)

- 조회: `GET /api/v1/interviews/{interviewId}/question-sets/{questionSetId}/feedback` (`QuestionSetController#64`, 현행 유지).
- 저장: `POST /api/internal/interviews/{interviewId}/question-sets/{questionSetId}/feedback` (`FeedbackController#22`). **prefix `/api/internal/`** — Lambda→BE 내부 호출용. 조회(`/api/v1/`)와 prefix 다름 주의.

### Response (200) — Post 롤백 (main 형태)

```json
{
  "questionSetId": 1,
  "streamingUrl": "...",
  "timestampFeedbacks": [
    {
      "questionId": 10,
      "questionType": "TECH_MAIN",
      "bestAnswer": "...",
      "content": {
        "verbalComment": { "positive": "...", "negative": "...", "suggestion": "..." },
        "accuracyIssues": [ { "claim": "...", "correction": "..." } ],
        "coaching": { "structure": "...", "improvement": "..." }
      },
      "delivery": {
        "nonverbal": { "eyeContactLevel": "GOOD", "postureLevel": "AVERAGE", "expressionLabel": "..." },
        "vocal": { "fillerWords": ["음","어"], "fillerWordCount": 5, "speechPace": "FAST", "toneConfidenceLevel": "HIGH", "emotionLabel": "..." },
        "attitudeComment": { "positive": "...", "negative": "...", "suggestion": "..." }
      },
      "overallComment": "..."
    }
  ]
}
```

- **제거 필드**: `technicalFeedback`, `nonverbalFeedback`(dimensions[]/score/observation/evidenceQuote/status), 최상위 `fillerWordCount`.
- **유지 필드명**: `bestAnswer`, `questionType`(현행 enum 7종).
- 세션 피드백 응답(`SessionFeedbackResponse`): `dimensionScores`/strength·gap 의 `dimension`/`levelGap` 필드는 **BE·FE 양쪽에서 물리 제거**됨 (PR2 구현 중 사용자 결정 — "rubric 관련 전부 제거". 당초 "직렬화 유지/제거 비스코프" 계획에서 변경).

### Error
- 404: 피드백/질문세트 없음(기존 코드 유지).

## 단계 분리 (PR 분할 — 사용자 결정)

답변별 롤백(되돌리기, 검증 분명)과 세션 피드백 입력 재설계(신규 어댑테이션, 불확실성 ↑)를 **2개 PR 로 분리**한다.

### ★ 의존 함정 (Blocking)

PR1 이 `question_score*` DROP + 루브릭 채점기 삭제 시, 그 데이터를 읽는 세션 피드백(`SessionFeedbackInputAssembler`)이 **PR2 전까지 데이터 소스를 잃는다**. → **PR1 에서 세션 피드백 생성을 임시 중립화**(아래)해야 부팅/런타임이 깨지지 않는다.

### PR1 — 답변별 롤백 (Epic 핵심)
- BE: `feedback/rubric/**`·`feedback/score/**`·infra/ai 루브릭 어댑터 DELETE, `TimestampFeedback`/`TimestampFeedbackResponse`/`SaveFeedbackRequest`/`QuestionSetFeedbackResponse`/`TimestampFeedbackMapper` REVERT, `FeedbackService` ADAPT, `V54` 마이그레이션.
- **세션 피드백 임시 중립화**: `SessionFeedbackEventListener`/`SessionFeedbackInputAssembler` 가 삭제된 `question_score*`·루브릭 클래스에 의존 → PR1 에서 (a) 세션 피드백 생성 경로를 비활성(리스너 no-op 또는 early-return) + FE 세션 모달/FAB 진입점 임시 숨김. `session_feedback` 테이블·기존 데이터는 보존.
- Lambda: 비언어 평면 필드 출력 복원(SDK 유지), `dimension_validator.py` DELETE, `verbal_analyzer.py` RESTORE.
- FE: 2탭(Content/Delivery) 복원 + 루브릭 컴포넌트 DELETE/RESTORE.
- 완결: product-spec Goal 1·2·4·5 (답변별 2탭 / 응답 main 형태 / dev 루브릭 테이블·row 부재 / 무관 기능 회귀 없음).

### PR2 — 세션 피드백 입력 재설계
- `SessionFeedbackInputAssembler`/`SessionFeedbackInput`/`TurnScoreView`/`SessionFeedbackParser`/`SessionFeedbackSynthesizer` + 프롬프트를 `timestamp_feedback` 코멘트 기반으로 재작성 → 세션 피드백 생성 재활성 + FE 모달/FAB 재노출.
- 완결: product-spec Goal 3 (세션 피드백 루브릭 용어 없이 서술형).
- 별도 tech-spec 또는 본 spec PR2 섹션 확장(implement 단계 결정).

## Verification (완료 판정)

### PR1
- [ ] **DB**: V54 적용 시 `timestamp_feedback` 14컬럼 존재, `question_score*` 테이블 부재 — Repository/Testcontainers 통합 테스트.
- [ ] **BE**: `TimestampFeedbackResponse.from(feedback)` content/delivery 반환 (Domain Unit). `FeedbackService` 코멘트 저장 후 조회 시 main 형태 응답 (Service Integration, 외부 API Mock). 루브릭 클래스 삭제 후 컴파일·ArchUnit 통과.
- [ ] **BE 세션 중립화**: 세션 피드백 생성 비활성 상태에서 인터뷰 완료 이벤트가 예외 없이 처리됨 (부팅 Smoke + Service Integration). 기존 `session_feedback` 데이터 보존.
- [ ] **Lambda**: `cd lambda/analysis && pytest` — 비언어 평면 필드 출력 검증, dimension 테스트 제거/대체.
- [ ] **빌드/린트**: `./gradlew build`, `npm run build && npm run lint`, `npm run test`.
- [ ] **FE**: Content/Delivery 2탭 렌더, 차원 점수 카드 부재 (vitest 재작성). 세션 모달/FAB 임시 숨김 확인.
- [ ] **관찰(dev)**: 인터뷰 1회 완주 → `question_score*` 신규 row 0건 + 화면 2탭 정상.
- [ ] **회귀**: 질문풀 어드민·이력서 트랙 흐름 통합 테스트. 인터뷰 생성/진행/follow-up E2E.

### PR2
- [ ] **BE 세션**: 인터뷰 완료 → `session_feedback` 적재(코멘트 기반 입력), 응답에 루브릭 차원명/점수 텍스트 부재 (Service Integration).
- [ ] **FE**: 세션 모달 재노출, 점수 행 부재, 강점/약점/계획 서술형 렌더.

## Pre / Post State

### Pre (develop)
- 답변별 = 루브릭 차원 점수 카드. `timestamp_feedback` 코멘트 컬럼 부재. `question_score*` 테이블 존재. `RubricScorer` LLM 포트 + 이벤트 채점.
- 세션 피드백 입력 = question_score 차원.
- FE 단일 ContentTab + RubricDimensionCard. Lambda nonverbalScore dimensions 출력.

### Post — PR1
- 답변별 = main 코멘트형(content/delivery). `timestamp_feedback` 14컬럼 재추가. `question_score*` 삭제. 루브릭 채점기/이벤트 리스너/infra 어댑터 삭제.
- 세션 피드백 = **생성 임시 비활성** + FE 모달/FAB 숨김. `session_feedback` 테이블·데이터 보존.
- FE 2탭(Content/Delivery) + 복원 컴포넌트. Lambda 평면 비언어 출력(SDK 유지).

### Post — PR2
- 세션 피드백 입력 = 코멘트형 재설계 + 생성 재활성. 차원점수 텍스트 부재. FE 모달/FAB 재노출, 점수 행 제거.

## 위험 / 마이그레이션 / 롤백

- **위험 1 (데이터 손실, 의도됨)**: `question_score*` DROP = dev 차원 점수 영구 삭제. 사용자 승인됨(dev 전용). prod 무관.
- **위험 2 (NULL 허용)**: 재추가 14컬럼은 기존 row NULL. main 응답 변환이 NULL 허용(원래 NULL 스키마) → 조회 시 N/A 처리 정상.
- **위험 3 (Lambda↔BE 계약 동시성)**: Lambda 가 평면 필드로 POST 하기 전 BE 가 코멘트 컬럼 기대 시 저장 실패. → **Lambda 출력 복원 + BE 저장 복원 + V54 를 같은 BE 머지에 묶거나, BE 가 신규 컬럼 NULL 허용으로 먼저 머지 후 Lambda 머지**. (구현 순서 = implement-be 명시.)
- **위험 4 (이벤트 미소비)**: `AnswerAnalysisCompletedEvent` 발행 유지·소비자 삭제 시 무해(미소비 이벤트). 발행처(`interview/FollowUpService`,`FollowUpTransactionHandler`)까지 제거할지 = 분기 결정에서 "발행 유지" 채택(인터뷰 도메인 cross-domain 변경 회피).
- **위험 5 (PR1 세션 중립화 누락)**: PR1 에서 `question_score*` 삭제 후 세션 피드백 생성을 비활성하지 않으면, 인터뷰 완료 이벤트 처리 시 삭제된 클래스/테이블 참조로 런타임 예외. → PR1 Verification "세션 중립화" 항목으로 강제 검증.
- **롤백 시나리오**: 본 PR 자체가 롤백. 실패 시 develop 재배포 + V54 미적용 환경 유지(V54 미실행 시 기존 develop 스키마 유지). prod 미적용이라 운영 영향 0.

## 분기 결정

- [x] **BE 선행 강제 (강결합)** → 각 PR 내 `implement-be.md` 머지 후 `implement-fe.md`.
  - 사유: DB 마이그레이션(V54) + 응답 contract REVERT + Lambda 출력형식 변경이 FE 렌더의 데이터 전제. BE/Lambda 가 코멘트형 데이터를 내보내야 FE 2탭이 의미 있음.
- **PR 분할**: PR1(답변별 롤백) → PR2(세션 입력 재설계). 단계 분리 섹션 참조. PR1 머지·검증 후 PR2 착수.
- **Lambda** = PR1 BE 트랙 포함(`implement-be.md` 내 Lambda 섹션 또는 `tasks/be-lambda-*`). BE 저장 계약과 동시 정합 필요.
- **FE** = 각 PR 내 BE 머지 후 시작. contract 는 main 기지(旣知) 형태라 mock 진행 가능하나, 강결합 위험 3 때문에 BE 선행 권장.
- 분리 임계: PR1 REVERT/DELETE/RESTORE 파일 다수(BE 30+, FE 15+, Lambda 8+) → `tasks/` 폴더 분리 권장(implement 단계 결정).
