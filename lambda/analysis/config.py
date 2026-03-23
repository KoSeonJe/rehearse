import os


class Config:
    INTERNAL_API_KEY = os.environ.get("INTERNAL_API_KEY", "")
    API_SERVER_URL = os.environ.get("API_SERVER_URL", "").rstrip("/")
    OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY", "")
    S3_BUCKET = os.environ.get("S3_BUCKET", "rehearse-videos-dev")
    FFMPEG_PATH = os.environ.get("FFMPEG_PATH", "ffmpeg")
    FFPROBE_PATH = os.environ.get("FFPROBE_PATH", "ffprobe")

    GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "")
    GEMINI_MODEL = os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")
    USE_GEMINI = os.environ.get("USE_GEMINI", "true").lower() == "true"

    FRAME_INTERVAL_SEC = 3
    MAX_VISION_FRAMES = 30
    MAX_VISION_FRAMES_PER_ANSWER = 10
    WHISPER_CHUNK_DURATION_SEC = 600
    WHISPER_CHUNK_OVERLAP_SEC = 2
    WHISPER_MAX_FILE_SIZE_MB = 25

    WHISPER_MODEL = "whisper-1"
    VISION_MODEL = "gpt-4o"
    LLM_MODEL = "gpt-4o"
