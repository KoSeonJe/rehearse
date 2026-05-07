# Tech Spec — IntentClassifier 전면 제거

> **작성자**: backend agent (Staff Engineer 페르소나 — Claude)
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement-be.md / implement-fe.md 진입 ★

---

## Why → Goal (1줄 미러)

면접 진행 차단 P0 결함 해소 — 의도 사전 분류 단계 제거 + 면접관 페르소나가 엣지 케이스 (모르겠다 / 주제 무관) 자연 흡수. (상세 = `product-spec.md`)

## Evidence

- **현재 구조**:
  - 진입점 3개 — `FollowUpService.processFollowUp` (Standard 트랙 메인), `ResumeInterviewOrchestrator.processUserTurn` (Resume 트랙), `TextFallbackTurnAnalyzer.analyze` (audio STT fallback).
  - 분류 단계 = `IntentClassifier.classify(...)` → `IntentResult` (4-intent: ANSWER / CLARIFY_REQUEST / GIVE_UP / OFF_TOPIC, confidence, reasoning, fallback).
  - 분기 = `IntentDispatcher.dispatch(intentType, IntentBranchInput)` → 4개 핸들러 중 매칭 (ANSWER 만 누락 — 정상 분기로 흐름).
  - non-answer 응답 = `FollowUpResponse.intentBranch(IntentBranchPayload)` (skip=true + presentToUser=true + type=`CLARIFY_REESTABLISH` / `OFF_TOPIC_REDIRECT` / `GIVE_UP_FALLBACK`).
  - audio 경로 = `AudioTurnAnalyzer.analyze` 가 단일 LLM 호출로 STT + intent + answer_analysis 통합 응답 (`audio-turn-analyzer.txt`). `L1FalseNegativeGuard.applyL1FalseNegativeGuard(intent.type())` 로 intent 사용.
  - FocusLayer / SkeletonCallType 에 `INTENT_CLASSIFIER` callType + `IntentClassifierHints` / `ClarifyResponseHints` 등록.
  - 설정 = `IntentClassifierProperties` (`rehearse.intent-classifier.fallback-on-low-confidence` / `off-topic-consecutive-limit`) + `application.yml:67-71` 블록.
- **테스트 영향** (25 파일):
  - 직접 삭제 대상 (11): `IntentClassifierTest`, `IntentDispatcherTest`, `IntentClassifierGoldenSetLiveTest`, `ClarifyResponseHandlerTest`, `ClarifyResponseGeneratorTest`, `OffTopicResponseHandlerTest`, `OffTopicResponseGeneratorTest`, `OffTopicEscalationDetectorTest`, `GiveUpResponseHandlerTest`, `GiveUpResponseGeneratorTest`, `FollowUpServiceIntentBranchTest`.
  - 단순화 대상 (다수): `FollowUpServiceTest`, `FollowUpServiceRubricEventTest`, `ResumeInterviewOrchestratorTest`, `ResumeTurnEventPublisherTest`, `TurnAnalysisPipelineTest`, `TextFallbackTurnAnalyzerTest`, `AudioTurnAnalyzerTest`, `FollowUpTransactionHandlerTest`, `RubricScorer/RubricScoringEventListener` 류, `FocusLayerTest`, `FixedContextLayerTest`.
- **FE 영향** — `frontend/src/types/interview.ts:262-267` (type literal), `components/interview/question-display.tsx:19-24` (라벨 매핑), `hooks/use-answer-flow.ts:359` (skip + presentToUser 분기 주석/로직).
- **컨벤션 / 룰**:
  - `backend/.claude/rules/conventions.md` — 트랜잭션 / 로깅 / DTO / Entity 직접 반환 금지.
  - `backend/.claude/rules/testing.md` — Service Integration Support + TRUNCATE @BeforeEach + Fixtures.
  - `frontend/.claude/rules/conventions.md` / `architecture.md` — type 강제 (`any` 금지), 컴포넌트 / 훅 분리.
