# RubricScore 반영 — 기술피드백 / 언어 / 비언어

> 턴 1개가 어떻게 점수로 환원되어 사용자에게 노출되는지 추적. "기술피드백 / 언어 / 비언어" 3개가 같은 평면 아니라는 점부터 분리.

---

## 0. 핵심 헷갈림 분리

| 평면 | 값 | 의미 |
|------|-----|-----|
| 채점 방식 (axis A) | **언어 (verbal)** vs **비언어 (nonverbal)** | LLM 채점 vs 결정적 가중. 분리된 흐름 + 분리된 데이터 소스 |
| 질문 perspective (axis B) | **TECHNICAL / BEHAVIORAL / EXPERIENCE** | 질문 자체 분류. 언어 채점 rubric 선택 키 (비언어 무관) |

**"기술피드백"** = 별도 평면 X. axis B 의 TECHNICAL/EXPERIENCE perspective 질문에 적용된 **언어 rubric 결과**. 비언어 점수와 동급으로 비교하면 안 됨.

저장 모델은 1축화: 두 흐름 모두 같은 [`question_score`](schema/question_score.md) + [`question_score_dimension`](schema/question_score_dimension.md) 테이블에 기록되며 `rubric_id` 로 구분.

```
턴 1개 → question_score 행 N개 (rubric별 1개)
  ├─ 언어 rubric (resume-v1 / concept-cs-fundamental-v1 / experience-technical-v1 / ...) — LLM 채점
  └─ rubric_id="nonverbal" — 결정적 가중, Lambda 분석 입력
```

---

## 1. 언어 (Verbal) — LLM 채점

### 1.1 트리거

턴 완료 시 [`TurnCompletedEvent`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/rubric/event/TurnCompletedEvent.java) publish.

| 트랙 | Publisher |
|------|-----------|
| 표준 | [`FollowUpTransactionHandler.publishTurnCompletedEvent`](../../../backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpTransactionHandler.java) → `TurnCompletedEvent.ofStandard` |
| Resume | [`ResumeTurnEventPublisher.publish`](../../../backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeTurnEventPublisher.java) → `TurnCompletedEvent.ofResumeTrack` |

이벤트 페이로드: `interviewId / turnIndex / userId / questionId / questionSetId / userAnswer / analysis(AnswerAnalysis) / intent(IntentType) / userLevel / resumeMode? / currentChainLevel? / resumeSkeleton?`

### 1.2 리스너

[`RubricScoringEventListener.on`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScoringEventListener.java)
- `@Async(rubricScoringExecutor)` + `@TransactionalEventListener(phase = AFTER_COMMIT)`
- 실패 = listener 흡수 + `aiCallMetrics.incrementRubricFailure("persist_failed")`. 턴 진행 차단 X.

### 1.3 Rubric 선택 (axis B 라우팅)

[`RubricLoader.resolveFor`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricLoader.java) → [`RubricFamily.resolve`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/rubric/entity/RubricFamily.java)

매핑 룰 ([`_mapping.yaml`](../../../backend/src/main/resources/rubric/_mapping.yaml), 위→아래 첫 match):

| 우선순위 | 조건 | rubric |
|---------|------|--------|
| 1 | `resumeTrack=true` (Interview.interviewTypes ⊇ RESUME_BASED) | `resume-v1` |
| 2 | `category=CS_FUNDAMENTAL` | `concept-cs-fundamental-v1` |
| 3 | `category ∈ [LANGUAGE_FRAMEWORK, UI_FRAMEWORK]` | `concept-lang-framework-v1` |
| 4 | `category=BEHAVIORAL` | `experience-collaboration-v1` |
| 5 | `feedbackPerspective=EXPERIENCE` | `experience-technical-v1` ← **기술피드백 핵심** |
| 6 | `category=RESUME_BASED` | `resume-v1` |
| default | (no match) | `fallback-generic-v1` |

각 rubric YAML 정의: [`backend/src/main/resources/rubric/`](../../../backend/src/main/resources/rubric/)

### 1.4 Dimension 선택 (intent 분기)

[`Rubric.selectDimensions(intent, resumeMode)`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/rubric/entity/Rubric.java)

| intent / mode | 동작 |
|---------------|------|
| `CLARIFY_REQUEST` / `GIVE_UP` | 빈 dimension 리스트 → **persist skip** (정상) |
| `PLAYGROUND` / `INTERROGATION` / `WRAP_UP` (resumeMode) | `per_turn_rules` YAML 분기 (e.g. `on_playground_mode`, `on_wrap_up_mode`) |
| 그 외 | rubric 의 `uses_dimensions` 전체 |

