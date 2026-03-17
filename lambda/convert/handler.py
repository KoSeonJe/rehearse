import json
import os
import urllib.parse


def lambda_handler(event, context):
    """S3 PutObject 이벤트 -> MediaConvert 변환 (스켈레톤)"""
    detail = event.get("detail", {})
    bucket = detail.get("bucket", {}).get("name", "")
    key = urllib.parse.unquote_plus(detail.get("object", {}).get("key", ""))

    print(f"[Convert Lambda] Triggered: bucket={bucket}, key={key}")

    if not key.startswith("videos/") or not key.endswith(".webm"):
        print(f"[Convert Lambda] Skipping non-video key: {key}")
        return {"statusCode": 200, "body": "Skipped"}

    # TODO: Task 7에서 구현
    # 1. MediaConvert Job 생성 (WebM -> MP4 faststart)
    # 2. Job 완료 대기
    # 3. API 서버에 convert-status=COMPLETED + streamingUrl 호출

    return {
        "statusCode": 200,
        "body": json.dumps({"message": "Convert skeleton", "key": key}),
    }
