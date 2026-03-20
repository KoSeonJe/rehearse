import os
import sys

import httpx

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'common'))

from config import Config
from retry import retry_on_transient

TIMEOUT = 30.0
HEADERS = {
    "Content-Type": "application/json",
    "X-Internal-Api-Key": Config.INTERNAL_API_KEY,
}

_correlation_id: str | None = None


def set_correlation_id(cid: str) -> None:
    global _correlation_id
    _correlation_id = cid


def _get_headers() -> dict:
    headers = dict(HEADERS)
    if _correlation_id:
        headers["X-Correlation-Id"] = _correlation_id
    return headers


@retry_on_transient()
def get_file_metadata_by_s3_key(s3_key: str) -> dict:
    url = f"{Config.API_SERVER_URL}/api/internal/files/by-s3-key"
    resp = httpx.get(url, params={"key": s3_key}, headers=_get_headers(), timeout=TIMEOUT)
    resp.raise_for_status()
    return resp.json()["data"]


@retry_on_transient()
def update_file_status(file_id: int, status: str, **kwargs) -> None:
    url = f"{Config.API_SERVER_URL}/api/internal/files/{file_id}/status"
    body = {"status": status, **kwargs}
    resp = httpx.put(url, json=body, headers=_get_headers(), timeout=TIMEOUT)
    resp.raise_for_status()
