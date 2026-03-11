# DevLens — AI Mock Interview Platform for Developers

## Overview

- Timestamp-synced video feedback for AI developer mock interviews
- Target: developers preparing for jobs/transitions, bootcamp graduates
- Plan: `docs/PLAN.md` (all features within MVP DO scope)
- Workflows: `docs/workflows.md`
- Tech stack details: `docs/tech-stack.md`
- Team workflow: `docs/team-workflow.md`
- Claude Code 효율 가이드: `docs/claude-code-efficiency-guide.md`

---

## Tech Stack (Summary)

- **Frontend**: React 18 + TypeScript 5+ / Tailwind CSS / Vite / Zustand / TanStack Query
- **Backend**: Java 21 + Spring Boot 3.x / Gradle (Kotlin DSL) / Spring Data JPA
- **Database**: MySQL 8.0 (prod) / H2 (dev)
- **AI**: Claude API (`claude-sonnet-4-20250514`) — backend only
- **Browser**: MediaPipe (face/pose), Web Audio API, MediaRecorder (WebM), Web Speech API

---

## Coding Conventions

### Frontend
- TypeScript strict mode, no `any`
- Functional components + arrow functions
- File names: kebab-case (`interview-player.tsx`)
- Component names: PascalCase (`InterviewPlayer`)
- Hooks: `use` prefix (`useMediaPipe`)
- Props interface defined inside component file
- No barrel exports (`index.ts`) — use direct imports
- Server state: TanStack Query / Client state: Zustand / Local state: `useState`/`useReducer`

### Backend
- `@RestController` with Controller → Service → Repository layering
- Unified error DTO format
- Claude API calls only from backend (never frontend)
- Jakarta Bean Validation

---

## Commit / PR Rules

- Commit messages: Korean, conventional commits (`feat:`, `fix:`, `refactor:`, etc.)
- PR title: `[Scope] type: short description` (in Korean)
  - Scope: `[FE]`, `[BE]`, `[FE/BE]`, omit for docs/chore
  - Type: `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`
  - e.g. `[FE/BE] feat: 면접 Setup 페이지 UX 리디자인`
- PR scope: per feature or per issue
- **Separate BE/FE PRs**: backend and frontend work must be in separate PRs
  - e.g. `[BE] feat: 면접 생성 API` → `[FE] feat: 면접 Setup 위저드 UI`
  - Merge BE PR first, then create FE PR
- Branches: `feat/{name}`, `fix/{name}`

---

## MVP Scope

**DO**: AI interviewer (resume-based Q&A + follow-ups), video recording, nonverbal analysis (MediaPipe), voice analysis (Web Audio), timestamp feedback UI, STT, AI feedback (Claude), summary report

**DON'T**: company-specific Q&A DB, interview history dashboard, peer review, mobile app, payments, coding test IDE

---

## Prohibited

- `any` type usage
- Committing `console.log` (remove after debugging)
- Direct Claude API calls from frontend (API key exposure)
- Implementing MVP DON'T features
- Unnecessary library additions (prefer browser-native APIs)
- Modifying `src/` without a spec document in `.omc/plans/` — always write/check spec first

---

## Decision Framework (Required)

Every decision (feature, tech, architecture, design) must answer:

1. **Why?** — What problem does this solve?
2. **Goal** — What specific outcome? How to measure success?
3. **Evidence** — What data or research supports this?
4. **Trade-offs** — What are we giving up? Alternatives considered?

Every `.omc/plans/` spec must include a **"Why"** section before implementation details.

---

## Spec-Driven Development (Required)

- All work must start with a spec document in `.omc/plans/`
  - **파일명**: `YYYY-MM-DD-<topic>.md` (예: `2026-03-11-latency-optimization.md`)
  - **New feature**: write spec (including "Why" section) → review → implement
  - **Modify existing**: check related spec → update → implement
  - **After completion**: update spec status to `Completed`

---

## Custom Sub-Agent Usage (Required)

- Actively use custom sub-agents defined in `.claude/agents/`
- **Prefer custom agents over built-in agents** (they are optimized for this project's context)
- Key agents:
  - **Architecture review**: `architect-reviewer` (Opus) — architectural consistency, SOLID, layering
  - **Code review**: `code-reviewer` (Opus) — security, performance, tech debt, comprehensive review
  - **FE implementation**: `frontend` / `frontend-developer` — logic, state management, components
  - **BE implementation**: `backend` / `backend-architect` — API, business logic, DB schema
  - **Others**: `designer`, `qa`, `devops`, `test-engineer`, `debugger`, etc.
- Run multiple agents **in parallel** for complex tasks to maximize efficiency

### Agent Assignment in Plan Mode (Required)

When writing plans, **each task must specify which agents to use**.

```
## Task N: {title}
- Implement: `{agent_name}` — {role}
- Review: `{agent_name}` — {verification focus}
```

Example:
```
## Task 1: Interview Creation API
- Implement: `backend` — API endpoints + service logic
- Review: `architect-reviewer` — layering, SOLID

## Task 2: Setup Wizard UI
- Implement: `frontend` — components + state management
- Review: `code-reviewer` — code quality, security
- Review: `designer` — UI/UX consistency
```

- A plan without agent assignments is considered **incomplete**
- Mark parallelizable tasks with `[parallel]` tag
