# Lane 2 — Follow-up Prompt Audit

- Target: `backend/src/main/resources/prompts/template/follow-up.txt`
- Model: Claude Haiku 4.5 (`temperature=0.7`, `max_tokens=1024`)
- Builder: `backend/src/main/java/com/rehearse/api/infra/ai/prompt/FollowUpPromptBuilder.java`
- Caller: `ClaudeApiClient.generateFollowUpQuestion`
- Scope: static audit + minimal, behavior-preserving redesign
- Target reduction: ≥15% tokens

## 1. Recent iteration history (DO NOT UNDO)

Commits touching this file:

| Commit | Summary | What it introduced — must be preserved |
|--------|---------|------------------------------------------|
| `03f5c3a` | refactor: 후속질문 자연스러움 개선 (이전 답변 연결·모르겠다 유연성) | (a) 3-step procedure (핵심 개념 추출 → 인용 구문 → 심화 질문); (b) multi-round 인용 대상 = "현재 답변"만; (c) "이전 후속" 섹션은 참고·중복 회피용; (d) skip 조건 3가지 (모르겠다/공백/인용 키워드 없음); (e) 자연스럽게 다음 주제로 넘어가는 면접관 태도 가이드 |
| `daf7394` | feat: GPT-4o-mini-audio 후속질문 통합 (Whisper+GPT → 1회) | `answerText` 필드 존재 이유 — audio 경로에서 전사 결과를 담는 단일 왕복 |
| `e3274fd` | feat: 프롬프트 빌더 리팩토링 + 토큰 최적화 | 기존 토큰 최적화 이력 있음 — 추가 감축 여지 제한적 |
| `04133a6` | feat: Position × TechStack 2차원 페르소나 | `{MEDIUM_PERSONA}`, `{FOLLOWUP_DEPTH}` placeholder 구조 |

**Preserved behaviors (frozen)**:

1. Skip logic and the three skip conditions (모르겠다 표현 / 공백·한두 단어 / 인용 키워드 없음).
2. JSON schema: `skip`, `skipReason`, `answerText`, `question`, `reason`, `type`, `modelAnswer`. Both variants.
3. `answerText` is mandatory on both skip and non-skip paths.
4. Quote-first question generation rule (핵심 개념 추출 → 인용 구문 → 심화 질문).
5. Multi-round semantics: "이전 후속" is reference-only; quotation source is always the latest "현재 답변".
6. Type enum: `DEEP_DIVE | CLARIFICATION | CHALLENGE | APPLICATION`.
7. One follow-up question only; no compound questions.
8. 모범답변 guidance: 2–4 sentences, core concept + 실무 관점.
9. Placeholders `{MEDIUM_PERSONA}`, `{FOLLOWUP_DEPTH}`.

## 2. Token estimate

Raw file ≈ 67 lines / ~1,730 characters after placeholder fill.
Rough token estimate (Korean-heavy, ~1.6 chars/token for mixed Hangul + ASCII): **~1,080 tokens** at the system-prompt side.

v2 target after trimming: **~900 tokens** (~17% reduction). Achieved primarily via:
- collapsing long explanatory sentences into bullet fragments,
- dropping the "왜 좋은가 / 왜 나쁜가" prose into short annotations,
- compressing the JSON format preamble,
- de-duplicating the "인용 대상은 현재 답변" reminder (stated once in procedure, once in rules — keep one).

## 3. Issue list

### I-1. `질문 생성 절차` wording density [severity: low]
Lines 16–21. Step 1 uses "구체 키워드·개념·예시·수치·고유명사를 최소 1개 식별" — long enumeration that can compress to "구체 키워드(개념/예시/수치/고유명사) 1개 이상 식별" without losing meaning.
Step 2 has three example phrases — keep two (the third is redundant).
Step 3 can merge into step 2 as one sentence: "그 키워드를 파고드는 단일 질문을 인용 구문 뒤에 이어 붙입니다."

**Tweak**: ~30 tokens saved, meaning identical.

### I-2. Duplicate "현재 답변 = 인용 대상" guidance [severity: low]
Line 19 (step 2), line 25 (rule), and line 26 (multi-round rule) all restate the same constraint. Currently the information is spread across procedure + rules sections.
**Tweak**: keep the procedure statement + the multi-round disambiguation. Remove the redundant rule bullet on line 25 (since step 2 already enforces quoting). This is behavior-preserving because step 2 is imperative.

**Saves**: ~25 tokens.

