# 에이전트 간 핸드오프 기록

> 에이전트가 다음 에이전트에게 작업을 넘길 때 기록합니다.
> 최신 항목이 맨 위에 옵니다.

---

## 핸드오프 템플릿

```
### [날짜] [From Agent] → [To Agent]
- **작업**: 무엇을 했는가
- **산출물**: 생성/수정한 파일 목록
- **다음 작업**: 인수받는 에이전트가 해야 할 일
- **주의사항**: 알아야 할 제약/이슈
- **상태**: ⏸️ 사용자 확인 대기 / ✅ 자동 진행
```

---

## 핸드오프 로그

### 2026-03-10 Frontend → QA/Backend
- **작업**: 면접 세션 UI 구현 (홈 리디자인 / 면접 설정 / 면접 대기 페이지 3개 + 공통 UI 컴포넌트 6개 + 도메인 컴포넌트 2개 + API 연동 훅)
- **산출물**:
  - `frontend/tailwind.config.js` — Pretendard 폰트 패밀리 추가
  - `frontend/src/index.css` — Pretendard CDN import 추가
  - `frontend/src/types/interview.ts` — Level, InterviewType, ApiResponse 등 타입 확장 + 라벨 매핑 상수
  - `frontend/src/lib/api-client.ts` — patch 메서드 추가
  - `frontend/src/components/ui/button.tsx` — 4 variant (primary, secondary, ghost, cta), fullWidth, loading 지원
  - `frontend/src/components/ui/text-input.tsx` — label, error, placeholder, disabled, maxLength, aria 지원
  - `frontend/src/components/ui/selection-card.tsx` — role="radio", aria-checked, 키보드 내비게이션
  - `frontend/src/components/ui/back-link.tsx` — 뒤로가기 링크
  - `frontend/src/components/ui/spinner.tsx` — animate-spin 로딩 인디케이터
  - `frontend/src/components/ui/skeleton.tsx` — animate-pulse 스켈레톤 블록
  - `frontend/src/components/interview/question-card.tsx` — 번호 뱃지 + 카테고리 태그 + 질문 텍스트
  - `frontend/src/components/interview/question-card-skeleton.tsx` — 질문 카드 스켈레톤
  - `frontend/src/hooks/use-interviews.ts` — useCreateInterview, useInterview, useUpdateInterviewStatus (TanStack Query)
  - `frontend/src/pages/home-page.tsx` — 리디자인 (DevLens 제목 + 설명 + CTA)
  - `frontend/src/pages/interview-setup-page.tsx` — 직무 입력 + 레벨/유형 선택 + 질문 생성
  - `frontend/src/pages/interview-ready-page.tsx` — 질문 5개 표시 + 면접 시작 + 질문 재생성
  - `frontend/src/app.tsx` — /interview/setup, /interview/:id/ready 라우트 추가
- **다음 작업**:
  - Backend: POST /api/v1/interviews, GET /api/v1/interviews/{id}, PATCH /api/v1/interviews/{id}/status 엔드포인트가 api-contracts.md 스펙대로 동작하는지 확인
  - QA: 3개 페이지 플로우 테스트 (홈 → 설정 → 대기), 반응형 동작, 키보드 내비게이션, 에러 상태
  - Frontend: 면접 세션 페이지 (/interview/:id/session) 구현 예정
- **주의사항**:
  - api-client.ts의 에러 응답 파싱이 현재 단순 Error.message 기반이므로, 서버 에러 응답 body를 정확히 파싱하려면 api-client.ts의 에러 처리 개선 필요
  - /interview/:id/session 라우트는 아직 미구현 (면접 시작 버튼 클릭 시 해당 경로로 navigate)
  - interview.ts 타입의 id가 number로 변경됨 (API 스펙 기준 Long)
- **상태**: ⏸️ 사용자 확인 대기

---

### 2026-03-10 Designer → Frontend
- **작업**: 면접 세션 기능 UI 설계 (홈 / 면접 설정 / 면접 대기 페이지 3개)
- **산출물**:
  - `.omc/notepads/team/design-tokens.md` — 디자인 토큰 전체 정의 (컬러, 타이포, 간격, 컴포넌트 토큰)
  - `.omc/notepads/team/handoffs.md` — 이 문서 (ASCII 목업 + 컴포넌트 구조 + 반응형 + 접근성)
