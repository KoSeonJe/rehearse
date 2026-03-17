from openai import OpenAI

from config import Config


def transcribe(audio_path: str) -> dict:
    client = OpenAI(api_key=Config.OPENAI_API_KEY)

    with open(audio_path, "rb") as f:
        response = client.audio.transcriptions.create(
            model=Config.WHISPER_MODEL,
            file=f,
            language="ko",
            response_format="verbose_json",
            timestamp_granularities=["segment"],
        )

    segments = []
    for seg in (response.segments or []):
        segments.append({
            "start_ms": int(seg.start * 1000),
            "end_ms": int(seg.end * 1000),
            "text": seg.text.strip(),
        })

    full_text = response.text or ""
    print(f"[STT] 전사 완료: {len(segments)}개 세그먼트, {len(full_text)}자")

    return {
        "full_text": full_text,
        "segments": segments,
    }
