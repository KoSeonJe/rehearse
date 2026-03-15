# Feature Specification: 타임스탬프 피드백 리뷰 UI

> **문서 ID**: PLAN-007
> **작성일**: 2026-03-10
> **상태**: Completed
> **우선순위**: P1 (High Impact)

---

## Overview

### 문제 정의

AI가 생성한 피드백(타임스탬프, 카테고리, 심각도, 내용, 제안)을 단순 리스트로 보여주면 사용자가 면접 영상과 연동하여 맥락을 파악할 수 없다. 면접 영상을 재생하면서 그 시점의 피드백을 실시간으로 확인하고, 클릭으로 영상 특정 구간으로 이동할 수 있어야 한다.

### 솔루션 요약

React + Zustand 기반의 타임스탐프 피드백 리뷰 페이지를 구현한다. 화면은 좌측 60%에 비디오 플레이어 + 타임라인 바, 우측 40%에 피드백 패널로 구성되며, 비디오 재생 시점과 피드백 리스트가 실시간 동기화된다.

### 우선순위 근거

| 기준 | 판단 |
|------|------|
| **Impact** | High - 면접 피드백의 맥락 이해를 위해 필수 |
| **Effort** | Medium - 기존 Zustand + 훅 인프라 활용, 컴포넌트 조합 |
| **결론** | **P1** - 음성/비언어 분석 완료 후 즉시 개발 |

---

## Frontend Components

### 페이지: `interview-review-page.tsx`

**목적**: 타임스탬프 피드백 리뷰 페이지의 레이아웃 및 상태 관리

**주요 기능**:
- 화면 좌측 60%: 비디오 플레이어 + 타임라인 바
- 화면 우측 40%: 피드백 패널 (sticky scrolling)
- 헤더: 로고 + "피드백 리뷰" 제목 + 종합 리포트 링크 + 홈 버튼
- `useFeedbacks()` 훅으로 `GET /api/v1/interviews/{id}/feedbacks` 호출
- Zustand `useReviewStore`로 피드백 목록, 현재 시간, 선택된 피드백 ID 관리
- 로딩 상태 시 Character mood="thinking" 표시

**Props**: 없음 (URL 파라미터 `id`에서 인터뷰 ID 추출)

**상태 관리**:
```typescript
// useReviewStore 사용
{
  currentTime: number              // 현재 비디오 시간 (초)
  feedbacks: TimestampFeedback[]   // AI 피드백 배열
  selectedFeedbackId: number | null // 선택된 피드백 ID
  isPlaying: boolean               // 비디오 재생 중 여부
}
```

---

### 컴포넌트: `video-player.tsx`

**목적**: 녹화된 면접 영상 재생

**Props**:
```typescript
interface VideoPlayerProps {
  videoRef: RefObject<HTMLVideoElement | null>
}
```

**기능**:
- `useInterviewStore().videoBlobUrl`에서 영상 URL 가져오기
- 영상 없을 시 "녹화 영상이 없습니다" 메시지
- 네이티브 `<video>` controls 표시
- `aspect-video` 클래스로 16:9 비율 유지
- `bg-black` 배경

---

### 컴포넌트: `feedback-timeline.tsx`

**목적**: 비디오 진행 상황 및 피드백 마커 표시

**Props**:
```typescript
interface FeedbackTimelineProps {
  totalDuration: number              // 전체 영상 길이 (초)
  onSeekToFeedback: (feedbackId: number) => void
}
```

**기능**:
- 상단: 진행 바 (`width: ${progress}%` 스타일)
  - 배경: border 색상
  - 진행: text-tertiary 색상
  - 부드러운 transition
- 마커들: 각 피드백의 TimelineMarker 컴포넌트로 렌더
  - 위치: `left: ${(timestampSeconds / totalDuration) * 100}%`
  - 선택된 마커는 `ring-2 ring-accent`
- 하단: 현재 시간 / 전체 시간 (분:초 형식)
- 범례: VERBAL(blue), NON_VERBAL(yellow), CONTENT(green) 색상 표시

**동기화 로직**:
```typescript
const progress = totalDuration > 0 ? (currentTime / totalDuration) * 100 : 0
```

---

### 컴포넌트: `timeline-marker.tsx`

**목적**: 타임라인 상에서 개별 피드백 마커 표시

**Props**:
```typescript
interface TimelineMarkerProps {
  feedback: TimestampFeedback
  totalDuration: number
  isSelected: boolean
  onClick: () => void
}
```

**기능**:
- 위치: `left: ${(feedback.timestampSeconds / totalDuration) * 100}%`
- 색상: `CATEGORY_COLORS` 맵으로 카테고리별 색상
  - VERBAL: bg-info (파란색)
  - NON_VERBAL: bg-warning (노란색)
  - CONTENT: bg-success (초록색)
- 크기: 3x3px (w-3 h-3)
- 선택 시: `scale-150 ring-2 ring-white`
- 호버 시: `scale-125`
- title 속성으로 피드백 내용 tooltip

