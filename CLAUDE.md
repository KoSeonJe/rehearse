# Rehearse — AI Mock Interview Platform for Developers

## Overview

- Timestamp-synced video feedback for AI developer mock interviews
- Target: developers preparing for jobs/transitions, bootcamp graduates
- Plan: `docs/product/PLAN.md` (all features within MVP DO scope)

---

## Quick Start

### Frontend

```bash
cd frontend
npm install
npm run dev          # Vite dev server
npm run build        # tsc -b && vite build
npm run lint         # ESLint
npm run test         # vitest run
npm run test:watch   # vitest (watch mode)
```

### Backend

```bash
cd backend
./gradlew bootRun                    # Spring Boot dev server (H2)
./gradlew test                       # 전체 테스트
./gradlew test --tests "InterviewServiceTest"  # 단일 클래스
./gradlew test --tests "com.rehearse.api.domain.interview.*"  # 도메인별
```

### Lambda

```bash
cd lambda
# analysis/ — OpenAI Whisper STT + GPT-4o Vision (Python 3.12)
# convert/  — MediaConvert 트리거
```

> 환경 전제조건 및 상세 설정은 `docs/guides/GETTING_STARTED.md` 참조.

---

## Project Structure

```
devlens/
├── frontend/          # React 18 + TypeScript + Vite + Tailwind
├── backend/           # Java 21 + Spring Boot 3.x + Gradle (Kotlin DSL)
├── lambda/            # AWS Lambda functions (Python 3.12)
│   ├── analysis/      # Whisper STT + GPT-4o Vision 비언어 분석
│   └── convert/       # MediaConvert 트리거
├── docs/              # 아키텍처, 가이드, 플랜 문서
│   ├── architecture/  # ERD, API 스펙, 시스템 플로우, 인프라 등
│   ├── product/       # PLAN.md
│   └── guides/        # 배포, 시작 가이드, 팀 워크플로우 등
└── .omc/              # 스펙 문서, 플랜, 상태
```

---

## Branch Strategy

- **GitHub default branch**: `main` (프로덕션 배포 브랜치, v0.1.0 부트스트랩 시 전환됨)
- **개발 통합 브랜치**: `develop` (모든 feature PR의 기본 타겟)
- **feature PR 생성 시**: 반드시 `--base develop` 명시 (`gh pr create --base develop`)
- **프로덕션 릴리즈 PR**: `develop → main` (스킬: `/prod-release`)
- Branch naming: `feat/{name}`, `fix/{name}`, `refactor/{name}`
- **BE/FE PR 분리 필수**: BE PR 먼저 머지 → FE PR 생성
- **CI 통과 필수**: `Frontend CI` (lint + build), `Backend CI` (test)

---

## Tech Stack

- **Frontend**: React 18 + TypeScript 5+ / Tailwind CSS / Vite / Zustand / TanStack Query
- **Backend**: Java 21 + Spring Boot 3.x / Gradle (Kotlin DSL) / Spring Data JPA
- **Database**: MySQL 8.0 (prod) / H2 (dev)
- **AI**: Claude API (Sonnet) — backend only, 모델 ID는 `application-*.yml`에서 관리
- **Analysis**: OpenAI API (Whisper STT, GPT-4o Vision/LLM) — Lambda only
- **Browser**: MediaRecorder (WebM), Web Speech API
- **Infra**: S3, EventBridge, Lambda (Python 3.12), MediaConvert, ECR, CloudFront

---

## Conventions & Guides

상세 컨벤션은 각 하위 문서 참조. CLAUDE.md에서는 핵심 규칙만 명시.

| 문서 | 내용 |
|------|------|
| `frontend/CONVENTIONS.md` | FE 네이밍, 디렉토리 구조, 컴포넌트/상태 패턴 |
| `frontend/CODING_GUIDE.md` | FE 클린코드, 훅 설계, 성능, a11y |
| `backend/CONVENTIONS.md` | BE 패키지 구조, 계층 규칙, DTO, 에러 처리 |
| `backend/CODING_GUIDE.md` | BE 클린코드, Entity/Service/Repository 패턴 |
| `backend/TEST_STRATEGY.md` | 테스트 대상 판단, 우선순위, Mock 정책 |

### 핵심 규칙 (전체 적용)

- TypeScript strict mode, `any` 금지
- `console.log` 커밋 금지
- Claude API 호출은 backend only (frontend에서 직접 호출 금지)
- Entity 직접 반환 금지 — Response DTO 변환 필수
- `@Transactional(readOnly = true)` 기본, 쓰기 메서드만 `@Transactional`

---

## Commit / PR Rules

- Commit messages: Korean, conventional commits (`feat:`, `fix:`, `refactor:`, etc.)
- PR title: `[Scope] type: short description` (in Korean)
  - Scope: `[FE]`, `[BE]`, `[FE/BE]`, omit for docs/chore
  - e.g. `[FE/BE] feat: 면접 Setup 페이지 UX 리디자인`
- PR scope: per feature or per issue
- **BE/FE PR 분리**: backend → frontend 순서로 별도 PR

---

## AWS 접근 정보

> EC2 IP, SSH 키 경로, 리소스명 등 민감 정보는 `.claude.local.md`에서 관리 (gitignored).
> AWS 인프라 상태 확인이 필요하면 `aws` CLI로 직접 조회할 것.

---

## MVP Scope

**DO**: AI interviewer (resume-based Q&A + follow-ups), video recording + S3 upload, nonverbal analysis (GPT-4o Vision via Lambda), STT (OpenAI Whisper via Lambda), timestamp feedback UI, AI feedback (Claude), summary report

**DON'T**: company-specific Q&A DB, interview history dashboard, peer review, mobile app, payments, coding test IDE

---

## Prohibited

- `any` type usage
- Committing `console.log` (remove after debugging)
- Direct Claude API calls from frontend (API key exposure)
- Implementing MVP DON'T features
- Unnecessary library additions (prefer browser-native APIs)
- `frontend/src/` 또는 `backend/src/` 소스 코드 수정 시 spec 문서 없이 진행 금지 — `.omc/plans/`에서 먼저 작성/확인

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