- **다음 작업**: 아래 설계 명세를 기반으로 3개 페이지 + 공통 UI 컴포넌트 구현
- **주의사항**:
  - `tailwind.config.js`에 Pretendard 폰트 및 커스텀 확장 추가 필요
  - `interview.ts` 타입에 `position`, `level`, `interviewType` 필드 추가 필요 (스펙 참조)
  - 기존 `home-page.tsx`를 리디자인 (아래 설계로 교체)
  - 선택 카드는 `<button role="radio">` 패턴 사용 (접근성)
- **상태**: ⏸️ 사용자 확인 대기

---

## 설계 명세: 면접 세션 UI

> 디자인 철학: "보이지 않는 속도" — 장식보다 정보, 화려함보다 즉각적 피드백
> 컬러 톤: slate 기반 모노톤 (전문적이고 차분한 면접 환경)
> 디자인 토큰: `.omc/notepads/team/design-tokens.md` 참조

---

### 1. 홈 페이지 `/`

#### ASCII 목업

```
Mobile (< 640px)                    Desktop (> 1024px)
┌─────────────────────┐             ┌──────────────────────────────────┐
│                     │             │                                  │
│                     │             │                                  │
│                     │             │                                  │
│      DevLens        │             │           DevLens                │
│                     │             │                                  │
│  AI 기반 개발자      │             │    AI 기반 개발자 모의면접 플랫폼  │
│  모의면접 플랫폼     │             │                                  │
│                     │             │    면접 녹화를 AI가 분석하여       │
│  면접 녹화를 AI가    │             │    타임스탬프 기반 피드백을        │
│  분석하여 타임스탬프  │             │    제공합니다.                    │
│  기반 피드백을       │             │                                  │
│  제공합니다.         │             │   ┌─────────────────────┐        │
│                     │             │   │   면접 시작하기  →   │        │
│  ┌─────────────┐    │             │   └─────────────────────┘        │
│  │면접 시작하기→│    │             │                                  │
│  └─────────────┘    │             │                                  │
│                     │             │                                  │
└─────────────────────┘             └──────────────────────────────────┘
```

#### 컴포넌트 구조

```
HomePage
├── <main> (전체 화면 중앙 정렬)
│   ├── <h1> "DevLens"                    -- text-4xl font-bold text-gray-900
│   ├── <p> 서비스 한 줄 소개              -- text-lg text-gray-600 mt-3
│   ├── <p> 상세 설명 (1-2줄)             -- text-base text-gray-500 mt-2
│   └── <Link to="/interview/setup">      -- CTA 버튼, mt-8
│       └── Button (variant="cta")
│           └── "면접 시작하기"
```

#### 레이아웃 상세
- `min-h-screen flex items-center justify-center bg-gray-50`
- 내부 컨테이너: `text-center px-4 sm:px-6`
- 제목: `text-3xl sm:text-4xl font-bold text-gray-900`
- 부제: `text-base sm:text-lg text-gray-600 mt-3 max-w-md mx-auto`
- CTA 버튼: `mt-8` (아래 Button 컴포넌트 토큰 참조)

---

### 2. 면접 설정 페이지 `/interview/setup`

#### ASCII 목업

