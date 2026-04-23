# Plan 01: Intent Classifier (M2 축소판)

> 상태: Draft
> 작성일: 2026-04-20
> 주차: W1
> 원본: `docs/todo/2026-04-20/01-m2-intent-classifier.md`

## Why

사용자 발화를 모두 "답변"으로 간주하는 현재 구조 탓에 "질문이 이해 안 가요" / "모르겠어요" 같은 비(非)답변 발화에도 기계적으로 꼬리질문이 발사된다 → 취조 톤 + 대화 단절. 꼬리질문 생성 파이프라인의 **전단 게이트**로 의도 분류기를 삽입하면, 이 아픈 지점이 즉시 해소되고 뒤따르는 M1/M3 품질도 레벨업된다.

TODO 원본의 5-intent(ANSWER/CLARIFY_REQUEST/GIVE_UP/META/OFF_TOPIC) 중 **META/OFF_TOPIC은 ANSWER로 병합**. 이유: (a) 발생 빈도 낮아 학습 케이스 확보 어려움, (b) 오분류 시 ANSWER fallback이 안전함, (c) W1 출시 속도가 정확도보다 중요.

### REMEDIATION 반영 (critic M3)
META ("시간 얼마 남았어요?") 나 OFF_TOPIC을 ANSWER로 병합했을 때 빈 답변 분석 → 엉뚱한 꼬리질문 발사 리스크가 있음. 이를 막기 위한 **2차 가드**를 plan-02 Answer Analyzer에 명시적으로 둔다:
- plan-02의 `answer_quality <= 1 AND claims.length == 0` 이면 `recommended_next_action = "CLARIFICATION"` 강제 → plan-03 Step B가 꼬리질문 대신 재설명 경로로.
- MANUAL_AB_PROTOCOL.md 수동 비교 세션에 META / OFF_TOPIC 시나리오 포함해 failure case 발견 시 즉시 5-intent로 확장 검토.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/prompts/template/intent-classifier.txt` | 신규. 3-intent 분류 프롬프트 |
| `backend/src/main/resources/prompts/template/clarify-response.txt` | 신규. CLARIFY 분기용 재설명 프롬프트 |
| `backend/src/main/resources/prompts/template/giveup-response.txt` | 신규. GIVE_UP 분기용 (SCAFFOLD or REVEAL_AND_MOVE_ON) |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/IntentClassifierPromptBuilder.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ClarifyResponsePromptBuilder.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/GiveUpResponsePromptBuilder.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/interview/vo/IntentType.java` | 신규 enum (ANSWER/CLARIFY_REQUEST/GIVE_UP) |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/IntentClassifier.java` | 신규 service. plan-00b의 `AiClient.chat(ChatRequest)` 사용 |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` | **수정 (기존 클래스 — `InterviewTurnService` 아님)**. `generateFollowUp()` 진입부에 `intentClassifier.classify()` 분기 삽입. 단일 경로(상시 활성) — runtime toggle 없음 |

## 상세

### JSON 계약 (분류기 출력)
```json
{
  "intent": "ANSWER | CLARIFY_REQUEST | GIVE_UP",
  "confidence": 0.0-1.0,
  "reasoning": "한 줄"
}
```

### 분기 라우팅 (의사 코드)
```java
IntentResult intent = intentClassifier.classify(mainQ, userUtterance, recentDialogue);
// 신뢰도 임계치는 application.yml 상수로 고정 (Feature Flag 없음)
if (intent.getConfidence() < intentClassifierProperties.getFallbackOnLowConf()) intent.forceAnswer();
switch (intent.getType()) {
  case CLARIFY_REQUEST: return clarifyResponseHandler.handle(session);
  case GIVE_UP:         return giveUpHandler.handle(session);
  case ANSWER: default: return followUpPipeline.process(session, userUtterance);
}
```

### 모델 파라미터
- Primary: GPT-4o-mini / Fallback: Claude Haiku
- temperature: **0.1** (분류 태스크)
- max_tokens: 200
- Prompt Caching: 시스템 프롬프트 전체 (정적)

