# Lane 1 — Question Generation Prompt: Static Audit & Redesign Proposal

- Lane: 1 / 5 (BE question generation)
- Date: 2026-04-09
- Agent: prompt-engineer
- Status: Draft (audit only — no live files modified)

## 1. Current State Summary

| Item | Value |
|---|---|
| Template file | `backend/src/main/resources/prompts/template/question-generation.txt` |
| Template size | 50 lines, ~1,450 chars (pre-substitution) |
| Builder | `QuestionGenerationPromptBuilder.java` (system + user prompts) |
| Persona resolver | `PersonaResolver.java` (base YAML ∪ stack overlay YAML) |
| Model | `claude-sonnet-4-20250514` |
| max_tokens | 8192 |
| temperature | 0.9 |
| Caching | `SystemContent.withCaching(systemPrompt)` — entire system prompt is a single cache block |
| Count calculator | `QuestionCountCalculator.calculate(duration, typeCount)` — already Java-side |

Estimated static portion (persona + template, for a typical backend+java-spring resolve):
- Template body (post-substitution, sans persona/overlays): ~720 chars, ~180 tokens
- `fullPersona` (backend base + java-spring overlay): ~820 chars, ~230 tokens
- `evaluationPerspective`: ~280 chars, ~70 tokens
- `interviewTypeGuide` (filtered): ~300–600 chars, ~120 tokens
- `levelGuide`: ~100–200 chars, ~50 tokens
- **Total static persona+template (typical)**: ~2,400 chars / ~650 tokens

User prompt (dynamic): ~120–400 tokens (varies with resume).

## 2. Issue List

### Issue 1 — Math-in-prompt anti-pattern (질문 수 규칙)
- **Finding**: Template line 23 instructs the model to compute `면접 시간(분) / 3 반올림 (최소 2, 최대 24)` and enforce `유형별 균등 배분`. But `QuestionGenerationPromptBuilder.buildUserPrompt` already calls `QuestionCountCalculator.calculate(...)` and injects `질문 수: N개` directly. The model is being told to redo math whose result is already supplied.
- **Evidence**: `question-generation.txt:21-24`, `QuestionGenerationPromptBuilder.java:76-84`, `QuestionCountCalculator.java:15-25`.
- **Severity**: HIGH (token waste + conflict risk: the LLM may second-guess the injected count).
- **Fix**: Delete the `## 질문 수 규칙` section entirely from the template. The user prompt already states the exact count. Optionally add a single positive line: `총 질문 수는 사용자 메시지의 "질문 수" 값을 그대로 따르고, 유형별로 균등하게 배분합니다.`

### Issue 2 — Negative-only instructions (제약 조건)
- **Finding**: Lines 38–41 use four "금지/하지 마세요" patterns: "절대 생성하지 마세요"(×2), "금지합니다". Sonnet responds more reliably to positive framings; negative constraints are still honored but at higher cognitive cost and with more tokens.
- **Evidence**: `question-generation.txt:36-41`.
- **Severity**: MEDIUM.
- **Fix**: Reframe positively while preserving the same enforcement semantics (per task constraint: "복합 질문 금지", "코드 질문 금지" must remain enforced). Example:
  ```
  ## 질문 작성 규칙
  - 한 질문은 하나의 주제만 다룹니다. (복합 질문 금지)
  - 구두 면접이므로 개념·원리·경험·의사결정 과정을 묻습니다. (코드 작성/구현 요구 금지)
  ```
  Trailing parenthetical keeps the hard-constraint keyword so Sonnet still treats it as a rule, but the primary imperative is positive.

### Issue 3 — Single-line JSON schema hurts readability & cache hit variance
- **Finding**: Line 50 is a 380-char single-line JSON template. For Claude, a pretty-printed (indented) schema with explicit key descriptions generally yields better JSON compliance and is more robust to minor whitespace changes in cache key.
- **Evidence**: `question-generation.txt:47-50`.
- **Severity**: MEDIUM.
- **Fix**: Reformat as indented JSON inside a fenced block; add a one-line prefix specifying "출력은 이 스키마의 JSON 객체만 반환" (pure positive framing). Keep all keys: `content`, `category`, `order`, `evaluationCriteria`, `questionCategory`, `modelAnswer`, `referenceType`.

