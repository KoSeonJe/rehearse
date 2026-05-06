# interview 도메인 용어집

> 한글 ↔ 영문 / 코드 식별자 / DB 컬럼 매핑. 모호 용어만 등록.

| 용어 (한) | 영문 / 코드 식별자 | 정의 | 참고 |
|---------|----------------|------|------|
| 면접 세션 | `Interview` / `interview` | 사용자가 시작한 모의면접 1회 단위 (1 row = 1 회) | `schema.md#interview` |
| 외부 식별자 | `publicId` / UUID | 외부 노출용 UUID. `@PrePersist` 시 1회 생성, 갱신 불가 | `schema.md#interview` |
| 면접 유형 | `InterviewType` / `interview_interview_types` | CS / 행동 / 이력서기반 / 언어프레임워크 등 다중 선택 enum | `schema.md#interview_interview_types` |
| 트랙 | `InterviewTrack` (`CS` / `RESUME` / `LANGUAGE`) | 정책 분기용 상위 분류. `Interview.getTrack()` 이 `interview_types` 로부터 도출 | `service/StandardFollowUpPolicy`, `ResumeTrackPolicy` |
| 라운드 | follow-up round | 한 메인 질문에 대한 꼬리질문 카운트. CS=2 / RESUME=7 | `api/follow-up.md` |
| 메인 질문 | `Question` (`questionType=MAIN`) | 질문세트의 첫 질문 | `domain/question` |
| 꼬리질문 | follow-up question (`questionType=FOLLOWUP`) | 직전 답변에 대한 추가 질문 | `api/follow-up.md` |
| 의도 | `IntentType` (`ANSWER` / `CLARIFY_REQUEST` / `GIVE_UP` / `OFF_TOPIC`) | 사용자 응답의 의도 분류 결과 | `service/IntentClassifier` |
| 의도 신뢰도 | confidence | 0..1. 0.7 미만이면 `forceAnswer` fallback | `application.yml:58` |
| OFF_TOPIC escalation | `OffTopicEscalationDetector` | 연속 3회 OFF_TOPIC → GIVE_UP 으로 격상 | `application.yml:60` |
| Step A | `AnswerAnalyzer` | 답변 분석 (claim / 모호도 / 다음 액션 권고) | `service/AnswerAnalyzer` |
| Step B | `FollowUpQuestionWriter` | 꼬리질문 생성 | `service/FollowUpQuestionWriter` |
| L1 FN 가드 | False Negative guard | claims 비고 quality≤1 이면 강제 CLARIFICATION override | `AnswerAnalyzer.applyL1FalseNegativeGuard` |
| 분석 권고 | `RecommendedNextAction` (`ASK_FOLLOWUP` / `CLARIFICATION` / `SKIP`) | Step A 결과 — Step B 호출 여부 결정 | `entity/RecommendedNextAction` |
| Perspective | `Perspective` | EXPERIENCE 모드에서 꼬리질문이 다룰 관점 | `entity/Perspective` |
| Asked perspectives | `AskedPerspectives` | 이전 라운드까지 다뤄진 perspective 집합 | `entity/AskedPerspectives` |
| Reference type | `ReferenceType` (`MODEL_ANSWER` / `GUIDE`) | 메인 질문 모드. CONCEPT vs EXPERIENCE 분기 | `domain/question` |
| 런타임 상태 | `InterviewRuntimeState` | DB 미사용. Caffeine in-memory POJO. claims / chain / playgroundTurns / mode 등 | `schema.md#interviewruntimestate` |
| 이력서 모드 | `ResumeMode` (`PLAYGROUND` / `INTERROGATION` / 등) | 이력서 트랙 FSM 상태 | `domain/resume` |
| 이력서 스켈레톤 | `ResumeSkeleton` | 이력서 PDF 추출 → 파싱한 구조 (프로젝트 / 경력 등) | `domain/resume` |
| 인터뷰 플랜 | `InterviewPlan` | 이력서 트랙 플래너 산출물 (질문 sequence 계획) | `domain/resume`, `interview_plan` 테이블 |
| 질문 생성 상태 | `QuestionGenerationStatus` (`PENDING` / `GENERATING` / `COMPLETED` / `FAILED`) | 비동기 질문 생성 진행 상태 | `schema.md#interview` |
| 면접 상태 | `InterviewStatus` (`READY` / `IN_PROGRESS` / `COMPLETED`) | 라이프사이클. 전이 매트릭스는 `canTransitionTo` | `schema.md#interview` |
| Tech stack | `TechStack` | 직무별 허용 기술 enum. NULL 시 직무 디폴트 (`getEffectiveTechStack`) | `entity/TechStack` |
| Hard turn cap | `HARD_TURN_CAP = 7` | RESUME 트랙 follow-up 하드 캡 | `service/ResumeTrackPolicy:14` |
| 답변 수 | `answerCount` | 분석 FAILED 가 아닌 question_set 의 question_answer COUNT | `api/list-interviews.md` |
| 주간 카운트 | `thisWeekCount` | Asia/Seoul 월요일 00:00 이후 생성된 면접 수 | `service/InterviewQueryService:67` |
| Skip remaining | skip-remaining | 진행 중 면접의 미응답 question_set 일괄 SKIPPED 처리 | `api/update-status.md` |
| TurnCompleted | `TurnCompletedEvent` | 답변 1턴 완료 이벤트. feedback.rubric 도메인이 listen | `domain/feedback/rubric/event` |

## 약어

| 약어 | 풀이 | 비고 |
|------|------|------|
| STT | Speech-to-Text | OpenAI Whisper (`WhisperService`) |
| TTS | Text-to-Speech | Google TTS (`infra/tts/`) — 응답 ttsQuestion 필드 |
| FSM | Finite State Machine | 이력서 트랙의 ResumeMode 흐름 |
| FN | False Negative | Step A 가 답변 부실인데 ANSWER 로 잘못 판정하는 케이스. L1 FN 가드가 보정 |
| FK | Foreign Key | 외래 키 |
| PK | Primary Key | 기본 키 |
| L1 / L3 / L4 | Context Engineering 레이어 | L1 caching / L3 compaction / L4 just-in-time. `application.yml:64-69` |
| AFTER_COMMIT | `@TransactionalEventListener(phase=AFTER_COMMIT)` | 이벤트 발행 정합성 강제 |
| VT | Virtual Thread | `vtExecutor` (`AsyncConfig`) — follow-up 비동기 실행 |
