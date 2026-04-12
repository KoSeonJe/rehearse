import json
import traceback
import urllib.parse
from uuid import uuid4

import api_client
from api_client import get_file_metadata_by_s3_key, update_file_status, update_convert_status
from converter import create_mediaconvert_job, wait_for_job
from s3_keys import parse_raw_key, derive_mp4_key


def lambda_handler(event, context):
    """S3 PutObject 이벤트 → MediaConvert 변환 (WebM → MP4)"""
    detail = event.get("detail", {})
    bucket = detail.get("bucket", {}).get("name", "")
    key = urllib.parse.unquote_plus(detail.get("object", {}).get("key", ""))

    print(f"[Convert] Triggered: bucket={bucket}, key={key}")

    parsed = parse_raw_key(key)
    if parsed is None:
        print(f"[Convert][Skipped] Non-matching key (not interviews/raw/ format): {key}")
        return {"statusCode": 200, "body": "Skipped: not a v1 raw key"}

    interview_id = parsed.interview_id
    question_set_id = parsed.question_set_id

    correlation_id = f"convert-{interview_id}-{question_set_id}-{uuid4().hex[:8]}"
    api_client.set_correlation_id(correlation_id)
    print(f"[Convert] correlation_id={correlation_id}")

    file_meta = None
    try:
        file_meta = get_file_metadata_by_s3_key(key)
        file_id = file_meta["id"]
        status = file_meta["status"]

        if status in ("PENDING", "FAILED"):
            update_file_status(file_id, "UPLOADED")

        # 변환 상태는 QuestionSetAnalysis로 관리
        update_convert_status(interview_id, question_set_id, "PROCESSING")

        output_key = derive_mp4_key(parsed)
        job_id = create_mediaconvert_job(key, output_key)
        wait_for_job(job_id)

        update_convert_status(
            interview_id, question_set_id, "COMPLETED",
            streaming_s3_key=output_key,
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

        try:
            update_convert_status(
                interview_id, question_set_id, "FAILED",
                failure_reason=error_msg[:500],
            )
        except Exception as api_err:
            print(f"[Convert] 실패 상태 업데이트 실패: {api_err}")

        raise
    finally:
        api_client.set_correlation_id(None)
