#!/usr/bin/env python3
"""
seed SQL 파일의 `content` 컬럼(질문 텍스트)을 TTS 친화적 한국어 발음 문장으로 변환.

사용법:
    export ANTHROPIC_API_KEY=sk-...
    python scripts/seed/generate_tts_content.py \
        --seed-dir backend/src/main/resources/db/seed \
        --out scripts/seed/out/tts_mapping.json \
        [--limit 20]              # 샘플 실행
        [--resume]                # 기존 out 파일이 있으면 미처리 항목만 추가
        [--concurrency 8]

출력: tts_mapping.json
    {
      "<cache_key>||<content sha1>": {
        "content": "...",
        "tts_content": "..."
      },
      ...
    }

주의:
- 이미 `tts_content` 컬럼이 포함된 INSERT (backend-java-spring.sql, batch.sql)는 건너뜀.
- 마크다운(`**`, `` ` ``, `#`) 이 출력에 포함되면 최대 3회 재시도 후 정규식으로 제거.
- Python `*args` / `**kwargs` 같은 원문 코드 조각은 그대로 보존되도록 프롬프트에 명시.
"""

from __future__ import annotations

import argparse
import asyncio
import hashlib
import json
import os
import re
import sys
from pathlib import Path
from typing import Iterator

try:
    from anthropic import AsyncAnthropic
except ImportError:
    sys.exit("[ERROR] anthropic 패키지가 필요합니다: pip install anthropic")


# ---------- INSERT 파싱 ----------

# 두 형태 모두 매칭:
#   (cache_key, content, category, model_answer, ...)           -- 구포맷 (tts_content 없음)
#   (cache_key, content, tts_content, category, model_answer, ...) -- 신포맷
INSERT_RE = re.compile(
    r"INSERT IGNORE INTO question_pool\s*"
    r"\(([^)]+)\)\s*VALUES\s*"
    r"\(\s*'((?:[^']|'')*)'\s*,\s*'((?:[^']|'')*)'\s*,",
    re.IGNORECASE,
)


def iter_inserts(sql_text: str) -> Iterator[tuple[str, str, bool]]:
    """
    seed SQL 에서 (cache_key, content, has_tts_column) 를 yield.
    has_tts_column=True 이면 이미 tts_content 가 있는 INSERT — 스킵 권장.
    """
    for m in INSERT_RE.finditer(sql_text):
        columns = [c.strip() for c in m.group(1).split(",")]
        cache_key = m.group(2).replace("''", "'")
        content = m.group(3).replace("''", "'")
        has_tts = "tts_content" in columns
        yield cache_key, content, has_tts


def row_key(cache_key: str, content: str) -> str:
    h = hashlib.sha1(content.encode("utf-8")).hexdigest()[:12]
    return f"{cache_key}||{h}"


# ---------- LLM 호출 ----------

PROMPT_SYSTEM = (
    "너는 한국어 TTS 엔진이 자연스럽게 발음할 수 있도록 개발자 면접 질문을 변환하는 전문가다. "
    "원문의 의미를 바꾸지 않고, 특수문자와 영어 약어만 한국어 구두 발음으로 바꿔야 한다."
)

PROMPT_RULES = """변환 규칙:
1. 영어 기술 용어·약어는 한국어 발음으로 표기. 예:
   - "Spring Boot" → "스프링 부트"
   - "IoC" → "아이오씨"
   - "DI" → "디아이"
   - "R2DBC" → "알투디비씨"
   - "JPA" → "제이피에이"
   - "MVC" → "엠브이씨"
   - "API" → "에이피아이"
   - "SQL" → "에스큐엘"
   - "HTTP" → "에이치티티피"
   - "CI/CD" → "씨아이 씨디"
2. `@` 기호로 시작하는 어노테이션은 "어노테이션 " 접두어를 붙이고 이름을 한국어 발음으로. 예:
   - "@Transactional" → "어노테이션 트랜잭셔널"
   - "@SpringBootApplication" → "어노테이션 스프링 부트 애플리케이션"
3. 괄호 안의 영어 풀네임(예: `IoC(Inversion of Control)`)은 괄호와 내용을 그대로 보존.
4. 파일 확장자/네임스페이스는 "점" 을 유지하되 확장자는 한국어로: "application.yml" → "application.와이엠엘".
5. 코드 토큰처럼 보이는 것(`*args`, `**kwargs`, `List<T>`)은 원문 그대로 유지.
6. 마크다운 문법(`**`, `` ` ``, `#`, `_`)은 절대 사용하지 말 것.
7. 물음표·쉼표·마침표 같은 한국어 문장부호는 원문을 유지.
8. 출력은 변환된 문장 한 줄만. 설명·따옴표·코드블록 금지."""


