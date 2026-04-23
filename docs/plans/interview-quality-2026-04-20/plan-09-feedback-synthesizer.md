# Plan 09: Feedback Synthesizer (M3 세션 종합)

> 상태: Draft
> 작성일: 2026-04-20 (2026-04-22 Content/Delivery 소스 분리 개정 반영)
> 주차: W7 (plan-08과 병렬, plan-13과 ECR cut-over 동시)
> 원본: `docs/todo/2026-04-20/03-m3-feedback-rubric.md` (종합기 부분)
> 연계: plan-08 (Rubric Family — content 유일 소스), plan-13 (Lambda Content Removal — delivery 전용 축소)

## Why

plan-08이 턴마다 쌓아준 루브릭 점수를 세션 종료 시 **사용자가 "다음에 뭘 다르게 해야 하는지 명확한"** 구조화된 피드백으로 합성해야 실제 학습 효과가 나온다. 현재 3줄 자유 생성은 "소통이 좋습니다. 기술 이해도 보완이 필요합니다" 수준으로 다음 액션이 없음.

5개 섹션 강제(Overall / Strengths / Gaps / Non-verbal & Delivery / Week Plan) + **모든 strength/gap에 관찰 인용 + 각 gap에 구체적 액션 1개** 강제하면 추상 탈출.

## 전제 (plan-00e FEEDBACK_DOMAIN.md 결정 소비)
- 기존 `TimestampFeedback` / `QuestionSetFeedback` 과 **병존** (대체 아님)
- `SessionFeedback` 은 별도 `session/` 서브패키지로 분리 — 기존 `FeedbackService` 건드리지 않음
- **Partial-first**: 세션 종료 즉시 기술 피드백만 생성(`status=PRELIMINARY`), Delivery 분석 도착 시 Delivery 섹션 보강(`status=COMPLETE`)
- Delivery 분석 10분 미도착 시 Delivery null + status=COMPLETE (무한 대기 방지)
- **Lambda 실패는 모두 재시도 가능**: admin 이 수동 재처리 가능하도록 상태 보존. 사용자는 "일시 오류" 로 안내 (영구 실패 표시 금지). 세부 정책은 §Lambda Error Handling
- **2026-04-22 결정: Content/Delivery 소스 분리** (plan-13 연계)
  - **Content 섹션 (Overall/Strengths/Gaps/Week Plan) 의 유일 소스는 `TURN_SCORES` (plan-08 Rubric)** — Lambda 기술 내용 분석은 plan-13 cut-over 시점에 제거됨
  - **Delivery 섹션의 유일 소스는 `DELIVERY_ANALYSIS` + `VISION_ANALYSIS`** (Lambda vocal + attitude + overall_delivery + vision)
  - **두 소스를 섞지 말 것**: Content 섹션 observation은 `turn_scores[].evidenceQuote` 에서만 인용, Delivery 섹션 observation은 `delivery_analysis` / `vision_analysis` 에서만 인용

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/prompts/template/session-feedback-synthesizer.txt` | 신규 |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/SessionFeedbackSynthesizerPromptBuilder.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/feedback/session/SessionFeedbackService.java` | 신규 (별도 서브패키지 — 기존 `FeedbackService` 불변). plan-00b `AiClient.chat()` 호출 (`callType = "feedback_synthesizer"`) |
| `backend/src/main/java/com/rehearse/api/domain/feedback/session/entity/SessionFeedback.java` | 신규 JPA Entity (V27 매핑). `status: PRELIMINARY/COMPLETE` |
| `backend/src/main/java/com/rehearse/api/domain/feedback/session/repository/SessionFeedbackRepository.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/feedback/session/dto/SessionFeedbackResponse.java` | 신규. 5섹션 DTO (Entity 직접 반환 금지) |
| `backend/src/main/java/com/rehearse/api/domain/feedback/session/controller/AdminSessionFeedbackController.java` | 신규. `GET /api/admin/interviews/{id}/session-feedback` (FE 연동 전 확인용) |
| `backend/src/main/java/com/rehearse/api/domain/interview/event/InterviewCompletedEvent.java` | 존재 확인(plan-00a 인벤토리), 없으면 신규. 기존 `FeedbackService` 리스너와 **독립적**인 새 리스너 `SessionFeedbackService.onInterviewCompleted()` 등록 |
| `backend/src/main/resources/application.yml` | 수정. `rehearse.feedback-synthesizer.model: gpt-4o-mini` (기본값 고정, Feature Flag 없음) |

## 상세

### 입력 (plan-08 Rubric Family + plan-13 Content 단일화 반영)

