from __future__ import annotations

import json


def parse_llm_json(text: str) -> dict:
    """LLM 응답에서 JSON 객체를 추출하여 파싱한다.

    마크다운 코드블록 래핑과 앞뒤 불필요한 텍스트를 허용한다.
    """
    text = text.strip()
    if text.startswith("```"):
        lines = text.split("\n")
        text = "\n".join(lines[1:-1]) if len(lines) > 2 else text
        text = text.strip()

    start = text.find("{")
    end = text.rfind("}")
    if start != -1 and end != -1 and end > start:
        try:
            return json.loads(text[start:end + 1])
        except json.JSONDecodeError:
            pass

    return json.loads(text)
