import time

from openai import OpenAI, RateLimitError, APIError, AuthenticationError

from config import Config

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
