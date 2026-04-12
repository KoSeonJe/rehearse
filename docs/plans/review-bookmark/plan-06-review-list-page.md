# Plan 06: 전역 `/review-list` 페이지 + 사이드바 진입점

> 상태: Draft
> 작성일: 2026-04-12
> 선행: Plan 03 (API) + Plan 04 (프로토타입) 완료

## Why

복습 북마크의 자산을 실제로 꺼내 보고, 여러 면접에 걸쳐 쌓인 답변을 **카테고리별로 횡단 조회**하는 핵심 화면. 사용자가 평소 학습 루틴으로 재방문하는 페이지가 되어야 하므로 전역 접근성과 정보 밀도가 중요하다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/pages/review-list-page.tsx` | 전용 풀페이지 신설 |
| `frontend/src/components/review/review-list-header.tsx` | 제목 + 총 개수 + 설명 |
| `frontend/src/components/review/review-list-filter-bar.tsx` | 상태 필터 칩 + 카테고리 드롭다운 |
| `frontend/src/components/review/review-category-section.tsx` | 카테고리 섹션(헤더 + 카드 그리드) |
| `frontend/src/components/review/review-bookmark-card.tsx` | 카드 — 질문/날짜/해결 배지/확장 |
| `frontend/src/components/review/answer-comparison-view.tsx` | 내 답변 vs 모범답변 좌우 비교 |
| `frontend/src/components/review/review-empty-state.tsx` | 빈 상태 |
| `frontend/src/constants/interview-type-groups.ts` | `InterviewType → UI 상위 그룹` 매핑 상수 (Plan 04에서 확정된 값) |
| `frontend/src/app.tsx` | `/review-list` 라우트 등록 (소문자 파일명 확인 완료) |
| `frontend/src/components/dashboard/sidebar.tsx` (또는 글로벌 nav) | "복습 북마크" 메뉴 항목 추가 |
| `frontend/src/hooks/use-review-bookmarks.ts` | 이미 Plan 05에서 생성, `listQuery`, `updateStatusMutation`, `deleteMutation` 추가 |

## 상세

### 페이지 구조

```tsx
<ReviewListPage>
  <ReviewListHeader total={total} />
  <ReviewListFilterBar
    status={status} onStatusChange={setStatus}
    categoryFilter={categoryFilter} onCategoryChange={setCategoryFilter}
  />
  {isEmpty ? (
    <ReviewEmptyState />
  ) : (
    groupedItems.map(group => (
      <ReviewCategorySection
        key={group.key}
        label={group.label}
        count={group.items.length}
        items={group.items}
      />
    ))
  )}