### 설정 (application.yml 상수)
```yaml
rehearse:
  intent-classifier:
    fallback-on-low-conf: 0.7
```

Feature Flag runtime toggle은 사용하지 않는다. 기본 활성화 경로로 단일화. 모델 및 설정 변경은 `application.yml` 직접 수정 후 배포.

## 전제 (Phase 0 선행 필수)
- plan-00a `IMPACT_MAP.md` 에서 `FollowUpService.generateFollowUp()` 실제 시그니처 확인 후 분기 삽입 위치 확정
- plan-00b 의 `AiClient.chat(ChatRequest)` 범용 메서드로 호출. `callType = "intent_classifier"` 태그
- 동시 답변 race: `InterviewRuntimeStateStore.update()` 가 `Caffeine.asMap().compute()` 로 동일 interviewId read-modify-write 를 직렬화 (plan-00c). 중복 요청 자체는 컨트롤러 idempotency 로 막음

## Aggregate Latency SLA (Interview Quality 스프린트 공용 규약)

본 스프린트의 턴 처리 파이프라인 전체 SLA. plan-01/02/03 모두 이 규약을 공유하며, 각 plan 의 개별 SLA 는 이 aggregate 안에 맞춰야 한다.

| 단계 | 호출 | 성격 | 개별 p95 | 누적 p95 |
|------|------|------|---------|---------|
| L1 Intent Classifier (plan-01) | 1회 LLM | 동기 (사용자 path) | ≤ 500ms | 500ms |
| L2 Answer Analyzer (plan-02) | 1회 LLM (intent=ANSWER 일 때만) | 동기 | ≤ 1500ms | 2000ms |
| L3 Follow-up Generator v3 (plan-03) | 1회 LLM | 동기 | ≤ 2000ms | **≤ 4000ms (Aggregate p95 상한)** |
| L4 Rubric Scorer (plan-08) | 1회 LLM | **비동기 post-turn (사용자 path 밖)** | — | 제외 |
| L5 Dialogue Compactor (plan-04) | 1회 LLM × ~0.15 트리거율 | 비동기 백그라운드 | — | 제외 |

- **Aggregate p95 ≤ 4,000ms** — 사용자가 답변 제출 → 다음 질문 수신까지. 이 값을 초과하는 plan 은 재설계 필요.
- intent=CLARIFY_REQUEST / GIVE_UP 분기에서는 L2/L3 생략 → p95 ≤ 1,500ms.
- Rubric Scorer 는 Event-driven 비동기 (plan-08 §호출 시점) 이므로 본 SLA 에 포함되지 않음.
- DialogueCompactor 는 백그라운드 (plan-04:57-61) 이므로 포함되지 않음. 단, 동기 fallback 발동 시에만 L3 에 +500ms 허용.

### 측정
- Micrometer 태그 `ai.call.chain=turn-pipeline` 으로 3개 호출 묶어 `rehearse.turn.pipeline.duration_seconds` 히스토그램 기록 (plan-00d 계측 인프라 활용)
- 각 plan 의 개별 SLA + aggregate SLA 동시 충족 여부를 배포 후 Grafana p95 로 확인

## 담당 에이전트

- Implement: `backend` — 프롬프트 + Builder + Classifier 서비스 + 분기 라우팅
- Review: `architect-reviewer` — 분기 레이어링(의도 분류가 follow-up 파이프라인 앞에 올바르게 위치했는지)
- Review: `code-reviewer` — 기존 `FollowUpService` 공개 API 변경 없음 확인, 3-intent 프롬프트 temperature/few-shot

## 검증

1. 수작업 골든셋 20개 (ANSWER 10 / CLARIFY 6 / GIVE_UP 4)로 confusion matrix 작성
2. OpenAI + Claude 양쪽에서 동일 결과(정확도 차이 ≤ 5%p) 확인
3. Intent 정확도 ≥ 90% (20개 기준 18/20 이상)
4. CLARIFY False Positive ≤ 3% (정상 답변을 CLARIFY로 오분류 ≤ 1/20)
5. Latency overhead ≤ 500ms (p95)
6. `progress.md` 01 → Completed
