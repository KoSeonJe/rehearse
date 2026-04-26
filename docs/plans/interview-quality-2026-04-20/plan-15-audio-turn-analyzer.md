# Plan 15: Audio Turn Analyzer (4-call → 2-call 통합) `[blocking]`

> 상태: Draft
> 작성일: 2026-04-27
> 주차: W4 (S9, plan-01/02 supersede)
> 원본: dev 실측 (2026-04-26 docker logs + actuator/prometheus)

## Why

현 면접 한 턴 처리는 4개 LLM 호출 직렬:

```
audio → STT + 무의미한 Step B v1 (legacy)        6.8s
text  → IntentClassifier                         1.2s   (confidence=0.0 매번 → forceAnswer)
text  → AnswerAnalyzer Step A                    3.3s   (Claim deserialize 502 60% 발생)
text  → Step B v3                                4.2s
                                              = 15.5s 이론 / 6.07s 실측 (200) / 16.05s 재시도 (502)
```

dev 면접 1세션 5턴 모두 후속질문 0건 발생:
- 3턴: `502 AI_005` (`Claim` deserialize 실패 — AI 가 string 배열 반환, 코드는 객체 배열 기대)
- 2턴: `200 skip=true` (Step B 자체 skip 판단)
- Aggregate p95 SLA 4s 위반 (실측 6.07s)

근본 원인은 4-call 직렬 자체. plan-01:64-77 의 "STT 분리는 비용 추가" 가정은 단일 audio chat (gpt-4o-mini-audio-preview) 으로 STT + 분석을 통합하면 무효화된다.

**제안 구조 (2-call)**:
```
audio → AudioTurnAnalyzer (transcribe + intent + answer_analysis 통합) ~5s
text  → Step B v3 (그대로)                                              ~4s
                                                                     = ~5s endpoint
```

→ 6초 절감, LLM 호출 4→2 (50% 감소), 비용 50% 감소, 502 영구 차단 (`response_format = json_schema strict`), IntentClassifier confidence 0.0 자연 해소.

기존 `Claim`, `IntentResult`, `AnswerAnalysis` record 그대로 재사용. Step B v3 호출 흐름 변경 없음.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/prompts/template/audio-turn-analyzer.txt` | 신규. 통합 분석 프롬프트 (보안+intent+claims 동시 출력) |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/AudioTurnAnalyzerPromptBuilder.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/interview/dto/TurnAnalysisResult.java` | 신규 record. `(answerText, intent, answerAnalysis)` wrapper |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/AudioTurnAnalyzer.java` | 신규 service. audio + 메타 → `chatWithAudio` → 파싱 → `TurnAnalysisResult` |
| `backend/src/main/java/com/rehearse/api/infra/ai/AiClient.java` | **수정**. `chatWithAudio(ChatRequest, MultipartFile)` 인터페이스 메서드 추가 |
| `backend/src/main/java/com/rehearse/api/infra/ai/ResilientAiClient.java` | **수정**. Primary OpenAI audio chat / Fallback Claude(=기존 IntentClassifier+AnswerAnalyzer 직렬 호출) |
| `backend/src/main/java/com/rehearse/api/infra/ai/OpenAiClient.java` | **수정**. `gpt-4o-mini-audio-preview` 호출 (input_audio content type, base64) |
| `backend/src/main/java/com/rehearse/api/infra/ai/dto/ChatRequest.java` | **수정**. audio 입력 보강 (또는 별도 builder 메서드) |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` | **리팩토링**. 1·2·3단계 제거 → AudioTurnAnalyzer 단일 호출 |
| `backend/src/main/resources/prompts/template/follow-up-concept.txt` | **수정**. skip 룰 엄격화 (별도 task) |
| `backend/src/main/resources/prompts/template/follow-up-experience.txt` | **수정**. 동일 |
| `backend/src/main/resources/application-dev.yml` | **수정**. `openai.audio-model: gpt-4o-mini-audio-preview` |
| `backend/src/main/resources/application-local.yml` | 동일 |
| `backend/src/main/resources/application-prod.yml` | 동일 |
| `backend/src/main/java/com/rehearse/api/infra/ai/metrics/AiCallMetrics.java` | **수정**. 신규 `audio_turn_analyzer` call_type + skip 사유 카운터 |