```
Mobile (< 640px)
┌──────────────────────────┐
│  ← 뒤로                  │
│                          │
│  면접 설정                │
│  맞춤형 면접 질문을       │
│  생성합니다.              │
│                          │
│  ─── 직무 ───            │
│  ┌────────────────────┐  │
│  │ 예: 백엔드 개발자    │  │
│  └────────────────────┘  │
│                          │
│  ─── 레벨 ───            │
│  ┌────────┐ ┌────────┐  │
│  │ 주니어  │ │  미드   │  │
│  └────────┘ └────────┘  │
│  ┌────────┐              │
│  │ 시니어  │              │
│  └────────┘              │
│                          │
│  ─── 면접 유형 ───       │
│  ┌──────────────────┐    │
│  │ CS 기초           │    │
│  │ 자료구조, OS ...  │    │
│  └──────────────────┘    │
│  ┌──────────────────┐    │
│  │ 시스템 설계       │    │
│  │ 아키텍처, 확장... │    │
│  └──────────────────┘    │
│  ┌──────────────────┐    │
│  │ Behavioral       │    │
│  │ 경험, 협업 ...   │    │
│  └──────────────────┘    │
│                          │
│  ┌──────────────────┐    │
│  │  질문 생성하기     │    │
│  └──────────────────┘    │
│                          │
└──────────────────────────┘

Desktop (> 1024px)
┌────────────────────────────────────────┐
│  ← 뒤로                                │
│                                        │
│     ┌────────────────────────┐         │
│     │                        │         │
│     │  면접 설정              │         │
│     │  맞춤형 면접 질문을     │         │
│     │  생성합니다.            │         │
│     │                        │         │
│     │  직무                   │         │
│     │  ┌──────────────────┐  │         │
│     │  │ 예: 백엔드 개발자  │  │         │
│     │  └──────────────────┘  │         │
│     │                        │         │
│     │  레벨                   │         │
│     │  ┌──────┐┌──────┐┌──────┐       │
│     │  │주니어 ││ 미드  ││시니어 │       │
│     │  └──────┘└──────┘└──────┘       │
│     │                        │         │
│     │  면접 유형              │         │
│     │  ┌──────────────────┐  │         │
│     │  │ CS 기초           │  │         │
│     │  └──────────────────┘  │         │
│     │  ┌──────────────────┐  │         │
│     │  │ 시스템 설계       │  │         │
│     │  └──────────────────┘  │         │
│     │  ┌──────────────────┐  │         │
│     │  │ Behavioral       │  │         │
│     │  └──────────────────┘  │         │
│     │                        │         │
│     │  ┌──────────────────┐  │         │
│     │  │  질문 생성하기     │  │         │
│     │  └──────────────────┘  │         │
│     │                        │         │
│     └────────────────────────┘         │
│                                        │
└────────────────────────────────────────┘
```

#### 컴포넌트 구조

```
InterviewSetupPage
├── <header>
│   └── BackLink ("← 뒤로", Link to="/")
├── <main> (max-w-lg mx-auto)
│   ├── <section> 페이지 헤더
│   │   ├── <h1> "면접 설정"                    -- text-2xl font-semibold
│   │   └── <p> "맞춤형 면접 질문을 생성합니다." -- text-sm text-gray-500 mt-1
│   │
│   ├── <section> 직무 입력 (mt-8)
│   │   ├── <label> "직무"                      -- text-sm font-medium text-gray-700
│   │   ├── TextInput                           -- placeholder="예: 백엔드 개발자"
│   │   └── <p> 에러 메시지 (조건부)             -- text-sm text-red-600 mt-1
│   │
│   ├── <section> 레벨 선택 (mt-6)
│   │   ├── <label> "레벨"
│   │   └── <div role="radiogroup"> (grid grid-cols-2 sm:grid-cols-3 gap-3)
│   │       ├── SelectionCard (value="JUNIOR", label="주니어", desc="0-3년차")
│   │       ├── SelectionCard (value="MID", label="미드", desc="3-7년차")
│   │       └── SelectionCard (value="SENIOR", label="시니어", desc="7년차 이상")
│   │
│   ├── <section> 면접 유형 선택 (mt-6)
│   │   ├── <label> "면접 유형"
│   │   └── <div role="radiogroup"> (flex flex-col gap-3)
│   │       ├── SelectionCard (value="CS", label="CS 기초",
│   │       │     desc="자료구조, 알고리즘, OS, 네트워크, DB")
│   │       ├── SelectionCard (value="SYSTEM_DESIGN", label="시스템 설계",
│   │       │     desc="아키텍처, 스케일링, 트레이드오프")
│   │       └── SelectionCard (value="BEHAVIORAL", label="Behavioral",
│   │             desc="경험, 협업, 문제 해결 (STAR)")
│   │
│   └── <div> 제출 영역 (mt-8 mb-8)
│       └── Button (variant="primary", fullWidth, disabled={!isValid || isLoading})
│           ├── [기본] "질문 생성하기"
│           └── [로딩] Spinner + "질문 생성 중..."
```

