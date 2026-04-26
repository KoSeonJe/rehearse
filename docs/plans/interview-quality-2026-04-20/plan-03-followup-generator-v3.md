# Plan 03: Follow-up Generator v3 (M1 Step B) `[parallel:02]`

> 상태: Completed (PR #353, 2026-04-25, mergeCommit `be68b0f`)
> 작성일: 2026-04-20
> 주차: W2
> 원본: `docs/todo/2026-04-20/02-m1-followup-pipeline.md` (Step B 부분)
> 선행: `docs/plans/prompt-improvement-2026-04/proposals/follow-up.v2.txt`

## Why

plan-02가 뽑아준 구조화 분석(`claims`, `missing_perspectives`, `unstated_assumptions`)을 실제 꼬리질문 작문에 반영해야 "사용자 답변의 실제 갭을 정확히 파고드는" 질문이 나온다. 현재 v2 프롬프트는 관점 순환 로직만 있고 `target_claim` 개념이 없어 "어느 claim을 파고드는지"가 불명시 → 기계적 전환 재발.

v3는 **`target_claim_idx` 명시 + 관점 선정 로직을 depth_score/evidence_strength 기반으로 변경**.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/prompts/template/follow-up-concept.txt` | **수정** v2 → v3 (Step B 구조로 변환, Step A 분석 입력 수용) |
| `backend/src/main/resources/prompts/template/follow-up-experience.txt` | **수정** v2 → v3 (동일) |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/FollowUpPromptBuilder.java` | 수정. Step A 결과를 `ANSWER_ANALYSIS` 섹션으로 주입 |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` | 수정. Step A 호출 → 결과 주입 → Step B 호출 순으로 재구성 |
| `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedFollowUp.java` | 수정. 기존 `question/reason/type/modelAnswer/answerText` 에 `targetClaimIdx`, `selectedPerspective` 2개 필드 추가 (`FollowUpQuestion` 클래스는 실재하지 않음 — 실제 DTO 경로 확인 완료) |

## 상세

### JSON 계약 (응답)
```json
{
  "skip": false,
  "skipReason": null,
  "target_claim_idx": 0,
  "selected_perspective": "RELIABILITY",
  "answerText": "사용자 답변에 대한 짧은 리액션",
  "question": "...",
  "ttsQuestion": "...",
  "reason": "depth_score 2짜리 claim을 RELIABILITY 관점으로 확장",
  "type": "DEEP_DIVE|CLARIFICATION|CHALLENGE|APPLICATION",
  "modelAnswer": "..."
}
```

### 관점 선정 우선순위 (프롬프트 내 명시)
1. `claims[i].depth_score <= 2` 인 claim의 인덱스를 `target_claim_idx`로 선정 (얕은 부분 먼저)
2. 없으면 `evidence_strength = WEAK` 인 claim
3. 없으면 `missing_perspectives` 중 `unstated_assumptions`와 교차하는 관점 우선
4. `ASKED_PERSPECTIVES`와 중복 금지

### Skip 조건
- `answer_quality <= 1`
- `recommended_next_action == "SKIP"`
- `missing_perspectives` 공집합 AND 모든 claim `depth_score >= 4`

### 모델 파라미터
- Primary: GPT-4o-mini / Fallback: Claude Haiku
- temperature: **0.6** (작문엔 약간의 창의성)
- max_tokens: 1024
- Prompt Caching: 시스템 프롬프트

### 금지 규칙 (프롬프트에 명시)
- 사용자 답변 긴 문장 인용 금지(키워드만) — 환각 방지
- 복합질문 금지 (한 번에 하나)

### Aggregate Latency SLA
본 plan 은 plan-01 §Aggregate Latency SLA 규약에 속함. Step B 개별 p95 ≤ **2000ms**. aggregate (Intent + Analyzer + Follow-up) p95 ≤ **4000ms** 상한 준수. Rubric Scorer (plan-08) 는 비동기 post-turn 이므로 본 SLA 제외.

## 담당 에이전트

- Implement: `backend` — Builder/Service 수정, JSON 확장
- Implement: `prompt-engineer` — v2 → v3 프롬프트 diff, few-shot 예시 작성
- Review: `code-reviewer` — JSON 파싱 방어(기존 SchemaValidator 재사용 여부), null 처리

## 검증

1. Before/After 5쌍 수동 비교 문서화(`eval/manual-ab/{YYYY-MM-DD}-plan-03.md`, MANUAL_AB_PROTOCOL.md 참조)
2. `target_claim_idx`가 `claims` 범위 내 유효(100%)
3. `selected_perspective`가 `ASKED_PERSPECTIVES`와 중복 ≤ 5%
4. Follow-up Relevance 수동 비교(3~5건): v3 꼬리질문이 v2 대비 사용자 답변 claim에 더 정확히 꽂힘 — 과반 이상
5. 전체 latency(Step A + Step B 합) p95 ≤ +2s 대비 기존
6. `progress.md` 03 → Completed

## 머지 결과 (PR #353, 2026-04-25, mergeCommit `be68b0f`)

### 신규/수정 DTO
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedFollowUp.java` — `targetClaimIdx`, `selectedPerspective` 필드 추가 (snake_case Jackson 매핑)
- `backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpRequest.FollowUpExchange` — `selectedPerspective` 옵션 A 필드 추가 (FE echo)
- `backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpResponse` — `selectedPerspective` echo 필드 추가
- `FollowUpContext.currentMainQuestionId` 필드 추가 (`FollowUpTransactionHandler` 에서 MAIN question id 도출)

### 수정 서비스/프롬프트
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` — ANSWER 경로 refactor: STT 재사용 → `AnswerAnalyzer.analyze` → SKIP 권고 시 cost saver → `aiClient.chat(ChatRequest)` Step B → parseOrRetry → save. callType=`follow_up_generator_v3`, temp 0.6, max 1024, JSON_OBJECT
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/FollowUpPromptBuilder.java` — `buildUserPromptWithAnalysis()` 추가. ANSWER_ANALYSIS 블록 직렬화 (claims/missing_perspectives/unstated_assumptions/recommended_next_action/asked_perspectives)
- `backend/src/main/resources/prompts/template/follow-up-concept.txt` v3 — ANSWER_ANALYSIS 입력 명세 + target_claim_idx 우선순위 + JSON 스키마 확장 (CONCEPT 모드 selected_perspective=null 강제)
- `backend/src/main/resources/prompts/template/follow-up-experience.txt` v3 — ANSWER_ANALYSIS + selected_perspective 7-enum 우선순위 + asked_perspectives 중복 금지

### askedPerspectives 추출
`previousExchanges.selectedPerspective` 관대 파싱 + distinct.

### 테스트
- `FollowUpServiceTest` 7 시나리오
- `FollowUpServiceIntentBranchTest` ANSWER 경로 Step A/B mock 추가
- 핸들러 3 테스트 FollowUpContext 시그니처 갱신
- 749 tests pass / 0 failures

### 잔여 게이트 (S7 시점, plan-04 와 병렬 진행)
- [ ] FE 계약 전달 — `FollowUpRequest.FollowUpExchange.selectedPerspective` 입력 + `FollowUpResponse.selectedPerspective` echo 사용
- [ ] LIVE 골든셋 (Step A depth_score ±1 / target_claim_idx 범위 / selected_perspective 중복)
- [ ] MANUAL_AB 3~5 건 (v2 vs v3 claim 타겟팅 정확도)
- [ ] 스테이징 Prometheus `rehearse.ai.call.duration_seconds{call.type=~"answer_analyzer|follow_up_generator_v3"}` p95

