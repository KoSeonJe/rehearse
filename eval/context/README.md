# eval/context — Context Engineering Token Measurement

## Purpose

Verifies that the 4-layer context assembly (`InterviewContextBuilder`) stays within the
token budget: **avg ≤ 8,000 tokens AND max ≤ 9,000 tokens** per session turn.

## How to Run

```bash
# From repo root
python3 eval/context/measure_tokens.py --sessions eval/context/fixtures/*.json
```

Optional: use tiktoken for more accurate counting (requires `pip install tiktoken`):

```bash
python3 eval/context/measure_tokens.py \
  --sessions eval/context/fixtures/*.json \
  --encoding cl100k_base
```

## What Counts as PASS

| Condition | Threshold |
|-----------|-----------|
| Average total tokens across all fixtures | ≤ 8,000 |
| Maximum total tokens in any single fixture | ≤ 9,000 |

Exit code `0` = PASS, `1` = FAIL.

## Layer Breakdown

| Layer | Java Class | Script Simulation |
|-------|-----------|-------------------|
| L1 Fixed | `FixedContextLayer` | Exact GLOBAL_CORE + callType skeleton text, same chars |
| L2 State | `SessionStateLayer` | JSON-serialized state snapshot from fixture fields |
| L3 Dialogue | `DialogueHistoryLayer` | Sliding window last-5 turns; older turns as compacted summary placeholder |
| L4 Focus | `FocusLayer` | Per-callType fragment rendered from last exchange |

## Heuristic vs tiktoken

The Java `TokenEstimator` uses **4 chars = 1 token** (cl100k_base approximation for
Korean/English mixed text). The Python script mirrors this heuristic by default.

When `--encoding cl100k_base` is passed **and** `tiktoken` is installed, the script
uses tiktoken for higher accuracy. For Korean text, tiktoken typically produces counts
5–15% higher than the 4-char heuristic (Korean characters are multi-byte but tiktoken
encodes them more finely). Expect actual staging numbers to be slightly higher than
what this script reports.

## What This Script Cannot Measure

**L1 cache hit ratio** — the plan targets ≥ 95% cache hit rate for L1 (Claude
`cache_read_input_tokens`). This is measured from **staging Prometheus** metrics
(`rehearse.ai.context.cache_hit_ratio`), not from this offline script.

**Compaction accuracy** — whether the compacted summary preserves all `covered_topics`
is a manual review task (5-session checklist in `MANUAL_AB_PROTOCOL.md`).

## Fixture Coverage

| File | Level | Track | Focus |
|------|-------|-------|-------|
| session-1.json | JUNIOR | STANDARD | CONCEPT-heavy (JVM/GC) |
| session-2.json | MID | STANDARD | Mixed (DB indexing, transactions) |
| session-3.json | SENIOR | STANDARD | EXPERIENCE-heavy (distributed systems) |
| session-4.json | MID | RESUME | active_chain, resume-based Q&A |
| session-5.json | JUNIOR | STANDARD | Edge case — very short answers |