### 1.5 LLM 호출 + 파싱

[`RubricScorer.score`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScorer.java) → [`RubricScorerPromptBuilder`](../../../backend/src/main/java/com/rehearse/api/infra/ai/prompt/RubricScorerPromptBuilder.java) (`gpt-4o-mini`, temp `0.2`) → [`RubricScoringAdapter`](../../../backend/src/main/java/com/rehearse/api/infra/ai/adapter/RubricScoringAdapter.java)
- Provider: GPT-4o-mini primary + Claude `claude-sonnet-4-20250514` fallback ([`ResilientAiClient`](../../../backend/src/main/java/com/rehearse/api/infra/ai/ResilientAiClient.java))
- 검증: SCORE_MIN=1, SCORE_MAX=3. 범위 밖 → null
- evidence_quote 누락 → 1회 schema retry → 실패 시 NA

### 1.6 저장

[`QuestionScorePersister.saveRubric`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/score/service/QuestionScorePersister.java)
- `question_score`: `rubric_id` = 매핑 결과, `feedback_perspective` = `Question.feedbackPerspective.name()` (TECHNICAL/BEHAVIORAL/EXPERIENCE)
- `question_score_dimension`: dimension 별 score(1~3) + observation + evidence_quote
- UNIQUE(`question_id`, `rubric_id`) → idempotent upsert

상세 흐름: [`api/score-turn.md`](api/score-turn.md)

---

## 2. 비언어 (Nonverbal) — 결정적 가중 채점

### 2.1 트리거

인터뷰 종료 후 Lambda 비언어 분석 → FE → POST `/feedback` ([`SaveFeedbackRequest`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/dto/SaveFeedbackRequest.java) `nonverbalScore` 페이로드).

언어와 다른 시점에 다른 데이터 소스로 진입. TurnCompletedEvent 경유 X.

### 2.2 채점 (LLM 없음)

[`NonverbalScorePersister.persistAll`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalScorePersister.java) → [`NonverbalRubricScorer.score`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalRubricScorer.java)

4 dimension 고정:

| dimension | 데이터 소스 (`_dimensions.yaml`) |
|-----------|------------------------------|
| `fluency` | `verbal.filler_word_count` |
| `confidence_tone` | `verbal.tone_label + verbal.speedVariance` |
| `eye_contact_posture` | `vision.gazeOnCameraRatio + vision.postureUnstableCount` |
| `composure` | 이전 턴 대비 fluency/confidence/eye_contact 비교 (difficulty ≥ medium) |

가중치 적용: [`NonverbalContextWeightsLoader.resolve(category, track, mode, difficulty)`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalContextWeightsLoader.java)
- `composureEnabled=false` → composure 점수 drop (null)
- `multiplier` → 후속 aggregate 단계에서 사용

### 2.3 저장

[`QuestionScorePersister.saveNonverbal`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/score/service/QuestionScorePersister.java)
- `question_score`: `rubric_id="nonverbal"` 고정. `feedback_perspective=null`. `level_flag=null`.
- `question_score_dimension`: 4개 (composureEnabled=false 면 3개) — observation/evidence_quote = null
- UNIQUE(`question_id`, `nonverbal`) idempotent

---

## 3. 기술피드백 — perspective 라우팅 + 분류

`feedback_perspective` 의 3가지 역할:

| 역할 | 위치 |
|------|------|
| Question 자체 분류 (질문 생성 시 결정) | [`Question.feedbackPerspective`](../../../backend/src/main/java/com/rehearse/api/domain/question/entity/Question.java) |
| 언어 rubric 선택 키 (axis B 라우팅) | `_mapping.yaml` 5번 룰 (EXPERIENCE → `experience-technical-v1`) |
| 후속 집계 라벨 | `question_score.feedback_perspective` 컬럼 — TECHNICAL/BEHAVIORAL/EXPERIENCE 그대로 저장 |

비언어 점수와 무관 (nonverbal row 의 perspective = null).

> "기술피드백 = 언어 채점 결과 중 TECHNICAL/EXPERIENCE perspective 행" — 비언어 점수와 다른 axis. 함께 평균 내거나 비교 X.

---

## 4. 후속 소비 — 사용자 노출 경로

### 4.1 QuestionSet 단위

[`QuestionSetService.getFeedback`](../../../backend/src/main/java/com/rehearse/api/domain/questionset/service/QuestionSetService.java)
- `rubric_id="nonverbal"` 필터링 → **언어 점수만** 노출
- 비언어는 별도 표현 ([`TimestampFeedbackMapper`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/mapper/TimestampFeedbackMapper.java) commentBlock 등)

