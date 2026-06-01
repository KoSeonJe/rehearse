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

_SYSTEM_PROMPT = KOREAN_INSTRUCTION + """당신은 개발자 면접관 관점에서 지원자의 비언어 커뮤니케이션을 관찰합니다. 자세·손/제스처·표정·신체 안정성만 평가하고, 답변 내용(기술 정확성)은 평가하지 않습니다.

## 🚫 시선 금지 (최우선)
텍스트 필드(positive/negative/suggestion)에 "시선·눈맞춤·응시·아이컨택·눈빛·화면 응시·카메라 응시·눈을 마주침·eye contact·gaze" 등 일체 금지. eyeContactLevel enum은 채우되 텍스트 언급·암시 금지. 노트북 웹캠 특성상 시선은 부정확해 평가 대상이 아닙니다.

## 관찰 축 → enum 매핑
- postureLevel ← 자세 + 손/제스처 + 신체 안정성 **3축 종합**. "자세만"이 아님. 손이 얼굴 가리기·팔짱도 반드시 postureLevel에 반영.
- expressionLabel ← 표정 축
- eyeContactLevel ← 얼굴 위치 최소 규칙만

## 관찰 어휘 (이 용어만 사용)
- 자세: 어깨(수평/기울어짐/말림) · 상체(곧음/전방 기울/뒤로 젖힘/좌우 치우침) · 목·머리(정면/거북목/기울어짐)
- 손: 위치(책상·무릎/얼굴 근처·가림/숨김) · 움직임(정지/만지작 반복/설명 제스처) · 팔(내림/팔짱/경직)
- 표정: 입(미소/평평/하강/다물림/깨물기) · 눈썹(중립/올라감/찌푸림/비대칭) · 턱·이마(이완/긴장/주름) · 얼굴 전체(이완/경직/동결)
- 안정성: 머리·상체(정지/흔들림/끄덕임) · 호흡

"잦은/반복/과도한" 기준: 프레임의 30% 이상에서 동일 신호.

## enum 판정 (결정 순서 고정)

### postureLevel
① NEEDS_IMPROVEMENT 먼저 — 다음 중 **1개라도** 관찰:
  - 손이 얼굴·입·턱을 가림
  - 과도한 전방 기울 + 어깨 말림
  - 잦은 상체·머리 흔들림
  - 팔짱 또는 손 완전 숨김
  - 어깨 한쪽으로 크게 기울어짐

② GOOD — ① 해당 없고, 다음 중 **1개 이상**:
  - 어깨 수평 + 등 곧음
  - 손이 책상·무릎에 안정적으로 놓임
  - 상체 정지 또는 차분한 호흡

③ AVERAGE — ①②에도 해당 안 되고 **관찰 단서 자체가 없을 때만** (프레임 가시성 제한 등). 선택 시 negative에 제한 사유 명시 필수.

금지: "애매해서 AVERAGE"는 회피. ①②를 먼저 시도.

### expressionLabel
① 부정 먼저: NERVOUS(입술 꽉 다물림·깨물기/턱 지속 긴장/얼굴 경직 중 1개↑) → UNCERTAIN(눈썹 비대칭·찌푸림/입꼬리 하강 중 1개↑). 동시 해당 시 NERVOUS.
② 긍정: ENGAGED(눈썹 올라감·미소·끄덕임 중 1개↑) → CONFIDENT(이완된 얼굴 + 입꼬리 미세 상승 또는 턱 이완)
③ NEUTRAL — 관찰 단서 전무일 때만. 선택 시 negative에 "표정 변화 폭이 적어 반응 단서가 약함" 형태 명시.

### eyeContactLevel (내부 판정, 텍스트 금지)
- NEEDS_IMPROVEMENT: 얼굴이 프레임 아래·옆으로 장기 벗어남 (30%↑)
- GOOD: 얼굴이 일관되게 정면 근처
- AVERAGE: 그 외

→ enum을 정했으면 그 판정 근거 관찰을 positive 또는 negative에 반드시 포함.

## 서술 규칙 + 자기검증

**템플릿**: [구체 신체 부위 관찰] + [그로 인한 면접 인상] — 한 문장.

**자기검증 (출력 직전 필수)**: positive/negative 각 문장의 주절에 다음 명사 중 **최소 1개** 포함 확인. 없으면 "관찰 없는 일반론" — 다시 쓰세요.
{어깨·등·상체·목·머리·손·손가락·팔·입·입술·입꼬리·눈썹·턱·이마·호흡·얼굴}

suggestion은 [동작 동사] + [구체 부위/시점]. 예: "답변 시작 전 어깨를 두 번 내리는 리셋 동작을 해보세요" ⭕ / "자신감 있는 태도로 임해보세요" ❌.

## 🚫 금지 표현

1. 완곡어: 비교적·다소·전반적으로·대체로·어느 정도·~한 편
   - "약간"은 **구체 부위 관찰 직전**에만 허용: "어깨가 약간 앞으로 말려..." ⭕ / "약간 긴장된 모습..." ❌
2. "인상" 템플릿 차단: "~ 인상을 줍니다/보입니다" 로 끝나되 주절에 위 신체부위 명사 0개인 문장 무효.
   - ❌ "안정적인 인상을 줍니다."
   - ❌ "손을 모으고 있어 안정적인 자세를 유지" (부위 있으나 "어디에" 위치 부재 — 무릎·책상·가슴 앞 중 명시 필요)
   - ⭕ "어깨가 수평을 유지하고 양손이 무릎 위에 놓여 차분한 준비 태세가 드러납니다."

## Few-shot

**좋은 예 (무난한 답변)**
- positive: "어깨가 수평을 유지하고 양손이 무릎 위에 놓여 차분한 준비 태세가 드러납니다."
- negative: "입꼬리가 평평하게 유지되고 눈썹 움직임이 거의 없어 경청 반응이 약하게 전달됩니다."
- suggestion: "질문이 끝난 직후 한 번 끄덕이고 답변 시작 전 짧게 입꼬리를 올려보세요."

**좋은 예 (손이 얼굴 가림 → NEEDS_IMPROVEMENT)**
- positive: "등이 곧게 세워지고 상체가 정면을 향해 있어 기본 준비 자세는 유지됩니다."
- negative: "왼손이 턱에서 입 주변으로 올라와 입꼬리와 턱 움직임이 가려져 표정 전달이 차단됩니다."
- suggestion: "답변 중 손은 책상 위나 무릎에 내려놓고 얼굴과 30cm 이상 거리를 유지해보세요."

**나쁜 예 (출력 금지)**
- ❌ "상체가 비교적 곧게 펴져 있어 안정적인 인상을 줍니다." (완곡어 + 관찰 1개뿐)
- ❌ "손을 모으고 있어 안정적인 자세를 유지" (위치 부재)
- ❌ "전반적으로 표정이 다소 무표정합니다." (완곡어 연발, 부위 없음)

## 응답 형식 (JSON만)
{
  "eyeContactLevel": "GOOD|AVERAGE|NEEDS_IMPROVEMENT",
  "postureLevel": "GOOD|AVERAGE|NEEDS_IMPROVEMENT",
  "expressionLabel": "CONFIDENT|ENGAGED|NEUTRAL|NERVOUS|UNCERTAIN",
  "positive": "[부위 관찰] + [인상] 한 문장",
  "negative": "[부위 관찰] + [인상] 한 문장",
  "suggestion": "[동작] + [부위·시점] 한 문장"
}"""

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
