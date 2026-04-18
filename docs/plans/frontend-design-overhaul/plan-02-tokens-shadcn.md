# Plan 02: 디자인 토큰 시스템 + shadcn/ui 초기화

> 상태: Draft
> 작성일: 2026-04-17

## Why

CSS 변수 토큰 레이어 없이 shadcn을 도입하면 신규 컴포넌트가 기존 Tailwind 하드코딩값과 충돌한다. 토큰을 먼저 정의한 뒤 shadcn init을 하면 shadcn primitive가 자동으로 DESIGN.md 토큰을 상속한다. [blocking] — Phase 3 이후 모든 작업의 전제.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/index.css` | shadcn-aligned CSS 변수 (light/dark) 추가 |
| `frontend/tailwind.config.js` | `colors`를 CSS 변수 참조형으로 재편, Cal Sans `display` font 추가 |
| `frontend/index.html` | Cal Sans Google Fonts import |
| `frontend/components.json` | shadcn init (style: new-york, base: neutral, cssVariables: true) |
| `frontend/src/lib/utils.ts` | `cn` helper (clsx + tailwind-merge) |
| `frontend/package.json` | `clsx`, `tailwind-merge`, `class-variance-authority`, `tailwindcss-animate` 추가 |

## 상세

### 1. CSS 변수 (`index.css`)

shadcn 규칙 준수. HSL 값으로 정의.

```css
@layer base {
  :root {
    /* Surface */
    --background: 0 0% 100%;              /* #ffffff */
    --foreground: 0 0% 14%;               /* #242424 Charcoal */

    /* Card / popover */
    --card: 0 0% 100%;
    --card-foreground: 0 0% 14%;
    --popover: 0 0% 100%;
    --popover-foreground: 0 0% 14%;

    /* Primary — DESIGN.md는 Charcoal을 primary로 */
    --primary: 0 0% 14%;
    --primary-foreground: 0 0% 100%;

    /* Secondary / muted / accent */
    --secondary: 0 0% 96%;
    --secondary-foreground: 0 0% 14%;
    --muted: 0 0% 96%;
    --muted-foreground: 0 0% 54%;          /* #898989 */
    --accent: 0 0% 96%;
    --accent-foreground: 0 0% 14%;

    /* Link only (DESIGN.md의 유일한 blue) */
    --link: 204 100% 50%;                  /* #0099ff */

    /* Feedback */
    --destructive: 0 84% 60%;
    --destructive-foreground: 0 0% 100%;

    /* Border / input / ring */
    --border: 0 0% 90%;
    --input: 0 0% 90%;
    --ring: 217 91% 60%;                   /* focus ring */

    /* Radius */
    --radius: 0.5rem;

    /* Legacy — Phase 3~5에서 사용처 제거 후 삭제 */
    --violet-legacy: 239 84% 67%;          /* #6366F1 */
  }

  .dark {
    --background: 0 0% 4%;                 /* #0a0a0a */
    --foreground: 0 0% 96%;                /* #f5f5f5 — pure white 금지 */
    --card: 0 0% 7%;
    --card-foreground: 0 0% 96%;
    --popover: 0 0% 7%;
    --popover-foreground: 0 0% 96%;
    --primary: 0 0% 96%;
    --primary-foreground: 0 0% 7%;
    --secondary: 0 0% 12%;
    --secondary-foreground: 0 0% 96%;
    --muted: 0 0% 12%;
    --muted-foreground: 0 0% 64%;
    --accent: 0 0% 12%;
    --accent-foreground: 0 0% 96%;
    --link: 204 100% 60%;
    --destructive: 0 70% 50%;
    --destructive-foreground: 0 0% 96%;
    --border: 0 0% 16%;
    --input: 0 0% 16%;
    --ring: 217 91% 60%;
  }
}
```

### 2. Tailwind config 재편

- `colors`를 `"hsl(var(--primary))"` 참조형으로 전환
- 기존 `accent.DEFAULT: #6366F1`은 제거하지 않고 `violet-legacy: "hsl(var(--violet-legacy))"`로 이름만 바꿔 임시 보존 (Phase 3~5에서 사용처 제거 후 최종 삭제)
- `fontFamily`에 `display: ['Cal Sans', 'Pretendard Variable', ...]` 추가
- `borderRadius`: `lg: var(--radius)`, `md: calc(var(--radius) - 2px)`, `sm: calc(var(--radius) - 4px)`
- 기존 `card: 20px`, `button: 24px`, `badge: 999px`는 당분간 유지 → Phase 3d 카드 작업에서 정리

### 3. Cal Sans 로드

`index.html` `<head>`:

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Cal+Sans&display=swap" rel="stylesheet">
```

### 4. shadcn init (파괴적 작업 — 롤백 절차 포함)

**init 전 백업 (필수)**

```bash
cd frontend
cp src/index.css src/index.css.bak
cp tailwind.config.js tailwind.config.js.bak
git status   # 작업 디렉토리 clean 상태 확인
```

**init 실행**

```bash
npx shadcn@latest init
# style: new-york
# base color: neutral
# CSS variables: yes
# tailwind.config.js 경로 확인
# components.json 생성
```

**init 후 diff 게이트 (사용자 승인 필수)**

```bash
diff src/index.css.bak src/index.css
diff tailwind.config.js.bak tailwind.config.js
```

- 본 Plan 1·2절에서 정의한 CSS 변수/Tailwind 확장이 유지되는지 확인
- shadcn 기본값이 덮어썼다면 본 Plan 값으로 **병합 복원**
- 사용자에게 diff 결과를 보고하고 승인 받기 전까지 다음 단계 진입 금지

**롤백 절차 (init 결과가 수용 불가한 경우)**

```bash
mv src/index.css.bak src/index.css
mv tailwind.config.js.bak tailwind.config.js
rm -f components.json
git checkout -- package.json package-lock.json  # shadcn이 추가한 deps 제거
```

- 롤백 후 원인 분석 → 사용자와 재논의 → 재시도

**백업 파일 정리**: 승인 완료 후 `.bak` 파일 삭제.

### 5. `cn` helper

```ts
// frontend/src/lib/utils.ts
import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
```

## 담당 에이전트

- Implement: `frontend` — CSS 변수, tailwind config, shadcn init
- Review: `designer` — 토큰 네이밍, light/dark 대칭성, DESIGN.md 값 일치 여부

## 검증

- `npm run dev` 실행 → 5 페이지 회귀 없음 (홈, 대시보드, interview-setup, interview-ready, interview-feedback)
- `npm run lint` green
- `npm run build` green
- `npm run test` green
- `components.json` 생성됨
- Cal Sans 네트워크 로드 확인 (DevTools Network)
- 기존 `accent-*` 클래스 사용처가 `violet-legacy`로만 제대로 이전됨
- **기존 시각적 스타일 변경 최소** — 이 Plan은 토큰만 준비, 실제 교체는 Phase 3부터
- `progress.md` Task 2 → Completed

## 체크포인트

변경 파일 목록 + 추가 토큰 리스트 보고 → 사용자 리뷰 후 Phase 3a 진입 승인.
