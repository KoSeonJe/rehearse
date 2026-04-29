# Frontend Agent Instructions

Use this file for all work under `frontend/`. Also read the root `AGENTS.md`.

## Required Reading

Before editing frontend code, consult:

- `frontend/CONVENTIONS.md` for naming, directory structure, component patterns, and state patterns.
- `frontend/CODING_GUIDE.md` for clean code, hook design, performance, and accessibility guidance.
- `frontend/CLAUDE.md` for additional historical project context if needed.
- `DESIGN.md` for the Cal.com-inspired monochrome design system and design tokens.
- `.claude/rules/frontend-design-rules.md` for visual quality checks and disallowed AI-looking patterns.
- `.claude/rules/testing_rule.md` for shared test principles.

## Stack

- React 18
- TypeScript 5+ in strict mode
- Vite
- Tailwind CSS
- shadcn/ui for base primitives
- Aceternity UI only as restrained decorative accents
- Zustand for global state
- TanStack Query for server state
- MediaRecorder and Web Speech API for browser recording/speech features

## Core Rules

- Do not use `any`. Use `unknown` with type guards when the type is genuinely uncertain.
- Do not commit `console.log` or debugging leftovers.
- Never call Claude or other LLM APIs directly from frontend code. Route AI behavior through backend APIs.
- Prefer shadcn/ui primitives for base UI controls.
- Use Aceternity UI sparingly, at most one or two accent components on a page.
- Comments should be rare and explain non-obvious why.
- Keep state ownership clear: server state in TanStack Query, durable global UI/app state in Zustand, local state in components/hooks.
- Follow accessibility guidance in `frontend/CODING_GUIDE.md`.

## Spec Requirement

- Before changing `frontend/src/`, check `.omc/plans/` or `docs/plans/` for a relevant spec.
- If the change is a new feature or meaningful behavior change, create or update the spec first.
- Include the why, goal, evidence, and trade-offs for non-trivial decisions.

## Design Workflow

For UI work:

1. Read `DESIGN.md` tokens and principles.
2. Compose the UI from shadcn primitives first.
3. Add Aceternity only when it creates a specific, restrained visual point.
4. Check `.claude/rules/frontend-design-rules.md` before finishing.

Current brand point color:

- Light: `#0F766E`
- Dark: `#2dd4bf`
- Use through project tokens such as `bg-brand`, `text-brand`, `ring-brand`, `hover:bg-brand-hover`.
- Reserve brand color for primary CTA, links, focus ring, active nav, and selected step states.
- Do not mix brand color with signal colors such as record, warning, or success on the same element.

## Commands

```bash
npm run dev
npm run build
npm run lint
npm run test
npm run test:watch
```

## Testing

- Prefer behavior tests over implementation-detail assertions.
- Mock at boundaries; prefer `msw` for fetch/network behavior where practical.
- Do not snapshot non-deterministic LLM or timestamp output.
- Keep tests focused on user-visible behavior, state transitions, and API integration contracts.

## Frontend Conventions to Re-check Often

- Component boundaries and naming: `frontend/CONVENTIONS.md`.
- Hook extraction and state rules: `frontend/CODING_GUIDE.md`.
- Directory placement: `frontend/CONVENTIONS.md`.
- Accessibility and performance checks: `frontend/CODING_GUIDE.md`.
