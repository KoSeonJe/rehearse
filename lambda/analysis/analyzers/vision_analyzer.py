from __future__ import annotations

import base64
import time

from openai import OpenAI, RateLimitError, AuthenticationError

from config import Config
from analyzers.json_utils import parse_llm_json
from analyzers.prompts import KOREAN_INSTRUCTION

MAX_RETRIES = 3
RETRY_DELAY = 2

_SYSTEM_PROMPT = KOREAN_INSTRUCTION + """면접 영상 프레임의 비언어적 커뮤니케이션만 평가합니다. 답변 내용은 평가하지 않습니다.

## 평가
1. eye_contact_score(0-100): 90+=안정응시, 70+=간헐흐트러짐, 50+=자주딴곳, 30+=불안정, 0+=미응시
2. posture_score(0-100): 90+=바른자세유지, 70+=간헐구부정, 50+=불안정/흔듦, 30+=지속구부정, 0+=부적절
3. expression_label: CONFIDENT(자신감) | ENGAGED(몰입) | NEUTRAL(무표정) | NERVOUS(긴장) | UNCERTAIN(혼란)

## 주의
- 여러 프레임의 평균 경향 평가. 이상치에 과도한 가중치 금지.
- 사람 미확인 시 점수 50, comment에 설명.
- 한국 면접 문화 고려(차분함 긍정적).

## 응답
JSON만 응답:
{"eye_contact_score":0,"posture_score":0,"expression_label":"","comment":"한국어 2-3문장"}"""

_FALLBACK = {
    "eye_contact_score": 50,
    "posture_score": 50,
    "expression_label": "NEUTRAL",
    "comment": "비언어 분석을 수행할 수 없어 기본값으로 설정되었습니다.",
}


def analyze_frames(frame_paths: list[str]) -> dict:
    if not frame_paths:
        print("[Vision] 프레임 없음 — 폴백 사용")
        return dict(_FALLBACK)

    client = OpenAI(api_key=Config.OPENAI_API_KEY)

    selected = _select_frames(frame_paths, Config.MAX_VISION_FRAMES_PER_ANSWER)
    print(f"[Vision] {len(selected)}장 프레임으로 분석 시작")

    user_text = f"다음은 면접 영상에서 {Config.FRAME_INTERVAL_SEC}초 간격으로 추출한 프레임입니다. 면접자의 비언어적 커뮤니케이션을 분석해주세요."
    content = [{"type": "text", "text": user_text}]
    for path in selected:
        b64 = _encode_image(path)
        content.append({
            "type": "image_url",
            "image_url": {"url": f"data:image/jpeg;base64,{b64}", "detail": "low"},
        })

    for attempt in range(MAX_RETRIES):
        try:
            response = client.chat.completions.create(
                model=Config.VISION_MODEL,
                messages=[
                    {"role": "system", "content": _SYSTEM_PROMPT},
                    {"role": "user", "content": content},
                ],
                max_tokens=500,
                temperature=0.3,
                response_format={"type": "json_object"},
            )

            raw = response.choices[0].message.content
            if not raw or not raw.strip():
                print(f"[Vision] 빈 응답 — 폴백 사용")
                return dict(_FALLBACK)

            result = parse_llm_json(raw.strip())
            result = _validate_result(result)
            print(f"[Vision] 분석 완료: eye={result.get('eye_contact_score')}, posture={result.get('posture_score')}")
            return result

        except AuthenticationError:
            raise

        except RateLimitError as e:
            wait = RETRY_DELAY * (2 ** attempt)
            print(f"[Vision] 시도 {attempt + 1}/{MAX_RETRIES} RateLimitError — {wait}초 후 재시도")
            if attempt < MAX_RETRIES - 1:
                time.sleep(wait)
            else:
                print("[Vision] 모든 재시도 실패 — 폴백 사용")
                return dict(_FALLBACK)

        except Exception as e:
            print(f"[Vision] 시도 {attempt + 1}/{MAX_RETRIES} 실패: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_DELAY * (attempt + 1))
            else:
                print("[Vision] 모든 재시도 실패 — 폴백 사용")
                return dict(_FALLBACK)


def _select_frames(frame_paths: list[str], max_count: int) -> list[str]:
    if len(frame_paths) <= max_count:
        return frame_paths
    step = len(frame_paths) / max_count
    return [frame_paths[int(i * step)] for i in range(max_count)]


def _encode_image(path: str) -> str:
    with open(path, "rb") as f:
        return base64.b64encode(f.read()).decode("utf-8")


def _validate_result(result: dict) -> dict:
    validated = {}
    validated["eye_contact_score"] = _clamp_int(result.get("eye_contact_score"), 0, 100, 50)
    validated["posture_score"] = _clamp_int(result.get("posture_score"), 0, 100, 50)

    label = result.get("expression_label", "NEUTRAL")
    valid_labels = {"CONFIDENT", "NERVOUS", "NEUTRAL", "ENGAGED", "UNCERTAIN"}
    validated["expression_label"] = label if label in valid_labels else "NEUTRAL"

    validated["comment"] = result.get("comment") or _FALLBACK["comment"]
    return validated


def _clamp_int(value, min_val: int, max_val: int, default: int) -> int:
    if value is None:
        return default
    try:
        v = int(value)
        return max(min_val, min(max_val, v))
    except (TypeError, ValueError):
        return default
