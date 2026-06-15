# Task B4 — Lambda analysis head_object 추출 + 자체 생성 제거

> **PR**: PR-B
> **영역**: Lambda
> **선행 Task**: A4 (rename 선행)

---

## 변경 파일

- `lambda/analysis/handler.py` — **변경**. L80-82 자체 생성 (`f"{interview_id}-{question_set_id}-{uuid4().hex[:8]}"`) 제거. `s3.head_object(Bucket, Key)` → `Metadata.get('trace-id')` 추출. 부재 시 fallback UUID + WARN.
- `lambda/analysis/tests/test_handler.py` — **신규 또는 확장**.

---

## 핵심 로직

```python
# handler.py (변경부)
import boto3
import uuid

s3_client = boto3.client('s3')

def _extract_trace_id(bucket: str, key: str) -> str | None:
    try:
        head = s3_client.head_object(Bucket=bucket, Key=key)
        metadata = head.get('Metadata', {})
        return metadata.get('trace-id')  # boto3 가 'x-amz-meta-' prefix 제거
    except Exception as e:
        logger.warning("S3 head_object 실패: bucket=%s key=%s error=%s", bucket, key, e)
        return None

def handler(event, context):
    bucket = event['detail']['bucket']['name']
    key = event['detail']['object']['key']

    trace_id = _extract_trace_id(bucket, key)
    if trace_id is None:
        trace_id = uuid.uuid4().hex[:16]
        logger.warning("S3 metadata trace-id 부재 - fallback uuid 사용: bucket=%s key=%s trace_id=%s",
                       bucket, key, trace_id)

    api_client.set_trace_id(trace_id)
    logger.info("analysis 시작 trace_id=%s key=%s", trace_id, key)
    # 기존 로직 진행
```

---

## 테스트

**카테고리**: Lambda 단위 (pytest + boto3 stub)

```python
def test_handler_extracts_trace_id_from_s3_metadata(s3_stubber):
    s3_stubber.add_response('head_object',
        {'Metadata': {'trace-id': 'abc12345'}},
        {'Bucket': '...', 'Key': '...'})
    response = handler(event, context)
    # api_client._trace_id == 'abc12345' 검증

def test_handler_uses_fallback_uuid_when_metadata_absent(s3_stubber, caplog):
    s3_stubber.add_response('head_object', {'Metadata': {}}, {...})
    handler(event, context)
    assert 'S3 metadata trace-id 부재' in caplog.text
    # api_client._trace_id 가 UUID hex 16자

def test_handler_uses_fallback_when_head_object_fails(s3_stubber, caplog):
    s3_stubber.add_client_error('head_object', 'NoSuchKey')
    handler(event, context)
    assert 'fallback uuid' in caplog.text
```

---

## 의존

- 선행: A4 (api_client `set_trace_id`)
- 외부: boto3 S3 client (기존)

---

## Verification Hook

- 명령: `cd lambda/analysis && pytest tests/test_handler.py`
- 통과 기준: 3 케이스 green
- 관찰 가능 동작: dev safe-deploy 후 분석 1건 → Lambda CloudWatch 로그에 `trace_id=<BE 발급값>` 일관 출력 + WARN 0건 (정상 케이스)

---

## 커밋 메시지 (예상)

```
feat(lambda): analysis S3 metadata 에서 traceId 추출 (자체 생성 제거)
```
