import json
import traceback
import urllib.parse
from uuid import uuid4

import api_client
from api_client import get_file_metadata_by_s3_key, update_file_status
from converter import create_mediaconvert_job, wait_for_job


def lambda_handler(event, context):
    """S3 PutObject 이벤트 → MediaConvert 변환 (WebM → MP4)"""
    detail = event.get("detail", {})
    bucket = detail.get("bucket", {}).get("name", "")
    key = urllib.parse.unquote_plus(detail.get("object", {}).get("key", ""))

    print(f"[Convert] Triggered: bucket={bucket}, key={key}")

    if not key.startswith("videos/") or not key.endswith(".webm"):
        print(f"[Convert] Skipping non-video key: {key}")
        return {"statusCode": 200, "body": "Skipped"}

    correlation_id = f"convert-{key.replace('/', '-')}-{uuid4().hex[:8]}"
    api_client.set_correlation_id(correlation_id)
    print(f"[Convert] correlation_id={correlation_id}")

    file_meta = None
    try:
        file_meta = get_file_metadata_by_s3_key(key)
        file_id = file_meta["id"]
        status = file_meta["status"]

        if status not in ("PENDING", "UPLOADED", "FAILED"):
            print(f"[Convert] 멱등성 스킵: file={file_id}, status={status}")
            return {"statusCode": 200, "body": "Already processed"}

        if status in ("PENDING", "FAILED"):
            update_file_status(file_id, "UPLOADED")
        update_file_status(file_id, "CONVERTING")

        output_key = key.rsplit(".", 1)[0] + ".mp4"
        job_id = create_mediaconvert_job(key, output_key)
        wait_for_job(job_id)

        update_file_status(
            file_id,
            "CONVERTED",
            streamingS3Key=output_key,
        )
        print(f"[Convert] 변환 완료: file={file_id}, output={output_key}")

        return {
            "statusCode": 200,
            "body": json.dumps({"message": "Convert complete", "key": output_key}),
        }

    except Exception as e:
        error_msg = str(e)
        error_detail = traceback.format_exc()
        print(f"[Convert] 변환 실패: {error_msg}\n{error_detail}")

        if file_meta:
            try:
                update_file_status(
                    file_meta["id"],
                    "FAILED",
                    failureReason=error_msg[:500],
                    failureDetail=error_detail[:2000],
                )
            except Exception as api_err:
                print(f"[Convert] 실패 상태 업데이트 실패: {api_err}")

        raise
    finally:
        api_client.set_correlation_id(None)