기존 `IntentClassifier`, `AnswerAnalyzer` 클래스는 **유지** — Claude fallback 경로에서만 호출 (FollowUpService 에서는 직접 호출 X).

## 상세

### 통합 응답 스키마 (Structured Outputs json_schema strict)

```json
{
  "answer_text": "STT 전사 결과",
  "intent": {
    "type": "ANSWER | CLARIFY_REQUEST | GIVE_UP | OFF_TOPIC",
    "confidence": 0.0,
    "reasoning": "한 줄"
  },
  "answer_analysis": {
    "claims": [
      {"text": "한 문장 요약 (60자)", "depth_score": 1, "evidence_strength": "STRONG", "topic_tag": "concurrency"}
    ],
    "missing_perspectives": ["TRADEOFF", "RELIABILITY"],
    "unstated_assumptions": ["..."],
    "answer_quality": 1,
    "recommended_next_action": "DEEP_DIVE | CLARIFICATION | CHALLENGE | APPLICATION | SKIP"
  }
}
```

intent != ANSWER 케이스에도 `answer_analysis` 필드는 모델이 빈 값(`claims=[]`, `answer_quality=1`)으로 채워 strict 스키마 만족. `FollowUpService` 가 intent 분기에서 `answer_analysis` 무시.

### AudioTurnAnalyzer.analyze 시그니처

```java
public TurnAnalysisResult analyze(
    Long interviewId,
    Long turnId,
    MultipartFile audio,
    String mainQuestion,
    ReferenceType questionReferenceType,
    List<Perspective> askedPerspectives
)
```

내부 흐름:
1. ContextBuilder 로 `audio_turn_analyzer` callType 컨텍스트 조립
2. `ChatRequest` + audio MultipartFile → `aiClient.chatWithAudio(...)`
3. `AiResponseParser.parseOrRetry(..., TurnAnalysisResult.class, ...)`
4. L1 False Negative 가드: `intent.type==ANSWER AND answer_analysis.claims==[] AND answer_analysis.answer_quality<=1` 이면 `recommended_next_action=CLARIFICATION` 강제
5. `runtimeStateStore.update(...)` 로 분석 결과 캐시
6. `TurnAnalysisResult` 반환 (`answerText`, `intent`, `answerAnalysis`)

### 모델 파라미터

- Primary: `gpt-4o-mini-audio-preview` (한국어 STT + JSON 분석 동시)
- Fallback: Claude (audio 미지원 → text-only 경로 = 기존 IntentClassifier + AnswerAnalyzer 직렬 호출)
- temperature: **0.2** (일관성)
- max_tokens: 1024 (claims/missing_perspectives 등 충분히)
- response_format: `json_schema` strict

### Audio 인코딩

```jsonc
{
  "role": "user",
  "content": [
    { "type": "text", "text": "<<<MAIN_QUESTION>>>...<<<END_MAIN_QUESTION>>>\nQUESTION_REFERENCE_TYPE: CONCEPT\nASKED_PERSPECTIVES: TRADEOFF" },
    { "type": "input_audio", "input_audio": { "data": "<base64-encoded webm/wav>", "format": "wav" } }
  ]
}
```

`MultipartFile` → byte[] → Base64. format 은 frontend가 전송하는 audio MIME 기반 (webm 또는 wav). 현 frontend MediaRecorder = webm.

### Prompt Caching

System prompt (보안 + 역할 + 분류 규칙 + few-shot) 1500~2000 토큰 → OpenAI 자동 캐시 임계값(1024) 초과. `ChatMessage.ofCached` 마킹. (단, audio 모델은 caching 미지원 가능성 — 첫 배포 후 메트릭 확인. 캐시 미적용이어도 본 안의 6초 절감은 호출 수 자체 감소에서 발생).

### Claude Fallback 경로 (단순화 안)

`ResilientAiClient.chatWithAudio` 가 OpenAI 실패 시:
1. 기존 `IntentClassifier.classify(...)` (text-only) 호출 — `answerText` 는 별도 STT (`SttService.transcribe`) 결과 사용
2. 기존 `AnswerAnalyzer.analyze(...)` 호출
3. 두 결과 조합해 `TurnAnalysisResult` 빌드

