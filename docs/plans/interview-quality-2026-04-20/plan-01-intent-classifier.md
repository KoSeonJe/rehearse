# Plan 01: Intent Classifier (4-intent)

> 상태: Draft
> 작성일: 2026-04-20 (2026-04-24 4-intent 로 확장)
> 주차: W1
> 원본: `docs/todo/2026-04-20/01-m2-intent-classifier.md`

## Why

사용자 발화를 모두 "답변"으로 간주하는 현재 구조 탓에 "질문이 이해 안 가요" / "모르겠어요" / "시간 얼마 남았어요?" 같은 비(非)답변 발화에도 기계적으로 꼬리질문이 발사된다 → 취조 톤 + 대화 단절. 꼬리질문 생성 파이프라인의 **전단 게이트**로 의도 분류기를 삽입하면 이 아픈 지점이 즉시 해소되고 뒤따르는 M1/M3 품질도 레벨업된다.

TODO 원본의 5-intent(ANSWER/CLARIFY_REQUEST/GIVE_UP/META/OFF_TOPIC) 를 **4-intent 로 확장**: `ANSWER / CLARIFY_REQUEST / GIVE_UP / OFF_TOPIC` (META 는 OFF_TOPIC 에 통합 — 둘 다 "질문에 대한 답이 아님" 본질 동일).

이유 (2026-04-24 결정):
- OFF_TOPIC 을 앞단에서 직접 분기하면 plan-02 Analyzer(L2) + plan-03 Follow-up(L3) 호출을 **완전히 생략** → 사용자 path p95 ≤ 500ms 보장.
- OFF_TOPIC handler 는 **LLM 호출 없는 템플릿 응답** 으로 구현 → 비용 0, 지연 최소, 톤 예측성 확보.
- 기존 3-intent 축소안은 META/OFF_TOPIC 누수 시 Analyzer 에서 빈 분석(`claims=[]`) 을 내는 LLM 호출 1회를 낭비했음. 앞단 분기로 제거.
- META 를 별도 5번째 intent 로 분리하지 않는 이유: 발생률 낮아 학습 케이스 부족, OFF_TOPIC 과 동일한 handler 응답(원 질문 재제시)으로 충분.

### REMEDIATION 반영 (critic M3 — 재정의)
M3 의 "ANSWER fallback 가드" 는 **L1 분류기 False Negative 안전망** 으로 목적 재정의(삭제 금지). OFF_TOPIC 을 앞단에서 분기하더라도 분류기가 OFF_TOPIC 을 ANSWER 로 놓칠 경우 빈 답변 분석이 꼬리질문으로 새어나갈 리스크가 남음. plan-02 Step A 의 가드 로직 `claims.length == 0 AND answer_quality <= 1 ⇒ recommended_next_action = "CLARIFICATION"` 은 그대로 유지.

