# Lane 3 — Verbal Analyzer Prompt Audit (Lambda, GPT-4o)

Scope: `lambda/analysis/analyzers/verbal_prompt_factory.py` (primary), `verbal_analyzer.py` (caller), `prompts.py`, `json_utils.py`.
Goal: ≥20% fewer input tokens on a typical system prompt while preserving the `build_system_prompt(position, tech_stack, feedback_perspective)` signature and downstream JSON keys.

---

## 1. Constant Inventory

| Constant | File / Lines | Char count (approx) | Injected every request? |
|---|---|---|---|
| `KOREAN_INSTRUCTION` | `prompts.py` | 52 | Yes (prefix of SYSTEM_TEMPLATE) |
| `DEFAULT_TECH_STACKS` (5 entries) | `verbal_prompt_factory.py:6-12` | 140 | No (dict lookup only) |
| `MINIMAL_PERSONAS` (5 entries) | `:14-20` | 330 (each ~60-70) | 1 injected (~65) |
| `VERBAL_EXPERTISE` (5 entries) | `:22-95` | ~2,650 total; per-entry: BACKEND 530, FRONTEND 620, DEVOPS 610, DATA 600, FULLSTACK 580 | 1 injected (~600) |
| `FEEDBACK_PERSPECTIVES` (3 entries) | `:97-107` | 400 total (~130 each) | 1 injected (~130) or 0 |
| `SYSTEM_TEMPLATE` scaffolding (headers + evaluation + JSON schema) | `:109-124` | ~520 | Yes |
| `USER_TEMPLATE` | `:126-128` | ~80 + question/transcript | Yes |

Typical assembled system prompt (BACKEND_JAVA_SPRING + TECHNICAL):
`52 (korean) + 65 (persona) + 530 (expertise) + 130 (perspective) + 520 (template) ≈ 1,300 chars ≈ 780 tokens` (Korean averages ~1.6 chars/token).

---

## 2. Issue List

