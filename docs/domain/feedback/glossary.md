# feedback 용어집

> 한글 ↔ 영문 / 코드 식별자 / DB 컬럼 매핑. 모호한 용어만 등록.

| 용어 (한) | 영문 / 코드 식별자 | 정의 | 참고 |
|---------|----------------|------|------|
| 세트 피드백 | `QuestionSetFeedback` / `question_set_feedback` | QuestionSet 1회 분석 헤더 | `schema/question_set_feedback.md` |
| 타임스탬프 피드백 | `TimestampFeedback` / `timestamp_feedback` | 영상 구간 = 1턴 단위 피드백 | `schema/timestamp_feedback.md` |
| 턴 채점 | `QuestionScore` / `question_score` | (질문, 루브릭) 1쌍 채점 헤더 | `schema/question_score.md` |
| 디멘션 점수 | `QuestionScoreDimension` / `question_score_dimension` | 채점 dimension 별 점수 + 근거 | `schema/question_score_dimension.md` |
| 종합 피드백 | `SessionFeedback` / `session_feedback` | 인터뷰 1회 종합 (PRELIMINARY → COMPLETE) | `schema/session_feedback.md` |
| 루브릭 | `Rubric` (record) | yaml 정의 채점 기준 묶음 | `RubricLoader` |
| 루브릭 패밀리 | `RubricFamily` | 매핑 규칙 (resumeTrack → category → perspective → default) | `RubricCatalog` |
| 디멘션 | `Dimension` / `_dimensions.yaml` | 채점 단위 항목 (clarity, depth, fluency 등) | `RubricLoader` |
| 컨텍스트 가중치 | `NonverbalContextWeights` | track_mode/category/difficulty 별 multiplier + composureEnabled | `nonverbal-context-weights.yaml` |
| 개선 액션 | `NonverbalImprovementAction` | avg<2.0 → level_1_to_2, else level_2_to_3 | `nonverbal-improvement-actions.yaml` |
| 비언어 점수 | rubric_id=`nonverbal` | fluency / confidence_tone / eye_contact_posture / composure | `NonverbalScorePersister` |
| 코멘트 블록 | `CommentBlock` (JSON) | 비언어 / 음성 / 종합 코멘트 구조체 | `TimestampFeedbackMapper` |
| 전달력 | `DeliverySection` | session-level 비언어 합산 narrative | `SessionFeedbackPayload` |
| 강점 / 약점 | `StrengthItem` / `GapItem` | session-level 분석 항목 | `SessionFeedbackPayload` |
| 주차 계획 | `WeekPlanItem` | 1주 단위 학습 계획 | `SessionFeedbackPayload` |
| 커버리지 | `coverage` (VARCHAR 64) | 분석 커버리지 라벨 | `SessionFeedback` |
| 종합 상태 | `SessionFeedbackStatus` | PRELIMINARY \| COMPLETE | `SessionFeedback` |
| 턴 완료 이벤트 | `TurnCompletedEvent` | 답변 1턴 완료 시 publish (rubric 채점 트리거) | `api/score-turn.md` |
| 종합 트리거 이벤트 | `DeliveryEnrichmentRequestedEvent` | 모든 QuestionSet resolve 시 publish | `api/save-feedback.md` |
| 인터뷰 완료 이벤트 | `InterviewCompletedEvent` | `InterviewCompletionService @Scheduled fixedDelay=30s` 발행 | `api/score-turn.md`, `api/get-session-feedback.md` |
| Watchdog | `SessionFeedbackWatchdog` | `@Scheduled fixedDelay=60s`, default cutoff=10분 | `schema/session_feedback.md` |
| 재시도 cool-down | `isRetryCoolingDown` | 60초 | `api/retry-delivery.md` |
| Lambda 재트리거 | `NoOpLambdaRetryTrigger` | 현재 모든 프로파일 no-op (#406 D1) | `api/retry-delivery.md` |

## 약어

| 약어 | 풀이 | 비고 |
|------|------|------|
| STT | Speech-to-Text | Whisper / Web Speech API |
| NA | Not Available | rubric scoring fallback (score=null) |
| LLM | Large Language Model | GPT-4o-mini primary / Claude `claude-sonnet-4-20250514` fallback |