#### 상태 관리

```
로컬 상태 (useState 또는 Zustand useSessionSetupStore):
- position: string          (직무 입력값)
- level: Level | null       (JUNIOR | MID | SENIOR)
- interviewType: Type | null (CS | SYSTEM_DESIGN | BEHAVIORAL)

유효성 검사:
- isValid = position.trim().length > 0 && level !== null && interviewType !== null
- position 최대 100자 (maxLength 속성)

서버 상태 (TanStack Query):
- useCreateInterview: useMutation
  - onSuccess: navigate(`/interview/${data.id}/ready`)
  - onError: 에러 토스트 또는 인라인 에러 메시지
```

#### 로딩 UX
- "질문 생성하기" 버튼 클릭 시:
  - 버튼 텍스트 → "질문 생성 중..." + 좌측 스피너
  - 버튼 disabled 상태
  - 폼 입력 필드 disabled (변경 방지)
  - 예상 소요 시간: 3-10초 (Claude API)

#### 에러 UX
- 400 (Validation): 해당 필드 아래 인라인 에러 메시지
- 502/504 (Claude API 실패): 버튼 아래 에러 메시지 + "다시 시도" 가능 상태로 복원

---

### 3. 면접 대기 페이지 `/interview/{id}/ready`

#### ASCII 목업

```
Mobile (< 640px)
┌──────────────────────────┐
│  ← 설정으로 돌아가기      │
│                          │
│  면접 준비 완료            │
│  백엔드 개발자 · 주니어    │
│  · CS 기초               │
│                          │
│  ┌──────────────────┐    │
│  │ (1)  자료구조      │    │
│  │                    │    │
│  │ HashMap과 TreeMap  │    │
│  │ 의 차이점과 각각의  │    │
│  │ 시간 복잡도를...    │    │
│  └──────────────────┘    │
│                          │
│  ┌──────────────────┐    │
│  │ (2)  운영체제      │    │
│  │                    │    │
│  │ 프로세스와 스레드   │    │
│  │ 의 차이점을 설명... │    │
│  └──────────────────┘    │
│                          │
│  ┌──────────────────┐    │
│  │ (3)  네트워크      │    │
│  │ ...                │    │
│  └──────────────────┘    │
│                          │
│  ┌──────────────────┐    │
│  │ (4)  DB           │    │
│  │ ...                │    │
│  └──────────────────┘    │
│                          │
│  ┌──────────────────┐    │
│  │ (5)  알고리즘      │    │
│  │ ...                │    │
│  └──────────────────┘    │
│                          │
│  ┌──────────────────┐    │
│  │    면접 시작       │    │
│  └──────────────────┘    │
│                          │
│  ┌──────────────────┐    │
│  │  질문 다시 생성    │    │
│  └──────────────────┘    │
│                          │
└──────────────────────────┘

Desktop (> 1024px)
┌──────────────────────────────────────────┐
│  ← 설정으로 돌아가기                       │
│                                          │
│      ┌──────────────────────────┐        │
│      │                          │        │
│      │  면접 준비 완료            │        │
│      │  백엔드 개발자 · 주니어    │        │
│      │  · CS 기초               │        │
│      │                          │        │
│      │  ┌────────────────────┐  │        │
│      │  │(1) 자료구조         │  │        │
│      │  │ HashMap과 TreeMap.. │  │        │
│      │  └────────────────────┘  │        │
│      │  ┌────────────────────┐  │        │
│      │  │(2) 운영체제         │  │        │
│      │  │ 프로세스와 스레드... │  │        │
│      │  └────────────────────┘  │        │
│      │  ┌────────────────────┐  │        │
│      │  │(3) 네트워크         │  │        │
│      │  │ TCP와 UDP의...     │  │        │
│      │  └────────────────────┘  │        │
│      │  ┌────────────────────┐  │        │
│      │  │(4) DB              │  │        │
│      │  │ 인덱스의 동작...    │  │        │
│      │  └────────────────────┘  │        │
│      │  ┌────────────────────┐  │        │
│      │  │(5) 알고리즘         │  │        │
│      │  │ 시간 복잡도와...    │  │        │
│      │  └────────────────────┘  │        │
│      │                          │        │
│      │  ┌────────────────────┐  │        │
│      │  │     면접 시작        │  │        │
│      │  └────────────────────┘  │        │
│      │     질문 다시 생성        │        │
│      │                          │        │
│      └──────────────────────────┘        │
│                                          │
└──────────────────────────────────────────┘
```