→ Audio chat 미지원 fallback 시 latency 늘지만(STT + 직렬 호출) 정상 동작 보장. 빈도 낮음.

**결정 (2026-04-27)**: 단순화 안 채택. ResilientAiClient 내부에서 분기 처리. 별도 fallback 클래스 신설 X.

### Step B 프롬프트 skip 룰 엄격화 (별도 task, 본 PR 동봉)

`follow-up-concept.txt` / `follow-up-experience.txt` 에서:
- `skip=true` 조건: **(a) answer_text 빈 문자열 (b) answer_text 가 main_question 과 명백히 무관** 만 허용
- "답변 부족", "잘 모르겠음", "단편적" 사유는 skip 금지 → CLARIFICATION 패턴 follow-up 생성 강제
- few-shot 추가: "잘 모르겠습니다" 답변에 깊이 낮춘 follow-up 1건 (skip 아님)

### Aggregate Latency SLA

본 plan 적용 후 `Interview Quality` 스프린트 공용 SLA 갱신:

| 단계 | LLM 호출 | 성격 | 개별 p95 | 누적 p95 |
|------|---------|------|---------|---------|
| AudioTurnAnalyzer | 1회 (audio chat) | 동기 | ≤ 5500ms | 5500ms |
| Step B v3 (Follow-up Generator) | 1회 | 동기 | ≤ 2000ms | **≤ 5500ms 총합 (병렬화 가능 시)** |

→ 4-call (15.5s 이론) → 2-call (~7.5s 이론, ~5s 실측 목표). aggregate SLA 4s 는 audio 모델 latency 특성상 단축 한계 — 5s 내외 목표로 조정 필요.

### 코스트

- 4-call → 2-call: LLM 호출 50% 감소
- audio 모델 단가는 텍스트보다 audio token 부분이 약간 비쌈. 30s audio 기준 대략 $0.001 / call (Whisper $0.006/min + chat 합계 대비 동등 또는 우위)
- 종합: 턴당 **$0.005 → $0.003 추정** (40% 절감)

## 담당 에이전트

- Implement: `backend` — Builder + Analyzer + AiClient 확장 + FollowUpService 리팩토링
- Review: `architect-reviewer` — audio chat 추상화 경계, ResilientAiClient fallback 분기, Step A/B 책임 분리 유지 확인
- Review: `code-reviewer` — 보안 (audio base64 처리, 메모리 누수), 502 차단 검증, 메트릭 누락 검증

## 검증

| # | 항목 | 상태 |
|---|------|------|
| 1 | 회귀 0 (`./gradlew test`) | ⏳ TODO |
| 2 | 신규 `AudioTurnAnalyzerTest` Mock 기반 단위 테스트 — 통합 응답 파싱, intent 4분기, L1 FN 가드 | ⏳ TODO |
| 3 | `FollowUpServiceTest` 갱신 — 신규 흐름 (audio analyzer + Step B v3) | ⏳ TODO |
| 4 | dev EC2 배포 후 면접 5턴 진행: 502 0건, skip rate < 20%, endpoint 200 avg ≤ 5500ms | ⏳ TODO (수동) |
| 5 | `progress.md` S9 entry 추가 + plan-15 status → Completed | ⏳ TODO |

## 이월 사항 / Out of Scope

- **A. Step B v3 자체 latency 추가 절감**: 출력 스키마 슬림화(`ttsQuestion` 제거 등)는 별도 plan
- **B. Audio 모델 한국어 STT 정확도 정량 평가**: dev 첫 배포 후 5세션 수동 비교. 정확도 ≤ 90% 시 모델 변경 검토 (`gpt-4o-audio-preview` full 등)
- **C. 기존 plan-01 (Intent Classifier) / plan-02 (Answer Analyzer)**: Status `Superseded by plan-15` 헤더 추가. Claude fallback 경로에서만 활성. 클래스 자체는 유지
- **D. Aggregate SLA 4s → 5.5s 조정**: plan-01 §Aggregate Latency SLA 표 갱신 필요