- **추정 / 미확인**:
  - 면접관 페르소나 prompt (resume-playground-responder + resume-chain-interrogator + follow-up-concept + follow-up-experience) 에 흡수 지시 추가만으로 GIVE_UP / OFF_TOPIC 케이스 자연 처리 가능 — **가설**, 수동 검수 5건이 검증 게이트 (product-spec AC).
  - 사용자 발화 도중 `audio-turn-analyzer.txt` schema 변경이 기존 LLM 응답 캐시 / 메트릭에 미치는 영향 = 없음 (즉시 적용).

## Trade-offs

### Option A (채택): 즉시 일괄 제거 + 면접관 prompt 흡수 지시 추가
- 장점:
  - 코드 단순화 즉시 효과 — 분기 / 핸들러 / config / prompt 13+개 동시 정리.
  - 결함 원인 (분기 차단) 근본 해소 — publish 가 항상 발생.
  - LLM 호출 1회 / 턴 절감 (분류 호출 + audio 통합 prompt 단순화).
  - 회귀 면적 = 단일 머지 단위 → revert 단순.
- 단점:
  - 면접관 흡수 가설 검증 실패 시 즉시 사용자 노출 (수동 검수가 mitigation).
  - 24개 테스트 정리 비용 한 번에.
- 사유: product-spec 가 단일 phase 명시. 분리 머지 시 엣지 케이스 무처리 회귀 발생 (분류만 먼저 제거 시).

### Option B (폐기): feature flag 점진 비활성화
- 장점:
  - 운영 토글로 즉시 롤백 가능.
  - 양 코드 패스 비교 측정 가능.
- 폐기 사유:
  - 본 작업의 목적 = 분류 단계 **폐기**. flag 유지 = 양 코드 유지 = 코드 단순화 가치 무효.
  - 상시 ON 채택이 결국 결정 — flag 자체가 over-engineering.
  - feature flag 도입 비용 > revert PR 비용 (삭제 작업 특성).

### Option C (폐기): 분류 제거 PR + prompt 흡수 PR 분리 머지
- 장점:
  - 각 PR 리뷰 면적 작음.
  - 흡수 PR 단독 검증 사이클 가능.
- 폐기 사유:
  - 분류만 먼저 제거 머지 시 prompt 흡수 미적용 상태 = 사용자 노출 시 동일 질문 반복 회귀 (product-spec Approach 명시).
  - product-spec 가 단일 phase 단일 머지 단위 지정 — 분리 시 안전장치 (feature flag / 임시 fallback) 필요한데 그 자체가 Option B 와 동등 비용.
  - BE+FE 분리는 별개 (응답 schema 의존). 분류 제거와 prompt 흡수 = 동일 BE PR 동시.

## Architecture

### Pre (현재)

```
[Client: FE]
  ↓ POST /follow-up (Standard) / /resume/follow-up
[FollowUpService.processFollowUp] / [ResumeInterviewOrchestrator.processUserTurn]
  ↓
[TurnAnalysisPipeline.analyze (Resume)] / [TextFallbackTurnAnalyzer.analyze (audio fallback)]
  ↓
  IntentClassifier.classify(question, answer, exchanges)  ───→ OpenAI (intent-classifier.txt)
  ↓ IntentResult
  if intent == ANSWER → AnswerAnalyzer.analyze() else AnswerAnalysis.empty()
  ↓ TurnAnalysisResult
[Orchestrator]
  if intent != ANSWER:
    IntentDispatcher.dispatch(type, IntentBranchInput)
      → ClarifyResponseHandler / OffTopicResponseHandler / GiveUpResponseHandler
        → ClarifyResponseGenerator / OffTopicResponseGenerator / GiveUpResponseGenerator
          → OpenAI (clarify-response.txt / giveup-response.txt)
    → FollowUpResponse.intentBranch(IntentBranchPayload)
      (skip=true, presentToUser=true, type=CLARIFY_REESTABLISH / OFF_TOPIC_REDIRECT / GIVE_UP_FALLBACK)
    [publish 이벤트 미발행 — 데이터 적재 누락]
  if intent == ANSWER:
    dispatchByMode (Resume) / FollowUpQuestionWriter (Standard)
      → Playground/Interrogation/WrapUp Handler
    turnEventPublisher.publish(...)  → Question / question_score / rubric 적재

[AudioTurnAnalyzer.analyze]
  → OpenAI (audio-turn-analyzer.txt) 단일 LLM 통합 호출 (STT + intent + answer_analysis)
  → L1FalseNegativeGuard.applyL1FalseNegativeGuard(intent.type())  ←  intent 의존
```