#### 컴포넌트 구조

```
InterviewReadyPage
├── <header>
│   └── BackLink ("← 설정으로 돌아가기", Link to="/interview/setup")
├── <main> (max-w-2xl mx-auto)
│   ├── <section> 세션 요약 헤더
│   │   ├── <h1> "면접 준비 완료"              -- text-2xl font-semibold text-gray-900
│   │   └── <p> "{position} · {level} · {type}" -- text-sm text-gray-500 mt-1
│   │
│   ├── <section> 질문 목록 (mt-6)
│   │   └── <ol> (flex flex-col gap-4)
│   │       └── QuestionCard (x5)
│   │           ├── <div> 헤더 행
│   │           │   ├── NumberBadge (1~5)      -- 원형 배경 숫자
│   │           │   └── CategoryBadge          -- 카테고리 태그
│   │           └── <p> 질문 텍스트             -- text-base leading-relaxed mt-3
│   │
│   ├── <div> 액션 영역 (mt-8)
│   │   ├── Button (variant="cta", fullWidth)
│   │   │   └── "면접 시작"
│   │   └── Button (variant="ghost", fullWidth, mt-3)
│   │       └── "질문 다시 생성"
│   │
│   └── [로딩 상태] (useQuery loading)
│       └── QuestionCardSkeleton (x5)          -- animate-pulse 스켈레톤
```

#### 상태 관리

```
서버 상태 (TanStack Query):
- useInterview(id): useQuery → GET /api/v1/interviews/{id}
  - 질문 목록, 세션 정보 로드
  - staleTime: Infinity (같은 세션은 캐시 유지)

- useUpdateInterviewStatus: useMutation → PATCH /api/v1/interviews/{id}/status
  - "면접 시작" 클릭 시: { status: "IN_PROGRESS" }
  - onSuccess: navigate(`/interview/${id}/session`)

- useCreateInterview: useMutation (질문 다시 생성 시 재사용)
  - onSuccess: navigate(`/interview/${newId}/ready`)
```

#### 로딩 / 에러 UX
- **초기 로딩** (질문 목록 fetch): 스켈레톤 카드 5개 표시
- **질문 다시 생성**: 버튼에 스피너 + "생성 중..." + 기존 질문 카드 유지 (낙관적)
- **면접 시작 클릭**: 버튼에 스피너 + 즉시 반응
- **404 에러** (세션 없음): "세션을 찾을 수 없습니다" + 홈으로 돌아가기 링크
- **네트워크 에러**: 인라인 에러 메시지 + 재시도 버튼

---

### 공통 UI 컴포넌트 목록

Frontend 에이전트가 구현해야 할 재사용 컴포넌트:

```
frontend/src/components/ui/
├── button.tsx              -- Button (variant: primary | secondary | ghost | cta)
├── text-input.tsx          -- TextInput (label, error, placeholder, disabled)
├── selection-card.tsx      -- SelectionCard (선택 카드, role="radio")
├── back-link.tsx           -- BackLink (← 텍스트 + 링크)
├── spinner.tsx             -- Spinner (animate-spin)
└── skeleton.tsx            -- Skeleton (animate-pulse 블록)

frontend/src/components/interview/
├── question-card.tsx       -- QuestionCard (번호 뱃지 + 카테고리 + 질문)
└── question-card-skeleton.tsx -- QuestionCardSkeleton
```

---

### 반응형 동작 정의

