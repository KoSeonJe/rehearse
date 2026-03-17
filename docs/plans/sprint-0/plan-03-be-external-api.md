# Task 3: BE 외부 API — 면접 생성 리팩토링 + 클라이언트 API 5개

## Status: Not Started

## Issues: #80, #81

## Why

새 파이프라인은 질문세트 단위로 동작하므로, 면접 생성 응답이 questionSets[] 구조여야 함.
모범답변/가이드도 질문 생성 시점에 사전생성하여 분석 대기 중 즉시 제공해야 함.
답변 저장, Presigned URL 발급, 상태/피드백 조회 API가 새로 필요.

## 의존성

- 선행: Task 1 (DB 스키마 — 엔티티 필요)
- 후행: Task 8 (FE 녹화에서 API 호출)

## 구현 계획

### PR 1: [BE] 면접 생성 API 리팩토링 (#80)

**면접 생성 응답 변경:**
```json
POST /api/interviews → {
  "interviewId": 42,
  "status": "IN_PROGRESS",
  "questionSets": [
    {
      "questionSetId": 1,
      "category": "RESUME",
      "mainQuestion": "...",
      "followUpQuestions": ["...", "...", "..."],
      "mainModelAnswer": "...",
      "followUpReferences": [{"type": "GUIDE", "content": "..."}, ...]
    }, ...
  ]
}
```

**모범답변/가이드 사전생성:**
- RESUME 카테고리: 원본 → 이력서 맞춤 모범답변, 후속 → 답변 가이드
- CS 카테고리: 원본 + 후속 모두 → 모범답변
- ClaudePromptBuilder에 모범답변/가이드 프롬프트 추가
- InterviewService에서 질문 생성 직후 모범답변 생성 → DB 저장

**수정 대상:**
- `InterviewController.java`
- `InterviewService.java`
- `ClaudePromptBuilder.java`
- 응답 DTO 신규/수정

- Implement: `backend`
- Review: `architect-reviewer` — 레이어링, Claude API 호출 구조

### PR 2: [BE] 클라이언트 API 5개 (#81)

| 엔드포인트 | 메서드 | 용도 |
|------------|--------|------|
| `/api/interviews/{id}/question-sets/{qsId}/answers` | POST | 답변 구간 메타데이터 저장 (4개 구간) |
| `/api/interviews/{id}/question-sets/{qsId}/upload-url` | POST | Presigned URL 발급 |
| `/api/interviews/{id}/status` | GET | 전체 면접 + 질문세트별 분석/변환 상태 |
| `/api/interviews/{id}/feedback` | GET | 전체 피드백 조회 |
| `/api/interviews/{id}/questions-with-answers` | GET | 모범답변/가이드 조회 |

**신규 파일:**
- `QuestionSetController.java`
- `QuestionSetService.java`
- DTO 클래스들

- Implement: `backend`
- Review: `architect-reviewer` — API 설계, 에러 핸들링

## Acceptance Criteria

- [ ] 면접 생성 시 questionSets[] 응답 + 모범답변/가이드 포함
- [ ] RESUME/CS 카테고리별 모범답변 유형 구분
- [ ] 답변 구간 저장 API 동작 (4개 구간)
- [ ] Presigned URL 발급 + S3 PUT 테스트
- [ ] 상태 조회 API에 analysisStatus + convertStatus 포함
- [ ] 피드백 조회 API에 타임스탬프별 피드백 포함
- [ ] 모범답변 조회 API 동작
