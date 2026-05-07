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
├── docs/       # architecture, product, plans, guides
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

환경 전제조건 상세: `docs/guides/GETTING_STARTED.md`.

## 작업 후 보고

모든 agent / 메인 세션 공통. 작업 완료 / 중단 시 변경 요약 외 **발견 사항 + 사용자 결정 필요 항목** 별도 처리. "문제 없음" 으로 묻어두지 말 것.

### 1. 발견 사항 보고 (텍스트)

후속 결정에 사용자 판단 불필요한 단순 발견은 텍스트 보고.

- 미수정 결정 (예: 과거 문서 / 역사 기록 / scope 외 파일) — 무엇을 / 왜 / 추후 어떻게 할지
- 가능 추정 / 미확인 가정 — 검증 필요 항목
- 발견된 다른 위반 / 불일치 (현재 작업 범위 외)
- 마이그레이션 필요 항목 (현재 코드와 새 컨벤션 갭 등)

형식:
```
**발견 사항**:
- {내용} — {조치 / 보류 사유 / 사용자 결정 필요 여부}
```

### 2. 사용자 결정 필요 → `AskUserQuestion` 도구 사용 (Blocking)

다음 케이스 = **`AskUserQuestion` 도구로 선택지 제시**. 텍스트 나열 X. 자유서술 받기 X.

- trade-off 결정 (옵션 비등 / NF 결정: 확장성·정합성·성능·동시성)
- 영향 범위 큰 변경 (공개 API 시그니처 / 이벤트 페이로드 / 마이그레이션 backfill)
- spec 미커버 / 컨벤션 미커버 케이스
- 발견 사항 중 후속 작업 분기 필요 (수정 / 보류 / 별도 PR)
- 요구사항 모호 / 누락

**원칙**:
- 옵션 2-4개. 자유서술 회피.
- 첫 자리 = 추천 옵션. 라벨에 `(추천)` 명시.
- 각 옵션 = trade-off 한 줄 (장 / 단 / 채택 사유).
- 사용자 답변 받기 전 후속 작업 / 코드 변경 금지 (Blocking).

**예시**:
```
question: "동시성 모델 결정 — 어떻게 진행할까요?"
options:
  - "낙관락 (추천) — 충돌 적은 도메인. 재시도 로직 단순"
  - "비관락 — 충돌 잦은 도메인. select for update 비용"
  - "이벤트 직렬화 — 강순서 보장. 큐 인프라 필요"
```

`AskUserQuestion` 도구 부재 환경에서만 텍스트 fallback (옵션 + 추천 + 사유 동일 형식).

발견 즉시 표면화. "일단 해보고" / "CI 통과" / "단순함" 우회 금지.

## Spec-Driven Work

### 폴더 구조

```
docs/plans/{YYYY-MM-DD-topic}/
├── product_spec/        # 기획 스펙 (사용자 작성). 무엇을 / 왜 / 수용 기준
│   └── requirements.md
└── tech_spec/           # 구현 설계 (backend / frontend agent 작성). 어떻게
    ├── design.md
    └── progress.md
```

### 워크플로우

```
[사용자] product_spec 작성
   ↓
[구현 agent] tech_spec 작성 (Why / Goal / Tasks / Trade-offs / Verification)
   ↓ (사용자 승인)
[구현 agent] 코드 작성
```
