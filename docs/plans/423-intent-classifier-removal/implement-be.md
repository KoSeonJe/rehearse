# Implement (Backend) — IntentClassifier 전면 제거

> **작성자**: backend agent (Staff Engineer 페르소나 — Claude)
> **답하는 질문**: BE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★ / ★ FE 진입 = BE 머지 후 ★

---

## Phase 0: API Contract 확인

`tech-spec.md#api-contract` 확정 여부 확인. FE 합의 상태.

- [ ] Endpoint 경로 / 메서드 동일 유지 확인 (POST `/api/v1/interviews/{id}/follow-up` + Resume)
- [ ] Response schema 변화 = `type` 3종 미발행 + `intentBranch` 필드 제거 + `skip=true + presentToUser=true` 동시 케이스 소멸
- [ ] Error 코드 매핑 변화 0건

미합의 → 즉시 STOP. tech-spec 갱신 + 사용자 승인 재요청.

---

## Phase / Step 개요

| Phase | 제목 | 구현 | 예상 PR | 의존 |
|-------|------|------|--------|------|
| 1 | 제거 자산 일괄 삭제 (Service 10 + Entity 3 + DTO 1 + PromptBuilder 3 + Hints 2 + Layer 메서드 + SkeletonCallType + Config + yml 블록 + prompt template 3 + 테스트 11) | `backend` | #N | Phase 0 |
| 2 | 핵심 수정 (Pipeline / TextFallbackAnalyzer / AudioTurnAnalyzer + L1Guard 시그니처 / Orchestrator / FollowUpService / ResumeTurnEventPublisher) + 신규 `[진행차단진단]` WARN 로그 | `backend` | #N | Phase 1 |
| 3 | Prompt 흡수 지시 4개 파일 추가 + audio-turn-analyzer.txt schema 단순화 | `backend` | #N | Phase 2 |
| 4 | 테스트 단순화 14개 + LogCaptor 검증 + Smoke / 정적 grep / 빌드 / 회귀 | `backend` | #N | Phase 3 |

> 단일 PR 단일 머지. 분리 머지 = product-spec 폐기 옵션 (Trade-off Option C). Phase = PR 내 커밋 단위 분해.

---

## Phase 1: 제거 자산 일괄 삭제

- **구현**: `backend` — 사용처 없는 자산 우선 삭제 → Phase 2 수정 시 컴파일 에러 노출 최소화

### 변경 파일

**Service / Generator / Handler / Detector (11)**:
- `backend/src/main/java/com/rehearse/api/domain/interview/service/IntentClassifier.java` — 삭제
- `.../interview/service/IntentDispatcher.java` — 삭제
- `.../interview/service/IntentResponseHandler.java` — 삭제
- `.../interview/service/ClarifyResponseHandler.java` — 삭제
- `.../interview/service/ClarifyResponseGenerator.java` — 삭제
- `.../interview/service/OffTopicResponseHandler.java` — 삭제
- `.../interview/service/OffTopicResponseGenerator.java` — 삭제
- `.../interview/service/OffTopicEscalationDetector.java` — 삭제
- `.../interview/service/GiveUpResponseHandler.java` — 삭제
- `.../interview/service/GiveUpResponseGenerator.java` — 삭제

**Entity / DTO (4)**:
- `.../interview/entity/IntentType.java` — 삭제
- `.../interview/entity/IntentResult.java` — 삭제
- `.../interview/entity/IntentBranchInput.java` — 삭제
- `.../interview/dto/IntentBranchPayload` (record, `FollowUpResponse.java` 내) — 제거

**Prompt Builder / Hints / Layer / Enum / Config (8)**:
- `.../infra/ai/prompt/IntentClassifierPromptBuilder.java` — 삭제
- `.../infra/ai/prompt/ClarifyResponsePromptBuilder.java` — 삭제
- `.../infra/ai/prompt/GiveUpResponsePromptBuilder.java` — 삭제
- `.../infra/ai/context/FocusHints.java` — `IntentClassifierHints`, `ClarifyResponseHints` inner record 제거
- `.../infra/ai/context/FocusLayer.java` — `buildIntentClassifier(...)` 메서드 제거
- `.../infra/ai/context/SkeletonCallType.java` — `INTENT_CLASSIFIER` enum 값 제거
- `backend/src/main/java/com/rehearse/api/global/config/IntentClassifierProperties.java` — 삭제
- `backend/src/main/resources/application.yml` — `rehearse.intent-classifier:` 블록 (라인 67-71) 삭제. dev / prod 프로파일 yml 동일 블록 grep 후 제거

