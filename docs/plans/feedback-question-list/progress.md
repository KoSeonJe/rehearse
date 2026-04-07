# 피드백 페이지 질문 목록 (feedback-question-list) — 진행 상황

## 태스크 상태

| # | 태스크 | 담당 (Implement / Review) | 상태 | 비고 |
|---|---|---|---|---|
| 1 | QuestionList 컴포넌트 + 페이지 통합 | `frontend` / `code-reviewer`, `designer` | Draft | 단일 PR (FE only) |

## 머지 순서

`[FE]` Plan 01 단일 PR. BE/Lambda 변경 없음.

## 진행 로그

### 2026-04-07
- 사용자 요청: feedback-v3 프리뷰 HTML 검토 중 영상 아래 질문 목록 카드를 보고 "실제로도 띄워달라"고 요청
- 데이터/API 사전 조사:
  - `QuestionWithAnswer`에 `questionType: 'MAIN' | 'FOLLOWUP'` + `startMs`/`endMs` 이미 있음 (`interview.ts:113`)
  - `/api/v1/interviews/{id}/question-sets/{qsId}/questions-with-answers` 엔드포인트 + `useQuestionsWithAnswers` 훅 이미 있음
  - `interview-feedback-page.tsx`가 이미 `questionsRes`를 받아 `FeedbackPanel`에 넘기는 중 — `QuestionList`도 같은 데이터로 즉시 만들 수 있음
- 결정: BE/Lambda 변경 0, 신규 컴포넌트 1개 + 페이지 1줄 추가만으로 구현 가능
- 산출 파일: `requirements.md`, `plan-01-question-list.md`, `progress.md`
