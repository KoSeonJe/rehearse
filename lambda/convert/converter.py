import time

import boto3

from config import Config

POLL_INTERVAL = 10
MAX_POLL_SECONDS = 600


def create_mediaconvert_job(input_s3_key: str, output_s3_key: str) -> str:
    client = boto3.client(
        "mediaconvert", endpoint_url=Config.MEDIACONVERT_ENDPOINT
    )

    input_uri = f"s3://{Config.S3_BUCKET}/{input_s3_key}"
    output_prefix = f"s3://{Config.S3_BUCKET}/{output_s3_key.rsplit('.', 1)[0]}"

    job_settings = {
        "Inputs": [
            {
                "FileInput": input_uri,
                "AudioSelectors": {
                    "Audio Selector 1": {"DefaultSelection": "DEFAULT"}
                },
                "VideoSelector": {},
            }
        ],
        "OutputGroups": [
            {
                "Name": "MP4 Output",
                "OutputGroupSettings": {
                    "Type": "FILE_GROUP_SETTINGS",
                    "FileGroupSettings": {
                        "Destination": output_prefix,
                    },
                },
                "Outputs": [
                    {
                        "ContainerSettings": {
                            "Container": "MP4",
                            "Mp4Settings": {"MoovPlacement": "PROGRESSIVE_DOWNLOAD"},
                        },
                        "VideoDescription": {
                            "CodecSettings": {
                                "Codec": "H_264",
                                "H264Settings": {
                                    "RateControlMode": "QVBR",
                                    "QvbrSettings": {"QvbrQualityLevel": 7},
                                    "CodecProfile": "HIGH",
                                    "CodecLevel": "AUTO",
                                },
                            },
                        },
                        "AudioDescriptions": [
                            {
                                "CodecSettings": {
                                    "Codec": "AAC",
                                    "AacSettings": {
                                        "Bitrate": 128000,
                                        "CodingMode": "CODING_MODE_2_0",
                                        "SampleRate": 48000,
                                    },
                                },
                                "AudioSourceName": "Audio Selector 1",
                            }
                        ],
                        "Extension": "mp4",
                    }
                ],
            }
        ],
    }

    response = client.create_job(
        Role=Config.MEDIACONVERT_ROLE,
        Settings=job_settings,
        StatusUpdateInterval="SECONDS_10",
    )
    job_id = response["Job"]["Id"]
    print(f"[Convert] MediaConvert Job 생성: {job_id}")
    return job_id


def wait_for_job(job_id: str) -> str:
    client = boto3.client(
        "mediaconvert", endpoint_url=Config.MEDIACONVERT_ENDPOINT
    )
    elapsed = 0

    while elapsed < MAX_POLL_SECONDS:
        resp = client.get_job(Id=job_id)
        status = resp["Job"]["Status"]
        print(f"[Convert] Job {job_id} status: {status} ({elapsed}s)")

        if status == "COMPLETE":
            return "COMPLETE"
        if status == "ERROR":
            msg = resp["Job"].get("ErrorMessage", "Unknown error")
            raise RuntimeError(f"MediaConvert Job 실패: {msg}")
        if status == "CANCELED":
            raise RuntimeError("MediaConvert Job 취소됨")

        time.sleep(POLL_INTERVAL)
        elapsed += POLL_INTERVAL

    raise TimeoutError(f"MediaConvert Job 타임아웃 ({MAX_POLL_SECONDS}초)")
