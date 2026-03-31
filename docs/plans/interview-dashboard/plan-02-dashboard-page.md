# Plan 02: 대시보드 페이지 UI

> 상태: Draft
> 작성일: 2026-03-31

## Why

로그인 사용자가 자신의 면접 이력을 한눈에 확인하고, 새 면접을 시작하거나 미완료 면접을 정리할 수 있는 메인 화면이 필요하다.

## 디자인 시스템 (기존 프로젝트 톤 준수)

**스타일:** Minimalism & Swiss Style — 화이트 배경, 충분한 여백, 기능 중심, 명확한 타이포 위계

### 디자인 토큰 (tailwind.config.js 기준)

| 토큰 | 값 | 용도 |
|------|-----|------|
| `bg-background` | `#FFFFFF` | 페이지 배경 |
| `bg-surface` | `#F8FAFC` | 카드/섹션 배경 |
| `border-border` | `#E2E8F0` | 구분선, 카드 테두리 |
| `text-text-primary` | `#0F172A` | 제목, 핵심 숫자 |
| `text-text-secondary` | `#475569` | 부제, 설명 |
| `text-text-tertiary` | `#94A3B8` | 날짜, 메타 정보 |
| `bg-accent` | `#6366F1` | CTA, 완료 배지, 강조 |
| `bg-accent-light` | `#EEF2FF` | 배지 배경, 호버 |
| `shadow-toss` | `0 8px 16px rgba(0,0,0,0.04)` | 카드 그림자 |
| `rounded-card` | `20px` | 카드 모서리 |
| `rounded-button` | `24px` | 버튼 모서리 |
| font-sans | Pretendard | 본문 |
| font-mono | JetBrains Mono | 숫자, 코드 |

### 핵심 디자인 원칙