- `SESSION_METADATA` (면접 유형, 페르소나, 레벨, 총 턴 수)
- `TURN_SCORES[]` (plan-08 누적 — 각 턴의 `rubric_id`, `scored_dimensions`, `scores_json`, **`status: OK|FAILED`** 포함)
  - `status=FAILED` 인 턴은 `scores_json` 이 null 가능 → 평균 집계에서 제외, `overall.coverage` 필드에 `"8/10 turns scored"` 형식으로 명시
  - Rubric LLM 실패 시 Lambda content fallback 없음 (plan-13 cut-over 후) → degraded 상태 그대로 합성
- **`SCORES_BY_CATEGORY`** — 카테고리별로 그룹핑된 평균 점수 (status=OK turn만 집계):
  ```json
  {
    "cs": {"D2": 2.8, "D3": 3.0, "D4": 2.9, "D8": null},
    "experience": {"D1": 2.0, "D3": 2.5, "D6": 1.8, "D8": null},
    "resume": {"D2": 2.7, "D9": 3.0, "D10": 2.2}
  }
  ```
- `APPLIED_RUBRICS` — 세션에 사용된 rubric_id 목록 (피드백 narrative에 언급용)
- `DELIVERY_ANALYSIS` (기존명 `VERBAL_ANALYSIS` — 2026-04-22 개명. plan-13 cut-over로 Lambda가 **delivery 전용**으로 축소됨)
  - 필드: `vocal.{fillerWords, speechPace, toneConfidenceLevel, emotionLabel, positive, negative, suggestion}`, `attitude.{positive, negative, suggestion}`, `overall_delivery.{positive, negative, suggestion}`, `transcript`
  - **제거된 필드** (plan-13 이전): `verbal.*`, `technical.accuracyIssues`, `technical.coaching.*` → 수신 안 함. 수신되면 Synthesizer는 무시 (방어적)
- `VISION_ANALYSIS` (기존 Gemini Vision, 동일) — Delivery 섹션 전용
- `NONVERBAL_SCORES_BY_TURN` / `NONVERBAL_AGGREGATE` (plan-11 연계, 해당 plan flag on 시에만) — Delivery 섹션 dimension D11~D14 점수
- `RUBRIC_FAMILY` (plan-08 `_dimensions.yaml`) — 차원 설명 참조용

### 출력 5섹션 (강제)
```json
{
  "overall": {
    "dimension_scores": {"technical_depth": 2.4, ...},
    "level_assessment": "주니어 기대치 충족, 미드 수준에는 테크니컬 뎁스 보강 필요",
    "narrative": "...",
    "coverage": "8/10 turns scored"
  },
  "strengths": [{
    "dimension": "reasoning_communication",
    "observation": "turn 3에서 Write-Through → Lazy 전환 의사결정 흐름 설명",
    "why_matters": "..."
  }],
  "gaps": [{
    "dimension": "technical_depth",
    "observation": "turn 5에서 Redis 선택 이유 '빠르다'만 언급",
    "level_gap": "...",
    "concrete_action": "Redis vs Memcached 비교표 작성 (Persistence/Data Structure/Memory/Use Case 4축)"
  }],
  "delivery": {"filler_words": "...", "tone_pattern": "...", "action": "..."},
  "week_plan": [{"priority": 1, "topic": "...", "resources": [...], "practice": "..."}]
}
```

### 작문 원칙 (프롬프트 내)

1. 모든 strength/gap에 `observation: "turn N에서 ~"` 포맷 강제
2. 레벨 보정 명시 ("주니어로서 훌륭한" / "시니어 기대치 대비")
3. 공감 톤 ("함께 개선해봅시다"), 평가자 톤 금지
4. **추상 금지** — "더 공부하세요" 감지되면 재작성
5. 5섹션 합계 800-1200 단어
6. **카테고리 크로스-비교** (신규, plan-08 Rubric Family 연계): `SCORES_BY_CATEGORY` 에 2개 이상 카테고리 존재 시 `overall.narrative` 에 교차 패턴 1회 이상 언급. 예: "CS 개념에선 Conceptual Accuracy(D4) 평균 2.9로 탄탄하지만, 경험 질문에선 Experience Concreteness(D6) 1.8로 약함 — 이론은 정확히 알지만 자기 경험으로 구체화하지 못하는 패턴". 이런 패턴 감지가 MVP의 가장 실행 가능한 피드백 생성 경로.
7. **Content/Delivery 소스 분리 강제** (2026-04-22, plan-13 연계):
   - Overall/Strengths/Gaps/Week Plan 섹션의 `observation` 은 **오직 `TURN_SCORES[].evidenceQuote` 에서만** 인용. `DELIVERY_ANALYSIS` / `VISION_ANALYSIS` 텍스트 인용 금지 (delivery 단서를 content 평가 근거로 삼지 말 것)
   - Delivery 섹션의 `observation` 은 **오직 `DELIVERY_ANALYSIS` + `VISION_ANALYSIS` + `NONVERBAL_SCORES_BY_TURN` 에서만** 인용. `TURN_SCORES` 의 D1~D10 점수/observation 인용 금지
   - 위반 감지 시 재작성 (정규식으로 소스 cross-reference 검출)