### Post (구현 후)

```
[Client: FE]
  ↓ POST /follow-up / /resume/follow-up
[FollowUpService.processFollowUp] / [ResumeInterviewOrchestrator.processUserTurn]
  ↓
[TurnAnalysisPipeline.analyze (Resume)] / [TextFallbackTurnAnalyzer.analyze]
  ↓ AnswerAnalyzer.analyze() 만 호출  ───→ OpenAI (answer-analyzer.txt)
  ↓ TurnAnalysisResult (intent 필드 없음 → answerText + answerAnalysis)
[Orchestrator]
  → 항상 dispatchByMode (Resume) / FollowUpQuestionWriter (Standard)
    → Playground/Interrogation/WrapUp Handler
      → OpenAI (resume-playground-responder.txt / resume-chain-interrogator.txt 등 — 흡수 지시 추가됨)
  → turnEventPublisher.publish(...)  → Question / question_score / rubric 적재 (항상)

[AudioTurnAnalyzer.analyze]
  → OpenAI (audio-turn-analyzer.txt — intent 단계 제거된 schema)
  → L1FalseNegativeGuard.apply()  ← intent 인자 제거, answer 자체로 단순화

[Orchestrator + FollowUpService 공통 — 진행 차단 식별 로그 (신규)]
  publish 미발행 분기 (handlerResult.questionId == null + skip=true + presentToUser=false) 진입 시
    log.warn("[진행차단진단] interviewId={} track={} stage={} reason={}", ...)
    — track ∈ {RESUME, STANDARD}, stage ∈ {playground, interrogation, wrapup, standard-followup, audio-fallback}
    — reason = "publish-skip" 또는 "questionId-missing" 등 식별 키
```

### 데이터 흐름 변화 요약

- LLM 호출 횟수 (턴당, 정상 ANSWER 케이스):

| 트랙 | Pre | Post | 절감 |
|------|-----|------|-----|
| Standard (text) | 분류 1 + answer 1 + 후속질문 1 = **3** | answer 1 + 후속질문 1 = **2** | -33% |
| Resume (text, Playground/Interrogation/WrapUp) | 분류 1 + answer 1 + 모드핸들러 1 = **3** | answer 1 + 모드핸들러 1 = **2** | -33% |
| Standard audio (integrated) | 통합 1 (STT+intent+answer) + 후속질문 1 = **2** | 통합 1 (STT+answer) + 후속질문 1 = **2** | 0% (호출 수 동일, schema 단순화) |
| Standard audio fallback (STT 실패) | STT 1 + 분류 1 + answer 1 + 후속질문 1 = **4** | STT 1 + answer 1 + 후속질문 1 = **3** | -25% |

- non-answer 케이스 (Pre): 분류 1 + intent 응답 1 = 2 호출 + publish 미발행. Post: 동일 답변 분기 (위 표 동일) + publish 발행.
- 데이터 적재: 답변 처리 = 항상 publish → 누락 0%.
- FE 응답 type: `CLARIFY_REESTABLISH` / `OFF_TOPIC_REDIRECT` / `GIVE_UP_FALLBACK` 미발행.

## NF 결정 (11개)

