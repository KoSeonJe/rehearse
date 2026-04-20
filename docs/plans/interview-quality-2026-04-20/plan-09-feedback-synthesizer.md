# Plan 09: Feedback Synthesizer (M3 세션 종합)

> 상태: Draft
> 작성일: 2026-04-20
> 주차: W4
> 원본: `docs/todo/2026-04-20/03-m3-feedback-rubric.md` (종합기 부분)

## Why

plan-08이 턴마다 쌓아준 루브릭 점수를 세션 종료 시 **사용자가 "다음에 뭘 다르게 해야 하는지 명확한"** 구조화된 피드백으로 합성해야 실제 학습 효과가 나온다. 현재 3줄 자유 생성은 "소통이 좋습니다. 기술 이해도 보완이 필요합니다" 수준으로 다음 액션이 없음.

5개 섹션 강제(Overall / Strengths / Gaps / Non-verbal & Delivery / Week Plan) + **모든 strength/gap에 관찰 인용 + 각 gap에 구체적 액션 1개** 강제하면 추상 탈출.

## 전제 (plan-00e FEEDBACK_DOMAIN.md 결정 소비)
- 기존 `TimestampFeedback` / `QuestionSetFeedback` 과 **병존** (대체 아님)
- `SessionFeedback` 은 별도 `session/` 서브패키지로 분리 — 기존 `FeedbackService` 건드리지 않음
- **Partial-first**: 세션 종료 즉시 기술 피드백만 생성(`status=PRELIMINARY`), Verbal/Vision 도착 시 Delivery 섹션 보강(`status=COMPLETE`)
- Verbal/Vision 10분 미도착 시 Delivery null + status=COMPLETE (무한 대기 방지)

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
| `backend/src/main/resources/application.yml` | 수정. `rehearse.features.feedback-rubric.synthesizer-model` |

## 상세

### 입력 (plan-08 Rubric Family 반영)
- `SESSION_METADATA` (면접 유형, 페르소나, 레벨, 총 턴 수)
- `TURN_SCORES[]` (plan-08 누적 — 각 턴의 `rubric_id`, `scored_dimensions`, `scores_json`)
- **`SCORES_BY_CATEGORY`** — 카테고리별로 그룹핑된 평균 점수 (신규, 크로스-비교용):
  ```json
  {
    "cs": {"D2": 2.8, "D3": 3.0, "D4": 2.9, "D8": null},
    "experience": {"D1": 2.0, "D3": 2.5, "D6": 1.8, "D8": null},
    "resume": {"D2": 2.7, "D9": 3.0, "D10": 2.2}
  }
  ```
- `APPLIED_RUBRICS` — 세션에 사용된 rubric_id 목록 (피드백 narrative에 언급용)
- `VERBAL_ANALYSIS` (기존 Gemini 결과, **구조 변경 없음** — Out of Scope)
- `VISION_ANALYSIS` (기존 Gemini Vision, 동일)
- `RUBRIC_FAMILY` (plan-08 `_dimensions.yaml`) — 차원 설명 참조용

### 출력 5섹션 (강제)
```json
{
  "overall": {
    "dimension_scores": {"technical_depth": 2.4, ...},
    "level_assessment": "주니어 기대치 충족, 미드 수준에는 테크니컬 뎁스 보강 필요",
    "narrative": "..."
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

### 모델 선택
- **Synthesizer는 더 큰 모델 필요** (통합 작문)
- Primary 옵션: GPT-4o-mini (기본, 코스트 우선) / GPT-4o or Claude Sonnet (품질 우선, flag로 교체 가능)
- temperature: 0.4
- max_tokens: 2048
- Feature flag `synthesizer-model: gpt-4o-mini` 로 쉽게 교체

### Verbal/Vision 통합 원칙
기술 피드백(Overall/Strengths/Gaps/Week Plan)과 **별도 섹션**(Delivery)으로 분리. 섞지 않음.

## 담당 에이전트

- Implement: `backend` — Synthesizer + DTO + 세션 종료 hook
- Implement: `prompt-engineer` — 5섹션 강제 프롬프트, 추상 감지 재작성 로직
- Review: `designer` — 피드백 구조가 FE에서 렌더링 가능한지 (5섹션이 UI에 매핑 쉬운지)
- Review: `code-reviewer` — DTO 계약, Entity 직접 반환 금지 확인, `@Transactional(readOnly=true)` 적용

## 검증

1. 세션 5개 수동 리뷰: 관찰 인용 포함률 100% (모든 strength/gap)
2. 액션 구체성 ≥ 90% — "더 공부하세요" 계열 0건 (정규식 감지)
3. 레벨 보정 문구 매 피드백에 등장
4. plan-10 J3(Feedback Rubric Adherence) ≥ 4.0
5. Synthesizer 모델 교체(gpt-4o-mini ↔ gpt-4o) 시 품질 차이 수치화 — 비용 대비 선택 기준 문서화
6. 기존 `FeedbackService` 호출 경로 회귀 없음
7. `progress.md` 09 → Completed
