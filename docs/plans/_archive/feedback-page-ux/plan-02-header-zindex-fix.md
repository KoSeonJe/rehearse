# Plan 02: 헤더 z-index 버그 수정 [parallel]

> 상태: Draft
> 작성일: 2026-03-20

## Why

sticky header(`z-50`)가 비디오 플레이어의 전체화면/컨트롤 위에 겹쳐서 비디오 상단 부분이 가려진다. 특히 스크롤 시 비디오가 헤더 아래로 들어가면서 재생 컨트롤이 클릭 불가능해지는 UX 문제. (이슈 #3)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/pages/interview-feedback-page.tsx` | 헤더 z-index를 `z-30`으로 낮추고, 비디오 영역은 `sticky` 시 `z-40`으로 설정 |

## 상세

### 방안: z-index 체계 정리

```
z-20: 타임라인 playhead (기존 z-20 유지)
z-30: sticky header (z-50 → z-30)
z-40: (미래 오버레이/모달용 예약)
z-50: (사용하지 않음)
```

변경 내용:
- 헤더: `z-50` → `z-30`
- 비디오 영역의 z-index는 건드리지 않음 (비디오는 sticky가 아니므로 겹침 자체가 해소됨)

```tsx
// Before
<header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md ...">

// After
<header className="sticky top-0 z-30 bg-white/80 backdrop-blur-md ...">
```

## 담당 에이전트

- Implement: `frontend` — z-index 수정
- Review: `qa` — 스크롤 시 헤더/비디오 겹침 없음 확인

## 검증

- 페이지 스크롤 시 비디오가 헤더 아래로 가려지지 않음
- 비디오 컨트롤이 항상 클릭 가능
- 헤더의 sticky 동작은 정상 유지
- `progress.md` 상태 업데이트 (Task 2 → Completed)
