# Task 3: 모범 답변 제공

## Status: Not Started

## Why

모범 답변이 없어 사용자가 "어떻게 답해야 하는지" 알 수 없음. 피드백의 실질적 학습 효과를 위해 필수.

> 비언어 분석(#59)은 Sprint 0 Task 6 (분석 Lambda — Vision)에서 서버사이드로 처리되므로 제거.

## Issues

| # | 제목 | 타입 |
|---|------|------|
| #58 | 질문별 모범 답변 및 관련 학습 자료 제공 | enhancement |

## 의존성

- Sprint 0 완료 후 시작 — Lambda 분석 파이프라인이 안정화되어야 함

## 구현 계획

### PR 1: [BE] — 모범 답변 API (#58-BE)

1. **모범 답변** (`ClaudePromptBuilder.java`)
   - 피드백 생성 시 각 질문에 `modelAnswer`, `references[]` 필드 추가
   - Claude 프롬프트에 모범 답변 생성 지시 (간결하고 구조화된 답변)
   - 학습 자료: Claude가 추천하는 키워드 기반 검색 링크
   - API 응답 확장: 피드백 응답에 필드 추가

**Agent**: `backend` (구현), `architect-reviewer` (리뷰)

### PR 2: [FE] — 모범 답변 UI (#58-FE)

1. 질문별 "모범 답변 보기" 토글 UI
2. 학습 자료 링크 표시

**Agent**: `frontend` (구현), `code-reviewer` (리뷰)

## Acceptance Criteria

- [ ] 각 질문에 모범 답변 표시 가능
- [ ] 학습 자료 링크 제공
- [ ] 모범 답변 토글 UI 동작