### 4.2 Session 종합

[`SessionFeedbackInputAssembler.assembleWithDelivery`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/session/synthesis/SessionFeedbackInputAssembler.java)

| 분기 | 입력 | 출력 |
|------|------|------|
| 언어 | `rubric_id != "nonverbal"` 행 | `scoresByCategory: Map<rubricId, Map<dimension, avg>>` → [`SessionFeedbackSynthesizer`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/session/synthesis/SessionFeedbackSynthesizer.java) LLM 입력 |
| 비언어 | `rubric_id == "nonverbal"` 행 | [`NonverbalDeliveryAggregate`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/session/synthesis/SessionFeedbackInput.java): 4 dimension 평균 + 최저 dimension + [`NonverbalImprovementActionsLoader`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalImprovementActionsLoader.java) 권장 액션 + `averageContextMultiplier` |

최종 산출물 → [`SessionFeedback`](schema/session_feedback.md) `payload_json` (DeliverySection / StrengthItem / GapItem / WeekPlanItem 등).

---

## 5. 데이터 흐름 한눈에

```
[턴 완료]                                              [인터뷰 종료 후]
     │                                                       │
     ▼                                                       ▼
TurnCompletedEvent (AFTER_COMMIT, async)        POST /feedback (nonverbalScore)
     │                                                       │
     ▼                                                       ▼
RubricScoringEventListener                       NonverbalScorePersister
     │                                                       │
     ├─ RubricLoader.resolveFor                              │
     │   (resumeTrack/category/perspective → rubric)         │
     ├─ Rubric.selectDimensions(intent, mode)                │
     │   (CLARIFY/GIVE_UP → skip)                            │
     ├─ RubricScorer → gpt-4o-mini (fallback claude)         │
     ├─ RubricScoringAdapter (1~3 검증)                      │
     │                                                       │
     ▼                                                       ▼
QuestionScorePersister.saveRubric                QuestionScorePersister.saveNonverbal
     │                                                       │
     └────────────► question_score (rubric_id 별 분리) ◄─────┘
                          │
                          ▼
              question_score_dimension (1:N)
                          │
       ┌──────────────────┴──────────────────┐
       ▼                                     ▼
QuestionSetService.getFeedback      SessionFeedbackInputAssembler
(nonverbal 제외)                    (언어/비언어 분리 집계)
       │                                     │
       ▼                                     ▼
   타임스탬프 피드백 응답           SessionFeedbackSynthesizer LLM
                                             │
                                             ▼
                                   SessionFeedback (payload_json)
```

---

## 6. 헷갈리는 포인트 정리

| 헷갈림 | 사실 |
|--------|------|
| "기술 / 언어 / 비언어" 3평면 동급 | X. **언어 vs 비언어** = 채점 방식. **기술/협업/경험** = 질문 perspective (언어 안 분류 키) |
| `rubric_id="nonverbal"` 도 `_mapping.yaml` 룰 | X. `_mapping.yaml` `always_apply` 메타데이터만 존재. 실제 `RubricLoader.resolveFor` 는 언어 rubric 1개만 반환. 비언어는 별도 경로 ([`NonverbalScorePersister`](../../../backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalScorePersister.java)) |
| CLARIFY/GIVE_UP 턴 score 누락 = 결함 | X. 정상 skip. dimension 빈 리스트 반환 → persist 자체 미실행 |
| 비언어 점수 = LLM 출력 | X. Lambda 분석 결과 페이로드 그대로 + multiplier. LLM 미관여 |
| `feedback_perspective` 가 비언어 행에도 채워짐 | X. nonverbal row 는 null 고정 |
| 언어 rubric `experience-technical-v1` = "기술 면접 전체" | X. `feedbackPerspective=EXPERIENCE` 매핑 (5번 룰). category 기반 매핑 (1~4) 가 우선 |
| 동일 (question, rubric) 재진입 시 중복 행 | X. UNIQUE(`question_id`, `rubric_id`) idempotent |

---

## 7. 관련 문서

- [`api/score-turn.md`](api/score-turn.md) — 언어 채점 endpoint 단위 상세 (timeout / retry / 메트릭)
- [`api/save-feedback.md`](api/save-feedback.md) — 비언어 진입점 (POST /feedback)
- [`schema/question_score.md`](schema/question_score.md) — 헤더 테이블 스키마
- [`schema/question_score_dimension.md`](schema/question_score_dimension.md) — dimension 테이블 스키마
- [`schema/session_feedback.md`](schema/session_feedback.md) — 종합 피드백 스키마
- [`glossary.md`](glossary.md) — 용어 매핑