| NF | 결정 | 근거 / 검증 |
|----|------|-----------|
| 영향 범위 | BE+FE | grep 결과 backend/src + frontend/src 양쪽 hit. 강결합 = BE 선행 |
| 정합성 | 트랜잭션 동일 | App Service 트랜잭션 경계 변화 없음. `TurnAnalysisPipeline.analyze` / `TextFallbackTurnAnalyzer.analyze` 메서드 시그니처만 단순화, `@Transactional` 어노테이션 위치 / propagation 동일 (`processUserTurn` 의 `NOT_SUPPORTED` 호출자 진입 패턴 유지). publish 이벤트 = `@TransactionalEventListener(AFTER_COMMIT)` 룰 동일 |
| 실시간성 | 향상 | LLM 호출 1회 절감 → 평균 응답 latency -25~33% 추정 (호출 표 참조) |
| 부하 | LLM 호출 절감 | 모드별 호출 표 (아래 "데이터 흐름 변화 요약" 참조). baseline = 분류 1 + answer 1 + 후속질문 1 = 3 호출 → 2 호출. 게이트 = 회귀 부재 (Service Integration 통과 + 수동 검수 latency 체감 회귀 0건) |
| 동시성 | 변경 없음 | 분류 단계 제거가 동일 자원 (interview row / runtime state) 동시 수정 모델 변경 없음. 기존 `InterviewRuntimeStateCache` lock 경계 동일 |
| 마이그레이션 | zero-downtime | DB 변경 0건. Prompt template = classpath resource → 부팅 시 로드 (런타임 캐시 부재 — `PromptTemplateLoader` 가 매 호출 read 또는 Spring 빈 단발 로드). 신규 인스턴스 부팅 시점에 새 prompt 적용. rolling deploy 안전. 검증 = dev 배포 후 부팅 로그 + 첫 turn 정상 처리 |
| 외부 의존 | OpenAI schema 영향 한정 | `audio-turn-analyzer.txt` 응답 schema breaking change (intent 필드 제거). 기존 `ResilientAiClient` 재시도 / fallback 모델 (Claude) 동일 schema 사용 = prompt 단일 소스 변경으로 양 모델 정합. in-flight 요청 영향 = 무 (요청 단위 stateless) |
| 보안 | 영향 없음 | 인증 / 인가 / 입력 검증 경로 변경 없음. SSRF / Injection 영역 미터치. 신규 WARN 로그에 민감정보 (token / PII) 포함 금지 — interviewId / track / stage / reason 만 |
| 관찰성 | metric 영향 한정 | 기존 `intent_classification_*` 류 metric 부재 확인 필요 — 없으면 dashboard / alarm 영향 0. 있으면 dashboard JSON / alarm 정의 동시 정리 (구현 단계 grep 검증). 신규 = `[진행차단진단]` WARN 로그 (P0-1 매핑) |
| 롤백 | revert PR 1회 | feature flag 부재. BE revert + FE revert 각 1 PR. DB 영향 없음 |
| 검증 | testing.md 매핑 | Service Integration (Resume + Standard turn) + Domain Unit (Pipeline / Audio / Layer) + Smoke (부팅) + 정적 grep + 수동 검수 5건 |

## Data Model

DB 스키마 변경 없음. `IntentType` / `IntentResult` / `IntentBranchInput` 모두 in-memory enum / record.

```
-- Flyway 신규 마이그레이션 없음
```

기존 DB 테이블 / 컬럼 / FK / Index 영향 없음.

## API Contract

### 변경 엔드포인트

기존 엔드포인트 시그니처 유지. **응답 schema 의 가능한 값 집합 축소**.

#### `POST /api/v1/interviews/{id}/follow-up` (Standard)
#### `POST /api/v1/interviews/{id}/resume/follow-up` (Resume)

### Response (200) — 변경 사항

`FollowUpResponse` 응답 필드:
- `type: string`
  - **Pre**: `EXPERIENCE` / `CONCEPT` / `RESUME_PLAYGROUND` / `RESUME_INTERROGATION` / `RESUME_WRAP_UP` / `RESUME_OPENER` / `RESUME_HARD_TIMEOUT` / `CONTEXT_BUDGET_EXCEEDED` / **`OFF_TOPIC_REDIRECT`** / **`CLARIFY_REESTABLISH`** / **`GIVE_UP_FALLBACK`**
  - **Post**: 위 목록에서 굵은 3종 제거.
