# Task 8: FE 질문세트 단위 녹화 + S3 업로드

## Status: Not Started

## Issue: #86

## Why

기존 답변별 분리 녹화에서 질문세트 단위 연속 녹화로 전환.
녹화 완료 즉시 S3에 업로드하여 백그라운드 분석 시작.

상세 흐름: `docs/architecture/recording-analysis-pipeline.md` 2단계

## 의존성

- 선행: Task 3 (외부 API — 답변 저장, Presigned URL)
- 후행: Task 9 (업로드 후 상태 추적)

## 구현 계획

### PR 1: [FE] 질문세트 녹화 + 타임스탬프 + S3 업로드

**녹화 흐름 (1개 질문세트):**
1. 원본 질문 표시 → "답변 시작" → 녹화 시작 (mainAnswerStartMs = 0)
2. "답변 종료" → mainAnswerEndMs 기록
3. 후속 질문 1~3 반복 (각각 start/end ms 기록)
4. 후속 질문 3 "답변 종료" → 녹화 종료
5. POST /answers → 메타데이터 전송 (4개 구간)
6. POST /upload-url → Presigned URL 발급
7. PUT → S3에 WebM 직접 업로드
8. 다음 질문세트로 전환

**수정 파일:**
- `use-media-recorder.ts`: 질문세트 단위 녹화 시작/종료
- `use-answer-flow.ts`: 질문세트 플로우 (원본 + 후속 3개 = 1세트)
- `interview-store.ts`: 질문세트 상태 관리
- `video-storage.ts`: S3 업로드로 전환

**신규 파일:**
- `use-s3-upload.ts`: Presigned URL 발급 + PUT 업로드 + 재시도

**실패 대응:**
- 녹화 중 크래시: 5초마다 IndexedDB에 청크 임시 저장
- 업로드 실패: 3회 재시도, 실패 시 IndexedDB 보관 + 면접 종료 후 재업로드
- Presigned URL 만료: 15분, 만료 시 자동 재발급
- beforeunload 경고: 업로드 중 페이지 이탈 방지

- Implement: `frontend`
- Review: `architect-reviewer` + `code-reviewer`

## Acceptance Criteria

- [ ] 질문세트 단위로 녹화 (원본 + 후속 3개 = 1영상)
- [ ] 답변 시작/종료 타임스탬프 정확 기록
- [ ] 메타데이터 API 전송 성공
- [ ] Presigned URL로 S3 PUT 업로드 성공
- [ ] 업로드 중 진행률 표시
- [ ] 업로드 실패 시 3회 재시도
- [ ] 다음 질문세트 전환 시 이전 업로드 백그라운드 진행
