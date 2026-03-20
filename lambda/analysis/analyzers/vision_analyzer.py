import base64
import json
import re
import time

from openai import OpenAI, RateLimitError, APIError, AuthenticationError

from config import Config

MAX_RETRIES = 3
RETRY_DELAY = 2

_SYSTEM_PROMPT = """당신은 면접 비언어 분석 전문가입니다.
면접 영상의 프레임 이미지들을 분석하여 면접자의 비언어적 커뮤니케이션을 평가합니다.

평가 기준:
1. 시선 처리 (eye_contact_score: 0-100)
   - 카메라를 적절히 응시하는지
   - 시선이 불안정하거나 자주 돌리는지
2. 자세 (posture_score: 0-100)
   - 바른 자세를 유지하는지
   - 어깨가 처지거나 몸을 흔드는지
3. 표정 (expression_label)
   - CONFIDENT: 자신감 있는 표정
   - NERVOUS: 긴장된 표정
   - NEUTRAL: 무표정
   - ENGAGED: 몰입된 표정
   - UNCERTAIN: 불확실한 표정

이미지에 사람이 보이지 않거나 분석이 어려운 경우에도 반드시 JSON으로 응답하되,
점수를 50으로 설정하고 comment에 상황을 설명하세요.

반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트 없이 JSON만 출력하세요:
{"eye_contact_score": 0, "posture_score": 0, "expression_label": "NEUTRAL", "comment": ""}"""

_FALLBACK = {
    "eye_contact_score": 50,
    "posture_score": 50,
    "expression_label": "NEUTRAL",
    "comment": "비언어 분석을 수행할 수 없어 기본값으로 설정되었습니다.",
}


def analyze_frames(frame_paths: list[str]) -> dict:
    client = OpenAI(api_key=Config.OPENAI_API_KEY)

    selected = _select_frames(frame_paths, Config.MAX_VISION_FRAMES)
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

            result = _parse_json(raw.strip())
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


def _parse_json(text: str) -> dict:
    text = text.strip()
    if text.startswith("```"):
        lines = text.split("\n")
        text = "\n".join(lines[1:-1]) if len(lines) > 2 else text
        text = text.strip()

    # JSON 블록 추출 시도
    match = re.search(r'\{[^{}]*\}', text, re.DOTALL)
    if match:
        return json.loads(match.group())

    return json.loads(text)


def _validate_result(result: dict) -> dict:
    """필수 키 존재 + 타입 검증, 없으면 기본값으로 채움."""
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
