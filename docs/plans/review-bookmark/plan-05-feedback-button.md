# Plan 05: 피드백 페이지 북마크 버튼 + 코치마크 + 토스트

> 상태: Draft
> 작성일: 2026-04-12
> 선행: Plan 03 (API) + Plan 04 (프로토타입) 완료

## Why

복습 북마크 기능의 진입점이자 가장 빈번히 노출되는 인터페이스. 사용자가 피드백 페이지에서 답변을 읽는 즉시 "다시 연습하자"는 판단을 내릴 수 있는 지점에 버튼을 배치해 학습 루프를 닫는다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/components/feedback/feedback-panel.tsx` | `FeedbackCard` 헤더에 북마크 버튼 추가, `aria-pressed` 토글 |
| `frontend/src/components/feedback/bookmark-toggle-button.tsx` | 재사용 토글 버튼 컴포넌트 신설 |
| `frontend/src/components/feedback/review-coach-mark.tsx` | 최초 사용자 popover 컴포넌트 신설 |
| `frontend/src/components/common/review-toast.tsx` | 토스트 컴포넌트 (Undo 링크 포함, 프로젝트에 공용 토스트가 있으면 재사용) |
| `frontend/src/hooks/use-review-bookmarks.ts` | TanStack Query 훅(목록/생성/삭제/존재 조회) |
| `frontend/src/api/review-bookmark.ts` | API 클라이언트 함수 |
| `frontend/src/types/review-bookmark.ts` | DTO 타입 정의 |
| `frontend/src/pages/interview-feedback-page.tsx` | 페이지 진입 시 `GET /exists` 배치 호출로 상태 동기화 |

## 상세

### BookmarkToggleButton 컴포넌트

Plan 04 프로토타입 스펙 그대로 구현. 핵심:
- `aria-pressed={isBookmarked}`
- `aria-label` 동적: "복습 북마크에 담기" / "복습 북마크에서 제거"
- 클릭 시 즉시 optimistic update → 실패 시 롤백 + 에러 토스트
- 토글 애니메이션: `scale(1) → 1.15 → 1` 180ms, `prefers-reduced-motion`에서 제거
- Lucide 아이콘: `ListPlus` / `ListChecks`
- 포커스 링: `focus-visible:ring-2 focus-visible:ring-[#FF6B5B] focus-visible:ring-offset-2`

### 데이터 흐름

1. 피드백 페이지 진입 → 현재 면접의 `timestampFeedbackIds` 수집 → `GET /api/review-bookmarks/exists` 1회 호출
2. 응답 `bookmarkedIds`를 Zustand 또는 Query 캐시에 저장
3. 각 `FeedbackCard`는 자신의 `feedback.id`가 `bookmarkedIds`에 포함되는지 확인해 버튼 상태 결정
4. 클릭 시:
   - 미북마크 → `POST /api/review-bookmarks` → 성공 시 캐시에 id 추가 + 토스트(최초 1회만) + optimistic scale 애니메이션
   - 북마크됨 → `DELETE /api/review-bookmarks/{bookmarkId}` → 성공 시 캐시에서 제거 (토스트 없음)

**주의**: `DELETE`는 `bookmarkId`(북마크 PK)를 알아야 한다. Plan 03의 `GET /exists`는 `{items: [{timestampFeedbackId, bookmarkId}]}` 구조로 쌍을 반환하도록 이미 정의되어 있으므로, 프론트는 응답을 `Map<timestampFeedbackId, bookmarkId>`로 보관해 삭제 시 즉시 사용한다.

### 최초 사용자 코치마크

- 트리거 조건: 피드백 페이지에서 `FeedbackCard`가 렌더된 후 600ms 지연 + `localStorage.getItem('rehearse:review-coach-seen-v1') !== '1'`
- 형태: 버튼 하단 popover (말풍선 + 화살표)
- 문구: "답변을 다시 꺼내 보고 싶을 때, 여기를 눌러 담아두세요."
- 닫기 조건:
  - "알겠어요" 버튼 클릭
  - 외부 클릭 / ESC 키
  - 버튼 최초 클릭(자동 닫힘)
  - 8초 경과 (자동 dismiss)
- 닫힘 시 `localStorage.setItem('rehearse:review-coach-seen-v1', '1')`

### 토스트

- 최초 1회만 노출 — `localStorage.getItem('rehearse:review-toast-seen') !== '1'` 확인
- 문구: "복습 북마크에 담겼어요. 사이드바에서 언제든 꺼내 볼 수 있어요."
- 위치: `bottom-6 right-6`, 3초 auto-dismiss
- "되돌리기" 링크 포함 (클릭 시 즉시 DELETE)
- `role="status"` + `aria-live="polite"`

### TanStack Query 키 전략

```ts
reviewBookmark: {
  all: ['review-bookmark'] as const,
  list: (status: BookmarkStatus) => [...reviewBookmark.all, 'list', status] as const,
  existsForInterview: (interviewId: number) =>
    [...reviewBookmark.all, 'exists', interviewId] as const,
}
```

`POST`/`DELETE` 후 해당 interview의 `exists` 쿼리와 `list` 전체 쿼리를 invalidate.

## 담당 에이전트

- Implement: `frontend` — 컴포넌트, 훅, API 클라이언트, 페이지 통합
- Review: `code-reviewer` — 옵티미스틱 업데이트, 에러 처리, 타입 안전성
- Review: `designer` — Plan 04 프로토타입과의 시각적 일치성
- Review: `ui-ux-designer` — 접근성(aria, 키보드, 대비), 코치마크 UX

## 검증

- `npm run lint` + `npm run build` 통과
- 피드백 페이지 수동 E2E:
  1. 미북마크 답변에서 버튼 클릭 → 토글 + 토스트(1회) + 사이드바 "복습 북마크"에 추가 확인
  2. 이미 북마크된 답변 새로고침 시 버튼 활성 상태 복원 확인
  3. "되돌리기" 클릭 시 즉시 삭제 확인
  4. 최초 진입 시 코치마크 노출, "알겠어요" 후 재진입 시 미노출 확인
  5. 키보드만으로 탭 이동/Enter 토글 가능 확인
  6. 스크린 리더에서 상태 전환 읽힘 확인 (VoiceOver/NVDA)
- `prefers-reduced-motion: reduce` 환경에서 scale 애니메이션 생략 확인
- `progress.md` 상태 업데이트 (Task 5 → Completed)