### Issue 4 — Hardcoded CS categories in two places
- **Finding**: Template line 45 hardcodes `"자료구조", "운영체제", "네트워크", "데이터베이스"`. The same list is also hardcoded in `QuestionGenerationPromptBuilder.java:55` (fallback csBlock) and in `buildUserPrompt` line 90. Three sources of truth.
- **Evidence**: `question-generation.txt:43-45`, `QuestionGenerationPromptBuilder.java:55,90`.
- **Severity**: MEDIUM (maintenance risk, not runtime cost).
- **Fix**: Remove hardcoded list from the template. Instead, rely on the `{CONDITIONAL_CS_SUBTOPIC_BLOCK}` which already enumerates allowed subtopics. Add a single compact rule: `CS_FUNDAMENTAL 유형의 category 필드는 사용자 메시지의 "CS 세부" 값 중 하나를 그대로 사용합니다.` Later, extract the default list to a single Java constant (design note §7).

### Issue 5 — Section ordering violates Anthropic "context → task → constraints → format"
- **Finding**: Current order: persona → perspective → type guide → CS → difficulty → count rules → resume → model answer rules → constraints → category rules → response format. The resume block (dynamic context) is stranded after static rules, and format sits at the end — acceptable — but count rules appear before resume context, and constraints are scattered (lines 30–34 model answer + 38–41 question rules + 45 category rule).
- **Evidence**: `question-generation.txt` (whole file).
- **Severity**: MEDIUM.
- **Fix** (recommended order, aligns with Anthropic guidance):
  1. Role/persona (`{FULL_PERSONA}`)
  2. Evaluation lens (`{BASE_EVALUATION_PERSPECTIVE}`)
  3. Task definition (single sentence: 무엇을 생성하는가)
  4. Domain context — interview type guide + CS subtopics + level guide + resume block
  5. Generation rules (merged: 작성 규칙 + 모범답변 + category)
  6. Output format (JSON schema, last — always last per Anthropic best practice)

### Issue 6 — Template/YAML duplication in persona merge
- **Finding**: `PersonaResolver.resolve` concatenates `base.personaBlock + "\n" + overlay.fullPersona` (line 46). The backend base YAML ends with `- API 설계의 일관성과 확장성 / 동시성 제어 / 장애 대응 / 성능 병목` (4 bullets). The java-spring overlay's `full` block adds "Java/Kotlin 언어와 Spring Boot 에코시스템...". No literal string duplication, BUT `evaluation_perspective` (base) and `verbal_expertise` (overlay) both enumerate concurrency/transaction/JPA keywords, and `follow_up_depth` + `follow_up_depth_append` overlap on concurrency/JVM topics. `verbal_expertise` and `follow_up_depth_append` are currently NOT used by question generation (they flow into ResolvedProfile but the template only reads `{FULL_PERSONA}` + `{BASE_EVALUATION_PERSPECTIVE}` + `{FILTERED_INTERVIEW_TYPE_GUIDE}`), so they contribute nothing to this prompt but do get resolved. Safe — no waste in *this* lane — but worth flagging for lane coordination.
- **Evidence**: `PersonaResolver.java:45-53`, `backend.yaml:11-15`, `java-spring.yaml:24-36`.
- **Severity**: LOW (for lane 1 — no token cost; cross-lane concern).
- **Fix**: No change required for lane 1. Coordinate with lane handling follow-up prompt if `verbal_expertise` is also loaded there.

