# eval/context — Context Engineering Token Measurement

## Purpose

Verifies that the 4-layer context assembly (`InterviewContextBuilder`) stays within the
token budget: **avg ≤ 8,000 tokens AND max ≤ 9,000 tokens** per session turn (Standard
track gate). Resume track fixtures provide visibility into Resume 4 callType token
distribution; the gate applies only to Standard fixtures.

## How to Run

```bash
# From repo root — auto-discover all eval/context/fixtures/session-*.json
python3 eval/context/measure_tokens.py

# Explicit subset
python3 eval/context/measure_tokens.py --sessions eval/context/fixtures/session-resume-*.json
```

Optional: use tiktoken for more accurate counting (requires `pip install tiktoken`):

```bash
python3 eval/context/measure_tokens.py --encoding cl100k_base
```

## What Counts as PASS

| Condition | Threshold |
|-----------|-----------|
| Standard fixture average total tokens | ≤ 8,000 |
| Standard fixture maximum total tokens | ≤ 9,000 |

Exit code `0` = PASS (or Resume-only fixture set), `1` = FAIL.

## Layer Breakdown

| Layer | Java Class | Script Simulation |
|-------|-----------|-------------------|
| L1 Fixed | `FixedContextLayer` | Loads same template files from `backend/src/main/resources/prompts/template/` (resume/ first) |
| L2 State | `SessionStateLayer` | JSON-serialized state snapshot from fixture `runtimeState` / legacy fields |
| L3 Dialogue | `DialogueHistoryLayer` | Sliding window last-5 turns; older turns as compacted summary placeholder |
| L4 Focus | `FocusLayer` | Per-callType fragment rendered from `focusHints` (mirrors `FocusLayer.buildResume*`) |

## Resume 4 callType Visibility

After per-fixture lines, the script prints a Resume block listing each of:

- `resume_playground_opener` (L4 cap 600)
- `resume_playground_responder` (L4 cap 1000)
- `resume_chain_interrogator` (L4 cap 1200)
- `resume_wrap_up` (L4 cap 600)

This makes it trivial to spot the operational regression that motivated the fix
(prompt ≈ 272 tokens because L1 template + L4 fragment were missing — both should now
contribute several hundred tokens each).

## Heuristic vs tiktoken

The Java `TokenEstimator` uses **4 chars = 1 token**. The Python script mirrors this
heuristic by default. With `--encoding cl100k_base` (tiktoken), Korean text typically
produces 5–15% higher counts.

## What This Script Cannot Measure

- **L1 cache hit ratio** — measured from staging Prometheus
  (`rehearse.ai.context.cache_hit_ratio`).
- **Compaction accuracy** — manual review task.
- **L4 cap enforcement** — the Java `FocusLayer.render` throws on overflow; the script
  truncates silently. Use `ContextEngineeringArchTest` + integration tests for the hard
  guarantee.

## Fixture Coverage

| File | Level | Track | Focus |
|------|-------|-------|-------|
| session-resume-1.json | MID | RESUME | resume_playground_opener (turn 0, project_info + opener_question) |
| session-resume-2.json | MID | RESUME | resume_playground_responder (turn 3, expected_claims + user_answer) |
| session-resume-3.json | MID | RESUME | resume_chain_interrogator (L3 chain, hot key trade-off) |