- `presentToUser=true` + `skip=true` 동시 발생 케이스 (의도 분기) 사라짐. AI 자체 skip (답변 불충분) 케이스만 `skip=true, presentToUser=false` 로 잔존.
- `IntentBranchPayload` record + `intentBranch()` 팩토리 삭제 — 외부 직렬화 영향 없음 (응답 JSON 키 동일).

### Pre / Post 응답 JSON 비교

**Pre (의도 분기 케이스 — "잘 모르겠습니다" 입력 시)**:
```json
{
  "type": "GIVE_UP_FALLBACK",
  "question": "(흡수 발화)",
  "ttsQuestion": "(...)",
  "presentToUser": true,
  "skip": true,
  "intentBranch": { "intentType": "GIVE_UP", "confidence": 0.92, "reasoning": "..." }
}
```

**Post (동일 입력 시)**:
```json
{
  "type": "RESUME_INTERROGATION",   // 또는 EXPERIENCE / CONCEPT (트랙/모드별)
  "question": "(흡수 발화 — prompt 가 다른 각도 / 다음 항목 전환)",
  "ttsQuestion": "(...)",
  "presentToUser": true,
  "skip": false
  // intentBranch 필드 자체 부재
}
```

핵심 변화: `type` 이 모드 type 으로 통일, `skip=false`, `intentBranch` 필드 제거.

### Error

기존 동일. 추가 / 제거 없음.

### FE 영향 contract

- `frontend/src/types/interview.ts` 의 `FollowUpType` literal union 에서 3종 제거.
- `question-display.tsx` 의 라벨 매핑 entry 3종 제거.
- `use-answer-flow.ts` 의 `presentToUser=true + skip=true` 분기 제거 → AI 자체 skip 만 처리.

## Verification (완료 판정)

### BE

- [ ] **Service Integration**: `ResumeInterviewOrchestratorTest` (단순화) — Playground / Interrogation / WrapUp 모드 답변 → 다음 질문 + Question / question_score / rubric 적재 (TRUNCATE @BeforeEach + 실제 주입). 외부 LLM 만 Mock = `ResilientAiClient` mock bean (`@MockBean`) + answer-analyzer / responder prompt 응답 stub fixture (`TestFixtures.answerAnalysisStub()` / `TestFixtures.followUpQuestionStub()`).
- [ ] **Service Integration**: `FollowUpServiceTest` (단순화) — Standard 트랙 답변 → publish 이벤트 + 적재 검증. LLM Mock 정책 동일.
- [ ] **Domain Unit**: `TurnAnalysisPipelineTest` (단순화) — `AnswerAnalyzer` 호출만 검증.
- [ ] **Domain Unit**: `TextFallbackTurnAnalyzerTest` (단순화) — STT → AnswerAnalyzer 호출만 검증.
- [ ] **Domain Unit**: `AudioTurnAnalyzerTest` (단순화) — audio LLM 응답 schema (intent 없음) 검증.
- [ ] **Smoke**: `./gradlew bootRun --args='--spring.profiles.active=local'` 부팅 성공 (`IntentClassifierProperties` 빈 + `application.yml` 의 `rehearse.intent-classifier:` 블록 동시 제거 후 unknown property 에러 0건).
- [ ] **회귀**: `FocusLayerTest` / `FixedContextLayerTest` / `RubricScoringEventListenerTest` 등 25개 영향 테스트 모두 통과.
- [ ] **빌드**: `./gradlew build` 통과.
- [ ] **정적 검증**: `grep -rEn "IntentClassifier\|ClarifyResponse\|OffTopicResponse\|OffTopicEscalation\|GiveUpResponse\|IntentDispatcher\|IntentResponseHandler\|IntentResult\|IntentType\|IntentBranchInput\|IntentBranchPayload\|IntentClassifierHints\|ClarifyResponseHints\|IntentPayload\|intentBranch\|rehearse.intent-classifier\|intent-classifier\\.txt\|clarify-response\\.txt\|giveup-response\\.txt" backend/src backend/src/main/resources` → 0건.
- [ ] **로깅 룰**: 신규 `[진행차단진단]` WARN 로그 = `@Slf4j` placeholder + key=value (interviewId / track / stage / reason). 한국어. 민감정보 (token / PII) 부재 (`backend/.claude/rules/conventions.md` Logging).
- [ ] **트랜잭션 룰**: `TurnAnalysisPipeline.analyze` / `TextFallbackTurnAnalyzer.analyze` 어노테이션 변경 0건 — 호출자 (`processUserTurn`) 의 `NOT_SUPPORTED` 진입 패턴 유지 (`@Transactional(readOnly=true)` 기본 룰 적용 영역 동일).
- [ ] **관찰**: turn 처리 publish skip / questionId-missing 분기 진입 시 `log.warn("[진행차단진단] interviewId={} track={} stage={} reason={}", ...)` 형식 로그 1건 기록 (product-spec AC 8). 검증 = Service Integration `ResumeInterviewOrchestratorTest` / `FollowUpServiceTest` 의 skip fixture 케이스에서 `LogCaptor` 또는 `OutputCaptureExtension` 으로 WARN 메시지 + 4개 키 (interviewId / track / stage / reason) 모두 포함 단언.

