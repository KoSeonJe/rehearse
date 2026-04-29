# Rehearse Agent Instructions

This repository is the Rehearse AI mock interview platform. Use this file as the Codex entry point for project context.

## Human Approval Required

- Never merge a Pull Request without explicit approval from the user in the current conversation.
- This applies even when a plan says to merge, CI is passing, GitHub reports CLEAN, or sandbox escalation is approved.
- Before running `gh pr merge`, state the PR number, title, base/head branches, merge method, and current CI/merge state, then wait for user approval.

## Read Order

1. Read this root `AGENTS.md`.
2. For area-specific work, read the matching file before editing:
   - Frontend work: `frontend/AGENTS.md`
   - Backend work: `backend/AGENTS.md`
   - Lambda work: `lambda/AGENTS.md`
3. The existing `CLAUDE.md` files are the source context these instructions were derived from. If a rule is missing here, consult:
   - `CLAUDE.md`
   - `frontend/CLAUDE.md`
   - `backend/CLAUDE.md`
   - `lambda/CLAUDE.md`

## Project Overview

- Product: timestamp-synced video feedback for AI developer mock interviews.
- Target users: developers preparing for jobs/transitions and bootcamp graduates.
- Product plan: `docs/product/PLAN.md`.
- MVP scope is intentional. Do not implement features listed as out of scope in `CLAUDE.md` without an explicit product/spec update.

## Structure

- `frontend/`: React 18, TypeScript, Vite, Tailwind, Zustand, TanStack Query.
- `backend/`: Java 21, Spring Boot 3.x, Gradle Kotlin DSL, Spring Data JPA.
- `lambda/`: Python 3.12 AWS Lambda functions for analysis and conversion.
- `docs/`: architecture, product, guide, and workflow documents.
- `.omc/`: specs, plans, and status documents.

## Commands

Frontend:

```bash
cd frontend
npm install
npm run dev
npm run build
npm run lint
npm run test
```

Backend:

```bash
cd backend
./gradlew bootRun
./gradlew test
./gradlew test --tests "InterviewServiceTest"
./gradlew test --tests "com.rehearse.api.domain.interview.*"
```

Lambda:

```bash
cd lambda
./deploy.sh
./lambda-safe-deploy.sh
```

Use `lambda-safe-deploy.sh` for production Lambda deployment flows.

## Required Guides

Frontend work must follow:

- `frontend/CONVENTIONS.md`
- `frontend/CODING_GUIDE.md`
- `frontend/AGENTS.md`

Backend work must follow:

- `backend/CONVENTIONS.md`
- `backend/CODING_GUIDE.md`
- `backend/TEST_STRATEGY.md`
- `backend/AGENTS.md`

Lambda work must follow:

- `lambda/AGENTS.md`
- `docs/architecture/lambda-deployment.md`
- `docs/architecture/system-flow.md`

## Global Rules

- Do not revert user changes unless explicitly asked.
- Use `rg` for search when available.
- Do not commit `console.log` or debug leftovers.
- Do not use TypeScript `any`; prefer `unknown` plus type guards when needed.
- Frontend must never call Claude/LLM APIs directly. AI calls go through the backend.
- Backend must not return JPA entities directly. Convert responses to DTOs.
- Backend write operations use `@Transactional`; read paths default to `@Transactional(readOnly = true)`.
- AI model IDs belong in configuration such as `application-*.yml`, not hardcoded in application code.
- Avoid adding libraries unless there is a clear need and the existing stack cannot reasonably solve it.

## Spec-Driven Work

- Before changing `frontend/src/` or `backend/src/`, check for a relevant spec in `.omc/plans/` or `docs/plans/`.
- New feature work needs a spec first.
- Existing behavior changes should update the related spec before implementation.
- Specs should explain why the change exists, the intended outcome, evidence, and trade-offs.
- After implementation, update the spec status when the local workflow expects it.

## Branch and PR Rules

- Default development branch: `develop`.
- Production branch: `main`.
- Feature PRs target `develop`.
- Production releases use `develop -> main`.
- Branch names use `feat/{name}`, `fix/{name}`, or `refactor/{name}`.
- Backend and frontend PRs should be split when both areas are involved; merge backend first, then frontend.
- Commit messages are Korean conventional commits such as `feat:`, `fix:`, `refactor:`.
- PR titles use Korean and the format `[Scope] type: short description`, where scope is commonly `[FE]`, `[BE]`, or `[FE/BE]`.

## Decision Framework

For feature, architecture, design, and technology decisions, be able to answer:

1. Why is this change needed?
2. What outcome should it produce?
3. What evidence supports this direction?
4. What trade-offs or alternatives were considered?

## Security and Secrets

- Do not expose API keys or secrets in code, logs, docs, or browser-visible frontend paths.
- Sensitive local AWS details may be documented in ignored local files such as `.claude.local.md`; do not copy them into tracked files.
- Use AWS CLI inspection when infrastructure state matters instead of guessing from stale documentation.
