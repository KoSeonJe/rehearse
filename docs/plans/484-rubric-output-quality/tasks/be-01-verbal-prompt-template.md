# Task BE-01 — verbal scorer prompt 한국어 + verbatim 룰 강화

> **Phase**: 1 (BE only)
> **답하는 질문**: verbal LLM 호출 prompt 가 한국어 observation + transcript verbatim evidence 만 산출하도록 어떻게 전환?

---

## 목적

`turn-rubric-scorer.txt` template + `RubricScorerPromptBuilder.buildSystemPrompt()` 의 영어 본문 / 영어 Output Schema 예시를 한국어로 교체. evidence_quote verbatim 룰을 prompt 내 명시적 차단 룰로 강화. AC-1 / AC-2 회복의 prompt 단 책임.

## 에이전트

- **구현**: `prompt-engineer` — LLM 출력 언어 결정 신호 / few-shot 정합성 / 위배 회피 룰 설계 책임. 사용자 발화 ("evidence quote도 이상해, 팀에서 했어요 수준만 반복") 의 원인 = prompt 단 영문/한국어 혼재 + verbatim 룰 약함.
- **리뷰**: BE-01 + BE-02 묶음 PR (Phase 1) 머지 직전 `code-reviewer-backend` 통합 리뷰.

## 변경 파일

- `backend/src/main/resources/prompts/template/turn-rubric-scorer.txt` (line 6-72)
  - line 34 `evidence_quote MUST be a verbatim excerpt (≤40 words) from the candidate answer` → 한국어 + 차단 룰 강화 (정의문 / 질문 본문 / 그 외 텍스트 인용 금지 명시)
  - line 43-53 Output Schema 영어 예시 → 한국어 예시로 교체
  - line 56-72 한국어 Scoring Example 유지 + 일관성 정비
  - Rules #3 강화: "evidence_quote MUST be a verbatim substring from `<<<USER_ANSWER>>>...<<<END_USER_ANSWER>>>`. Quoting the question, rubric definition, or any other text is FORBIDDEN."
  - Rules #4 신설: "observation MUST be in Korean (한국어 1~2문장)."
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/RubricScorerPromptBuilder.java` (line 94-98)
  - `buildSystemPrompt()` 본문 = 한국어 단일 문자열: "당신은 한국어로 코칭하는 면접 평가자입니다. observation 은 한국어 1~2문장, evidence_quote 는 사용자 답변 substring 만 인용."

## 핵심 로직 / 변경 요약

```
[Pre]  system prompt = 영어 단일 + template = 영어 Output Schema + 한국어 few-shot 혼재
       → LLM 출력 언어 비결정 학습 신호 → 30/30 영문 출력 발생
       evidence_quote 룰 = line 34 단문, "verbatim" 만 명시
       → LLM 이 루브릭 정의문 / 질문 본문 substring 도 verbatim 으로 해석 → anchor 인용 반복

[Post] system prompt = 한국어 (역할 + 한국어 observation + transcript substring evidence)
       template = 한국어 Output Schema 예시 + 한국어 few-shot 단일
       Rules #3 = "from <<<USER_ANSWER>>>...<<<END_USER_ANSWER>>>" 마커 안의 substring 만
                   허용 + 그 외 텍스트 (질문 / 루브릭 정의문 / 그 외) 인용 금지 명시
       Rules #4 = observation 한국어 1+음절 강제
```

## 의존

- 선행: Phase 0 (API contract 확인)
- 외부: 없음 (OpenAI GPT-4o-mini 기존 의존 그대로)

## 테스트 케이스 (BE-02 와 동일 PR 머지 — 본 Task 단독은 prompt 자산만)

- [ ] `RubricScorerPromptBuilderTest.buildSystemPrompt_contains_korean_role` — system prompt 에 "한국어" substring 포함
- [ ] `RubricScorerPromptBuilderTest.build_prompt_contains_verbatim_rule` — `build()` 결과 prompt 에 "verbatim substring" + "FORBIDDEN" substring 포함
- [ ] template snapshot 검증: 영어 Output Schema 예시 (`"The candidate explained the principle..."`) substring 부재 assert

## 완료 기준

- [ ] 변경 파일 commit (단일 커밋, 본 Task 범위)
- [ ] BE-02 와 묶음 PR (#1) 안에서 Phase 1 회귀 테스트 green
- [ ] template diff = prompt 본문 영어 잔존 0 (`grep -nE "[A-Z][a-z]+ [a-z]+" backend/src/main/resources/prompts/template/turn-rubric-scorer.txt` 으로 영문 문장 잔존 점검 — 영문 코드 식별자 / JSON key 는 예외)
- [ ] `RubricScorerPromptBuilderTest` + template snapshot 회귀 green
- [ ] **`code-reviewer-backend` 실행** (PR#1 머지 직전, MANDATORY) — prompt 작성자 셀프 승인 금지

## 커밋 메시지

```
refactor(BE): verbal scorer prompt 한국어 + verbatim 룰 강화
```

## 비고

- prompt 본문에 보안 마커 (`<<<USER_ANSWER>>>...<<<END_USER_ANSWER>>>`) 보존 강제 (OWASP A03 — `tech-spec.md` NF 보안 항목).
- few-shot 한국어 예시 수정 시 evidence_quote 가 동일 예시 안의 user_answer substring 인지 자체 검증 후 작성 (LLM 학습 신호 정합).
