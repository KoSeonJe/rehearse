import os


class Config:
    INTERNAL_API_KEY = os.environ.get("INTERNAL_API_KEY", "")
    API_SERVER_URL = os.environ.get("API_SERVER_URL", "").rstrip("/")
    OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY", "")
    S3_BUCKET = os.environ.get("S3_BUCKET", "rehearse-videos-dev")

    FRAME_INTERVAL_SEC = 3
    MAX_VISION_FRAMES = 10
    WHISPER_MODEL = "whisper-1"
    VISION_MODEL = "gpt-4o"
    LLM_MODEL = "gpt-4o"