**Prompt template (3)**:
- `backend/src/main/resources/prompts/template/intent-classifier.txt` — 삭제
- `backend/src/main/resources/prompts/template/clarify-response.txt` — 삭제
- `backend/src/main/resources/prompts/template/giveup-response.txt` — 삭제

**테스트 직접 삭제 (11)**:
- `IntentClassifierTest`, `IntentDispatcherTest`, `IntentClassifierGoldenSetLiveTest`, `ClarifyResponseHandlerTest`, `ClarifyResponseGeneratorTest`, `OffTopicResponseHandlerTest`, `OffTopicResponseGeneratorTest`, `OffTopicEscalationDetectorTest`, `GiveUpResponseHandlerTest`, `GiveUpResponseGeneratorTest`, `FollowUpServiceIntentBranchTest` — 각 파일 삭제

### 핵심 로직

순수 삭제. 사용처 검증 = grep 0건 확인 후 진행 (사용처 잔존 시 Phase 2 의존 — 함께 진행).

### 의존

- 선행: Phase 0 (contract 합의)
- 외부: 없음

### Verification

- `./gradlew compileJava` — 컴파일 에러 발생 위치 = Phase 2 수정 대상 식별 (정상 신호)
- 삭제 자산 path 부재 확인

### 커밋 메시지

```
refactor(BE): IntentClassifier 관련 자산 일괄 삭제
```

---

## Phase 2: 핵심 수정 + WARN 로그

- **구현**: `backend` — Phase 1 컴파일 에러 영역 모두 수정 + 진행 차단 진단 로그 추가

### 변경 파일

**Pipeline / Analyzer (3)**:
- `backend/src/main/java/com/rehearse/api/domain/interview/service/TurnAnalysisPipeline.java` — `IntentClassifier.classify` 호출 제거. `AnswerAnalyzer.analyze` 만 호출. `TurnAnalysisResult` 단순화 (`record TurnAnalysisResult(String answerText, AnswerAnalysis answerAnalysis)`)
- `.../interview/service/TextFallbackTurnAnalyzer.java` — 동일. STT → AnswerAnalyzer 만
- `.../interview/service/AudioTurnAnalyzer.java` — `L1FalseNegativeGuard.applyL1FalseNegativeGuard(intent.type())` → `applyL1FalseNegativeGuard()` (intent 인자 제거). LLM 응답 파싱에서 intent 필드 제거

**Guard 시그니처**:
- `.../interview/service/L1FalseNegativeGuard.java` — `applyL1FalseNegativeGuard(IntentType)` → `applyL1FalseNegativeGuard()` 또는 answer 자체로 단순화. 기존 호출자 (AudioTurnAnalyzer) 만 영향

**Orchestrator / Service / Publisher (3)**:
- `.../resume/service/ResumeInterviewOrchestrator.java`:
  - line 11 `IntentDispatcher` import 제거
  - line 6-13 intent 관련 import 제거 (`IntentResult`, `IntentType`, `IntentBranchInput`)
  - line 41 `intentDispatcher` 필드 제거
  - line 82-86 `if (intent.type() != IntentType.ANSWER)` 분기 + `handleNonAnswerIntent` 호출 제거 → 항상 answer 흐름
  - line 109-113 `shouldSkipTurnCompletedEvent` 분기 = DEBUG → WARN 격상 + `[진행차단진단]` 포맷 적용 (interviewId, track="RESUME", stage=mode 이름, reason="publish-skip")
  - line 116 `turnEventPublisher.publish(...)` 의 `intent` 파라미터 제거
  - line 208-217 `handleNonAnswerIntent` 메서드 전체 삭제
  - line 226-233 `validateQuestionId` WARN 메시지 = `[진행차단진단]` 포맷 (reason="questionId-missing")
- `.../resume/service/ResumeTurnEventPublisher.java` — `publish(...)` 시그니처에서 `IntentResult` 파라미터 제거. 호출자 (Orchestrator) 동시 수정
- `.../interview/service/FollowUpService.java`:
  - intent 분기 (intent != ANSWER → handleNonAnswerIntent) 제거 — line 79-100 영역
  - Standard 트랙 publish skip 분기 진입 시 `log.warn("[진행차단진단] interviewId={} track=STANDARD stage={} reason={}", ...)` 추가
  - 관련 metrics increment 제거

**DTO**:
- `.../interview/dto/FollowUpResponse.java` — `IntentBranchPayload` record + `intentBranch()` 정적 팩토리 삭제. `presentToUser` 주석 정리

### 핵심 로직

