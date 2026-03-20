# 피드백 페이지 UI/UX 개선 — 요구사항 정의

> 상태: Draft
> 작성일: 2026-03-20

## Why

- **Why?** — 피드백 페이지(`/interview/:id/feedback`)가 분석 데이터를 대부분 숨기고 있어 사용자가 면접 코칭 정보를 얻을 수 없다. 조건부 렌더링(`isAnalyzed`, null 체크)으로 점수/코멘트가 가려지고, 트랜스크립트만 표시되어 페이지 존재 의미가 퇴색.
- **Goal** — 피드백 카드를 "트랜스크립트 뷰어"에서 "코칭 카드"로 전환. 점수에 컨텍스트 부여, 정보 계층 정상화, 레이아웃/타이포그래피 개선. 사용자가 영상과 피드백을 동기화하며 구체적 개선 포인트를 얻을 수 있는 경험.
- **Evidence** — dev.rehearse.co.kr/interview/15/feedback에서 브라우저 QA + UI/UX 리뷰 수행. 8개 이슈 발견 (Critical 3 / High 3 / Medium 2). Nielsen Norman Group 정보 계층 원칙, 토스 디자인 시스템 참고.
- **Trade-offs** — BE API 변경 없이 FE만 수정하므로, 데이터가 null인 경우 "미분석" 명시적 표시로 대체. 대규모 레이아웃 변경보다 기존 컴포넌트 리팩토링 우선.

## 목표

1. 피드백 카드 재설계 — 점수/코멘트가 항상 보이고, 트랜스크립트는 접을 수 있게
2. 점수 컨텍스트 — 라벨(우수/보통/미흡) + 시각적 게이지
3. z-index 버그 수정
4. 분석 대기 상태 개선 — 단계 표시
5. 비디오 우선 레이아웃
6. 타임라인 색상 통일
7. 타이포그래피 계층 정상화

## 아키텍처 / 설계

### 변경 대상 컴포넌트

```
interview-feedback-page.tsx    # 레이아웃, 헤더 z-index, 분석 대기 UI
├── video-player.tsx           # (변경 없음)
├── timeline-bar.tsx           # 색상 인코딩 통일
└── feedback-panel.tsx         # 카드 재설계, 점수 컨텍스트, 타이포그래피
```

### 데이터 흐름 (변경 없음)

```
useInterview() → questionSets[]
                    ↓
useQuestionSetFeedback() → timestampFeedbacks[]
useQuestionsWithAnswers() → questions[]
                    ↓
useFeedbackSync(videoRef, feedbacks) → { activeFeedbackId, currentTimeMs, seekTo }
                    ↓
VideoPlayer ↔ TimelineBar ↔ FeedbackPanel
```

### 피드백 카드 정보 계층 (변경 후)

```
┌─ 코칭 카드 ──────────────────────────┐
│ [시간] 0:32 — 1:15     [타입] 원본   │
│                                       │
│ Q. 질문 텍스트                        │
│                                       │
│ ┌── 점수 요약 ──────────────────┐     │
│ │ 언어 72/100 [━━━━━━━░░░] 보통 │     │
│ │ 시선 85     자세 60    표정 😐│     │
│ └──────────────────────────────┘     │
│                                       │
│ 💡 종합 코멘트                        │
│ "구체적인 개선 포인트..."             │
│                                       │
│ ▸ 답변 텍스트 보기 (접힘)             │
│ ▸ 모범답변 비교 (접힘)                │
└───────────────────────────────────────┘
```

## Scope

- **In**: 피드백 페이지 FE 컴포넌트 (feedback-panel, timeline-bar, interview-feedback-page)
- **Out**: BE API 변경, 새로운 API 엔드포인트, 다른 페이지 수정, 새 라이브러리 추가

## 제약조건 / 환경

- `TimestampFeedback` 타입 구조 변경 없음 (BE 호환)
- 토스 스타일 디자인 토큰 유지 (coral accent, Pretendard, 모노톤)
- FE만 변경 — TypeScript strict, no `any`
- 기존 TanStack Query 훅, useFeedbackSync 로직 유지

## 이슈 목록 (우선순위순)

| # | 심각도 | 이슈 | 대상 파일 |
|---|--------|------|-----------|
| 1 | Critical | 피드백 카드가 점수/코멘트를 숨김 | feedback-panel.tsx |
| 2 | Critical | 점수에 컨텍스트 부재 (숫자만 표시) | feedback-panel.tsx |
| 3 | Critical | 헤더 z-index 버그 | interview-feedback-page.tsx |
| 4 | High | 빈 "분석 대기 중" 블록 | interview-feedback-page.tsx |
| 5 | High | 비디오 너무 작음 (50/50 레이아웃) | interview-feedback-page.tsx |
| 6 | High | 카드 정보 계층 역전 | feedback-panel.tsx |
| 7 | Medium | 타임라인 색상 인코딩 혼란 | timeline-bar.tsx |
| 8 | Medium | font-black 남용 | 전체 |