1. **여백으로 구조를 만든다** — 섹션 간 `48px+`, 카드 내 `20-24px` 패딩
2. **숫자는 모노스페이스** — 통계 숫자에 `font-mono` + `text-3xl font-extrabold`
3. **상태는 색상 + 텍스트** — 색상만으로 구분하지 않음 (접근성)
4. **호버는 미묘하게** — `transition-all duration-200`, shadow 또는 border 변화만
5. **SVG 아이콘만 사용** — Lucide React (이모지 금지)
6. **cursor-pointer 필수** — 모든 클릭 가능한 요소에 적용
7. **모바일 퍼스트** — 1열 → md:2열 → lg:3열 (통계), 카드는 항상 1열

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/pages/dashboard-page.tsx` | 대시보드 페이지 (신규) |
| `frontend/src/components/dashboard/stats-cards.tsx` | 통계 카드 컴포넌트 (신규) |
| `frontend/src/components/dashboard/interview-card.tsx` | 면접 카드 컴포넌트 (신규) |
| `frontend/src/components/dashboard/interview-list.tsx` | 면접 목록 컴포넌트 (신규) |
| `frontend/src/components/dashboard/empty-state.tsx` | 빈 상태 컴포넌트 (신규) |
| `frontend/src/components/dashboard/delete-confirm-dialog.tsx` | 삭제 확인 다이얼로그 (신규) |
| `frontend/src/hooks/use-interviews.ts` | 목록/통계/삭제 API 훅 추가 |
| `frontend/src/types/interview.ts` | 목록/통계 응답 타입 추가 |

## 상세

### 대시보드 레이아웃

```
┌─────────────────────────────────────────────────┐
│ [Logo 리허설]                    [유저명] [로그아웃] │
│  sticky top-0, bg-white/80 backdrop-blur-md     │
├─────────────────────────────────────────────────┤
│                                                  │
│  max-w-3xl mx-auto px-5                         │
│                                                  │
│  ── 인사 섹션 ──                                  │
│  "OO님의 면접 기록"                                │
│  font-extrabold text-2xl tracking-tighter       │
│                                                  │
│  ── 통계 카드 (3열 그리드) ──                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐        │
│  │ 총 면접    │ │ 완료      │ │ 이번 주   │        │
│  │           │ │           │ │           │        │
│  │  10       │ │   7       │ │   2       │        │
│  │ font-mono │ │ font-mono │ │ font-mono │        │
│  │ text-3xl  │ │ text-3xl  │ │ text-3xl  │        │
│  │ extrabold │ │ extrabold │ │ extrabold │        │
│  │           │ │           │ │           │        │
│  │ bg-surface│ │ bg-surface│ │ bg-surface│        │
│  │ rounded-  │ │ rounded-  │ │ rounded-  │        │
│  │ card      │ │ card      │ │ card      │        │
│  └──────────┘ └──────────┘ └──────────┘        │
│  gap-4, 모바일: 1열 → md: 3열                     │
│                                                  │
│  ── CTA ──                                       │
│  ┌────────────────────────────────────────┐      │
│  │  + 새 면접 시작하기                       │      │
│  │  bg-accent text-white rounded-button   │      │
│  │  h-14 w-full font-bold                 │      │
│  │  hover:bg-accent-hover active:scale-95 │      │
│  └────────────────────────────────────────┘      │
│                                                  │
│  ── 면접 목록 ──                                  │
│  "면접 기록" text-lg font-bold                    │
│                                                  │
│  ┌────────────────────────────────────────┐      │
│  │  백엔드 · Spring Boot 백엔드             │      │
│  │  text-text-primary font-bold           │      │
│  │                                         │      │
│  │  ┌─────┐ ┌────────┐ ┌─────┐           │      │
│  │  │ CS  │ │PROJECT │ │30분 │  12문항    │      │
│  │  └─────┘ └────────┘ └─────┘           │      │
│  │  text-xs rounded-badge bg-accent-light │      │
│  │                                         │      │
│  │  2026-03-30                   [완료]    │      │
│  │  text-text-tertiary    bg-accent/text-white   │
│  │                                         │      │
│  │  bg-surface rounded-card shadow-toss   │      │
│  │  p-5 cursor-pointer                    │      │
│  │  hover:shadow-toss-lg transition-all   │      │
│  └────────────────────────────────────────┘      │
│  gap-3                                           │
│  ┌────────────────────────────────────────┐      │
│  │  프론트엔드 · React 프론트엔드           │      │
│  │                                         │      │
│  │  ┌────┐ ┌─────┐                        │      │
│  │  │ CS │ │20분 │  8문항                 │      │
│  │  └────┘ └─────┘                        │      │
│  │                                         │      │
│  │  2026-03-28   [준비됨]    [삭제 아이콘]   │      │
│  │               blue badge  Trash2 icon  │      │
│  │               text-xs     text-text-   │      │
│  │                           tertiary     │      │
│  │                           hover:text-  │      │
│  │                           error        │      │
│  └────────────────────────────────────────┘      │
│                                                  │
└─────────────────────────────────────────────────┘
```

### 통계 카드 세부

```tsx
// stats-cards.tsx 구조
<div className="grid grid-cols-1 md:grid-cols-3 gap-4">
  <StatCard label="총 면접" value={stats.totalCount} />
  <StatCard label="완료" value={stats.completedCount} />
  <StatCard label="이번 주" value={stats.thisWeekCount} />
</div>

// StatCard
<div className="rounded-card bg-surface p-5">
  <p className="text-xs font-semibold text-text-tertiary tracking-wide uppercase">{label}</p>
  <p className="mt-2 font-mono text-3xl font-extrabold text-text-primary">{value}</p>
</div>
```

### 면접 카드 세부

```tsx
// interview-card.tsx 구조
<div className="rounded-card bg-surface p-5 shadow-toss cursor-pointer
                hover:shadow-toss-lg transition-all duration-200">
  {/* 1행: 포지션 */}
  <h3 className="font-bold text-text-primary">{positionLabel} · {positionDetail}</h3>

  {/* 2행: 태그 + 메타 */}
  <div className="mt-3 flex flex-wrap items-center gap-2">
    {interviewTypes.map(type => (
      <span className="rounded-badge bg-accent-light px-2.5 py-0.5 text-xs font-semibold text-accent">
        {type}
      </span>
    ))}
    <span className="text-xs text-text-tertiary">{durationMinutes}분</span>
    <span className="text-xs text-text-tertiary">{questionCount}문항</span>
  </div>

  {/* 3행: 날짜 + 상태 배지 + 삭제 */}
  <div className="mt-4 flex items-center justify-between">
    <span className="text-xs text-text-tertiary">{formattedDate}</span>
    <div className="flex items-center gap-2">
      <StatusBadge status={status} />
      {isDeletable && <DeleteButton onClick={onDelete} />}
    </div>
  </div>
