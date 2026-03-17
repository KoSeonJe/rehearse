import base64
import json
import time

from openai import OpenAI

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

반드시 아래 JSON 형식으로만 응답하세요:
{
  "eye_contact_score": <0-100>,
  "posture_score": <0-100>,
  "expression_label": "<CONFIDENT|NERVOUS|NEUTRAL|ENGAGED|UNCERTAIN>",
  "comment": "<한국어로 2-3문장의 비언어 피드백>"
}"""


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
            )
            raw = response.choices[0].message.content.strip()
            result = _parse_json(raw)
            print(f"[Vision] 분석 완료: eye={result.get('eye_contact_score')}, posture={result.get('posture_score')}")
            return result

        except Exception as e:
            print(f"[Vision] 시도 {attempt + 1}/{MAX_RETRIES} 실패: {e}")
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_DELAY * (attempt + 1))
            else:
                raise


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
    return json.loads(text)
