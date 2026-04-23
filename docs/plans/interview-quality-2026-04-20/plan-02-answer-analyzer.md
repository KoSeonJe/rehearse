
gogo# Plan 02: Answer Analyzer (M1 Step A) `[parallel:03]`

> 상태: Draft
> 작성일: 2026-04-20
> 주차: W2
> 원본: `docs/todo/2026-04-20/02-m1-followup-pipeline.md` (Step A 부분)

## Why

현재 꼬리질문 생성(`follow-up-concept.txt` / `follow-up-experience.txt`)은 한 번의 LLM 호출 안에서 **답변 분석 + 관점 선정 + 질문 작문 + skip 판단 + 모델답변 생성** 7가지를 동시에 수행한다. 이로 인해 (a) 어느 단계가 틀렸는지 관측 불가, (b) Haiku/4o-mini 수준 모델이 전부 잘하긴 버거움, (c) 기계적 관점 전환으로 답변과 단절된 꼬리질문 발생.

Step A(분석 전용)를 분리하면 Step B(생성)가 claim 타겟을 정확히 잡을 수 있고, 동일 분석 결과를 **M3 루브릭 채점에 재활용**(plan-08)해 코스트+일관성을 동시에 확보한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/prompts/template/follow-up-step-a-analyzer.txt` | 신규. 답변 분석 전용 프롬프트 |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/AnswerAnalyzerPromptBuilder.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/interview/AnswerAnalysis.java` | 신규 record. claims/missing_perspectives/unstated_assumptions/answer_quality |
| `backend/src/main/java/com/rehearse/api/domain/interview/Claim.java` | 신규 record. text/depth_score/evidence_strength/topic_tag |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/AnswerAnalyzer.java` | 신규 service. plan-00b `AiClient.chat()` 호출 (`callType = "answer_analyzer"`) |
| `backend/src/main/java/com/rehearse/api/domain/interview/runtime/InterviewRuntimeState.java` | **수정 (plan-00c에서 생성)**. 분석 결과 캐시 필드 추가. `InterviewSession` 클래스는 실재하지 않음 — runtime 상태는 `InterviewRuntimeStateStore`로 관리 |
| `backend/src/main/resources/application.yml` | 수정. `rehearse.answer-analyzer.enabled: true` (기본 활성, Feature Flag 없음) |
| `backend/src/main/resources/db/migration/V{XX}__alter_rubric_score_turn_fk.sql` | **신규 (2026-04-22, db-00c-C1 리뷰 이관)**. plan-02 에서 `interview_turn` 테이블 확정 시점에 `rubric_score.turn_id` FK 추가. `ALTER TABLE rubric_score ADD CONSTRAINT fk_rubric_score_turn FOREIGN KEY (turn_id) REFERENCES interview_turn(id) ON DELETE CASCADE;` |

## 상세

### JSON 계약 (Step B + Rubric Scorer가 consume)
```json
{
  "claims": [
    {"text": "...", "depth_score": 1-5, "evidence_strength": "STRONG|WEAK|ASSUMED", "topic_tag": "..."}
  ],
  "missing_perspectives": ["TRADEOFF", "MAINTAINABILITY", "RELIABILITY", "SCALABILITY", "TESTING", "COLLABORATION", "USER_IMPACT"],
  "unstated_assumptions": ["..."],
  "answer_quality": 1-5,
  "recommended_next_action": "DEEP_DIVE|CLARIFICATION|CHALLENGE|APPLICATION|SKIP"
}
```

### 입력
- `MAIN_QUESTION`, `QUESTION_REFERENCE_TYPE` (CONCEPT/EXPERIENCE), `USER_ANSWER`
- `ASKED_PERSPECTIVES` (EXPERIENCE일 때만) — missing_perspectives 계산 시 기존 다룬 것 제외

### 모델 파라미터
- Primary: GPT-4o-mini / Fallback: Claude Haiku
- temperature: **0.2** (분석은 일관성)
- max_tokens: 800
- Prompt Caching: 시스템 프롬프트 전체

### 세션 캐싱 (plan-00c 전제)
`InterviewRuntimeStateStore.update(interviewId, state -> state.recordAnalysis(turnId, analysis))` — plan-08 Rubric Scorer가 같은 세션에서 읽어감. 2번 호출 방지.

### META/OFF_TOPIC 2차 가드 (critic M3 반영)
plan-01 Intent Classifier가 META/OFF_TOPIC을 ANSWER로 병합했을 때의 오염을 이 단계에서 감지:
- Step A가 `claims.length == 0 AND answer_quality <= 1` 이면 `recommended_next_action = "CLARIFICATION"` 강제
- plan-03 Step B가 이 신호를 받으면 꼬리질문 대신 "질문이 이해되지 않으셨다면 다시 설명드릴게요" 재설명 경로로

### 코스트 영향
기존 follow-up 1회 → Step A 1회 + Step B 1회 = 2회. Prompt Caching 적용 시 실제 코스트 증가 1.3~1.5배, 10분 세션당 +$0.01~0.02.

### Aggregate Latency SLA
본 plan 은 plan-01 §Aggregate Latency SLA 규약에 속함. Step A 개별 p95 ≤ **1500ms** (intent=ANSWER 분기에서만 호출), aggregate 에서의 위치는 Intent(500ms) → **Analyzer(1500ms)** → Follow-up(2000ms) → 합계 ≤ 4000ms.

## 담당 에이전트

- Implement: `backend` — Builder + Analyzer 서비스 + 세션 캐시
- Review: `architect-reviewer` — Step A/B 분리 경계, 세션 상태 관리 패턴(JPA Entity 직접 노출 금지 등 CLAUDE.md 원칙)

## 검증

1. 수동 골든셋 15개(다양한 depth_score 분포)에서 JSON 스키마 100% 파싱 성공
2. `depth_score` 인간 라벨과 ±1점 내 일치율 ≥ 80%
3. `missing_perspectives`가 `ASKED_PERSPECTIVES`와 중복 0건
4. p95 latency ≤ 1.5s
5. plan-03(Step B)와 JSON 계약 호환 통합 테스트
6. `progress.md` 02 → Completed