| 요소 | Mobile (< 640px) | Tablet (640-1024px) | Desktop (> 1024px) |
|------|-------------------|---------------------|---------------------|
| **홈 - 제목** | `text-3xl` | `text-4xl` | `text-4xl` |
| **홈 - CTA** | `w-full` | `w-auto px-8` | `w-auto px-8` |
| **설정 - 폼 너비** | `w-full px-4` | `max-w-lg mx-auto px-6` | `max-w-lg mx-auto` |
| **설정 - 레벨 카드** | `grid-cols-2` (시니어만 아래 행) | `grid-cols-3` | `grid-cols-3` |
| **설정 - 유형 카드** | 세로 스택 `flex-col` | 세로 스택 `flex-col` | 세로 스택 `flex-col` |
| **대기 - 콘텐츠 너비** | `w-full px-4` | `max-w-2xl mx-auto px-6` | `max-w-2xl mx-auto` |
| **대기 - 질문 카드** | 풀 너비 | 풀 너비 | 풀 너비 (max-w-2xl 내) |
| **대기 - 버튼 영역** | 세로 스택, 풀 너비 | 세로 스택, 풀 너비 | 세로 스택, 풀 너비 |

---

### 접근성 (Accessibility) 노트

#### 시맨틱 HTML
- 페이지 제목: `<h1>` 1개만 (각 페이지당)
- 섹션 구분: `<section>` + 시각적으로 숨긴 `<h2>` (sr-only)
- 질문 목록: `<ol>` (순서 있는 목록)
- 네비게이션 링크: `<a>` 또는 `<Link>` (버튼 아님)

#### 키보드 내비게이션
- Tab 순서: 뒤로가기 → 직무 입력 → 레벨 카드들 → 유형 카드들 → 제출 버튼
- 레벨/유형 선택 카드: `role="radiogroup"` + `role="radio"` + `aria-checked`
  - 카드 그룹 내 화살표 키로 이동 (ArrowUp/Down, ArrowLeft/Right)
  - Enter/Space로 선택
- 버튼: Enter/Space로 활성화
- focus-visible 스타일: `ring-2 ring-slate-500 ring-offset-2`

#### ARIA
- 직무 입력: `aria-required="true"`, 에러 시 `aria-invalid="true"` + `aria-describedby="position-error"`
- 레벨 그룹: `role="radiogroup"` + `aria-label="레벨 선택"`
- 유형 그룹: `role="radiogroup"` + `aria-label="면접 유형 선택"`
- 로딩 버튼: `aria-disabled="true"` + `aria-busy="true"`
- 질문 카드 목록: `aria-label="면접 질문 목록"`
- 스켈레톤: `aria-hidden="true"` + 별도 `aria-live="polite"` 영역에 "질문을 불러오는 중입니다"

#### 색상 대비
- 본문 텍스트 `gray-900` on `gray-50` → 대비 약 15.4:1 (AA 통과)
- 보조 텍스트 `gray-600` on `white` → 대비 약 5.7:1 (AA 통과)
- placeholder `gray-400` on `white` → 대비 약 3.0:1 (placeholder는 WCAG 대상 외)
- 버튼 `white` on `slate-900` → 대비 약 15.3:1 (AA 통과)
- 에러 `red-600` on `white` → 대비 약 4.5:1 (AA 통과)

---

### tailwind.config.js 확장 사항

Frontend 에이전트가 적용해야 할 Tailwind 설정:

```js
// tailwind.config.js
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: [
          'Pretendard Variable', 'Pretendard',
          '-apple-system', 'BlinkMacSystemFont',
          'system-ui', 'sans-serif'
        ],
        mono: ['JetBrains Mono', 'Consolas', 'monospace'],
      },
    },
  },
  plugins: [],
}
```

index.css에 Pretendard 웹폰트 CDN 추가:
```css
@import url('https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css');
```

---

### 레벨/면접유형 표시 텍스트 매핑

| enum 값 | 한글 라벨 | 설명 텍스트 |
|----------|-----------|-------------|
| `JUNIOR` | 주니어 | 0-3년차 |
| `MID` | 미드 | 3-7년차 |
| `SENIOR` | 시니어 | 7년차 이상 |
| `CS` | CS 기초 | 자료구조, 알고리즘, OS, 네트워크, DB |
| `SYSTEM_DESIGN` | 시스템 설계 | 아키텍처, 스케일링, 트레이드오프 |
| `BEHAVIORAL` | Behavioral | 경험, 협업, 문제 해결 (STAR) |
