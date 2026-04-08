from __future__ import annotations

import base64
import re
import time

from openai import OpenAI, RateLimitError, AuthenticationError

from config import Config
from analyzers.json_utils import parse_llm_json
from analyzers.prompts import KOREAN_INSTRUCTION

MAX_RETRIES = 3
RETRY_DELAY = 2

# 텍스트 피드백(positive/negative/suggestion)에서 금지하는 시선 관련 어휘.
# 프롬프트 가드(B) 실패 시 사후 필터(C)가 이 패턴으로 재검사/치환한다.
#
# 두 범주로 나눈 이유:
# - substring: 다중 어절·조사 붙는 한국어 표현은 그대로 부분 일치
# - word-boundary: "eye", "eyes" 는 word boundary 없이 매칭하면
#   "eye movement / close your eyes" 같은 합법 표정 서술까지 치환되는
#   거짓양성이 발생하므로 \b 로 단어 경계를 강제한다.
_GAZE_KEYWORDS_SUBSTRING = (
    # 한국어
    "시선", "눈맞춤", "눈 맞춤", "눈빛", "응시", "주시",
    "아이컨택", "아이 컨택", "아이콘택", "눈을 마주", "눈을 피",
    "화면 응시", "화면을 응시", "화면을 바라", "카메라 응시", "카메라를 응시",
    "카메라를 바라", "카메라에 시선", "정면 응시", "정면을 응시",
    # 영어 — 복합어는 substring 매치로 충분
    "eye contact", "eye-contact", "eyecontact", "eyecontactlevel",
    "eye contact level", "gaze", "looking at the camera", "look at the camera",
)
_GAZE_KEYWORDS_WORD = (
    # 영어 단일 단어 — word boundary 강제 (eye movement/close eyes 거짓양성 방지)
    "eye", "eyes",
)
# 하위 호환을 위해 합쳐진 튜플 유지 (디버그/테스트용)
_GAZE_KEYWORDS = _GAZE_KEYWORDS_SUBSTRING + _GAZE_KEYWORDS_WORD
_GAZE_PATTERN = re.compile(
    "|".join(
        [re.escape(kw) for kw in _GAZE_KEYWORDS_SUBSTRING]
        + [rf"\b{re.escape(kw)}\b" for kw in _GAZE_KEYWORDS_WORD]
    ),
    flags=re.IGNORECASE,
)

_SYSTEM_PROMPT = KOREAN_INSTRUCTION + """면접 영상 프레임의 비언어적 커뮤니케이션만 평가합니다. 답변 내용은 평가하지 않습니다.

## 🚫 최우선 금지 규칙 (위반 시 응답 무효)
텍스트 필드(positive / negative / suggestion)에서는 **시선 관련 내용을 절대 언급하지 마세요.**
- 금지 어휘 예시: 시선, 눈맞춤, 눈빛, 응시, 아이컨택, eye contact, eye-contact, gaze, 화면 응시, 카메라 응시, 눈을 마주침 등.
- 텍스트는 오직 **자세(어깨/목/상체/손/몸의 방향)** 와 **표정/감정 상태** 두 축으로만 서술하세요.
- `eyeContactLevel` enum 값은 평가하되, 그 결과를 **텍스트로 풀어쓰거나 암시하지 마세요.**
- 노트북/웹캠 특성상 시선이 화면 아래로 향하는 것은 정상이며, 어차피 텍스트에 등장해서는 안 됩니다.

## 평가 enum (모두 영어 코드값으로 응답)
- eyeContactLevel: GOOD | AVERAGE | NEEDS_IMPROVEMENT  (enum만 채우고 텍스트로 언급 금지)
- postureLevel: GOOD | AVERAGE | NEEDS_IMPROVEMENT
- expressionLabel: CONFIDENT | ENGAGED | NEUTRAL | NERVOUS | UNCERTAIN

## 주의
- 여러 프레임의 평균 경향 평가. 이상치에 과도한 가중치 금지.
- 사람 미확인 시 AVERAGE / NEUTRAL 로 응답.

## 응답 형식 (정확히 이 JSON 스키마, 다른 키 추가 금지)
{
  "eyeContactLevel": "GOOD|AVERAGE|NEEDS_IMPROVEMENT",
  "postureLevel": "GOOD|AVERAGE|NEEDS_IMPROVEMENT",
  "expressionLabel": "CONFIDENT|ENGAGED|NEUTRAL|NERVOUS|UNCERTAIN",
  "positive": "자세 또는 표정에서 잘한 점 1문장 (구체적 근거, 시선 언급 금지)",
  "negative": "자세 또는 표정에서 보완할 점 1문장 (구체적 근거, 시선 언급 금지)",
  "suggestion": "자세 또는 표정 개선 방법 1문장 (실행 가능, 시선 언급 금지)"
}

## 작성 규칙
- positive / negative / suggestion 각 1문장. 두 문장 이상 금지.
- "전반적으로 잘했습니다" 같은 모호한 표현 금지. 반드시 구체적 **자세 또는 표정** 근거를 인용.
- 보완점이 거의 없어도 작은 점이라도 1문장 작성. null·빈 문자열 금지.
- 다시 강조: 텍스트 3개 필드에 시선·눈·eye contact·gaze·응시 등 어떤 시선 관련 단어도 등장해서는 안 됩니다."""