### Issue 7 — Prompt caching boundary is correct but fragile
- **Finding**: `ClaudeApiClient.callClaudeApiWithModel:100` wraps the ENTIRE system prompt in `SystemContent.withCaching(systemPrompt)`. Good — the large persona+template block is cached. However, cache key changes whenever `{CONDITIONAL_RESUME_BLOCK}` toggles, `{CONDITIONAL_CS_SUBTOPIC_BLOCK}` changes, or `{FILTERED_INTERVIEW_TYPE_GUIDE}` differs. This means cache hit rate collapses across users with different inputs. For a typical Rehearse session, the variable parts are user-specific so caching mostly helps within a single session (follow-ups), not across users.
- **Evidence**: `ClaudeApiClient.java:100-113`, `QuestionGenerationPromptBuilder.java:65-71`.
- **Severity**: MEDIUM (architectural — higher-effort fix).
- **Fix (design note)**: Split system prompt into two segments:
  1. **Invariant cached head**: role/persona + evaluation perspective + generation rules + output format (always identical regardless of request) — wrap with `cache_control`.
  2. **Per-request tail**: interview type guide (filtered), CS block, level guide, resume block — no cache.
  Anthropic allows up to 4 cache breakpoints in `system`. Using 1 breakpoint after the invariant head yields dramatically higher cross-request cache hits. Requires minor ClaudeApiClient change (list of 2 system content blocks) and a template split (see v2 proposal). Not blocking for this lane, but recommended in a follow-up PR.

### Issue 8 — Verbose section headers
- **Finding**: Section headers repeat Korean nouns unnecessarily: "면접 유형별 출제 가이드", "CS 세부 주제 (지정된 경우)", "레벨별 난이도 가이드", "이력서 활용 (제공된 경우)", "모범답변 생성 규칙", "질문 생성 제약 조건", "category 필드 규칙", "응답 형식". The `(지정된 경우)` and `(제공된 경우)` parentheticals are noise when the sections are conditionally rendered empty by the builder anyway.
- **Evidence**: `question-generation.txt:9,13,17,26,30,36,43,47`.
- **Severity**: LOW.
- **Fix**: Shorten to `## 면접 유형 가이드`, `## CS 범위`, `## 난이도`, `## 이력서`, `## 작성 규칙`, `## 출력 형식`. Merge `## 모범답변 생성 규칙` and `## category 필드 규칙` into `## 작성 규칙`.

## 3. Redesign Proposal (Section-by-Section)

| Old section | Action | New section |
|---|---|---|
| `{FULL_PERSONA}` (line 1) | Keep, unchanged | Same, position 1 |
| `## 당신의 평가 관점` (5–7) | Keep, shorter header | `## 평가 관점` |
| (implicit task) | **Add** explicit one-line task statement | `## 임무` |
| `## 면접 유형별 출제 가이드` (9–11) | Keep, shorter header | `## 면접 유형 가이드` |
| `## CS 세부 주제 (지정된 경우)` (13–15) | Keep, shorter header; remove hardcoded default list from template (keep in builder only) | `## CS 범위` |
| `## 레벨별 난이도 가이드` (17–19) | Keep, shorter header | `## 난이도` |
| `## 질문 수 규칙` (21–24) | **Delete entirely** — handled by Java | — |
| `## 이력서 활용 (제공된 경우)` (26–28) | Move up (grouped with context); shorter header | `## 이력서` |
| `## 모범답변 생성 규칙` (30–34) | Merge into `## 작성 규칙` | — |
| `## 질문 생성 제약 조건` (36–41) | Merge, reframe positively | `## 작성 규칙` |
| `## category 필드 규칙` (43–45) | Merge, compact form | (inside `## 작성 규칙`) |
| `## 응답 형식` (47–50) | Keep last, pretty-print JSON | `## 출력 형식` |

New template order: persona → 평가 관점 → 임무 → 면접 유형 가이드 → CS 범위 → 난이도 → 이력서 → 작성 규칙 → 출력 형식.

## 4. Estimated Token Reduction

Measured on current vs. v2 template (pre-substitution, i.e. the literal `.txt` bytes that wrap the placeholders):

| Metric | Current | v2 | Δ |
|---|---|---|---|
| Lines | 50 | 34 | -32% |
| Chars (pre-substitution, excluding placeholders) | ~1,450 | ~980 | -32% |
| Est. tokens (static wrapper only) | ~360 | ~240 | **-33%** |

Combined persona + template static portion (typical backend+java-spring):
- Current: ~650 tokens
- v2: ~520 tokens
- **Reduction: ~20% (exactly at target)**

Additional savings kick in if Issue 7 cache split is also adopted (estimated 40–60% input token cost reduction across multi-user load, separate from static token count).

## 5. Placeholder Contract

