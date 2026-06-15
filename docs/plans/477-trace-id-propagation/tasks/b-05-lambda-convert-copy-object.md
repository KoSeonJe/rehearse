# Task B5 — Lambda convert head_object + copy_object MetadataDirective=REPLACE

> **PR**: PR-B
> **영역**: Lambda
> **선행 Task**: A5 (rename 선행)

---

## 변경 파일

- `lambda/convert/handler.py` — **변경**. 자체 생성 제거 (`L28-30`). 진입 시 `head_object` → metadata 추출. transcoded MP4 출력 시 `s3.copy_object(MetadataDirective='REPLACE', Metadata={'trace-id': trace_id})` 로 신규 객체에도 metadata 보존.
- `lambda/convert/tests/test_handler.py` — **신규 또는 확장**. head_object 추출 + copy_object 검증.

---

## 핵심 로직

```python
# convert/handler.py (변경부)
def _extract_trace_id(bucket: str, key: str) -> str | None:
    # B4 와 동일 패턴
    ...

def handler(event, context):
    bucket = event['detail']['bucket']['name']
    src_key = event['detail']['object']['key']

    trace_id = _extract_trace_id(bucket, src_key)
    if trace_id is None:
        trace_id = uuid.uuid4().hex[:16]
        logger.warning("S3 metadata trace-id 부재 - fallback uuid 사용: bucket=%s key=%s trace_id=%s",
                       bucket, src_key, trace_id)

    api_client.set_trace_id(trace_id)

    # MediaConvert 작업 제출 / 완료 후 transcoded 객체 metadata 복사
    dst_key = derive_output_key(src_key)
    s3_client.copy_object(
        Bucket=bucket,
        Key=dst_key,
        CopySource={'Bucket': bucket, 'Key': src_key},
        MetadataDirective='REPLACE',
        Metadata={'trace-id': trace_id},
    )
```

> **주의**: convert Lambda 가 직접 copy 하는 케이스와 MediaConvert job 결과를 후처리하는 케이스 구분 필요. tech-spec.md §Architecture 시퀀스 = "transcoded MP4 출력 시 copy_object". 실제 흐름은 코드 진입 시 확인 후 결정.

---

## 테스트

**카테고리**: Lambda 단위 (pytest + boto3 stub)

```python
def test_convert_extracts_trace_id_from_source(s3_stubber):
    s3_stubber.add_response('head_object', {'Metadata': {'trace-id': 'abc12345'}}, {...})
    handler(event, context)
    # api_client._trace_id == 'abc12345'

def test_convert_copies_metadata_to_output(s3_stubber):
    s3_stubber.add_response('head_object', {'Metadata': {'trace-id': 'abc12345'}}, {...})
    s3_stubber.add_response('copy_object', {}, {
        'Bucket': '...',
        'Key': '<dst>',
        'CopySource': {'Bucket': '...', 'Key': '<src>'},
        'MetadataDirective': 'REPLACE',
        'Metadata': {'trace-id': 'abc12345'},
    })
    handler(event, context)
```

---

## 의존

- 선행: A5 (api_client `set_trace_id`)
- 외부: boto3 S3 client (기존)

---

## Verification Hook

- 명령: `cd lambda/convert && pytest tests/test_handler.py`
- 통과 기준: 2 케이스 green
- 관찰 가능 동작: dev safe-deploy 후 변환 1건 → S3 출력 객체 (transcoded MP4) metadata 에 `trace-id` 보유 + analysis Lambda 가 동일 traceId 추출

---

## 커밋 메시지 (예상)

```
feat(lambda): convert 출력 객체에 trace-id metadata 복사
```
