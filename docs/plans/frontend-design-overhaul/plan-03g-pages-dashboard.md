# Plan 03g: 대시보드 / 리뷰 리스트 레이아웃 재구성

> 상태: Draft
> 작성일: 2026-04-17

## Why

대시보드는 재방문 사용자가 가장 많이 보는 화면. 카드 그리드 밀도, 빈 상태(empty state), 목록 가독성을 DESIGN.md 기준으로 재정렬.

## 대상 페이지

- `frontend/src/pages/dashboard/**`
- `frontend/src/pages/review-list/**`

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/pages/dashboard/**` | 레이아웃 그리드, 카드 밀도, empty state 재구성 |
| `frontend/src/pages/review-list/**` | 목록 밀도, 필터/정렬 UI 정렬 |
| `frontend/src/components/dashboard/**` | 서브 컴포넌트 조정 |

## 상세

### 대상 섹션

1. **헤더/타이틀 영역**: section heading typography 적용
2. **카드 그리드**: Phase 3d 결과(shadcn Card) 사용. 그리드 간격/컬럼 반응형
3. **Empty state**: 일러스트 금지(faceless 3D 금지 규칙), 텍스트 중심 + 모노크롬 아이콘
4. **필터/정렬**: shadcn `Select`/`Tabs` 활용 (필요 시 추가 설치)
5. **페이지네이션 / 무한 스크롤**: 기존 로직 유지

### 변경 범위

- 섹션 spacing, 카드 padding/gap, typography scale
- 색상 토큰 적용 (퍼플/하드코딩 제거)
- 접근성: 목록 `<ul>/<li>`, heading 계층

### 비변경

- 데이터 페칭(TanStack Query), 라우팅, 정렬/필터 상태 로직

## 담당 에이전트

- Implement: `frontend` + `designer`
- Review: `designer` — 카드 밀도, 반응형
- Review: `code-reviewer` — 회귀

## 검증

- `npm run lint/build/test` green
- before/after 스크린샷 (데스크톱 + 모바일)
- 빈 상태 / 에러 상태 / 로딩 상태 3개 모두 확인
- `progress.md` Task 3g → Completed

## 체크포인트

스크린샷 + 상태별 확인 보고 → 사용자 승인 후 Phase 3h 진입.
