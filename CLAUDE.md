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

- **GitHub default branch**: `develop` (개발 통합 브랜치, 모든 feature PR의 기본 타겟)
- **프로덕션 배포 브랜치**: `main` (deploy-prod.yml이 `push: main`으로 자동 트리거)
- **feature PR 생성**: base는 `develop` (default). `/create-pr` 스킬이 명시적으로 지정함
- **프로덕션 릴리즈 PR**: `develop → main` (스킬: `/prod-release`, `gh release create --target main`)
- Branch naming: `feat/{name}`, `fix/{name}`, `refactor/{name}`
- **BE/FE PR 분리 필수**: BE PR 먼저 머지 → FE PR 생성
- **CI 통과 필수**: `Frontend CI` (lint + build), `Backend CI` (test)

### 직접 push 절대 금지 (Required)

- `main`, `develop`은 **브랜치 보호 규칙으로 직접 push 차단**됨. 모든 변경은 PR 경유.
- **`main`으로 머지**: `develop → main` 릴리즈 PR로만 가능. feature/fix 브랜치에서 main 직접 PR 금지. 반드시 `/prod-release` 스킬 사용.
- **`develop`으로 머지**: `feat/*`, `fix/*`, `refactor/*` 브랜치 PR로만 가능. 반드시 `/create-pr` 스킬 사용.
- 릴리즈 머지 직후 develop은 `main` 머지 커밋이 빠진 상태가 됨 → 다음 릴리즈 PR 생성 전 `git merge origin/main` back-merge 필수.

---

## Tech Stack

- **Frontend**: React 18 + TypeScript 5+ / Tailwind CSS / Vite / Zustand / TanStack Query
- **Backend**: Java 21 + Spring Boot 3.x / Gradle (Kotlin DSL) / Spring Data JPA
- **Database**: MySQL 8.0 (prod) / H2 (dev)
- **AI (Backend)**: OpenAI **GPT-4o-mini** primary + **Claude Sonnet/Haiku** fallback — `ResilientAiClient` 이중화, 프롬프트 빌더 공용. 모델 ID는 `application-*.yml`에서 관리
- **Analysis (Lambda)**: **Gemini** (audio 통합 분석, 주력) + OpenAI **GPT-4o Vision** (프레임) + OpenAI **Whisper** (STT fallback 경로)
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

## 디자인 기준 (Frontend)

프론트엔드 UI/디자인 작업 시 아래 지침을 **반드시 먼저 로드**하고 따를 것.

- **비주얼 방향**: `DESIGN.md` — Cal.com 기반 모노크롬 디자인 시스템 (색상/타이포/섀도/스페이싱 토큰)
- **AI 티 방지 규칙**: `.claude/rules/frontend-design-rules.md` — 금지 색상/폰트/레이아웃 및 self-check 체크리스트
- **베이스 컴포넌트**: shadcn/ui (ui.shadcn.com) — 모든 기본 UI primitive는 shadcn 우선 사용. `/shadcn` 스킬로 추가·검색
- **장식 컴포넌트**: Aceternity UI — **레퍼런스 용도**, 페이지당 **최대 1-2개**까지만 사용 (남용 금지)

작업 순서:
1. `DESIGN.md` 토큰/원칙 확인 → 2. shadcn primitive로 구성 → 3. 필요 시 Aceternity 1-2개로 포인트 → 4. `frontend-design-rules.md` self-check 통과 후 완료

---

## Commit / PR Rules

- Commit messages: Korean, conventional commits (`feat:`, `fix:`, `refactor:`, etc.)
- PR title: `[Scope] type: short description` (in Korean)
  - Scope: `[FE]`, `[BE]`, `[FE/BE]`, omit for docs/chore
  - e.g. `[FE/BE] feat: 면접 Setup 페이지 UX 리디자인`
- PR scope: per feature or per issue
- **BE/FE PR 분리**: backend → frontend 순서로 별도 PR
- **PR 생성은 반드시 `/create-pr` 스킬 사용** — 직접 `gh pr create` 호출 금지. 스킬이 BE/FE 분리·base 브랜치(`develop`)·한국어 컨벤션·브랜치 네이밍을 강제함

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