### FE

- [ ] **빌드 / 린트**: `npm run build` / `npm run lint` 통과.
- [ ] **단위**: `use-answer-flow` 훅 테스트 — AI 자체 skip 케이스만 처리 (의도 분기 fixture 제거).
- [ ] **Integration**: `question-display` 컴포넌트 — `FollowUpType` 3종 제거 후 알 수 없는 type 수신 시 라벨 fallback (`?? '안내'`) 동작 + 정상 type 라벨 표시 검증.
- [ ] **정적 검증**: `grep -rEn "OFF_TOPIC_REDIRECT\|CLARIFY_REESTABLISH\|GIVE_UP_FALLBACK\|IntentBranch\|intentBranch" frontend/src` → 0건.

### 수동 검수 (product-spec AC 4-7)

5건 모두 통과 = 합격 라인. 1건이라도 실패 시 재작업 (product-spec AC).

| # | 카테고리 | 사용자 발화 (예시) | 기대 면접관 응답 패턴 | DB 적재 확인 (SQL) | 로그 확인 |
|---|---------|----------------|------------------|-----------------|----------|
| 1 | GIVE_UP | "잘 모르겠습니다" | 다른 각도 / 힌트 / 다음 항목 전환 발화 (직전 질문 동일 표현 반복 X) | `SELECT id, question_text FROM question WHERE interview_id=?` row 신규 1건 + `SELECT * FROM question_score WHERE question_id=?` 1건 + rubric row 매핑 1건 | `[진행차단진단]` WARN 로그 부재 |
| 2 | GIVE_UP | "기억이 안 나요 죄송합니다" | 동일 (다른 각도 또는 다음 항목) | 동일 | 동일 |
| 3 | OFF_TOPIC | "오늘 점심 뭐 드셨어요?" | redirect (예: "면접 주제로 돌아가서...") 또는 다음 항목 전환 | 동일 | 동일 |
| 4 | OFF_TOPIC | "이 회사 연봉이 얼마인가요?" | 동일 | 동일 | 동일 |
| 5 | 일반 | (직무 관련 정상 답변, 예: "Spring Boot 의 IoC 컨테이너는...") | 자연스러운 꼬리질문 또는 다음 항목 진행 | 동일 | 동일 |

- 검증 환경: dev 환경 (`54.180.188.135`) Resume 트랙 1회 + Standard 트랙 (audio fallback) 1회 = 사례 5건 × 2 트랙 = 10건 권장 (필수 = 트랙당 5건). 트랙별 모두 통과 시 합격.
- 통과 판정 = 위 표 3개 컬럼 (응답 패턴 / DB 적재 / 로그 부재) 모두 만족. 1개라도 실패 시 해당 case 실패.