</div>
```

### 상태 배지 디자인

| 상태 | 배경 | 텍스트 | 라벨 |
|------|------|--------|------|
| COMPLETED | `bg-accent text-white` | `text-xs font-bold` | 완료 |
| READY | `bg-blue-50 text-blue-600` | `text-xs font-bold` | 준비됨 |
| IN_PROGRESS | `bg-warning-light text-warning` | `text-xs font-bold` | 진행 중 |

### 삭제 버튼 디자인

- Lucide `Trash2` 아이콘 (16px)
- 기본: `text-text-tertiary`
- 호버: `text-error` + `transition-colors duration-200`
- 클릭 영역: `p-2` (44x44px 터치 타겟 확보)
- `e.stopPropagation()` — 카드 클릭과 분리

### 삭제 확인 다이얼로그

- 배경: `bg-black/40 backdrop-blur-sm` 오버레이
- 다이얼로그: `bg-white rounded-card p-6 shadow-toss-lg max-w-sm mx-auto`
- 제목: "면접을 삭제하시겠습니까?"
- 설명: "삭제된 면접은 복구할 수 없습니다."
- 버튼: [취소 (ghost)] [삭제 (bg-error text-white)]
- ESC / 배경 클릭으로 닫기

### 카드 클릭 동작

| 상태 | 클릭 | 삭제 |
|------|------|------|
| COMPLETED | → `/interview/{publicId}/feedback` | 불가 |
| READY | → `/interview/{id}/ready` (이어하기) | 가능 |
| IN_PROGRESS | 비활성 (opacity-60, 툴팁) | 가능 |

### 빈 상태

```tsx
<div className="py-20 text-center">
  <Character mood="happy" size={120} className="mx-auto mb-6" />
  <h2 className="text-xl font-extrabold text-text-primary">아직 면접 기록이 없어요</h2>
  <p className="mt-2 text-sm text-text-secondary">첫 모의 면접을 시작해보세요!</p>
  <button className="mt-8 h-14 w-full max-w-xs rounded-button bg-accent font-bold text-white
                      hover:bg-accent-hover active:scale-95 transition-all cursor-pointer">
    새 면접 시작하기
  </button>
</div>
```

### 로딩 상태

- 통계 카드: `bg-surface rounded-card p-5 animate-pulse` + `h-10 w-16 bg-border/50 rounded-lg`
- 면접 카드: 3개 스켈레톤 카드 (`bg-surface rounded-card p-5 animate-pulse`)
- 스피너 사용하지 않음 — 스켈레톤이 콘텐츠 레이아웃을 미리 잡아 CLS 방지

### 반응형 브레이크포인트

| 화면 | 통계 카드 | 카드 목록 | max-width |
|------|----------|----------|-----------|
| < 768px | 1열 | 1열 | 100% |
| >= 768px | 3열 | 1열 | `max-w-3xl` |

### 접근성 체크리스트

- [ ] 모든 클릭 요소에 `cursor-pointer`
- [ ] 삭제 버튼 `aria-label="면접 삭제"`
- [ ] 상태 배지는 색상 + 텍스트 (색상만 의존 X)
- [ ] 다이얼로그 열림 시 focus trap
- [ ] `prefers-reduced-motion` 시 `transition-all` 비활성화
- [ ] 터치 타겟 최소 44x44px

## 담당 에이전트

- Implement: `frontend` — 컴포넌트, API 훅, 상태 관리
- Review: `code-reviewer` — 코드 품질, 접근성
- Review: `designer` — UI/UX 일관성 (디자인 토큰 준수, 토스 스타일)

## 검증

- 통계 카드 데이터 정확성 + 로딩 스켈레톤
- 카드 클릭 시 올바른 라우팅 (상태별)
- 삭제 동작 + 확인 다이얼로그 + 목록 갱신
- 빈 상태 UI 확인
- 반응형 (375px / 768px / 1024px)
- 접근성 체크리스트 전항목 통과
- `progress.md` 상태 업데이트 (Task 2 → Completed)