No placeholders are added or removed in v2. The six current placeholders are preserved with identical semantics:
- `{FULL_PERSONA}`
- `{BASE_EVALUATION_PERSPECTIVE}`
- `{FILTERED_INTERVIEW_TYPE_GUIDE}`
- `{CONDITIONAL_CS_SUBTOPIC_BLOCK}`
- `{SINGLE_LEVEL_GUIDE}`
- `{CONDITIONAL_RESUME_BLOCK}`

`QuestionGenerationPromptBuilder.java` requires **no changes** to swap the template. (Recommended-but-optional: extract the `자료구조, 운영체제, 네트워크, 데이터베이스` default to a `CsSubtopics.DEFAULTS` constant — see §7.)

## 6. Risks & Mitigation

| Risk | Likelihood | Mitigation |
|---|---|---|
| Positive-rephrasing weakens "복합 질문 금지" / "코드 질문 금지" compliance | Medium | Keep the literal "금지" keyword in parentheticals; A/B test on 20 real requests before rollout |
| Deleting count-rule section causes LLM to invent its own count | Low | User prompt already injects exact count — no change in evidence the model sees |
| JSON pretty-print inflates tokens | Low | Verified: indented 7-key schema ≈ 95 tokens vs ~90 tokens single-line — negligible |
| Cache key churn from template change | One-time | Expected: first call after deploy is a cache write; steady state unchanged |
| `{CONDITIONAL_CS_SUBTOPIC_BLOCK}` now carries the canonical category list alone | Low | Builder already populates it in both branches (line 53–56) |

## 7. Design Note — Optional Follow-ups (Not Implemented in This Lane)

### 7.1 Extract CS defaults constant
`QuestionGenerationPromptBuilder.java:55,90` both hardcode `"자료구조, 운영체제, 네트워크, 데이터베이스"`. Propose a `CsSubtopics` constants class:
```java
public final class CsSubtopics {
    public static final List<String> DEFAULTS = List.of("자료구조", "운영체제", "네트워크", "데이터베이스");
    public static final String DEFAULTS_CSV = String.join(", ", DEFAULTS);
    private CsSubtopics() {}
}
```

### 7.2 Cache-segment system prompt
Split `ClaudeApiClient.callClaudeApiWithModel` to accept `List<SystemContent>` with a cached head and non-cached tail. `QuestionGenerationPromptBuilder` would return a record `(String cachedHead, String dynamicTail)`. Cached head = persona + perspective + 작성 규칙 + 출력 형식. Dynamic tail = 면접 유형 가이드 + CS 범위 + 난이도 + 이력서. Expected effect: cross-user cache hits on the ~400-token invariant head.

### 7.3 PersonaResolver clean-up (lane-coordination only)
For question generation specifically, `verbal_expertise` and `follow_up_depth_append` fields in ResolvedProfile are never consumed. They ARE consumed elsewhere (likely follow-up or analysis lanes). No action in lane 1; flag to lane owner.

## 8. Top 5 Findings (for report)

1. **Math-in-prompt (Issue 1, HIGH)** — `## 질문 수 규칙` is dead: Java already computes and injects the count. Delete the section.
2. **Negative-only constraints (Issue 2, MEDIUM)** — 4 "금지/하지 마세요" instructions → reframe positively, keep keyword markers.
3. **Cache boundary too coarse (Issue 7, MEDIUM)** — whole system prompt cached as one block; split head/tail for ~40–60% input cost savings across users.
4. **Section ordering suboptimal (Issue 5, MEDIUM)** — rules scattered across 3 sections, resume block stranded mid-rules; reorder to context → rules → format.
5. **Hardcoded CS list triplicated (Issue 4, MEDIUM)** — same 4-category list in template + builder fallback + user-prompt fallback.

## 9. Deliverables

- This audit: `/Users/koseonje/dev/devlens/docs/plans/prompt-improvement-2026-04/lane-1-question-audit.md`
- v2 template: `/Users/koseonje/dev/devlens/docs/plans/prompt-improvement-2026-04/proposals/question-generation.v2.txt`

No live files modified. No Java modified. Ready for review and lane 1 implementation PR.