```
TurnAnalysisPipeline.analyze:
  AnswerAnalysis analysis = answerAnalyzer.analyze(...)
  return new TurnAnalysisResult(answerText, analysis)

ResumeInterviewOrchestrator.processUserTurnInternal:
  TurnAnalysisResult turnResult = turnAnalysisPipeline.analyze(...)
  AnswerAnalysis analysis = turnResult.answerAnalysis()
  // intent 분기 제거 — 항상 dispatchByMode
  ...
  if (shouldSkipTurnCompletedEvent(handlerResult)) {
    log.warn("[진행차단진단] interviewId={} track=RESUME stage={} reason=publish-skip",
             interviewId, currentMode);
    return handlerResult.response();
  }
  validateQuestionId(...)  // 내부 WARN 도 [진행차단진단] 포맷
  turnEventPublisher.publish(interviewId, turnIndex, analysis, currentMode, ...)  // intent 인자 없음

FollowUpService.processFollowUp:
  // intent 분기 제거 — 항상 답변 처리
  if (publishSkip 조건) {
    log.warn("[진행차단진단] interviewId={} track=STANDARD stage=standard-followup reason={}", ...)
  }
```

### 의존

- 선행: Phase 1 (삭제 자산)
- 외부: 없음

### Verification

- `./gradlew compileJava` 통과 (Phase 1+2 합쳐 컴파일 성공)
- WARN 로그 포맷 = `@Slf4j` placeholder + key=value (한국어 헤더 + 영문 key) + 민감정보 부재 (`backend/.claude/rules/conventions.md` Logging 룰)
- 트랜잭션 어노테이션 위치 / propagation 변경 0건 (`grep -n "@Transactional" .../ResumeInterviewOrchestrator.java .../FollowUpService.java` 로 Pre/Post 비교)

### 커밋 메시지

```
fix(BE): 의도 분기 제거 + 진행 차단 진단 WARN 로그 추가
```

---

## Phase 3: Prompt 수정

- **구현**: `backend` — 면접관 페르소나 흡수 지시 + audio prompt schema 단순화

### 변경 파일

- `backend/src/main/resources/prompts/template/resume/resume-playground-responder.txt` — system 섹션 마지막 (응답 가이드 직전)에 흡수 지시 추가
- `.../template/resume/resume-chain-interrogator.txt` — 동일 (chain 맥락)
- `.../template/follow-up-concept.txt` — 동일 (개념 질문 맥락)
- `.../template/follow-up-experience.txt` — 동일 (경험 질문 맥락)
- `.../template/audio-turn-analyzer.txt` — intent 분류 단계 + 응답 schema 의 intent 필드 제거. STT + answer_analysis 만 남김

### 핵심 로직

흡수 지시 문구 (4개 파일 공통, 어조는 각 파일 컨텍스트 맞게 backend agent 조정):

```
[엣지 응답 처리 지시]
- 응시자가 "모르겠다" / "기억 안 난다" 류로 답하면 동일 질문 반복 금지.
  더 쉬운 각도로 환기하거나 다음 항목으로 자연스럽게 전환.
- 응시자가 면접 주제와 무관한 발화 시 면접 맥락으로 redirect 후 다음 항목 진행.
```

audio-turn-analyzer.txt schema 변경:

```
Pre 응답 schema:
{ "transcript": "...", "intent": "ANSWER|CLARIFY_REQUEST|GIVE_UP|OFF_TOPIC", "answer_analysis": {...} }

Post 응답 schema:
{ "transcript": "...", "answer_analysis": {...} }
```

### 의존

- 선행: Phase 2 (`AudioTurnAnalyzer` schema 파싱 코드 동시 정합 필요)
- 외부: OpenAI prompt 운영

### Verification

- 4개 prompt 파일 흡수 지시 섹션 1회씩 추가 확인 (`grep -l "엣지 응답 처리 지시" backend/src/main/resources/prompts/template/`)
- audio-turn-analyzer.txt 의 intent 키워드 grep → 0건
- 수동 검수 5건은 Phase 4 검증

### 커밋 메시지

```
feat(BE): 면접관 prompt 엣지 흡수 지시 추가 + audio prompt intent 단계 제거
```

---

## Phase 4: 테스트 단순화 + 검증

- **구현**: `backend` — 14개 영향 테스트 단순화 + 신규 `[진행차단진단]` 검증 + 회귀 / Smoke / 정적 grep / 빌드 / 수동 검수

### 변경 파일

**단순화 대상 (14)**:
- `FollowUpServiceTest`, `FollowUpServiceRubricEventTest`, `ResumeInterviewOrchestratorTest`, `ResumeTurnEventPublisherTest`, `TurnAnalysisPipelineTest`, `TextFallbackTurnAnalyzerTest`, `AudioTurnAnalyzerTest`, `FollowUpTransactionHandlerTest`, `RubricScorerTest`, `RubricScoringEventListenerTest`, `FocusLayerTest`, `FixedContextLayerTest` 외 2개 (실제 grep 후 식별)
- 각 테스트에서 intent 의존 제거 (Mock setup, fixture, 검증 라인). `TurnAnalysisResult` 생성 시 intent 인자 제거

