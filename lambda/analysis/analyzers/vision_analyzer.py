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

## 평가 enum (모두 영어 코드값으로 응답)
- eyeContactLevel: GOOD | AVERAGE | NEEDS_IMPROVEMENT
- postureLevel: GOOD | AVERAGE | NEEDS_IMPROVEMENT
- expressionLabel: CONFIDENT | ENGAGED | NEUTRAL | NERVOUS | UNCERTAIN

## 주의
- 여러 프레임의 평균 경향 평가. 이상치에 과도한 가중치 금지.
- 사람 미확인 시 AVERAGE로 응답.
- 노트북/웹캠으로 진행하는 온라인 면접입니다. 시선이 화면 아래쪽으로 향하는 것은 정상.

## 응답 형식 (정확히 이 JSON 스키마, 다른 키 추가 금지)
{
  "eyeContactLevel": "GOOD|AVERAGE|NEEDS_IMPROVEMENT",
  "postureLevel": "GOOD|AVERAGE|NEEDS_IMPROVEMENT",
  "expressionLabel": "CONFIDENT|ENGAGED|NEUTRAL|NERVOUS|UNCERTAIN",
  "positive": "잘한 점 1문장 (구체적 근거 포함)",
  "negative": "보완할 점 1문장 (구체적 근거 포함)",
  "suggestion": "개선 방법 1문장 (실행 가능한 형태)"
}

## 작성 규칙
- positive/negative/suggestion 각 1문장. 두 문장 이상 금지.
- "전반적으로 잘했습니다" 같은 모호한 표현 금지. 반드시 구체적 행동/표정/자세를 인용.
- 보완점이 거의 없어도 작은 점이라도 1문장 작성. null·빈 문자열 금지."""

_FALLBACK = {
    "eyeContactLevel": "AVERAGE",
    "postureLevel": "AVERAGE",
    "expressionLabel": "NEUTRAL",
    "positive": "분석 대상이 확인됐습니다.",
    "negative": "비언어 분석을 수행할 수 없어 기본값으로 설정했습니다.",
    "suggestion": "카메라가 얼굴을 잘 비추도록 조정해보세요.",
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
            print(f"[Vision] 분석 완료: eye={result.get('eyeContactLevel')}, posture={result.get('postureLevel')}")
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
    valid_levels = {"GOOD", "AVERAGE", "NEEDS_IMPROVEMENT"}
    valid_labels = {"CONFIDENT", "NERVOUS", "NEUTRAL", "ENGAGED", "UNCERTAIN"}

    validated = {}
    validated["eyeContactLevel"] = (
        result.get("eyeContactLevel", "AVERAGE")
        if result.get("eyeContactLevel") in valid_levels
        else "AVERAGE"
    )
    validated["postureLevel"] = (
        result.get("postureLevel", "AVERAGE")
        if result.get("postureLevel") in valid_levels
        else "AVERAGE"
    )
    validated["expressionLabel"] = (
        result.get("expressionLabel", "NEUTRAL")
        if result.get("expressionLabel") in valid_labels
        else "NEUTRAL"
    )
    for key in ("positive", "negative", "suggestion"):
        validated[key] = result.get(key) or _FALLBACK[key]
    return validated