_RETRY_REMINDER = (
    "\n\n이전 응답이 텍스트 필드에 시선 관련 어휘를 포함했습니다. "
    "positive / negative / suggestion 에는 시선·눈맞춤·응시·아이컨택·eye contact·gaze 등을 "
    "절대 사용하지 말고, 오직 자세(어깨/목/상체)와 표정/감정에 대해서만 1문장씩 다시 작성하세요."
)

# 시선 어휘가 감지됐을 때 사용하는 posture/expression 중심 치환 문구.
_GAZE_SAFE_FALLBACK = {
    "positive": "상체가 화면 중앙에 안정적으로 자리 잡고 어깨 균형이 유지됐습니다.",
    "negative": "어깨가 다소 경직돼 보이고 표정 변화 폭이 제한적이었습니다.",
    "suggestion": "답변 중 어깨 긴장을 풀고 표정에 미세한 변화를 주도록 연습해보세요.",
}

_FALLBACK = {
    "eyeContactLevel": "AVERAGE",
    "postureLevel": "AVERAGE",
    "expressionLabel": "NEUTRAL",
    # 중립 폴백: 분석 실패 경로이므로 긍정/부정 판정 없이 중립 서술만.
    "positive": "자세와 표정 분석에 필요한 데이터가 일부 확인됐습니다.",
    "negative": "프레임 품질 또는 분량 제약으로 자세·표정 세부 평가가 제한됐습니다.",
    "suggestion": "조명과 카메라 각도를 확인한 뒤 연습 녹화를 다시 시도해보세요.",
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

    system_prompt = _SYSTEM_PROMPT
    gaze_retry_used = False

    for attempt in range(MAX_RETRIES):
        try:
            response = client.chat.completions.create(
                model=Config.VISION_MODEL,
                messages=[
                    {"role": "system", "content": system_prompt},
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

            # 사후 필터: 시선 관련 어휘가 감지되면 1회 LLM 재시도, 그래도 남아 있으면 치환.
            if _has_gaze_mention(result):
                if not gaze_retry_used and attempt < MAX_RETRIES - 1:
                    gaze_retry_used = True
                    system_prompt = _SYSTEM_PROMPT + _RETRY_REMINDER
                    print("[Vision] 시선 언급 감지 — 강화 프롬프트로 1회 재시도")
                    continue
                stripped = _strip_gaze_mentions(result)
                print(
                    f"[Vision] 시선 언급 사후 치환 적용: "
                    f"positive={stripped['_stripped_fields'].get('positive', False)}, "
                    f"negative={stripped['_stripped_fields'].get('negative', False)}, "
                    f"suggestion={stripped['_stripped_fields'].get('suggestion', False)}"
                )
                result = {k: v for k, v in stripped.items() if not k.startswith("_")}

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


def _has_gaze_mention(result: dict) -> bool:
    """positive/negative/suggestion 중 어느 하나라도 시선 관련 어휘를 포함하면 True."""
    for key in ("positive", "negative", "suggestion"):
        value = result.get(key)
        if not value:
            continue
        if _GAZE_PATTERN.search(str(value)):
            return True
    return False


def _strip_gaze_mentions(result: dict) -> dict:
    """시선 관련 어휘를 포함한 문장을 posture/expression 중심 안전 문장으로 치환한다.

    문장 단위로 검사하여 가능한 원문을 보존하고, 모든 문장이 제거되면
    `_GAZE_SAFE_FALLBACK` 으로 대체한다. 디버깅을 위해 `_stripped_fields`
    메타데이터를 함께 반환한다 (caller가 로깅 후 제거).
    """
    out = dict(result)
    stripped_flags: dict[str, bool] = {}
    for key in ("positive", "negative", "suggestion"):
        original = str(out.get(key) or "").strip()
        if not original:
            out[key] = _GAZE_SAFE_FALLBACK[key]
            stripped_flags[key] = True
            continue

        # 한국어/영어 문장 구분 후 시선 어휘가 포함되지 않은 문장만 유지
        sentences = re.split(r"(?<=[\.!?。！？])\s+|(?<=[다요])\s+", original)
        clean_sentences = [
            s.strip()
            for s in sentences
            if s.strip() and not _GAZE_PATTERN.search(s)
        ]

        if not clean_sentences:
            out[key] = _GAZE_SAFE_FALLBACK[key]
            stripped_flags[key] = True
        elif len(clean_sentences) != len([s for s in sentences if s.strip()]):
            out[key] = " ".join(clean_sentences)
            stripped_flags[key] = True
        else:
            # split 이 문장을 온전히 나누지 못한 경우를 대비해 전체 문자열 재검사
            if _GAZE_PATTERN.search(out[key] if isinstance(out.get(key), str) else ""):
                out[key] = _GAZE_SAFE_FALLBACK[key]
                stripped_flags[key] = True
            else:
                stripped_flags[key] = False

        # 최종 안전망: 치환 후에도 패턴이 남아있으면 강제로 fallback
        if _GAZE_PATTERN.search(str(out.get(key) or "")):
            out[key] = _GAZE_SAFE_FALLBACK[key]
            stripped_flags[key] = True

    out["_stripped_fields"] = stripped_flags
    return out