</ReviewListPage>
```

### 필터 바

- 상태 필터: `전체 / 연습 중 / 해결됨` 3-way 칩 (단일 선택)
  - TanStack Query `queryKey`에 status 포함 → 자동 재조회
- 카테고리 드롭다운: Plan 04에서 확정된 상위 그룹(5~6개) + "전체"
  - 단일 선택 드롭다운(`select` 또는 shadcn Select 컴포넌트)

### 카테고리 그룹핑 로직

```ts
// frontend/src/constants/interview-type-groups.ts
export const INTERVIEW_TYPE_GROUPS = {
  CS_FUNDAMENTALS: { label: 'CS 기초', types: ['CS_FUNDAMENTAL'] },
  SYSTEM_DESIGN: { label: '시스템 설계', types: ['SYSTEM_DESIGN'] },
  LANGUAGE_FRAMEWORK: {
    label: '언어·프레임워크',
    types: ['LANGUAGE_FRAMEWORK', 'UI_FRAMEWORK', 'BROWSER_PERFORMANCE', 'FULLSTACK_STACK']
  },
  INFRA_CLOUD: { label: '인프라·클라우드', types: ['INFRA_CICD', 'CLOUD'] },
  DATA: { label: '데이터', types: ['DATA_PIPELINE', 'SQL_MODELING'] },
  BEHAVIORAL: { label: '행동·경험', types: ['BEHAVIORAL', 'RESUME_BASED'] },
} as const;
```

**주의**: 위 그룹핑은 제안이며 Plan 04 HTML 프로토타입 단계에서 `designer` 에이전트가 최종 결정. 확정 후 이 상수로 이관.

목록을 가져온 뒤 `item.interviewType`을 그룹 키로 매핑하고, 그룹별로 정렬해 섹션 배열을 구성한다. 사용자가 카테고리 드롭다운에서 특정 그룹 선택 시 해당 그룹만 렌더.

### 카드 + 확장 비교 뷰

**카드 (접힌 상태)**:
- 질문 텍스트 (2줄 ellipsis)
- 면접 제목 + 면접 날짜 (보조 텍스트)
- 해결 상태 배지 (연습 중: 중성 회색 / 해결됨: 녹색 또는 Plan 04에서 확정)
- 우상단: 해결 토글 버튼 + 삭제 버튼(ghost icon)

**확장 상태 (카드 클릭 시)**:
- 좌: "내 답변 (`transcript`)" (분석 전일 경우 안내)
- 우: "모범 답변 (`modelAnswer`)" (없으면 "모범 답변이 제공되지 않은 질문입니다")
- 하단: AI 코칭 요약 (`coachingImprovement`, 축약)
- 반응형: 태블릿 이하에서는 상하 1열로 변환

### 상태 변경 / 삭제

- 해결 토글: `PATCH /api/review-bookmarks/{id}/status` → optimistic update → 실패 시 롤백
- 삭제: `DELETE /api/review-bookmarks/{id}` → **확인 모달 노출** 후 삭제
  - **v1 범위에서 "되돌리기" 토스트는 제외**한다. 이유: hard delete 후 복구하려면 POST + PATCH로 재생성해야 하는데 `created_at`이 갱신되고 원래 `bookmarkId`를 복구할 수 없어 상태 일관성이 깨진다. 대신 삭제 전 확인 모달로 실수를 막는다. v2에서 soft delete 도입 시 재검토.

### 사이드바 진입점

- 현재 전역 사이드바 컴포넌트에 "복습 북마크" 메뉴 항목 추가
- 아이콘: `ListChecks` (피드백 페이지 토글 후 아이콘과 통일 → 시각적 연결)
- 위치: "대시보드" 아래 또는 "면접 피드백" 근처 — 실제 구조는 구현 시 확인
- 활성 라우트 표시 일관

### 빈 상태

- 아이콘 + 한 줄 안내 + 서브 CTA
- 문구(초안): "아직 담긴 답변이 없어요. 면접 피드백에서 `ListPlus` 버튼을 눌러 담아보세요."
- Plan 04에서 최종 문구 확정

## 담당 에이전트

- Implement: `frontend-developer` — 페이지 + 6개 컴포넌트 + 라우팅 + 사이드바 통합
- Review: `designer` — Plan 04 프로토타입과의 시각적 일치성, 반응형 레이아웃
- Review: `code-reviewer` — TanStack Query 캐시 전략, optimistic update, 타입 안전성
- Review: `ui-ux-designer` — 접근성, 정보 위계, 빈 상태 UX

## 검증

- `npm run lint` + `npm run build` + `npm run test` 통과
- 수동 E2E:
  1. 사이드바에서 "복습 북마크" 클릭 시 `/review-list` 진입
  2. 상태 필터 3종 전환 정상 작동
  3. 카테고리 드롭다운 전환 시 섹션 필터링 정상
  4. 카드 클릭 시 비교 뷰 확장, 다시 클릭 시 접힘
  5. 해결 토글 optimistic 업데이트 + 서버 동기화 확인
  6. 삭제 시 확인 모달 노출 → 확인 클릭 시 즉시 제거, 취소 클릭 시 유지
  7. 북마크 0건일 때 빈 상태 정상 노출
  8. 모바일 360 / 태블릿 768 / 데스크톱 1280에서 레이아웃 정상
  9. 키보드 탐색으로 모든 액션 도달 가능
- `progress.md` 상태 업데이트 (Task 6 → Completed)
