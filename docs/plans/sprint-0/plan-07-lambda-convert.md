# Task 7: 변환 Lambda — WebM → MP4 (MediaConvert)

## Status: Not Started

## Issue: #85

## Why

브라우저 MediaRecorder는 WebM으로 녹화하지만, 스트리밍 재생에는 MP4(faststart)가 필요.
S3 이벤트로 자동 트리거되어 변환하고, 완료 시 API 서버에 streaming_url을 알림.

## 의존성

- 선행: Task 2 (인프라 — MediaConvert 템플릿), Task 4 (내부 API — convert-status)
- 후행: Task 10 (피드백 뷰어에서 MP4 재생)

## 구현 계획

### PR 1: [Lambda] 변환 파이프라인 (Python)

**입력:** S3 이벤트 (영상 업로드 알림 — 분석 Lambda와 동시 트리거)

**처리 단계:**

1. **멱등성 체크**: convert_status 확인 → PENDING 아니면 스킵
2. **MediaConvert 작업 생성**:
   - 입력: `s3://.../videos/{id}/qs_{qsId}.webm`
   - 출력: `s3://.../videos/{id}/qs_{qsId}.mp4`
   - 설정: H.264, AAC, faststart, 원본 해상도 유지
3. **작업 완료 대기** (또는 EventBridge로 완료 이벤트 수신)
4. **API 서버 알림**:
   - PUT /api/internal/.../convert-status
   - `{ "convertStatus": "COMPLETED", "streamingUrl": "s3://..." }`

**실패 처리:**
- MediaConvert 실패: 1회 재시도 → 재실패 시 convert_status = FAILED
- 원본 WebM으로 폴백 재생 가능

**기술 스택:**
- Python 3.12, boto3

- Implement: `devops-engineer`
- Review: `code-reviewer`

## Acceptance Criteria

- [ ] S3 이벤트로 자동 트리거
- [ ] 멱등성: 중복 이벤트 시 스킵
- [ ] MediaConvert로 WebM → MP4 변환 성공
- [ ] 변환 완료 후 API 서버에 streaming_url 전달
- [ ] 실패 시 convert_status = FAILED
