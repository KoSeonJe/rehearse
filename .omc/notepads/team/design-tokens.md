# 디자인 시스템 토큰

> UI 일관성을 위한 디자인 토큰을 정의합니다.
> Designer 에이전트가 관리하며, Frontend 에이전트가 구현에 참조합니다.
> 최종 수정: 2026-03-10 (Designer)

---

## 컬러

### Primary
| 토큰 | Tailwind 클래스 | HEX | 용도 |
|------|-----------------|-----|------|
| `--color-primary` | `bg-slate-900` / `text-slate-900` | `#0f172a` | 주요 액션 버튼, 강조 텍스트 |
| `--color-primary-hover` | `bg-slate-800` | `#1e293b` | 버튼 호버 상태 |
| `--color-primary-active` | `bg-slate-950` | `#020617` | 버튼 active 상태 |
| `--color-primary-light` | `bg-slate-100` | `#f1f5f9` | 선택된 카드 배경, 하이라이트 |

### Neutral (배경/텍스트/보더)
| 토큰 | Tailwind 클래스 | HEX | 용도 |
|------|-----------------|-----|------|
| `--color-bg-page` | `bg-gray-50` | `#f9fafb` | 페이지 배경 |
| `--color-bg-card` | `bg-white` | `#ffffff` | 카드/패널 배경 |
| `--color-text-primary` | `text-gray-900` | `#111827` | 제목, 본문 텍스트 |
| `--color-text-secondary` | `text-gray-600` | `#4b5563` | 보조 텍스트, 설명 |
| `--color-text-tertiary` | `text-gray-400` | `#9ca3af` | placeholder, 비활성 텍스트 |
| `--color-border` | `border-gray-200` | `#e5e7eb` | 카드 보더, 구분선 |
| `--color-border-focus` | `ring-slate-500` | `#64748b` | focus ring |

### Semantic
| 토큰 | Tailwind 클래스 | HEX | 용도 |
|------|-----------------|-----|------|
| `--color-success` | `text-emerald-600` / `bg-emerald-50` | `#059669` / `#ecfdf5` | 성공 상태 |
| `--color-warning` | `text-amber-600` / `bg-amber-50` | `#d97706` / `#fffbeb` | 경고 |
| `--color-error` | `text-red-600` / `bg-red-50` | `#dc2626` / `#fef2f2` | 에러, 유효성 검사 실패 |
| `--color-info` | `text-blue-600` / `bg-blue-50` | `#2563eb` / `#eff6ff` | 정보성 메시지 |

### 선택 카드 상태
| 상태 | 배경 | 보더 | 텍스트 |
|------|------|------|--------|
| 기본 | `bg-white` | `border-gray-200` | `text-gray-900` |
| 호버 | `bg-gray-50` | `border-gray-300` | `text-gray-900` |
| 선택됨 | `bg-slate-50` | `border-slate-900 ring-1 ring-slate-900` | `text-slate-900` |
| 비활성 | `bg-gray-50` | `border-gray-100` | `text-gray-400` |

---

## 타이포그래피

### 폰트 패밀리
| 토큰 | 값 | 용도 |
|------|-----|------|
| `--font-family` | `'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, sans-serif` | 기본 (한국어 최적화) |
| `--font-family-mono` | `'JetBrains Mono', Consolas, monospace` | 코드, 기술 카테고리 |

### 폰트 크기 / 행간
| 토큰 | Tailwind | 크기 / 행간 | 용도 |
|------|----------|-------------|------|
| `--text-xs` | `text-xs` | 12px / 16px | 뱃지, 캡션 |
| `--text-sm` | `text-sm` | 14px / 20px | 보조 텍스트, 카드 설명 |
| `--text-base` | `text-base` | 16px / 24px | 본문, 입력 필드 |
| `--text-lg` | `text-lg` | 18px / 28px | 카드 제목, 소제목 |
| `--text-xl` | `text-xl` | 20px / 28px | 섹션 제목 |
| `--text-2xl` | `text-2xl` | 24px / 32px | 페이지 제목 |
| `--text-3xl` | `text-3xl` | 30px / 36px | 히어로 제목 (모바일) |
| `--text-4xl` | `text-4xl` | 36px / 40px | 히어로 제목 (데스크톱) |

### 폰트 웨이트
| 토큰 | Tailwind | 용도 |
|------|----------|------|
| `--font-normal` | `font-normal` (400) | 본문 |
| `--font-medium` | `font-medium` (500) | 카드 제목, 라벨 |
| `--font-semibold` | `font-semibold` (600) | 페이지 제목, 버튼 |
| `--font-bold` | `font-bold` (700) | 히어로 제목 |

---

## 간격 (Spacing) - 8px 그리드 시스템

| 토큰 | Tailwind | 값 | 용도 |
|------|----------|-----|------|
| `--space-1` | `p-1` / `m-1` / `gap-1` | `4px` | 아이콘-텍스트 간격, 인라인 간격 |
| `--space-2` | `p-2` / `m-2` / `gap-2` | `8px` | 요소 내부 패딩 (최소) |
| `--space-3` | `p-3` / `m-3` / `gap-3` | `12px` | 소그룹 간 간격 |
| `--space-4` | `p-4` / `m-4` / `gap-4` | `16px` | 카드 내부 패딩, 폼 필드 간격 |
| `--space-5` | `p-5` / `m-5` / `gap-5` | `20px` | 카드 내부 패딩 (넓은) |
| `--space-6` | `p-6` / `m-6` / `gap-6` | `24px` | 섹션 간 간격 |
| `--space-8` | `p-8` / `m-8` / `gap-8` | `32px` | 페이지 레벨 간격 |
| `--space-10` | `p-10` / `m-10` | `40px` | 히어로 섹션 패딩 |
| `--space-12` | `p-12` / `m-12` | `48px` | 페이지 상하 여백 |
| `--space-16` | `p-16` / `m-16` | `64px` | 큰 섹션 분리 |