## Pre / Post State

### Pre (현재)
- `backend/.../interview/service/`: IntentClassifier, IntentDispatcher, IntentResponseHandler, ClarifyResponseHandler, ClarifyResponseGenerator, OffTopicResponseHandler, OffTopicResponseGenerator, OffTopicEscalationDetector, GiveUpResponseHandler, GiveUpResponseGenerator (10개) 존재.
- `backend/.../interview/entity/`: IntentType, IntentResult, IntentBranchInput, TurnAnalysisResult.intent 필드 (+ IntentPayload).
- `backend/.../interview/dto/FollowUpResponse.java`: IntentBranchPayload + intentBranch() 팩토리.
- `backend/.../infra/ai/prompt/`: IntentClassifierPromptBuilder, ClarifyResponsePromptBuilder, GiveUpResponsePromptBuilder.
- `backend/.../infra/ai/context/`: FocusHints.IntentClassifierHints, FocusHints.ClarifyResponseHints, FocusLayer.buildIntentClassifier, SkeletonCallType.INTENT_CLASSIFIER.
- `backend/.../global/config/IntentClassifierProperties.java` + `application.yml:67-71` 블록.
- `backend/src/main/resources/prompts/template/`: intent-classifier.txt, clarify-response.txt, giveup-response.txt + audio-turn-analyzer.txt (intent 분류 단계 포함) + resume/resume-playground-responder.txt / resume-chain-interrogator.txt + follow-up-concept.txt / follow-up-experience.txt (흡수 지시 부재).
- `ResumeInterviewOrchestrator`: handleNonAnswerIntent + intentDispatcher 의존 + intent 분기.
- `ResumeTurnEventPublisher.publish(... IntentResult intent ...)`.
- `FollowUpService`: handleNonAnswerIntent + intent 분기.
- `AudioTurnAnalyzer`: L1FalseNegativeGuard 에 intent 인자 전달.
- `frontend/src/types/interview.ts`: FollowUpType literal 에 OFF_TOPIC_REDIRECT / CLARIFY_REESTABLISH / GIVE_UP_FALLBACK 포함.
- `frontend/src/components/interview/question-display.tsx`: 위 3종 라벨 매핑.
- `frontend/src/hooks/use-answer-flow.ts`: presentToUser=true + skip=true 의도 분기 분기.
- 25개 테스트 파일 intent 의존.

### Post (구현 후)
- 위 Service 10개 / Entity 3개 / DTO record 1개 / Prompt builder 3개 / Hints 2개 / Layer 메서드 1개 / SkeletonCallType 1개 / Config 1개 / yml 블록 1개 / Prompt template 3개 모두 **삭제**.
- `TurnAnalysisResult` 단순화 — `record TurnAnalysisResult(String answerText, AnswerAnalysis answerAnalysis)`.
- `TurnAnalysisPipeline.analyze` / `TextFallbackTurnAnalyzer.analyze` — AnswerAnalyzer 만 호출 (분기 / fallback 제거).
- `AudioTurnAnalyzer` — L1FalseNegativeGuard.apply() 인자 단순화. audio-turn-analyzer.txt prompt 에서 intent 분류 단계 + 해당 schema 필드 제거.
- `ResumeInterviewOrchestrator` — handleNonAnswerIntent 메서드 + intentDispatcher 필드 + intent 분기 if 제거. 항상 ANSWER 흐름.
- `ResumeTurnEventPublisher.publish(...)` — IntentResult 파라미터 제거.
- `FollowUpService` — handleNonAnswerIntent 메서드 + intent 분기 + 관련 metrics increment 제거.
- `ResumeInterviewOrchestrator.processUserTurnInternal` (line 109-114 분기) — 기존 DEBUG 격상 → WARN 으로 변경 + key=value 4개 (`interviewId`, `track=RESUME`, `stage`, `reason=publish-skip`). `validateQuestionId` 의 WARN 도 동일 포맷 (`reason=questionId-missing`).
- `FollowUpService` — Standard 트랙 publish skip 동등 분기에 동일 `[진행차단진단]` WARN 로그 추가 (`track=STANDARD`).
- `FollowUpResponse` — IntentBranchPayload record / intentBranch() 팩토리 삭제. presentToUser 주석 단순화.
- 4개 prompt 파일에 GIVE_UP / OFF_TOPIC 흡수 지시 섹션 추가:

  | 파일 | 추가 위치 | 추가 문구 (구현 단계 backend agent 가 어조 조정 가능 — 의미 유지) |
  |------|---------|-----------|
  | `resume-playground-responder.txt` | system 섹션 마지막 (응답 가이드 직전) | "응시자가 '모르겠다' / '기억 안 난다' 류로 답하면 동일 질문 반복 금지. 더 쉬운 각도로 환기하거나 다음 항목으로 자연스럽게 전환. 주제와 무관한 발화 시 면접 맥락으로 redirect 후 다음 항목." |
  | `resume-chain-interrogator.txt` | 동일 (system 섹션 마지막) | 동일 (chain 맥락에 맞춰 "다른 각도" 강조) |
  | `follow-up-concept.txt` | system 섹션 마지막 | 동일 (개념 질문 맥락) |
  | `follow-up-experience.txt` | system 섹션 마지막 | 동일 (경험 질문 맥락) |
  - 검증 게이트 = 수동 검수 5건 통과 (위 Verification 표). 실패 시 backend agent 가 prompt 어조 / 위치 조정 후 재검수.
