# Task 03 — Prompt template `missing_perspectives` → `dimension_gaps`

> **위치**: `tasks/p2-be-03-prompt-template.md`
> **답하는 질문**: LLM prompt template 어떻게 변경?

---

## 목적

`AudioTurnAnalyzer` / `AnswerAnalyzer` / `FollowUp` prompt template 응답 schema 변경. `missing_perspectives` 토큰 제거 + `dimension_gaps` / `weakest_dimension` 토큰 추가. `askedPerspectives` 인자 시그니처 제거.

## 에이전트

- **구현**: `backend` — prompt template 파일 + PromptBuilder 인자 시그니처 + JsonRenderer / Formatter
- **리뷰**: `code-reviewer-backend` — prompt JSON schema / parser 회귀

## 변경 파일

- `backend/src/main/resources/ai/prompt/audio-turn-analyzer.txt` (또는 동등 경로) — `missing_perspectives` 응답 schema 제거 + `dimension_gaps` / `weakest_dimension` 추가
- `backend/src/main/resources/ai/prompt/answer-analyzer.txt` — 동일
- `backend/src/main/resources/ai/prompt/follow-up-experience.txt` — `selected_perspective` 응답 schema 제거 (Task 04 연동)
- `backend/src/main/resources/ai/prompt/follow-up-concept.txt` — 동일
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/AudioTurnAnalyzerPromptBuilder.java` — `askedPerspectives` 인자 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/AnswerAnalyzerPromptBuilder.java` — 동일
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/FollowUpPromptBuilder.java` — `askedPerspectives` 인자 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/context/AnswerAnalysisJsonRenderer.java` — `askedPerspectives` 인자 + 렌더링 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/PromptFormatters.java` — `formatPerspectives` 메서드 삭제
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedAnswerAnalysis.java` — `missingPerspectives` 필드 제거 + `dimensionGaps` / `weakestDimension` 추가
- `backend/src/test/.../AnswerAnalyzerPromptRenderingTest.java` — token 검증

## 핵심 로직

```
LLM 응답 JSON schema (After):
{
  "claims": [...],
  "dimension_gaps": {"depth": 2, "specificity": 1, ...},
  "weakest_dimension": "depth",
  "unstated_assumptions": [...],
  "recommended_next_action": "DEEPENING" | "CLARIFICATION" | "SKIP"
}

GeneratedAnswerAnalysis (After):
record GeneratedAnswerAnalysis(
  List<Claim> claims,
  Map<String, Integer> dimensionGaps,
  String weakestDimension,
  List<String> unstatedAssumptions,
  RecommendedNextAction recommendedNextAction
)
```

prompt 가이드 — "claim 부재 + 품질 저하 시 `recommended_next_action=CLARIFICATION`" 의도 자연어 가이드 (L1FN 가드 폐기 대체).

## 의존
- 선행 Task: 02 (AnswerAnalysis 신규 record + GeneratedAnswerAnalysis 매핑 정합)
- 외부: `AiResponseParser.parseOrRetry` — schema hint retry 동작 유지

## 테스트 케이스
- [ ] `AnswerAnalyzerPromptRenderingTest` — `dimension_gaps` / `weakest_dimension` 토큰 포함, `missing_perspectives` / `selected_perspective` / `askedPerspectives` 토큰 부재
- [ ] mock LLM 응답 (After schema) 입력 시 `GeneratedAnswerAnalysis` 정상 deserialize
- [ ] 기존 schema (`missing_perspectives` 포함) 응답 입력 시 schema hint retry 트리거 → 두 번째 시도 성공 / 실패 시 parser exception
- [ ] PromptBuilder 호출 시그니처 = `askedPerspectives` 인자 부재 (컴파일)

## 완료 기준
- [ ] prompt template + builder + renderer + formatter + DTO 모두 갱신
- [ ] `AnswerAnalyzerPromptRenderingTest` green
- [ ] grep `missing_perspectives` / `selected_perspective` / `askedPerspectives` / `formatPerspectives` 잔존 0
- [ ] code-reviewer-backend 실행

## 커밋 메시지

```
refactor(BE): LLM prompt schema dimension_gaps/weakest_dimension 전환
```

## 비고

prompt 파일 경로는 실제 위치 grep 후 정정 (`backend/src/main/resources/` 기준). schema hint retry 동작 유지 확인 = R2 위험 완화.
