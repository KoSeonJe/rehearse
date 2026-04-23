# Frontend — Rehearse Web

> 이 파일은 `frontend/` 하위 파일 작업 시 자동 로드된다. 루트 `CLAUDE.md`(프로젝트 전체 맥락)와 함께 적용.

## Stack

- React 18 + TypeScript 5+ (strict mode) / Vite
- Tailwind CSS / shadcn/ui (base primitives) / Aceternity UI (장식 포인트용)
- 상태: Zustand (글로벌) + TanStack Query (서버 상태)
- 녹화: MediaRecorder (WebM), Web Speech API

## 작업 전 필독

- `frontend/CONVENTIONS.md` — 네이밍, 디렉토리 구조, 컴포넌트/상태 패턴
- `frontend/CODING_GUIDE.md` — 클린코드, 훅 설계, 성능, a11y
- `DESIGN.md` (루트) — Cal.com 기반 모노크롬 디자인 시스템, 색상/타이포/섀도/스페이싱 토큰
- `.claude/rules/frontend-design-rules.md` (루트) — AI 티 방지: 금지 색상/폰트/레이아웃 + self-check 체크리스트
- `.claude/rules/testing_rule.md` (루트) — 테스트 원칙

## 핵심 규칙

- **`any` 금지** — TypeScript strict 준수, 정말 필요하면 `unknown` + 타입 가드
- **`console.log` 커밋 금지** — 디버깅 후 제거
- **Claude/LLM API 직접 호출 금지** — 모든 AI 호출은 backend 경유 (API 키 노출 방지)
- **shadcn primitive 우선** — 기본 UI 요소는 shadcn, `/shadcn` 스킬로 추가/검색
- **Aceternity 절제** — 페이지당 최대 1–2개 포인트용으로만
- **주석 ZERO 기본** — WHY가 비자명할 때만
- **Spec 없는 수정 금지** — `frontend/src/` 변경 전 `.omc/plans/` 또는 `docs/plans/` spec 확인

## 디자인 작업 플로우

1. `DESIGN.md` 토큰/원칙 확인
2. shadcn primitive로 구성
3. 필요 시 Aceternity 1–2개로 포인트
4. `.claude/rules/frontend-design-rules.md` self-check 통과 후 완료

**Brand Point Color (2026-04-18 확정)**: Teal (`#0F766E` light / `#2dd4bf` dark). `bg-brand`, `text-brand`, `ring-brand`, `hover:bg-brand-hover`. Primary CTA / 링크 / focus ring / active nav / 선택된 step에만 사용. Signal(record/warning/success) 계열과 같은 요소 동시 배치 금지. Feedback 페이지의 editorial 장식은 `accent-editorial` 유지 (상세: `.claude/rules/frontend-design-rules.md`).

## 테스트 & 개발

```bash
npm run dev          # Vite dev server
npm run build        # tsc -b && vite build
npm run lint         # ESLint
npm run test         # vitest run
npm run test:watch   # vitest watch
```

테스트 원칙: 행위 테스트, 경계에서만 Mock (fetch는 `msw` 선호), Snapshot 중 비결정적 출력(LLM/timestamp) 금지. 상세는 `.claude/rules/testing_rule.md`.

## 에이전트 호출 시

- FE 구현: `frontend` / `frontend-developer`
- 디자인/UX: `designer`
- 디버깅: `debugger`
- 리뷰: `code-reviewer`, `architect-reviewer`

영역 파일 터치 시 이 `CLAUDE.md`와 참조 문서들이 자동 주입된다.
