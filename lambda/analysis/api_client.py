import json
import os
import sys

import boto3
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


def _base_url(interview_id: int, question_set_id: int) -> str:
    return (
        f"{Config.API_SERVER_URL}/api/internal/interviews/{interview_id}"
        f"/question-sets/{question_set_id}"
    )


@retry_on_transient()
def get_answers(interview_id: int, question_set_id: int) -> dict:
    url = f"{_base_url(interview_id, question_set_id)}/answers"
    resp = httpx.get(url, headers=_get_headers(), timeout=TIMEOUT)
    resp.raise_for_status()
    return resp.json()["data"]


@retry_on_transient()
def update_progress(
    interview_id: int,
    question_set_id: int,
    progress: str,
    failure_reason: str | None = None,
    failure_detail: str | None = None,
) -> None:
    url = f"{_base_url(interview_id, question_set_id)}/progress"
    body: dict = {"progress": progress}
    if failure_reason:
        body["failureReason"] = failure_reason
    if failure_detail:
        body["failureDetail"] = failure_detail
    resp = httpx.put(url, json=body, headers=_get_headers(), timeout=TIMEOUT)
    resp.raise_for_status()


@retry_on_transient()
def save_feedback(
    interview_id: int, question_set_id: int, feedback: dict
) -> None:
    url = f"{_base_url(interview_id, question_set_id)}/feedback"
    resp = httpx.post(url, json=feedback, headers=_get_headers(), timeout=TIMEOUT)
    if resp.status_code >= 400:
        print(f"[Analysis] 피드백 저장 실패: status={resp.status_code}, body={resp.text}")
    resp.raise_for_status()


def backup_to_s3(interview_id: int, question_set_id: int, data: dict) -> None:
    s3 = boto3.client("s3")
    key = f"analysis-backup/{interview_id}/qs_{question_set_id}.json"
    s3.put_object(
        Bucket=Config.S3_BUCKET,
        Key=key,
        Body=json.dumps(data, ensure_ascii=False),
        ContentType="application/json",
    )
    print(f"[Analysis] S3 백업 완료: {key}")
