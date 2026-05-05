# Frontend Agent Instructions

`frontend/` 하위 작업 진입점. 루트 `AGENTS.md` 도 함께 적용.

> **필독**: `frontend/.claude/rules/` 하위 모든 문서를 작업 전 Read.
> 현재 = `conventions.md` / `architecture.md` / `testing.md`. 추가 룰 등장 시 자동 포함.

## Rules

@.claude/rules/conventions.md
@.claude/rules/architecture.md
@.claude/rules/testing.md

## 디자인 작업 한정 참조

`frontend/DESIGN.md` = 디자인 토큰 / 모노크롬 시스템 / 비주얼 원칙 단일 소스.

- **로드 시점**: UI / 시각 컴포넌트 / 토큰 / 컬러 / 타이포 / 스페이싱 / a11y 시각 영역 변경 시.
- **로드 생략**: 비즈니스 로직 / API / 훅 / 상태 / 테스트 / 빌드 / 인프라 작업.
- 토큰 분량 큼 (~440줄) — 매 작업 자동 로드 X. 디자인 작업 진입 시 명시적 Read.

## 기술 스택

- React 18
- TypeScript 5+ (strict mode)
- Vite
- Tailwind CSS
- shadcn/ui (base primitives)
- Aceternity UI (장식 포인트 한정)
- Zustand (글로벌 상태)
- TanStack Query (서버 상태)
- MediaRecorder + Web Speech API (녹화 / STT)
- Vitest (jsdom) + React Testing Library + user-event
- MSW (네트워크 Mock)

## 작업 진입 순서

1. 본 `frontend/AGENTS.md`
2. `frontend/.claude/rules/conventions.md` — 가독성 / 예측가능성 / 응집도 / 결합도 + 네이밍 / 상태 / TS / Tailwind / a11y (`@import` 자동 로드)
3. `frontend/.claude/rules/architecture.md` — 진입점 / 라우팅 / API / 인증 / 녹화 흐름 / 환경 / 시스템 경계 (`@import` 자동 로드)
4. `frontend/DESIGN.md` — **디자인 작업 시에만** (위 "디자인 작업 한정 참조" 참조)
5. 루트 `AGENTS.md` — `@import` 룰 (commit / comments / security / plan-mode)

## 빠른 명령

```bash
npm run dev          # Vite dev server (port 5173, /api → backend:8080 proxy)
npm run build        # tsc -b && vite build
npm run lint         # ESLint
npm run test         # vitest run
npm run test:watch   # vitest watch
```

## 핵심 룰 (프로젝트 결정)

- **LLM API 직접 호출 금지** — Claude / OpenAI 등 모든 AI 호출은 backend 경유.
- **shadcn primitive 우선 + Aceternity 절제** — 기본 UI = shadcn (`/shadcn` 스킬). Aceternity = 페이지당 최대 1–2개 포인트.
- **Spec 없는 수정 금지** — `frontend/src/` 변경 전 `docs/plans/{N}-{slug}/tech-spec.md` 존재 확인. 부재 시 설계부터. 자세한 워크플로우는 루트 `AGENTS.md` "Spec-Driven Work" + `docs/plans/AGENTS.md` 참조.

> 코드 수준 컨벤션 = `conventions.md`. 런타임 / 데이터 흐름 / 시스템 경계 = `architecture.md`.

## 도메인 맵

각 도메인 진입 페이지 + 핵심 훅 + 스토어 매트릭스. 진입점 / 라우트 가드 / API 흐름 = `architecture.md`.

| 도메인 | 진입 페이지 | 주요 훅 | 스토어 |
|--------|-----------|---------|-------|
| 인증 | (전역) | `use-auth`, `use-auth-interceptor`, `use-logout`, `use-post-login-redirect` | `auth-store` |
| 인터뷰 셋업 | `interview-setup-page`, `interview-ready-page` | `use-interview-setup`, `use-device-test`, `use-question-sets` | `interview-store` |
| 인터뷰 진행 | `interview-page` | `use-interview-session`, `use-answer-flow`, `use-interview-greeting`, `use-media-recorder`, `use-media-stream`, `use-audio-capture`, `use-tts`, `use-interview-event-recorder`, `use-interview-exit-guard`, `use-cross-tab-sync` | `interview-store` |
| 분석 / 피드백 | `interview-analysis-page`, `interview-feedback-page` | `use-feedback-sync`, `use-session-feedback`, `use-service-feedback` | — |
| 리뷰 / 북마크 | `review-list-page` | `use-review-bookmarks` | — |
| 대시보드 | `dashboard-page` | `use-interviews` | — |
| 업로드 | (전역) | `use-s3-upload` | — |

