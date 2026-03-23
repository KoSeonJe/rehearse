import os
import time

from openai import OpenAI, RateLimitError, APIError, AuthenticationError

from config import Config
from extractors.ffmpeg_extractor import split_audio_chunks

MAX_RETRIES = 3
RETRY_DELAY = 2


def transcribe(audio_path: str) -> dict:
    client = OpenAI(api_key=Config.OPENAI_API_KEY)
    last_exception = None

    for attempt in range(MAX_RETRIES):
        try:
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

        except AuthenticationError:
            raise

        except RateLimitError as e:
            last_exception = e
            wait = RETRY_DELAY * (2 ** attempt)
            if attempt < MAX_RETRIES - 1:
                print(f"[STT] 시도 {attempt + 1}/{MAX_RETRIES} RateLimitError — {wait}초 후 재시도")
                time.sleep(wait)
            else:
                print(f"[STT] 모든 재시도 실패 (RateLimitError)")

        except APIError as e:
            last_exception = e
            if attempt < MAX_RETRIES - 1:
                delay = RETRY_DELAY * (2 ** attempt)
                print(f"[STT] 시도 {attempt + 1}/{MAX_RETRIES} APIError: {e} — {delay}초 후 재시도")
                time.sleep(delay)
            else:
                print(f"[STT] 모든 재시도 실패 (APIError)")

    raise last_exception


MAX_FILE_SIZE = 25 * 1024 * 1024  # 25MB


def _merge_segments(existing: list[dict], new_segments: list[dict]) -> list[dict]:
    """오버랩 구간의 중복 세그먼트를 제거하고 병합."""
    if not existing:
        return new_segments

    cutoff_ms = existing[-1]["end_ms"]
    for seg in new_segments:
        if seg["start_ms"] >= cutoff_ms:
            existing.append(seg)
    return existing


def transcribe_chunked(audio_path: str, output_dir: str) -> dict:
    """Whisper API 25MB 제한을 초과하는 오디오를 청크로 분할하여 전사.

    25MB 이하면 기존 transcribe() 직접 호출.
    """
    if os.path.getsize(audio_path) <= MAX_FILE_SIZE:
        return transcribe(audio_path)

    chunks = split_audio_chunks(audio_path, output_dir)
    all_segments: list[dict] = []

    try:
        for chunk_path, offset_ms in chunks:
            result = transcribe(chunk_path)
            adjusted = [
                {
                    "start_ms": seg["start_ms"] + offset_ms,
                    "end_ms": seg["end_ms"] + offset_ms,
                    "text": seg["text"],
                }
                for seg in result["segments"]
            ]
            all_segments = _merge_segments(all_segments, adjusted)
    finally:
        for chunk_path, _ in chunks:
            if chunk_path != audio_path and os.path.exists(chunk_path):
                os.remove(chunk_path)

    full_text = " ".join(seg["text"] for seg in all_segments)
    return {"full_text": full_text, "segments": all_segments}
