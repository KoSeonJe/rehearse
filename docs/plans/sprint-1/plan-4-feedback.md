# Task 4: 피드백/분석 강화

## Status: Not Started

## Why

비언어적 피드백이 전혀 생성되지 않아 서비스의 차별화 포인트가 작동하지 않음. 또한 모범 답변이 없어 사용자가 "어떻게 답해야 하는지" 알 수 없음. 피드백의 실질적 학습 효과를 위해 필수.

## Issues

| # | 제목 | 타입 |
|---|------|------|
| #59 | 비언어적 피드백이 생성되지 않는 문제 | bug |
| #58 | 질문별 모범 답변 및 관련 학습 자료 제공 | enhancement |

## 의존성

- Task 1 (영상 파이프라인) 완료 후 시작 — 연속 녹화 전환 후 비언어 분석 파이프라인이 안정화되어야 함

## 구현 계획

### PR 1: [BE] — 모범 답변 API + 비언어 피드백 프롬프트 (#58, #59-BE)

1. **모범 답변** (`ClaudePromptBuilder.java`, `FeedbackService.java`)
   - 피드백 생성 시 각 질문에 `modelAnswer`, `references[]` 필드 추가
   - Claude 프롬프트에 모범 답변 생성 지시 (간결하고 구조화된 답변)
   - 학습 자료: Claude가 추천하는 키워드 기반 검색 링크
   - API 응답 확장: `FeedbackResponse`에 필드 추가

2. **비언어 피드백 보장** (`ClaudePromptBuilder.java`)
   - 비언어 데이터가 빈 값이어도 기본 NON_VERBAL 피드백 생성하도록 프롬프트 조정

관련 파일:
- `backend/src/.../ClaudePromptBuilder.java`
- `backend/src/.../FeedbackService.java`
- `backend/src/.../GeneratedFeedback.java` (DTO 확장)

**Agent**: `backend` (구현), `architect-reviewer` (리뷰)

### PR 2: [FE] — 비언어 분석 파이프라인 수정 + 모범 답변 UI (#59-FE, #58-FE)

1. **비언어 분석 수정** (#59)
   - MediaPipe 초기화 성공 여부 로깅 + 실패 시 사용자 알림
   - `event-detector.ts` 이벤트 감지 로그 추가
   - 비언어 이벤트 0개일 때 별도 처리 (기본 메시지)
   - `interview-complete-page.tsx`: `nonVerbalSummary` 빈 문자열 방지

2. **모범 답변 UI** (#58)
   - 질문별 "모범 답변 보기" 토글 UI
   - 학습 자료 링크 표시

관련 파일:
- `frontend/src/hooks/use-face-mesh.ts`
- `frontend/src/hooks/use-pose-detection.ts`
- `frontend/src/lib/event-detector.ts`
- `frontend/src/pages/interview-complete-page.tsx`
- `frontend/src/components/review/` (모범 답변 UI 컴포넌트)

**Agent**: `frontend` (구현), `code-reviewer` (리뷰)

## Acceptance Criteria

- [ ] 비언어적 피드백(NON_VERBAL)이 1개 이상 생성됨
- [ ] MediaPipe 로드 실패 시 사용자에게 알림 표시
- [ ] 비언어 이벤트 0개일 때 기본 피드백 생성
- [ ] 각 질문에 모범 답변 표시 가능
- [ ] 학습 자료 링크 제공
- [ ] 모범 답변 토글 UI 동작