MARKDOWN_RE = re.compile(r"\*\*|__|`|^#\s", re.MULTILINE)


async def convert_one(
    client: AsyncAnthropic,
    sem: asyncio.Semaphore,
    model: str,
    content: str,
    max_retries: int = 3,
) -> str:
    async with sem:
        last_err: Exception | None = None
        for attempt in range(max_retries):
            try:
                resp = await client.messages.create(
                    model=model,
                    max_tokens=512,
                    system=PROMPT_SYSTEM,
                    messages=[
                        {
                            "role": "user",
                            "content": f"{PROMPT_RULES}\n\n원문:\n{content}",
                        }
                    ],
                )
                text = "".join(
                    block.text for block in resp.content if getattr(block, "type", "") == "text"
                ).strip()
                # 방어: 마크다운 제거 (Python 코드 토큰 `**kwargs` 는 백틱 없으므로 통과)
                if MARKDOWN_RE.search(text) and attempt < max_retries - 1:
                    continue
                text = text.replace("**", "").replace("`", "")
                return text
            except Exception as e:  # noqa: BLE001
                last_err = e
                await asyncio.sleep(1.5 * (attempt + 1))
        raise RuntimeError(f"변환 실패 (content={content[:40]}...): {last_err}")


# ---------- 메인 ----------

async def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed-dir", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--model", default="claude-haiku-4-5-20251001")
    parser.add_argument("--concurrency", type=int, default=8)
    parser.add_argument("--limit", type=int, default=0, help="0이면 전체")
    parser.add_argument("--resume", action="store_true")
    args = parser.parse_args()

    seed_dir = Path(args.seed_dir)
    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    existing: dict[str, dict[str, str]] = {}
    if args.resume and out_path.exists():
        existing = json.loads(out_path.read_text("utf-8"))
        print(f"[resume] 기존 매핑 {len(existing)}건 로드")

    # 수집
    tasks_input: list[tuple[str, str]] = []  # (key, content)
    seen_keys: set[str] = set()
    for sql_path in sorted(seed_dir.glob("*.sql")):
        sql_text = sql_path.read_text("utf-8")
        for cache_key, content, has_tts in iter_inserts(sql_text):
            if has_tts:
                continue
            key = row_key(cache_key, content)
            if key in seen_keys or key in existing:
                continue
            seen_keys.add(key)
            tasks_input.append((key, content))

    if args.limit > 0:
        tasks_input = tasks_input[: args.limit]

    print(f"[plan] 변환 대상 {len(tasks_input)}건 (기존 {len(existing)}건 재사용)")
    if not tasks_input:
        out_path.write_text(json.dumps(existing, ensure_ascii=False, indent=2), "utf-8")
        print("[done] 할 일 없음")
        return

    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        sys.exit("[ERROR] ANTHROPIC_API_KEY 환경변수가 필요합니다")

    client = AsyncAnthropic(api_key=api_key)
    sem = asyncio.Semaphore(args.concurrency)

    results = dict(existing)
    completed = 0
    total = len(tasks_input)

    async def worker(key: str, content: str) -> None:
        nonlocal completed
        try:
            tts = await convert_one(client, sem, args.model, content)
            results[key] = {"content": content, "tts_content": tts}
        except Exception as e:  # noqa: BLE001
            print(f"[fail] {key}: {e}", file=sys.stderr)
        finally:
            completed += 1
            if completed % 25 == 0 or completed == total:
                print(f"[progress] {completed}/{total}")
                # 중간 저장
                out_path.write_text(
                    json.dumps(results, ensure_ascii=False, indent=2), "utf-8"
                )

    await asyncio.gather(*(worker(k, c) for k, c in tasks_input))

    out_path.write_text(json.dumps(results, ensure_ascii=False, indent=2), "utf-8")
    print(f"[done] {len(results)}건 저장: {out_path}")


if __name__ == "__main__":
    asyncio.run(main())