8. **Coverage 필수**: `TURN_SCORES` 전체 중 `status=OK` 비율이 100% 미만이면 `overall.coverage` 에 `"N/M turns scored"` 형식으로 명시. 100%면 필드 생략 또는 `"all turns scored"`. 이는 Rubric 실패가 은폐되지 않도록 하는 투명성 장치

### 모델 선택
- **Synthesizer는 더 큰 모델 필요** (통합 작문)
- 기본값: GPT-4o-mini (코스트 우선). 품질 부족 확인 시 `application-prod.yml` 에서 `rehearse.feedback-synthesizer.model` 을 `gpt-4o` 또는 `claude-sonnet-*` 로 변경 후 재배포.
- Feature Flag runtime toggle은 사용하지 않는다. 단일 모델 경로로 고정.
- temperature: 0.4
- max_tokens: 2048

### Delivery/Vision 통합 원칙 (2026-04-22 개정 — plan-13 연계)

기술 피드백(Overall/Strengths/Gaps/Week Plan)과 **별도 섹션**(Delivery)으로 분리. **섞지 않음**.

**소스 엄격 분리**:
- Content 섹션 (Overall/Strengths/Gaps/Week Plan) 은 `TURN_SCORES` 에서만 파생
- Delivery 섹션은 `DELIVERY_ANALYSIS` + `VISION_ANALYSIS` + `NONVERBAL_SCORES_BY_TURN` 에서만 파생

**크로스 모달 signal (선택)**:
`DELIVERY_ANALYSIS.vocal.emotionLabel=긴장` + `transcript` 의 헤지 마커("아마도", "잘 모르겠는데") 동시 발생 시, **Synthesizer가 별도 cross-modal hint 로만** 기록 — D4/D8 차원 점수를 **수정하지 않음** (점수는 plan-08 Rubric이 독점). `overall.narrative` 에 "긴장감이 기술 자신감 서술에 영향을 준 듯" 정도의 **연성 관찰**로만 표현 가능.

**금지**:
- Delivery 단서("필러워드가 많았다")를 Content 차원("Conceptual Accuracy 점수 낮음")의 근거로 삼는 것
- Content 차원 점수를 Delivery 섹션에 재인용 ("기술 깊이 점수가 낮으므로 자신감 부족")
- Lambda가 (잘못) 반환한 `verbal`/`technical` 필드를 읽는 것 (plan-13 cut-over 후 이 필드는 존재하지 않음 — 방어적으로 무시)

### Lambda Error Handling (비언어/Verbal 실패 처리)

Lambda `handler.py` 는 이미 `failure_reason` / `failure_detail` / `isVerbalCompleted` / `isNonverbalCompleted` 필드를 전달함. plan-09 는 이를 소비해 **모든 실패를 admin 재시도 가능 상태**로 저장하고, 사용자에게는 "일시 오류" 로만 안내.

#### 수신 상태 → 처리 매트릭스

| 수신 상태 | `SessionFeedback.status` | `deliveryRetryable` | 사용자 표시 (FE) | 자동 재시도 | admin 재시도 |
|-----------|-------------------------|---------------------|------------------|-----------|-----------|
| `isNonverbalCompleted=false` + `failure_reason=null` (진행 중) | PRELIMINARY | — | "분석 중..." spinner | — | — |
| `failure_reason=TIMEOUT` | PRELIMINARY | **true** | "비언어 분석 일시 오류, 재처리 중" 배너 | 1회 (Lambda 재호출) | 가능 |
| `failure_reason=VISION_ERROR` / `API_ERROR` | PRELIMINARY | **true** | "비언어 분석 일시 오류" 배너 | 1회 | 가능 |
| `failure_reason=TRANSCRIPTION_ERROR` | PRELIMINARY | **true** | "음성 분석 일시 오류" 배너 | 1회 | 가능 |
| `failure_reason=INTERNAL_ERROR` | PRELIMINARY | **true** | "비언어 분석 일시 오류" 배너 | — (로그만) | 가능 |
| `failure_reason=SCHEMA_MISSING_FIELDS` (plan-11a 신규) | PRELIMINARY | **true** | "비언어 분석 일시 오류" 배너 | — | 가능 (Lambda 갱신 후) |
| 10분 timeout (Backend 측 watchdog) | PRELIMINARY | **true** | "비언어 분석 지연" 배너 | — | 가능 |

