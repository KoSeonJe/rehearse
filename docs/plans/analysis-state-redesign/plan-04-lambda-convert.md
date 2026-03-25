# Plan 04: Lambda Convert 수정

> 상태: Draft
> 작성일: 2026-03-24

## Why

변환 상태가 FileMetadata에서 QuestionSetAnalysis.convertStatus로 이동한다. Convert Lambda는 더 이상 FileMetadata의 status를 CONVERTING/CONVERTED로 변경하지 않고, QuestionSetAnalysis의 convertStatus를 업데이트해야 한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/convert/handler.py` | FileMetadata status 업데이트 → QuestionSetAnalysis convertStatus 업데이트로 변경. CONVERTING→PROCESSING, CONVERTED→COMPLETED 문자열 변경 |
| `lambda/convert/api_client.py` | update_file_status() → update_convert_status() 함수 변경. API 엔드포인트 변경 |

## 상세

### handler.py 변경

```python
# 변경 전
update_file_status(file_id, "UPLOADED")
update_file_status(file_id, "CONVERTING")
# ... MediaConvert 실행 ...
update_file_status(file_id, "CONVERTED", streamingS3Key=output_key)

# 변경 후
update_file_status(file_id, "UPLOADED")  # FileMetadata는 업로드 확인만 유지
update_convert_status(interview_id, question_set_id, "PROCESSING")
# ... MediaConvert 실행 ...
update_convert_status(interview_id, question_set_id, "COMPLETED", streamingS3Key=output_key)
```

실패 시:
```python
# 변경 전
update_file_status(file_id, "FAILED", failureReason=..., failureDetail=...)

# 변경 후
update_convert_status(interview_id, question_set_id, "FAILED", failureReason=..., failureDetail=...)
```

### api_client.py 변경

```python
# 신규 함수
@retry_on_transient()
def update_convert_status(
    interview_id: int,
    question_set_id: int,
    status: str,
    **kwargs,
) -> None:
    url = f"{Config.API_SERVER_URL}/api/internal/interviews/{interview_id}/question-sets/{question_set_id}/convert-status"
    body = {"status": status, **kwargs}
    resp = httpx.put(url, json=body, headers=_get_headers(), timeout=TIMEOUT)
    resp.raise_for_status()
```

### S3 key에서 interview_id, question_set_id 파싱

Convert Lambda는 현재 file_id 기반으로 동작한다. QuestionSetAnalysis 업데이트를 위해 interview_id와 question_set_id가 필요하다. S3 key 형식이 `videos/{interviewId}/qs_{questionSetId}.webm`이므로 파싱 가능:

```python
def _parse_ids_from_key(key: str):
    # videos/42/qs_123.webm → (42, 123)
    parts = key.split("/")
    interview_id = int(parts[1])
    qs_part = parts[2].split(".")[0]  # qs_123
    question_set_id = int(qs_part.split("_")[1])
    return interview_id, question_set_id
```

## 담당 에이전트

- Implement: `backend` (Lambda Python)
- Review: `code-reviewer` — API 엔드포인트 매핑, 에러 처리

## 검증

- 정상 변환: PROCESSING → COMPLETED 전이 + streamingS3Key 저장 확인
- 변환 실패: FAILED + failureReason 저장 확인
- 멱등성: 중복 트리거 시 이미 COMPLETED면 스킵
- FileMetadata.status는 UPLOADED에서 변경 안 되는지 확인
- `progress.md` 상태 업데이트 (Task 4 → Completed)