**신규 / 보강**:
- `ResumeInterviewOrchestratorTest` — `[진행차단진단]` WARN 로그 검증 케이스 추가 (LogCaptor 또는 `OutputCaptureExtension`). 4개 키 (`interviewId`, `track=RESUME`, `stage`, `reason`) 모두 메시지 포함 단언
- `FollowUpServiceTest` — Standard 트랙 동등 케이스 (`track=STANDARD`)
- `TestFixtures.java` — `answerAnalysisStub()` / `followUpQuestionStub()` 팩토리 추가 (Service Integration LLM Mock 응답 stub. 기존 fixture 재사용 가능 시 신규 불필요)

### 핵심 로직

```java
// ResumeInterviewOrchestratorTest 신규 케이스 예시 (CavemanMode 외 코드 정상 작성)
@Test
@DisplayName("publish skip 분기 진입 시 [진행차단진단] WARN 로그를 4개 키와 함께 기록한다")
void should_logProgressBlockWarn_when_publishSkipBranchEntered() {
    // given: Mock(ResilientAiClient) → AnswerAnalysis stub + handlerResult skip=true 시나리오
    // when: orchestrator.processUserTurn(...)
    // then: LogCaptor 가 WARN level + "[진행차단진단]" prefix + interviewId + track=RESUME + stage + reason=publish-skip 모두 포함 검증
}
```

### 의존

- 선행: Phase 1+2+3 (삭제 + 수정 + prompt 모두 적용)
- 외부: Testcontainers MySQL (Service Integration), `LogCaptor` 라이브러리 또는 Spring Boot `OutputCaptureExtension`

### Verification

- [ ] `./gradlew test` 전체 통과
- [ ] `./gradlew test --tests "*Resume*"` `*FollowUp*` `*TurnAnalysis*` `*Audio*` `*Layer*` 통과
- [ ] **Smoke**: `./gradlew bootRun --args='--spring.profiles.active=local'` 부팅 성공 (yml 블록 동시 제거 후 unknown property 에러 0건)
- [ ] **정적 검증**: `grep -rEn "IntentClassifier|ClarifyResponse|OffTopicResponse|OffTopicEscalation|GiveUpResponse|IntentDispatcher|IntentResponseHandler|IntentResult|IntentType|IntentBranchInput|IntentBranchPayload|IntentClassifierHints|ClarifyResponseHints|IntentPayload|intentBranch|rehearse.intent-classifier|intent-classifier\.txt|clarify-response\.txt|giveup-response\.txt" backend/src backend/src/main/resources` → **0건**
- [ ] `./gradlew build` 통과 (Checkstyle / SpotBugs 등 정적 분석 포함)
- [ ] **수동 검수 5건** (dev 환경 Resume + Standard 트랙 각 5건 = 10건 권장, 트랙당 5건 필수) — `tech-spec.md#수동-검수` 표 통과

### 커밋 메시지

```
test(BE): IntentClassifier 제거 영향 테스트 단순화 + 진행 차단 진단 로그 검증 추가
```

---

## FE 와 통합 시점

- **BE 머지 직후** = FE 진입 신호. branch-pr.md 룰에 따라 BE PR 머지 → develop 동기화 → FE PR 생성
- BE 머지 직후 Issue #423 댓글로 FE 진입 알림 (담당자 / 시간 명시)
- BE 머지 ~ FE 머지 window 동안 FE 가 unknown type (`type` 이 3종 중 하나로 안 옴) 받으면 라벨 fallback (`?? '안내'`) 동작 — anomaly window 위험 mitigation (tech-spec 위험 섹션)

## 통합 Verification

- [ ] tech-spec.md `Verification > BE` 모든 항목 통과
- [ ] tech-spec.md `위험 / 마이그레이션 / 롤백` 의 zero-downtime 단언 = dev 배포 후 부팅 + 첫 turn 정상 처리 확인
- [ ] FE 통합 후 회귀 체크 (FE PR 머지 후 dev 환경 Resume + Standard 트랙 정상 진행 1건 이상)

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-backend` 실행 (구현 완료 직후 — 메인 세션 책임)
- [ ] FE 진입 시점 = `code-reviewer-frontend` 와 **병렬** 호출 안 함 (BE 선행 강제 = 시점 분리)
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `backend/.claude/rules/testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md `Pre / Post State` 섹션 기준)