| # | Severity | Issue |
|---|---|---|
| I1 | HIGH | **`VERBAL_EXPERTISE` has a 20-30 term keyword dictionary per stack.** Keyword lists are used only indirectly ("키워드 사전 기반으로..."); GPT-4o rarely needs all terms in-context to detect well-formed Korean tech speech. Estimated 40%+ compression possible. |
| I2 | HIGH | **`tone_comment` is generated but never consumed.** Grep confirms: backend `TimestampFeedback` entity has no `toneComment` column; `handler.py` never reads `verbal.get("tone_comment")`. Pure wasted output tokens (and the schema line in SYSTEM_TEMPLATE + JSON skeleton). |
| I3 | HIGH | **Duplicate filler-word list across files.** The filler list `"음","어","그"...` appears both in `SYSTEM_TEMPLATE` (verbal_prompt_factory.py:117) and in the dead `_SYSTEM_PROMPT` inside `verbal_analyzer.py:15-40`. The latter is only used when `position` is falsy — still emits its own ~700-char prompt. Consolidate. |
| I4 | MEDIUM | **`FEEDBACK_PERSPECTIVES` selection logic is OK** — `build_system_prompt` already injects only 1 (`verbal_prompt_factory.py:137`). NOT "all 3". Issue instead: `verbal_analyzer.py` default falls back to `""` when perspective missing, while `handler.py:350` defaults to `"TECHNICAL"`. Make `"TECHNICAL"` the single source of truth in the factory. |
| I5 | MEDIUM | **`MINIMAL_PERSONAS` is nearly redundant.** All 5 entries follow template `"{position}({stack}) 면접 답변의 언어적 커뮤니케이션을 분석합니다."` — 62 chars of boilerplate × 5 = 310 chars that could be a 1-line f-string. |
| I6 | MEDIUM | **`DEFAULT_TECH_STACKS` is fine as module constant** (not config-worthy — only 5 stable values, no env-driven override needed). Keep, but colocate with factory. Not a leak risk. |
| I7 | LOW | **Symbols `✓ / △ / →`.** Used in `comment` and `attitude_comment` 3-line format. Backend parses them as plain string (`_legacy_string_to_block` in handler.py:364 wraps raw string, and DB stores TEXT). No explicit Unicode normalization, but these are BMP chars (U+2713, U+25B3, U+2192) — safe in UTF-8. No parsing regex dependency found in BE. Low risk, keep. |
| I8 | LOW | **`SYSTEM_TEMPLATE` section ordering.** "전문 분야" comes before perspective, which comes before "평가". That's fine, but schema is duplicated (prose instructions at step 4/5 + compact JSON skeleton at bottom). Merge into single schema block. |
| I9 | LOW | **No OpenAI prompt caching exploitation.** GPT-4o chat completions do not yet support the `cache_control` header (that's Anthropic). OpenAI added automatic prefix caching in Oct 2024 for 1024+ token prefixes — relevant only if we could consistently pin the stack-specific block. Given we always change per-question user message but the *system* prefix is identical for all questions of the same interview, prompt caching **does** apply automatically if we keep the system prompt deterministic and ≥1024 tokens. Current ~780 tokens is below the threshold; compressing won't enable or disable it meaningfully, and we don't need to chase this. Mark as non-goal. |

---

## 3. Downstream Consumer Check

`analyze_verbal()` result is consumed only in `lambda/analysis/handler.py:361-375`.

| Key returned by GPT-4o | Consumed? | Mapped to |
|---|---|---|
| `filler_word_count` | YES | `fb["fillerWordCount"]` → `TimestampFeedback.fillerWordCount` (INT) |
| `comment` | YES | `fb["verbalComment"]` via `_legacy_string_to_block()` → `TimestampFeedback.verbalComment` (TEXT JSON) |
| `attitude_comment` | YES | `fb["attitudeComment"]` via `_legacy_string_to_block()` → `TimestampFeedback.attitudeComment` (TEXT JSON, V18 migration) |
| `tone_label` | YES (thin) | `_tone_label_to_level()` → `fb["toneConfidenceLevel"]` (3-level bucket: HIGH/MEDIUM/LOW) |
| `tone_comment` | **NO** | Not read anywhere. No DB column. **Safe to drop.** |

Confirmed by grep across `backend/src/main/java`, `handler.py`, and migrations (`V4`, `V16`, `V18`). No `toneComment` field anywhere in BE.

**Token savings from dropping `tone_comment`:**
- Remove eval bullet #3 (~35 chars)
- Remove JSON skeleton key (~20 chars)
- Remove ~50-100 output tokens per call (model no longer generates the sentence)

---

## 4. Compression Proposals (Before / After)

### 4.1 VERBAL_EXPERTISE per-stack block

**BEFORE (BACKEND_JAVA_SPRING, ~530 chars):**
```
Java/Spring 답변 분석 기준:
- JVM, Spring, JPA 기술 용어의 정확한 사용
- 성능 수치(TPS, 응답시간, GC pause time) 구체적 언급
- "원인→해결→결과" 구조 설명 능력

키워드 사전:
JVM, GC, 힙 메모리, 메타스페이스, 스레드 풀, HikariCP, 커넥션 풀,
트랜잭션 격리 수준, @Transactional, 전파 속성, 롤백,
영속성 컨텍스트, 1차 캐시, 지연 로딩, 즉시 로딩, N+1, fetch join, EntityGraph,
Spring IoC, 빈 스코프, AOP, 프록시, CGLIB,
@Version, 낙관적 락, 비관적 락, 데드락,
Spring Cloud, 서킷 브레이커, Resilience4j, Spring Kafka
```

**AFTER (~200 chars, -62%):**
```
Java/Spring 백엔드: 기술용어 정확성, 성능수치(TPS/응답시간/GC) 구체성, 원인→해결→결과 구조.
핵심 영역: JVM/GC, JPA(N+1·fetch join·영속성), 트랜잭션 전파·격리, 동시성(락·풀), Spring IoC/AOP.
```

Rationale: GPT-4o already knows Java/Spring vocabulary from training. We only need to signal *which domains to prioritize* and *what structural patterns to look for*. Exhaustive term listing was a cargo-cult from early LLM days.

Applied to all 5 stacks: 2,650 → ~1,050 chars (−60%).

### 4.2 MINIMAL_PERSONAS

**BEFORE:** 5 dict entries × ~65 chars = 330 chars.
**AFTER:** 1 f-string in `build_system_prompt`:
```python
persona = f"{_LABEL[key]} 면접 답변의 언어 커뮤니케이션을 분석합니다."
```
where `_LABEL` maps `BACKEND_JAVA_SPRING → "백엔드(Java/Spring)"` (5 entries × ~25 chars = 125 chars). Savings: ~200 chars of module weight. Zero per-request savings (same injected text).

### 4.3 SYSTEM_TEMPLATE

**BEFORE (~520 chars scaffolding):** Headers `## 전문 분야`, `## 평가` (5 bullets), `## 응답` + JSON skeleton.

**AFTER (~340 chars):** Merge bullets and schema, drop `tone_comment`, drop duplicate filler list (move to single-line eval rule):
```
{minimal_persona}
전문 분야: {verbal_expertise}
{feedback_perspective}
평가 기준:
- filler_word_count: "음/어/그/아/뭐/이제/약간/좀/그러니까" 등장 횟수
- tone_label ∈ {{PROFESSIONAL|CASUAL|HESITANT|CONFIDENT|VERBOSE}}
- comment: 3줄 "✓ 잘한 점 / △ 보완할 점 / → 개선 방법" (기술 용어 정확성 포함)
- attitude_comment: 3줄 "✓ 긍정 / △ 부정 / → 개선" (구체적 표현/어투 근거 필수)

JSON만 응답:
{{"filler_word_count":0,"tone_label":"","comment":"","attitude_comment":""}}
```

### 4.4 Per-request total

| Block | v1 chars | v2 chars |
|---|---|---|
| Korean instruction | 52 | 52 |
| Persona | 65 | 65 |
| Verbal expertise | 530 | 200 |
| Perspective | 130 | 130 |
| Template scaffold | 520 | 340 |
| **Total (system)** | **~1,297** | **~787** |
| Tokens (~1.6 chars/tok) | ~810 | ~490 |
| **Reduction** | | **~39%** |

Target of ≥20% met with margin. Output tokens additionally saved by dropping `tone_comment` (~50-80 tokens per response).

---

## 5. Migration Notes

- **Signature unchanged:** `build_system_prompt(position, tech_stack, feedback_perspective=None)` and `build_user_prompt(...)` kept identical.
- **Output JSON keys:** `filler_word_count`, `tone_label`, `comment`, `attitude_comment` — ALL preserved. `tone_comment` REMOVED (confirmed unused).
- **Perspective default:** Factory now defaults to `"TECHNICAL"` internally when `feedback_perspective` is falsy (matches `handler.py:350` default). No change at call sites.
- **`MINIMAL_PERSONAS` removed** as a public constant. If any external code imports it, flag. Grep: no external imports found.
- **`DEFAULT_TECH_STACKS`:** Retained, same keys.
- **Legacy `_SYSTEM_PROMPT` in `verbal_analyzer.py`:** Out of scope for this file but flagged — it's used when `position` is None. Recommend a follow-up (Lane 3.5) to collapse it to a call to `build_system_prompt("BACKEND", "JAVA_SPRING")`.

---

## 6. Non-Goals / Risks

- **OpenAI prompt caching:** Not pursued. Automatic prefix caching activates at ≥1024 tokens; v2 is ~490 tokens. Chasing the threshold by re-inflating the prompt would be counterproductive for cost.
- **Symbol parsing risk:** `✓ △ →` already round-tripped through DB in V18 migration, no BE-side regex parsing. Safe.
- **Format drift:** Compressed expertise block may slightly reduce keyword-specific detection ability for niche terms. Mitigation: structural prompts ("핵심 영역") still list the categories, and `comment` rule explicitly asks the model to flag "기술 용어 정확성". Acceptable trade-off for 39% input token reduction.
- **Shared format with BE YAML personas (`backend.yaml` etc.):** BE personas are multi-line, 10+ line role descriptions used for generating interview questions (Claude backend). Verbal analyzer personas are 1-line analysis-role intros for GPT-4o (Lambda). Different purpose, different model, different output structure. **Not worth unifying** — would force one side to carry the other's complexity.

---

## 7. Deliverables

- This audit: `docs/plans/prompt-improvement-2026-04/lane-3-verbal-audit.md`
- Proposed v2 file: `docs/plans/prompt-improvement-2026-04/proposals/verbal_prompt_factory.v2.py`
