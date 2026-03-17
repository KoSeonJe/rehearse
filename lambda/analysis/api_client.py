import json

import boto3
import httpx

from config import Config

TIMEOUT = 30.0
HEADERS = {
    "Content-Type": "application/json",
    "X-Internal-Api-Key": Config.INTERNAL_API_KEY,
}


def _base_url(interview_id: int, question_set_id: int) -> str:
    return (
        f"{Config.API_SERVER_URL}/api/internal/interviews/{interview_id}"
        f"/question-sets/{question_set_id}"
    )


def get_answers(interview_id: int, question_set_id: int) -> dict:
    url = f"{_base_url(interview_id, question_set_id)}/answers"
    resp = httpx.get(url, headers=HEADERS, timeout=TIMEOUT)
    resp.raise_for_status()
    return resp.json()["data"]


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
    resp = httpx.put(url, json=body, headers=HEADERS, timeout=TIMEOUT)
    resp.raise_for_status()


def save_feedback(
    interview_id: int, question_set_id: int, feedback: dict
) -> None:
    url = f"{_base_url(interview_id, question_set_id)}/feedback"
    resp = httpx.post(url, json=feedback, headers=HEADERS, timeout=TIMEOUT)
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