---

### 컴포넌트: `feedback-panel.tsx`

**목적**: 우측 패널에서 피드백 카드 리스트 표시

**Props**:
```typescript
interface FeedbackPanelProps {
  onSeekToFeedback: (feedbackId: number) => void
}
```

**기능**:
- Zustand에서 `feedbacks`, `currentTime`, `selectedFeedbackId` 가져오기
- 피드백 배열을 `timestampSeconds` 순서로 정렬
- 각 피드백을 FeedbackCard로 렌더
- 빈 상태 시: "피드백이 없습니다" 메시지
- 스크롤 가능 (`overflow-y-auto`)

**FeedbackCard 서브컴포넌트**:

| 필드 | 설명 |
|------|------|
| feedback | TimestampFeedback 객체 |
| isActive | 현재 시간에서 ±10초 범위 내 여부 |
| isSelected | 선택된 피드백 ID 일치 여부 |
| onClick | seekToFeedback 콜백 |

카드 스타일:
- 배경: severity별 light 색상 (INFO=blue-light, WARNING=yellow-light, SUGGESTION=green-light)
- 선택 시: `ring-2 ring-accent`
- 왼쪽 4px 컬러 바 (severity별 색상)
- 내용:
  - 시간 (분:초)
  - 심각도 배지 (라벨: "정보", "주의", "제안")
  - 카테고리 배지 (라벨: "언어적", "비언어적", "내용")
  - 피드백 내용
  - 제안사항 (있으면)

---

## Zustand Store: `review-store.ts`

```typescript
interface ReviewState {
  currentTime: number
  feedbacks: TimestampFeedback[]
  selectedFeedbackId: number | null
  isPlaying: boolean
}

interface ReviewActions {
  setCurrentTime: (time: number) => void
  setFeedbacks: (feedbacks: TimestampFeedback[]) => void
  selectFeedback: (id: number | null) => void
  setIsPlaying: (playing: boolean) => void
  reset: () => void
}
```

**초기값**:
```typescript
{
  currentTime: 0,
  feedbacks: [],
  selectedFeedbackId: null,
  isPlaying: false,
}
```

---

## Custom Hook: `use-video-sync.ts`

**목적**: 비디오 재생 상태와 Zustand 스토어 동기화

**반환값**:
```typescript
{
  videoRef: React.RefObject<HTMLVideoElement>
  seekTo: (time: number) => void
  seekToFeedback: (feedbackId: number) => void
}
```

**동작**:
- `videoRef`: HTML5 video 엘리먼트 참조
- `timeupdate` 이벤트: `setCurrentTime()` 호출 (0.1초 단위 업데이트)
- `play` 이벤트: `setIsPlaying(true)`
- `pause` 이벤트: `setIsPlaying(false)`
- `seekTo(time)`: 비디오 시간 이동 + Zustand 동기화
- `seekToFeedback(feedbackId)`: 피드백 ID로 해당 시간으로 이동 + 피드백 선택

---

## Custom Hook: `use-feedback.ts`

**목적**: 피드백 API 호출

**함수**:

### `useFeedbacks(interviewId: string)`

- **역할**: 피드백 리스트 조회
- **반환**: TanStack Query `useQuery` 결과
- **API**: `GET /api/v1/interviews/{interviewId}/feedbacks`
- **조건**: `enabled: !!interviewId` (ID 있을 때만 자동 호출)

---

## Data Types

### `TimestampFeedback`

```typescript
interface TimestampFeedback {
  id: number
  timestampSeconds: number              // 피드백 발생 시점 (초)
  category: 'VERBAL' | 'NON_VERBAL' | 'CONTENT'
  severity: 'INFO' | 'WARNING' | 'SUGGESTION'
  content: string                       // 피드백 내용
  suggestion: string | null             // 개선 제안 (선택사항)
}
```

### `FeedbackListResponse`

```typescript
interface FeedbackListResponse {
  interviewId: number
  feedbacks: TimestampFeedback[]
  totalCount: number
}
```

---

## Page Flow

```
[면접 진행 완료]
       ↓
[완료 페이지]
       ↓
[타임스탬프 피드백 리뷰] ← 이 페이지
       │
       ├── "종합 리포트" 버튼 → [종합 리포트 페이지]
       ├── "홈으로" 버튼 → [홈 페이지]
       │
       └─ 비디오 + 타임라인 + 피드백 동기화
```

---

## 주요 구현 결정사항

1. **비디오-타임라인-피드백 동기화**
   - Zustand `currentTime`으로 중앙 관리
   - `useVideoSync()` 훅에서 video.currentTime 추적
   - FeedbackTimeline/FeedbackPanel은 `currentTime` 구독하여 UI 업데이트

2. **레이아웃 반응형**
   - 데스크톱 (lg 이상): 좌 60% (비디오) + 우 40% (패널)
   - 모바일: 스택 레이아웃 (비디오 → 타임라인 → 패널)
   - 우측 패널: `sticky top-6` 적용 (스크롤 중 고정)

