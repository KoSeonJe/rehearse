> **🛑 STOP — IMPLEMENTATION GATE**
> 모든 코드 변경 (FE/BE/Lambda 포함) 은 Plan 작성 + 사용자 명시 승인 후에만 시작한다.
> "혼자 판단해서 진행" 금지. 모호하면 즉시 사용자에게 되묻는다.
> 적용 예외 없음. CI 통과·테스트 작성·리팩 단순함 모두 사유 안 됨.

# Rehearse Agent Instructions

루트 진입점. Claude/Codex 공용. Claude 전용 추가 규칙은 `CLAUDE.md` 참조 (이 파일을 먼저 읽은 뒤).

## Read Order

1. 이 루트 `AGENTS.md`.
2. Claude 세션이면 `CLAUDE.md` 도 로드 (서브에이전트/스킬/위임 규칙).

## Rules

전역 규칙. 모든 작업에 자동 적용.

@.claude/rules/commit.md
@.claude/rules/comments.md
@.claude/rules/security.md
@.claude/rules/plan-mode.md
@.claude/rules/branch-pr.md
@.claude/rules/reporting.md

## AGENTS.md / CLAUDE.md 관리

`AGENTS.md` / `CLAUDE.md` (root + backend/frontend/lambda) 수정·추가·점검 시 `claude-md-management` 플러그인 사용. 상세 룰 (도구 매핑 / 트리거 신호 / 운영 룰) = `.claude/rules/meta-docs.md` Read.

## Project Overview

- Product: AI 모의면접 + 타임스탬프 동기화 영상 피드백 SaaS.
- Target: 개발자 취준생 / 이직 / 부트캠프 수료생.
- 핵심 플로우: 이력서 업로드 → 컨텍스트 기반 AI 인터뷰 (꼬리질문) → 영상+음성 녹화 / S3 → Lambda 분석 (Gemini 비언어 + Whisper STT) → 타임스탬프별 AI 피드백 + 요약 리포트.
- 핵심 도메인: `resume` (이력서 컨텍스트), `interview` (세션 / runtime state / intent 분기), `feedback` (rubric / score / 북마크), `lambda/analysis` (비언어).
- 차별점: 영상 타임스탬프와 AI 피드백 동기화 — "이 순간 시선 흔들림 + 답변 모호" 식 지점 코칭.
- MVP 스코프 의도적 제한. DON'T 항목 (회사별 Q&A DB / peer review / 모바일 / 결제 / 코테 IDE) 구현 금지 (스펙 갱신 없이).

## Structure

```
devlens/
├── frontend/   # React 18 + TypeScript + Vite + Tailwind + Zustand + TanStack Query
├── backend/    # Java 21 + Spring Boot 3.x + Gradle Kotlin DSL + Spring Data JPA
├── lambda/     # Python 3.12 (analysis: Gemini/Vision/Whisper, convert: MediaConvert)
├── docs/       # plans (Issue spec), images, performance-tests, troubleshooting
```

### Placement Rule

신규 파일 / 디렉토리는 영역별 모듈 안에 둔다. 루트는 메타 자산만 보유.

| 영역 | 위치 | 예시 |
|------|------|------|
| Backend | `backend/` | Spring 코드, Gradle, 로컬 인프라 compose, JVM eval / 토큰 측정 스크립트 |
| Frontend | `frontend/` | React 코드, Vite/ESLint 설정, FE 전용 스크립트 |
| Lambda | `lambda/` | Python analyzer / converter, Lambda 배포 스크립트 |
| Repo-wide 메타 | `scripts/`, `.github/`, `docs/`, `.claude/` | git hooks 호출 스크립트, CI 워크플로우, 멀티-영역 문서, 에이전트 / 룰 |

판단 기준: **이 자산이 한 영역에만 의존하는가?** Yes → 해당 모듈 하위. No (멀티-영역 또는 repo 전체 메타) → 루트 메타 디렉토리.

루트 직속에 새 파일 추가 전 위 표 재확인. 모호하면 사용자에게 질문 (자율 판단 금지).

## Tech Stack

- **Frontend**: React 18, TypeScript 5+, Vite, Tailwind, Zustand, TanStack Query
- **Backend**: Java 21, Spring Boot 3.x, Gradle (Kotlin DSL), Spring Data JPA
- **Database**: MySQL 8.0 (local / dev / prod 동일). 테스트는 Testcontainers (mysql).
- **AI (Backend)**: OpenAI **GPT-4o-mini** primary + **Claude Sonnet/Haiku** fallback. `ResilientAiClient` 이중화. 모델 ID는 `application-*.yml` 관리 (하드코딩 금지).
- **Analysis (Lambda)**: **Gemini** (audio 통합 분석, 주력) + OpenAI **GPT-4o Vision** (프레임) + OpenAI **Whisper** (STT fallback)
- **Browser**: MediaRecorder (WebM), Web Speech API
- **Infra**: S3, EventBridge, Lambda (Python 3.12), MediaConvert, ECR, CloudFront

## Commands

Frontend:

```bash
cd frontend
npm install
npm run dev          # Vite dev server
npm run build        # tsc -b && vite build
npm run lint         # ESLint
npm run test         # vitest run (전체)
npm run test -- src/path/to/file.test.ts   # 단일 파일
npm run test:watch
```

Backend:

```bash
cd backend
docker compose -f docker-compose.local.yml up -d        # MySQL local 컨테이너
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew test                                          # 전체
./gradlew test --tests "InterviewServiceTest"           # 단일 클래스
./gradlew test --tests "com.rehearse.api.domain.interview.*"  # 도메인
python3 eval/context/measure_tokens.py                  # context 토큰 측정
```

Lambda:

```bash
cd lambda
./deploy.sh
./lambda-safe-deploy.sh                                 # 프로덕션 배포 플로우
cd analysis && pytest                                   # 단위 테스트
cd analysis && pytest tests/test_vision_analyzer.py     # 단일 파일
```

## Spec-Driven Work

모든 spec 필요 작업 = GitHub Issue (Epic) + `docs/plans/{N}-{slug}/` 폴더 1:1 매핑. 작은 bug/chore 는 Issue body 만 (폴더 X).

플로우: `product-spec.md` (사용자) → `tech-spec.md` (agent, API contract 포함) → ★승인★ → `implement.md` (또는 `-be`/`-fe`) → ★승인★ → 구현 → PR (`Closes #N`) → Issue close.

BE/FE 동시 작업 시 API contract 합의 후 병렬 시작 (FE 는 mock 진행). 세션 종료 / 컨텍스트 30-40% 잔여 시 `handoff.md` 작성.

상세 룰 (폴더 구조 / 명명 / 파일 역할 / 분리 임계 / handoff / 종료) + 템플릿: `docs/plans/AGENTS.md` + `docs/plans/_templates/`.
