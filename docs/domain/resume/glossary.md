# resume 도메인 용어집

> 한글 ↔ 영문 / 코드 식별자 / DB 컬럼 매핑. resume 트랙 FSM / chain 모델 용어가 외부에 모호 → 본 문서로 단일화.

## 용어

| 용어 (한) | 영문 / 코드 식별자 | 정의 | 참고 |
|---------|----------------|------|------|
| 골격 / 스켈레톤 | `ResumeSkeleton` / `resume_skeleton` | LLM 추출 이력서 정규화 구조 (projects / claims / implicitCsTopics / interrogationPriorityMap). 인터뷰 1:1 | `schema.md#resume_skeleton` |
| 인터뷰 플랜 | `InterviewPlan` / `interview_plan` | LLM Planner 산출 진행 계획 (ProjectPlan 리스트, priority 오름차순) | `schema.md#interview_plan` |
| 프로젝트 플랜 | `ProjectPlan` | InterviewPlan 의 단일 프로젝트 단위. priority + chains 보유 | `api/prepare-interview-plan.md` |
| 모드 / FSM 모드 | `ResumeMode` enum | resume 트랙 진행 단계. `PLAYGROUND` → `INTERROGATION` → `WRAP_UP` (단방향) | `api/process-user-turn.md#상태 전이` |
| Playground (플레이그라운드) | `PLAYGROUND` mode / `RESUME_PLAYGROUND` questionType | 대화 기반 탐색 단계. 사용자가 자유롭게 답변 → 4 boolean (a_covered / b_length_ok / c_signal / d_turn_limit) ≥ 2 시 Interrogation 전이 | `PlaygroundModeHandler` |
| Interrogation (심화) | `INTERROGATION` mode / `RESUME_INTERROGATION` questionType | chain 기반 심층 질문 단계. level 1..4 + chain switching | `InterrogationModeHandler` |
| WrapUp (마무리) | `WRAP_UP` mode / `RESUME_WRAP_UP` questionType | 회고 / 종료 단계. remainingMin ≤ 2 진입 | `WrapUpModeHandler` |
| Opener (오프너) | `RESUME_OPENER` questionType | 인터뷰 첫 질문. Playground 진입 시 1회 발행 (재사용 가능) | `api/start-session.md` |
| 후속 질문 | follow-up | 매 턴 사용자 답변 받아 다음 질문 발행. resume 트랙은 mode 별 question 발행 | `api/process-user-turn.md` |
| chain (체인) | `chain` / `chainId` (= `synthesizeChainId(projectId, topic)`) | Interrogation 의 단일 심화 줄기. 1 chain = 동일 주제로 LEVEL 1..4 진행 | `ChainStateTracker` |
| level (레벨) | `currentLevel` (1..4) | chain 내 심화 단계. 4 가 최대 (hardcoded cap) | `ChainStateTracker:66` |
| Level Stay (레벨 유지) | `LEVEL_STAY` action | LLM nextAction 결정 — 같은 level 에서 모호도 해소 후속 질문. 누적 ≥ 2 시 강제 LEVEL_UP | `LEVEL_STAY_MAX_TURNS=2` |
| Level Up (레벨 상승) | `LEVEL_UP` action | 다음 level 로 진입. level 4 도달 시 더 진입 안 함 | `ChainStateTracker.levelUp` |
| Chain Switch (체인 전환) | `CHAIN_SWITCH` action | 현 chain 완료 / level 4 LEVEL_STAY 한계 → 다음 chain (priority) | `resolveNextChain` |
| Chain 소진 | `RESUME_INTERROGATION_EXHAUSTED` | 모든 chain 완료 시 응답. 모드 변경 안 함 (409) | `api/process-user-turn.md` |
| Claim (주장) | `claim` / `claimId` | 이력서 텍스트에서 LLM 추출한 단일 주장 단위. chain 의 부모 노드 | `ResumeSkeleton.projects[].claims` |
| Project (프로젝트) | `project` / `projectId` | 이력서 단일 프로젝트 단위. claim / chain 의 그룹 부모 | `ResumeSkeleton.projects` |
| Implicit CS Topic | `implicit_cs_topic` | 이력서에서 암시적으로 도출된 CS 주제. confidence ≥ 0.3 만 채택 | `MIN_CONFIDENCE_THRESHOLD=0.3` |
| Hard Timeout | `hard-timeout-min` (yml) | WRAP_UP 진입 후 `elapsed ≥ duration + 10분` 시 강제 종료 | `ResumeModeTransitionPolicy.isHardTimeoutExceeded` |
| WrapUp 임계 | `wrap-up-threshold-min` (yml) | 잔여 시간 ≤ 2 분 시 WRAP_UP 전이 | `ResumeModeTransitionPolicy.advanceToWrapUpIfDue` |
| Fallback (폴백) — provider | provider fallback | OpenAI 실패 시 Claude 1회 시도 (`ResilientAiClient`). `CLIENT_ERROR / PARSE_FAILED` 는 진입 X | `ResilientAiClient` |
| Fallback (폴백) — questions | `ResumeFallbackQuestions` | 4종 한국어 폴백 문장 (OPENER / PLAYGROUND_RESPONDER / INTERROGATION / WRAP_UP). LLM 응답이 동일 시 WARN 로그 (LLM-side 폴백 감지 패턴) | Issue #408 A3 |
| Schema-hint Retry | `withSchemaRetryHint` | parse 실패 시 schema 예시를 prompt 에 추가해 1회 재호출 | `AiResponseParser.parseOrRetry` |
| Chain Hallucination Retry | `ResumeInterviewPlanAdapter.execute` | chain_id allowlist drop 후 missing 시 1회 재시도 → 실패 시 INVALID_PLAN | `infra/ai/adapter` |
| Runtime State | `InterviewRuntimeState` | Caffeine 캐시 (TTL 8h, maxSize 10k) 기반 세션 런타임. resume 트랙 mode / chain / counter 보유 | `RuntimeCacheConfig` |
| File Hash | `file_hash` (DB) / `FileHasher.sha256` | 이력서 파일 SHA-256 hex. 로그는 8자 prefix 마스킹 | `FileHasher` |
| Session Plan ID | `session_plan_id` (DB) / `sessionPlanId` | LLM Planner 산출 trace 식별자. DB UNIQUE 부재 | `interview_plan` V31 |
| Candidate Level | `candidate_level` (DB) / `CandidateLevel` enum | JUNIOR / MID / SENIOR. fallback=JUNIOR (DB CHECK 부재) | `resume_skeleton` |
| L1 False Negative Guard | `applyL1FalseNegativeGuard` | `claims=[] AND quality≤1 AND action!=CLARIFICATION` → 강제 CLARIFICATION (안전망) | `AnswerAnalyzer:92` |

## 약어

| 약어 | 풀이 | 비고 |
|------|------|------|
| FSM | Finite State Machine | resume 트랙 mode 전이 모델 |
| LLM | Large Language Model | OpenAI / Claude |
| STT | Speech-to-Text | resume 트랙 미사용 (text-only) |
| JSON_OBJECT | OpenAI `response_format=json_object` | resume 모든 LLM 호출 사용 |
| CASCADE | DB ON DELETE CASCADE | interview 삭제 시 resume_skeleton / interview_plan 동시 삭제 |
| PK / FK | Primary / Foreign Key | DB 식별자 / 참조 |
| TTL | Time To Live | Caffeine 캐시 만료 (8h) |
| LRU | Least Recently Used | Caffeine eviction 정책 (maxSize 10k 기준) |
| OOM | Out Of Memory | Caffeine maxSize 보호 |
