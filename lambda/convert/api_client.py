import httpx

from config import Config

TIMEOUT = 10.0
HEADERS = {
    "Content-Type": "application/json",
    "X-Internal-Api-Key": Config.INTERNAL_API_KEY,
}


def get_file_metadata_by_s3_key(s3_key: str) -> dict:
    url = f"{Config.API_SERVER_URL}/api/internal/files/by-s3-key"
    resp = httpx.get(url, params={"key": s3_key}, headers=HEADERS, timeout=TIMEOUT)
    resp.raise_for_status()
    return resp.json()["data"]


def update_file_status(file_id: int, status: str, **kwargs) -> None:
    url = f"{Config.API_SERVER_URL}/api/internal/files/{file_id}/status"
    body = {"status": status, **kwargs}
    resp = httpx.put(url, json=body, headers=HEADERS, timeout=TIMEOUT)
    resp.raise_for_status()
