import json
import os
import urllib.parse


def lambda_handler(event, context):
    """S3 PutObject 이벤트 -> 분석 파이프라인 (스켈레톤)"""
    detail = event.get("detail", {})
    bucket = detail.get("bucket", {}).get("name", "")
    key = urllib.parse.unquote_plus(detail.get("object", {}).get("key", ""))

    print(f"[Analysis Lambda] Triggered: bucket={bucket}, key={key}")

    if not key.startswith("videos/") or not key.endswith(".webm"):
        print(f"[Analysis Lambda] Skipping non-video key: {key}")
        return {"statusCode": 200, "body": "Skipped"}

    # TODO: Task 6에서 구현
    # 1. API 서버에 progress=STARTED 호출
    # 2. S3에서 영상 다운로드
    # 3. FFmpeg으로 오디오/프레임 추출
    # 4. Whisper STT
    # 5. GPT-4o Vision 비언어 분석
    # 6. LLM 언어 분석
    # 7. API 서버에 피드백 저장

    return {
        "statusCode": 200,
        "body": json.dumps({"message": "Analysis skeleton", "key": key}),
    }