MANUAL_AB_PROTOCOL.md 수동 비교 세션에 OFF_TOPIC 전용 시나리오(META/무관 발화 각 1건 이상) 포함해 분기 정확도 실측.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/prompts/template/intent-classifier.txt` | 신규. **4-intent** 분류 프롬프트 (ANSWER/CLARIFY_REQUEST/GIVE_UP/OFF_TOPIC) |
| `backend/src/main/resources/prompts/template/clarify-response.txt` | 신규. CLARIFY 분기용 재설명 프롬프트 (LLM) |
| `backend/src/main/resources/prompts/template/giveup-response.txt` | 신규. GIVE_UP 분기용 (LLM, SCAFFOLD or REVEAL_AND_MOVE_ON) |
| ~~`off-topic-response.txt`~~ | **생성 안 함**. OFF_TOPIC handler 는 LLM 미사용 (템플릿 조립만) |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/IntentClassifierPromptBuilder.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ClarifyResponsePromptBuilder.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/GiveUpResponsePromptBuilder.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/interview/vo/IntentType.java` | 신규 enum (ANSWER/CLARIFY_REQUEST/GIVE_UP/**OFF_TOPIC**) |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/IntentClassifier.java` | 신규 service. plan-00b의 `AiClient.chat(ChatRequest)` 사용 |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/OffTopicResponseHandler.java` | **신규**. LLM 호출 없음. 리드인 풀 + 원 질문 재제시 템플릿 조립. 자세한 사양은 아래 §OFF_TOPIC Handler |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` | **수정 (기존 클래스 — `InterviewTurnService` 아님)**. `generateFollowUp()` 진입부에 `intentClassifier.classify()` 분기 삽입. 단일 경로(상시 활성) — runtime toggle 없음 |

## 상세

### JSON 계약 (분류기 출력)
```json
{
  "intent": "ANSWER | CLARIFY_REQUEST | GIVE_UP | OFF_TOPIC",
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
  case CLARIFY_REQUEST: return clarifyResponseHandler.handle(session);         // LLM 1회
  case GIVE_UP:         return giveUpHandler.handle(session);                  // LLM 1회
  case OFF_TOPIC:       return offTopicResponseHandler.handle(session);        // LLM 0회 (템플릿)
  case ANSWER: default: return followUpPipeline.process(session, userUtterance); // L2+L3 LLM 2회
}
```

### OFF_TOPIC Handler (LLM-free 템플릿)

입력: 세션 컨텍스트 (현재 메인 질문, sessionId, turnIndex)
출력: `{객관적 리드인} {connector} {mainQuestion}` 형태의 단일 문자열 (FollowUpResponse DTO 와 동일 형식으로 래핑)

**톤 원칙 (2026-04-24 갱신)**: 겉치레 호응("감사합니다", "좋은 말씀이에요" 등) 금지. **객관적·중립적 문구** 로 발화가 질문 범위 밖임을 사실 지시하고, 원 질문 답변을 요청.

- **리드인 풀** (객관·중립, 4개 — 반복 체감 완화):
  - "방금 답변은 질문 주제에서 벗어난 것 같습니다."
  - "응답이 질문 범위 밖으로 보입니다."
  - "지금 내용은 현재 질문과 직접 관련이 없습니다."
  - "질문과 다소 다른 방향의 답변으로 판단됩니다."
- **Connector (고정)**: `"질문에 대한 답변을 적절히 해주세요."`
- **선택 방식**: `int idx = Math.floorMod(Objects.hash(sessionId, turnIndex), pool.size());` — 세션·턴 조합으로 결정적 선택. 단위 테스트 용이, 재현 가능.
- **TTS 경로**: 같은 문자열을 `ttsQuestion` 에 재사용 (별도 합성 없음).
- **포맷 예**: `"방금 답변은 질문 주제에서 벗어난 것 같습니다. 질문에 대한 답변을 적절히 해주세요. {mainQuestion}"`
- **메타 기록**: `followUpType = "OFF_TOPIC_REDIRECT"` 로 로그 태깅 (MANUAL_AB_PROTOCOL 비교에서 분기 확인용).
- **테스트**: 단위 테스트로 풀 내 모든 리드인 변형 + mainQuestion escape(큰따옴표/개행) 검증. LLM 호출 없으므로 통합 테스트 불필요.

#### OFF_TOPIC 턴 소비 정책 (2026-04-24 추가)

OFF_TOPIC 은 실질 답변이 아니므로 **라운드 카운터 소비 금지**. 다음 사용자 발화는 동일 `mainQuestion` 에 대한 재시도로 간주:

- `FollowUpTransactionHandler` 의 round count 증가 **안 함** (plan-00f `InterviewTurnPolicy.assertCanContinue(...)` 호출 skip). `StandardFollowUpPolicy.maxFollowUpRounds=2` 가 OFF_TOPIC 반복으로 조기 소진되는 것 방지.
- `InterviewRuntimeState.currentMainQuestion` **유지** — 질문 전환 금지.
- 대화 로그에는 OFF_TOPIC 발화 + handler 응답을 **기록** (다음 ANSWER 분석 시 L2 Analyzer 가 맥락 파악 가능 + MANUAL_AB 비교용).
- 다음 턴이 `intent = ANSWER` 로 분류되면 정상 파이프라인 (L1 → L2 Analyzer → L3 Follow-up Generator v3) 가 **원 mainQuestion + ANSWER 조합** 으로 동작 → 꼬리질문은 원 주제 기반으로 정상 생성.
- OFF_TOPIC 연속 반복 방지책: 동일 세션에서 **연속 3회 OFF_TOPIC** 감지 시 `followUpType = "OFF_TOPIC_ESCALATED"` 로 태깅하고 GIVE_UP handler 경로로 전환 (reveal_and_move_on). 임계치는 `application.yml` `rehearse.intent-classifier.off-topic-consecutive-limit: 3`.

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

#### 분기별 사용자 path SLA

| 분기 | LLM 호출 구성 | 누적 p95 |
|------|-------------|---------|
| **ANSWER** | L1 + L2 + L3 (500 + 1500 + 2000) | ≤ **4000ms** |
| **CLARIFY_REQUEST** | L1 + clarify handler LLM (500 + 1000) | ≤ **1500ms** |
| **GIVE_UP** | L1 + giveup handler LLM (500 + 1000) | ≤ **1500ms** |
| **OFF_TOPIC** | L1 only (handler 는 템플릿 조립, LLM 0회) | ≤ **500ms** |

- **Aggregate p95 ≤ 4,000ms** — 사용자가 답변 제출 → 다음 질문 수신까지. ANSWER 분기 상한. 이 값을 초과하는 plan 은 재설계 필요.
- OFF_TOPIC 분기는 L2/L3 전부 생략 + handler LLM 도 0회 → 가장 빠른 경로 (≤ 500ms).
- Rubric Scorer 는 Event-driven 비동기 (plan-08 §호출 시점) 이므로 본 SLA 에 포함되지 않음.
- DialogueCompactor 는 백그라운드 (plan-04:57-61) 이므로 포함되지 않음. 단, 동기 fallback 발동 시에만 L3 에 +500ms 허용.

### 측정
- Micrometer 태그 `ai.call.chain=turn-pipeline` 으로 3개 호출 묶어 `rehearse.turn.pipeline.duration_seconds` 히스토그램 기록 (plan-00d 계측 인프라 활용)
- 각 plan 의 개별 SLA + aggregate SLA 동시 충족 여부를 배포 후 Grafana p95 로 확인

## 담당 에이전트

- Implement: `backend` — 프롬프트 + Builder + Classifier 서비스 + OffTopicResponseHandler + 분기 라우팅
- Review: `architect-reviewer` — 분기 레이어링(의도 분류가 follow-up 파이프라인 앞에 올바르게 위치했는지, OFF_TOPIC 이 L2/L3 를 완전 bypass 하는지)
- Review: `code-reviewer` — 기존 `FollowUpService` 공개 API 변경 없음 확인, 4-intent 프롬프트 temperature/few-shot, OFF_TOPIC 템플릿 리드인 풀 결정론성

## 검증

1. 수작업 골든셋 **25개** (ANSWER 10 / CLARIFY 6 / GIVE_UP 4 / **OFF_TOPIC 5**) 로 confusion matrix 작성
   - OFF_TOPIC 5개 세부: META 형 3개 ("시간 얼마 남았어요?", "다음 질문 갈게요", "잠시만요 전화 왔어요") + 무관 발화 2개 ("배고프네요", "아 오늘 날씨 좋네요")
2. OpenAI + Claude 양쪽에서 동일 결과(정확도 차이 ≤ 5%p) 확인
3. Intent 전체 정확도 ≥ 90% (25개 기준 23/25 이상)
4. CLARIFY False Positive ≤ 3% (정상 답변을 CLARIFY 로 오분류 ≤ 1/25)
5. OFF_TOPIC 분류 정확도 ≥ 80% (5개 중 4개 이상 정분류)
6. OFF_TOPIC False Positive ≤ 5% (정상 ANSWER/CLARIFY/GIVE_UP 을 OFF_TOPIC 으로 오분류 ≤ 1/25)
7. Latency overhead: ANSWER 분기 ≤ +500ms (p95), OFF_TOPIC 분기 ≤ 500ms end-to-end (handler LLM 0회)
8. OffTopicResponseHandler 단위 테스트: 리드인 풀 전 변형 재현 + mainQuestion 특수문자 escape
9. `progress.md` 01 → Completed, M3 체크리스트 item 완료 처리