### I-3. Few-shot "왜 좋은가 / 왜 나쁜가" verbosity [severity: medium]
Lines 48, 53 — two full sentences of meta-commentary. Haiku benefits from concise labels: "키워드: `fetch join 페이징`, `@BatchSize` 인용 + 트레이드오프 심화" is enough.
**Tweak**: replace prose with short keyword annotations.

**Saves**: ~40 tokens. Example remains representative.

### I-4. Skip section — 반복 압박 금지 [severity: low, framing]
Line 37: "지원자가 확실히 모르는 주제를 반복해서 압박하지 마세요" — negative instruction. Haiku compliance is slightly better with positive framing: "모른다고 표현하면 자연스럽게 다음 주제로 넘겨주세요".
Also the "skip=true일 때는 … null로 두고 skipReason에 …" sentence duplicates the JSON-format example in section `## 응답 형식`. Can collapse.

**Tweak**: positive reframing + drop the null-field sentence (the JSON spec already shows it).

**Saves**: ~35 tokens.

### I-5. JSON 응답 형식 서두 verbosity [severity: medium]
Lines 61: "반드시 아래 JSON 형식으로만 응답하세요. `answerText`와 `skip` 필드는 skip 여부와 무관하게 항상 포함하세요. audio 경로에서 answerText에는 전사 결과를 담으며, text 경로에서는 받은 답변을 그대로 echo하거나 빈 문자열로 두어도 됩니다. 단, skip이라고 해서 answerText를 생략하면 안 됩니다."

Three sentences restating the same rule. Can compress to one line:
> "아래 JSON으로만 응답. `skip`, `answerText`는 항상 포함하세요 (audio 경로는 전사 결과, text 경로는 원본 답변)."
The "skip이라고 해서 answerText를 생략하면 안 됩니다" reminder is redundant once we say "항상 포함".

**Saves**: ~45 tokens. **Most impactful single change.**

### I-6. `규칙` section — compound question 금지 phrasing [severity: low, framing]
Line 24: "복합 질문(A 그리고 B?) 금지" — negative. Positive alternative: "단 하나의 초점만 다루는 질문을 작성하세요." But the parenthetical example is actually helpful for Haiku; keep it. Net neutral; skip unless we can retain the example.
**Tweak**: keep as-is (parenthetical example is high signal-to-token).

### I-7. 모범답변 section — can inline into JSON preamble [severity: low]
Lines 39–41 occupy 3 lines for a single sentence. Inline as a JSON-field annotation near `modelAnswer` description, or keep as a single bullet inside `규칙`. Small win.

**Saves**: ~15 tokens.

### I-8. Haiku compliance sweep
Negative instructions currently in file:
- "복합 질문 … 금지" (L24) — keep, parenthetical disambiguation is valuable.
- "답변과 무관한 일반 지식 질문 금지" (L25) — removing this (I-2) also removes a negative.
- "후속 질문을 억지로 만들지 말고" (L31) — reframe to "억지 생성 대신 skip=true 선택".
- "반복해서 압박하지 마세요" (L37) — reframe (I-4).
- "answerText를 생략하면 안 됩니다" (L61) — remove (I-5).

Net: 3 negative clauses flipped to positive, 2 removed as redundant.

## 4. Token reduction tally

| Issue | Estimated savings |
|-------|-------------------|
| I-1 procedure density | ~30 |
| I-2 duplicate 인용 대상 | ~25 |
| I-3 few-shot verbosity | ~40 |
| I-4 skip framing | ~35 |
| I-5 JSON preamble | ~45 |
| I-7 모범답변 inline | ~15 |
| **Total** | **~190 tokens (~17.5%)** |

Meets the ≥15% target.

## 5. Risk assessment

- **Behavior drift risk**: low. No skip condition, no schema field, no quote rule, no type enum modified.
- **Haiku regression risk**: low-to-neutral. Positive reframing historically improves compliance on Haiku-tier models, and the few-shot example is preserved verbatim on the question side (only commentary trimmed).
- **Review ask**: the team should eyeball v2 against 2–3 real follow-up logs before swap (no API calls required — just visual diff).

## 6. Recommendation

**Proceed with v2.** The changes are purely compression and positive-framing, with the quote rule, skip logic, JSON schema, and multi-round semantics held byte-for-byte equivalent in meaning. Estimated ~17.5% token reduction satisfies the Lane 2 target.

v2 draft: `./proposals/follow-up.v2.txt`.