**핵심 원칙**:
- 모든 실패는 `deliveryRetryable=true` (기본값). 영구 실패로 굳히지 않음.
- 사용자 노출 문구는 전부 "일시 오류" 계열. "실패" / "불가" / "제외됨" 같은 종결 표현 금지.
- `SessionFeedback.status` 는 `PRELIMINARY` 유지 → admin 재시도 후 Delivery 섹션 보강 시 `COMPLETE` 전환.
- 세션 종료 후 2주 이내는 admin 재시도 허용. 그 이후는 `EXPIRED` 전환 (사용자에게는 그냥 "분석 없음" 표시, 실패 노출 X).

#### Admin 재시도 엔드포인트

- `POST /api/admin/interviews/{id}/session-feedback/retry-delivery` — 해당 interview 의 Lambda 분석 재실행 트리거
- 권한: `ROLE_ADMIN` 만 허용
- 동작: 원본 S3 미디어 경로로 Lambda 재호출 → 성공 시 `SessionFeedbackService.onAnalysisArrived()` 가 Delivery 섹션 보강 + `status=COMPLETE` 전환
- 멱등성: 같은 interview 에 대해 재시도 진행 중이면 409 Conflict
- 로그: `rehearse.ai.lambda.retry.*` 메트릭 + admin 식별자 기록

#### 수정 파일 추가

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/domain/feedback/session/entity/SessionFeedback.java` | 수정 (위 테이블). `deliveryRetryable: boolean` + `lastFailureReason: String` + `retryAttempts: int` 필드 추가 |
| `backend/src/main/java/com/rehearse/api/domain/feedback/session/controller/AdminSessionFeedbackController.java` | 수정. retry-delivery 엔드포인트 추가 |
| `backend/src/main/java/com/rehearse/api/domain/feedback/session/SessionFeedbackService.java` | 수정. `onAnalysisFailed(failureReason)`, `retryDelivery(interviewId, adminUserId)`, `onAnalysisArrived(...)` 메서드 추가 |
| `frontend/src/components/feedback/DeliverySection.tsx` (FE 연동 별건) | **Out of Scope (본 plan)**. FE PR 에서 배너 문구 "일시 오류" 적용 |

## 담당 에이전트

- Implement: `backend` — Synthesizer + DTO + 세션 종료 hook
- Implement: `prompt-engineer` — 5섹션 강제 프롬프트, 추상 감지 재작성 로직
- Review: `designer` — 피드백 구조가 FE에서 렌더링 가능한지 (5섹션이 UI에 매핑 쉬운지)
- Review: `code-reviewer` — DTO 계약, Entity 직접 반환 금지 확인, `@Transactional(readOnly=true)` 적용

## 검증

1. 세션 5개 수동 리뷰: 관찰 인용 포함률 100% (모든 strength/gap)
2. 액션 구체성 ≥ 90% — "더 공부하세요" 계열 0건 (정규식 감지)
3. 레벨 보정 문구 매 피드백에 등장
4. 수동 비교 3~5건 (MANUAL_AB_PROTOCOL.md): 신규 5섹션 피드백이 기존 3줄 포맷 대비 "관찰 인용 포함 + 다음 액션 구체" 항목에서 과반 이상 우세
5. Synthesizer 모델 교체(gpt-4o-mini → gpt-4o) 시 품질 차이 정성 평가 — 비용 대비 선택 기준 `eval/manual-ab/` 에 기록
6. 기존 `FeedbackService` 호출 경로 회귀 없음
7. **Content/Delivery 소스 분리 (2026-04-22 신설)**:
   - 10개 샘플 세션에서 Content 섹션 observation 이 `TURN_SCORES[].evidenceQuote` 에만 매칭 (정규식으로 delivery_analysis 텍스트 포함 여부 확인 — 0건)
   - Delivery 섹션이 `TURN_SCORES` dimension 점수/observation 을 재인용하지 않음 (0건)
   - Rubric `status=FAILED` turn 이 섞인 세션에서 `overall.coverage` 필드 정확히 렌더 (예: "8/10 turns scored")
8. **plan-13 Lambda 계약 방어**: Lambda가 (잘못) `verbal`/`technical` 필드를 반환한 경우 Synthesizer가 무시하고 정상 출력 생성 (stub 테스트)
9. `progress.md` 09 → Completed