- `frontend/src/types/interview.ts` — FollowUpType literal 3종 제거.
- `frontend/src/components/interview/question-display.tsx` — 라벨 매핑 3종 제거.
- `frontend/src/hooks/use-answer-flow.ts` — presentToUser=true + skip=true 분기 제거. 주석 정리.
- 25개 테스트 — 11개 삭제 + 14개 단순화.

## 위험 / 마이그레이션 / 롤백

- **위험**:
  - 면접관 페르소나 prompt 의 흡수 가능성 가설 — 검증 실패 시 사용자가 "모르겠다" 발화 시 면접관이 동일 질문 반복 회귀. **완화**: 수동 검수 5건 통과를 머지 게이트로 강제 (product-spec AC).
  - audio-turn-analyzer.txt schema 변경 — 기존 응답 캐시 / 메트릭 영향 (해당 캐시 / 메트릭 부재 확인됨, 영향 없음).
  - FE 가 BE 응답에서 type literal 3종 받지 않을 것을 가정 — BE 머지 후 FE 머지 사이 (window) 에서 FE 가 unknown type 받으면 라벨 미표기 (UX 손상). **완화**: BE 가 type 미발행 보장 + FE 가 unknown type 대비 fallback (`?? '안내'`) 유지.
- **마이그레이션 전략**:
  - BE 단일 PR 단일 머지 — 코드 + prompt template 동시.
  - FE 별도 PR — BE 머지 후 develop 동기화 후 시작 (branch-pr.md 룰).
  - DB 마이그레이션 없음 (zero-downtime).
- **롤백 시나리오**:
  - BE: revert PR 1회. flag 부재 (삭제 작업).
  - FE: revert PR 1회. literal 복원만으로 라벨 복구.
  - 운영 데이터 영향 없음 (DB 스키마 미변경).

## 분기 결정

이번 작업 = **BE+FE 강결합 (BE 선행 강제)**.

- [ ] 단일 영역 → `implement.md` 1개
- [ ] BE+FE 동시 → `implement-be.md` + `implement-fe.md` (API contract 합의 후 병렬)
- [x] **BE 선행 강제 (강결합)** → `implement-be.md` 머지 후 `implement-fe.md`

근거:
- FE 의 `FollowUpType` literal / 라벨 매핑 / skip+presentToUser 분기 = BE 응답 schema 의존.
- BE 가 type 미발행 보장 후 FE 정리 시 anomaly window 최소화.
- BE/FE PR 분리 룰 (`.claude/rules/branch-pr.md`) 정합.
