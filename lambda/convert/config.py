import os


class Config:
    INTERNAL_API_KEY = os.environ.get("INTERNAL_API_KEY", "")
    API_SERVER_URL = os.environ.get("API_SERVER_URL", "").rstrip("/")
    MEDIACONVERT_ENDPOINT = os.environ.get("MEDIACONVERT_ENDPOINT", "")
    MEDIACONVERT_ROLE = os.environ.get("MEDIACONVERT_ROLE", "")
    S3_BUCKET = os.environ.get("S3_BUCKET", "rehearse-videos-dev")