3. **카테고리 색상 통일**
   - VERBAL (언어적): info (파란색, #3182F6)
   - NON_VERBAL (비언어적): warning (주황색, #FFB84D)
   - CONTENT (내용): success (초록색, #00C48C)

4. **심각도 표시**
   - INFO (정보): 파란 배경 + 파란 텍스트
   - WARNING (주의): 주황 배경 + 주황 텍스트
   - SUGGESTION (제안): 초록 배경 + 초록 텍스트

5. **Active 피드백 강조**
   - 현재 시간에서 ±10초 범위의 피드백: `opacity-100`
   - 범위 밖: `opacity-60` (시각적 구분)

6. **마커 호버 인터랙션**
   - 타임라인 마커: hover/select 시 `scale-125` ~ `scale-150`
   - 피드백 카드: ring과 배경색 변화
   - 부드러운 transition (0.2s ease)

7. **API 캐싱**
   - TanStack Query `queryKey: ['feedbacks', interviewId]`
   - 같은 인터뷰 ID 재방문 시 캐시 활용

---

## Backend API 연동

### `GET /api/v1/interviews/{interviewId}/feedbacks`

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "interviewId": 1,
    "feedbacks": [
      {
        "id": 1,
        "timestampSeconds": 15,
        "category": "VERBAL",
        "severity": "INFO",
        "content": "말하는 속도가 조금 빨랐습니다.",
        "suggestion": "더 천천히 또렷하게 말씀해 주세요."
      },
      {
        "id": 2,
        "timestampSeconds": 45,
        "category": "NON_VERBAL",
        "severity": "WARNING",
        "content": "시선이 화면에서 벗어났습니다.",
        "suggestion": "카메라를 정면으로 바라보세요."
      }
    ],
    "totalCount": 2
  },
  "message": null
}
```

---

## 상태 전이 (state transitions)

**리뷰 페이지 생명주기**:

1. **마운트**:
   - `useFeedbacks()` 호출 → API 요청
   - video.onloadedmetadata → `totalDuration` 설정

2. **사용자 상호작용**:
   - 타임라인 마커 클릭 → `seekToFeedback(id)` → 비디오 시간 이동 + 피드백 선택
   - 피드백 카드 클릭 → 동일 로직

3. **언마운트**:
   - `useReviewStore().reset()` 호출 (메모리 정리)

---

## 테스트 전략 (Backend QA 참고)

- [x] 피드백 조회 API: 존재하는 인터뷰 ID → 200 + 피드백 배열
- [x] 피드백 조회 API: 존재하지 않는 ID → 404
- [x] 비디오 동기화: video.currentTime 변화 시 Zustand 업데이트
- [x] 마커 클릭: 해당 피드백 시간으로 비디오 이동
- [x] 피드백 패널: 시간대별 활성화/비활성화
- [x] 반응형 레이아웃: 모바일/태블릿/데스크톱

---

## 의존성

| 의존성 | 상태 | 설명 |
|--------|------|------|
| `react-router-dom` | 기존 | URL 파라미터 추출 |
| `zustand` | 기존 | 상태 관리 |
| `@tanstack/react-query` | 기존 | 서버 상태 |
| `interview-store` | 기존 | videoBlobUrl |
| Backend API | 완료 | GET /api/v1/interviews/{id}/feedbacks |

---

## 우려사항 및 해결책

| 우려사항 | 해결책 |
|---------|--------|
| 대규모 피드백(100개+)일 시 성능 저하 | virtualize-list 검토, 페이지네이션 |
| 비디오 형식 호환성 | WebM/VP9 (기존 녹화 포맷) 활용, fallback 링크 제공 |
| 타임스탬프 정확도 | 피드백 생성 API에서 ±1초 오차 범위 내 정확도 보장 |

---

## 완료 기준

- [x] 페이지 좌우 레이아웃 (60:40 비율)
- [x] 비디오 플레이어 + 타임라인 + 피드백 패널 컴포넌트
- [x] Zustand 상태 동기화 (currentTime, feedbacks, selectedFeedbackId)
- [x] useVideoSync 훅 (timeupdate, seekToFeedback)
- [x] useFeedbacks 훅 (TanStack Query)
- [x] 모바일 반응형 (스택 레이아웃)
- [x] 카테고리/심각도별 색상 표시
- [x] API 통합 테스트 (피드백 로드, 시간 이동)

---

## 완료된 작업

**Frontend 구현 완료 (PR #7)**:
- interview-review-page.tsx
- video-player.tsx
- feedback-timeline.tsx
- timeline-marker.tsx
- feedback-panel.tsx
- review-store.ts
- use-video-sync.ts
- use-feedback.ts (부분: useFeedbacks만 구현)

**Backend API 완료**:
- GET /api/v1/interviews/{id}/feedbacks (FeedbackController)