---

## 레이아웃

### 최대 너비
| 토큰 | Tailwind | 값 | 용도 |
|------|----------|-----|------|
| `--max-w-form` | `max-w-lg` | `512px` | 폼 컨테이너 (설정 페이지) |
| `--max-w-content` | `max-w-2xl` | `672px` | 콘텐츠 영역 (질문 목록) |
| `--max-w-page` | `max-w-4xl` | `896px` | 페이지 최대 너비 |

### 컨테이너 패딩
| 화면 | 좌우 패딩 |
|------|-----------|
| 모바일 (< 640px) | `px-4` (16px) |
| 태블릿 (640-1024px) | `px-6` (24px) |
| 데스크톱 (> 1024px) | `px-8` (32px) 또는 `mx-auto` |

---

## 반응형 브레이크포인트

| 토큰 | 값 | Tailwind 접두사 | 용도 |
|------|-----|-----------------|------|
| `--breakpoint-sm` | `640px` | `sm:` | 모바일 -> 소형 태블릿 |
| `--breakpoint-md` | `768px` | `md:` | 태블릿 |
| `--breakpoint-lg` | `1024px` | `lg:` | 데스크톱 |
| `--breakpoint-xl` | `1280px` | `xl:` | 와이드 데스크톱 |

---

## 그림자 / 보더 반경

| 토큰 | Tailwind | 용도 |
|------|----------|------|
| `--shadow-sm` | `shadow-sm` | 카드 기본 |
| `--shadow-md` | `shadow-md` | 카드 호버, 드롭다운 |
| `--rounded-md` | `rounded-md` | 버튼, 입력 필드 (6px) |
| `--rounded-lg` | `rounded-lg` | 카드 (8px) |
| `--rounded-xl` | `rounded-xl` | 큰 카드, 모달 (12px) |

---

## 컴포넌트 토큰

### 버튼

| 변형 | 배경 | 텍스트 | 호버 | 비활성 | 패딩 | 반경 |
|------|------|--------|------|--------|------|------|
| **Primary** | `bg-slate-900` | `text-white` | `bg-slate-800` | `bg-gray-300 text-gray-500 cursor-not-allowed` | `px-6 py-3` | `rounded-md` |
| **Secondary** | `bg-white border border-gray-300` | `text-gray-700` | `bg-gray-50` | `bg-gray-50 text-gray-400 border-gray-200` | `px-6 py-3` | `rounded-md` |
| **Ghost** | `bg-transparent` | `text-gray-600` | `bg-gray-100` | `text-gray-400` | `px-4 py-2` | `rounded-md` |
| **CTA (큰)** | `bg-slate-900` | `text-white` | `bg-slate-800` | 동일 | `px-8 py-4 text-lg` | `rounded-lg` |

**공통 속성:**
- `font-medium`
- `transition-colors duration-150`
- `focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-500 focus-visible:ring-offset-2`

### 입력 필드 (Input)

| 속성 | Tailwind |
|------|----------|
| 기본 | `w-full px-4 py-3 text-base border border-gray-200 rounded-md bg-white` |
| placeholder | `placeholder:text-gray-400` |
| focus | `focus:border-slate-500 focus:ring-1 focus:ring-slate-500 focus:outline-none` |
| 에러 | `border-red-500 focus:border-red-500 focus:ring-red-500` |
| 에러 메시지 | `text-sm text-red-600 mt-1` |

### 선택 카드 (SelectionCard)

| 속성 | Tailwind |
|------|----------|
| 컨테이너 | `p-4 border rounded-lg cursor-pointer transition-all duration-150` |
| 기본 | `border-gray-200 bg-white hover:border-gray-300 hover:bg-gray-50` |
| 선택됨 | `border-slate-900 bg-slate-50 ring-1 ring-slate-900` |
| 제목 | `text-base font-medium text-gray-900` |
| 설명 | `text-sm text-gray-500 mt-1` |
| focus | `focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-500 focus-visible:ring-offset-2` |

### 질문 카드 (QuestionCard)

| 속성 | Tailwind |
|------|----------|
| 컨테이너 | `p-5 bg-white border border-gray-200 rounded-lg` |
| 번호 | `inline-flex items-center justify-center w-7 h-7 rounded-full bg-slate-900 text-white text-sm font-medium` |
| 카테고리 | `text-xs font-medium text-slate-600 bg-slate-100 px-2 py-0.5 rounded` |
| 질문 텍스트 | `text-base text-gray-900 leading-relaxed mt-3` |

### 로딩 상태

| 속성 | Tailwind / 설명 |
|------|----------|
| 스피너 | `animate-spin h-5 w-5 border-2 border-white border-t-transparent rounded-full` |
| 로딩 오버레이 | 버튼 내부 스피너 + 텍스트 변경 ("질문 생성 중...") |
| 스켈레톤 | `animate-pulse bg-gray-200 rounded` |

---

## 트랜지션 / 애니메이션

| 토큰 | Tailwind | 용도 |
|------|----------|------|
| `--transition-fast` | `transition-colors duration-150` | 버튼 호버, 카드 선택 |
| `--transition-normal` | `transition-all duration-200` | 카드 상태 변경 |
| `--transition-slow` | `transition-all duration-300` | 페이지 전환 요소 |
