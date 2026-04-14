#!/usr/bin/env python3
"""
generate_tts_content.py 가 만든 tts_mapping.json 을 seed SQL 파일에 주입.

기존 INSERT:
    INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
    ('X', 'Q', 'CAT', 'A', 'MODEL_ANSWER', TRUE, NOW());

변환 후:
    INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
    ('X', 'Q', 'Q_TTS', 'CAT', 'A', 'MODEL_ANSWER', TRUE, NOW());

사용법:
    python scripts/seed/inject_tts_into_seed.py \
        --seed-dir backend/src/main/resources/db/seed \
        --mapping scripts/seed/out/tts_mapping.json \
        [--dry-run]
        [--files backend-kotlin-spring.sql behavioral-junior.sql ...]  # 생략 시 전 파일
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path


INSERT_STMT_RE = re.compile(
    r"(INSERT IGNORE INTO question_pool\s*)"
    r"\(([^)]+)\)"
    r"(\s*VALUES\s*)"
    r"\(\s*'((?:[^']|'')*)'\s*,\s*'((?:[^']|'')*)'\s*,\s*",
    re.IGNORECASE,
)


def sql_escape(s: str) -> str:
    return s.replace("'", "''")


def row_key(cache_key: str, content: str) -> str:
    h = hashlib.sha1(content.encode("utf-8")).hexdigest()[:12]
    return f"{cache_key}||{h}"


def inject_into_text(text: str, mapping: dict, log: list[str]) -> tuple[str, int, int]:
    """
    반환: (new_text, injected_count, skipped_count)
    """
    injected = 0
    skipped = 0

    def replace(m: re.Match) -> str:
        nonlocal injected, skipped
        header = m.group(1)
        cols_raw = m.group(2)
        values_kw = m.group(3)
        cache_key_lit = m.group(4)
        content_lit = m.group(5)
        tail_start = m.end()

        cols = [c.strip() for c in cols_raw.split(",")]
        if "tts_content" in cols:
            skipped += 1
            return m.group(0)

        cache_key = cache_key_lit.replace("''", "'")
        content = content_lit.replace("''", "'")
        key = row_key(cache_key, content)

        entry = mapping.get(key)
        if not entry:
            skipped += 1
            log.append(f"  [miss] {cache_key} :: {content[:40]}")
            return m.group(0)

        tts = entry["tts_content"]
        # 컬럼 삽입: content 다음에 tts_content
        new_cols = []
        for c in cols:
            new_cols.append(c)
            if c == "content":
                new_cols.append("tts_content")
        new_cols_str = ", ".join(new_cols)

        # 값 부분도 content 다음에 tts_content 값 삽입
        # m 은 VALUES ( 'cache', 'content', 까지 매칭 → 그 뒤에 tts 값 추가
        injected += 1
        return (
            f"{header}({new_cols_str}){values_kw}"
            f"('{cache_key_lit}', '{content_lit}', '{sql_escape(tts)}', "
        )

    new_text = INSERT_STMT_RE.sub(replace, text)
    return new_text, injected, skipped


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed-dir", required=True)
    parser.add_argument("--mapping", required=True)
    parser.add_argument("--files", nargs="*", default=None)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    seed_dir = Path(args.seed_dir)
    mapping = json.loads(Path(args.mapping).read_text("utf-8"))
    print(f"[load] 매핑 {len(mapping)}건")

    if args.files:
        targets = [seed_dir / name for name in args.files]
    else:
        targets = sorted(seed_dir.glob("*.sql"))

    total_inj = 0
    total_skip = 0
    for sql_path in targets:
        if not sql_path.exists():
            print(f"[skip] {sql_path} 없음")
            continue
        text = sql_path.read_text("utf-8")
        log: list[str] = []
        new_text, injected, skipped = inject_into_text(text, mapping, log)
        total_inj += injected
        total_skip += skipped
        if injected > 0 and not args.dry_run:
            sql_path.write_text(new_text, "utf-8")
        status = "DRY" if args.dry_run else "WRITE"
        print(f"[{status}] {sql_path.name}: injected={injected} skipped={skipped}")
        for line in log[:5]:
            print(line)
        if len(log) > 5:
            print(f"  ... ({len(log) - 5} more)")

    print(f"\n[total] injected={total_inj}, skipped={total_skip}")
    if not args.dry_run and total_inj > 0:
        print("다음 명령으로 변경 사항 검증:")
        print(
            f"  grep -L 'tts_content' {seed_dir}/*.sql  # tts_content 없는 파일 목록"
        )


if __name__ == "__main__":
    main()
