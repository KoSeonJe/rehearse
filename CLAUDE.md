# DevLens — AI 기반 개발자 모의면접 플랫폼

## 프로젝트 개요

- **한 줄 설명**: 타임스탬프 기반 비디오 피드백을 제공하는 AI 개발자 모의면접 플랫폼
- **대상 사용자**: 취업/이직 준비 개발자, 부트캠프 수료생
- **핵심 차별점**: 면접 녹화본을 재생하며 언어적/비언어적 피드백을 타임스탬프에 동기화하여 제공
- **기획서**: `docs/PLAN.md` 참조. 모든 기능 구현은 MVP DO 범위 내에서 진행
- **워크플로우**: `docs/workflows.md` 참조. 기능 개발, 버그 수정, 리팩토링, 스프린트 등 표준 워크플로우 정의
- **기술 스택 상세**: `docs/tech-stack.md` 참조. 기술 선택 근거 및 버전 정보

---

## 기술 스택

> 상세 기술 스택 및 선택 근거는 `docs/tech-stack.md` 참조

| 계층 | 기술 | 비고 |
|------|------|------|
| **Frontend** | React 18 + TypeScript 5+ | |
| 스타일링 | Tailwind CSS 3.x | |
| 빌드 | Vite 5.x | 빠른 HMR, ESM 네이티브 |
| 상태 (Client) | Zustand 4.x | |
| 상태 (Server) | TanStack Query 5.x | |
| 테스트 (FE) | Vitest + Playwright | |
| **Backend** | Java 21 + Spring Boot 3.x | |
| 빌드 (BE) | Gradle (Kotlin DSL) 8.x | |
| ORM | Spring Data JPA + Hibernate | |
| 테스트 (BE) | JUnit 5 + Mockito | |
| **Database** | MySQL 8.0 | 운영 환경 |
| DB (Dev) | H2 In-Memory | 로컬 개발 |
| **AI** | Claude API (claude-sonnet-4-20250514) | 질문생성 + 피드백생성 + 후속질문 |
| 비언어 분석 | MediaPipe (Face Mesh + Pose) | 브라우저 실행 |
| 음성 분석 | Web Audio API + AudioWorklet | 브라우저 실행 |
| 영상 녹화 | MediaRecorder API (WebM) | 브라우저 네이티브 |
| STT | Web Speech API / Whisper API 폴백 | |

---

## 디렉토리 구조

```
frontend/                   # React + Vite
├── src/
│   ├── components/
│   │   ├── ui/             # 공통 UI 컴포넌트
│   │   ├── interview/      # 면접 관련 컴포넌트
│   │   ├── review/         # 피드백 리뷰 컴포넌트
│   │   └── mediapipe/      # MediaPipe 관련 컴포넌트
│   ├── hooks/              # 커스텀 훅
│   ├── stores/             # Zustand 스토어
│   ├── lib/                # 유틸리티, API 클라이언트
│   │   ├── mediapipe/      # MediaPipe 설정/유틸
│   │   └── audio/          # Web Audio 분석
│   ├── pages/              # 페이지 컴포넌트
│   └── types/              # TypeScript 타입 정의

backend/                    # Java Spring Boot
├── src/main/java/
│   ├── controller/         # REST API 컨트롤러
│   ├── service/            # 비즈니스 로직
│   ├── repository/         # JPA 레포지토리
│   ├── entity/             # JPA 엔티티
│   ├── dto/                # 요청/응답 DTO
│   └── config/             # 설정 클래스
├── src/main/resources/
│   └── application.yml     # Spring 설정
└── build.gradle.kts        # Gradle 빌드 파일
```

---

## 코딩 컨벤션

### 일반
- TypeScript strict 모드 사용
- `any` 타입 사용 금지 — 반드시 명시적 타입 정의
- 함수형 컴포넌트 + 화살표 함수
- 파일명: kebab-case (`interview-player.tsx`)
- 컴포넌트명: PascalCase (`InterviewPlayer`)
- 훅: `use` 접두사 (`useMediaPipe`)

### 컴포넌트 패턴
- Props 인터페이스는 컴포넌트 파일 내부에 정의
- barrel export (`index.ts`) 사용하지 않음 — 직접 import

### 상태 관리
- 서버 상태: TanStack Query (`useQuery`, `useMutation`)
- 클라이언트 전역 상태: Zustand (면접 진행 상태, 비디오 플레이어 상태)
- 로컬 상태: `useState` / `useReducer`

### Backend
- REST API: Spring Boot `@RestController`
- 계층 구조: Controller → Service → Repository
- 에러 응답: 통일된 에러 DTO 형태
- Claude API 호출은 반드시 백엔드(Spring Boot)에서만 실행
- Jakarta Bean Validation 사용

---

## 커밋/PR 규칙

- 커밋 메시지: 한국어, conventional commits (`feat:`, `fix:`, `refactor:` 등)
- PR 단위: 기능별 또는 이슈별
- 브랜치: `feat/기능명`, `fix/버그명`

---

## MVP 스코프 (DO)

1. AI 면접관 (이력서 기반 질문생성 + 후속질문)
2. 영상 녹화 (MediaRecorder, WebM)
3. 비언어 분석 (MediaPipe Face Mesh + Pose)
4. 음성 분석 (Web Audio API)
5. 타임스탬프 피드백 UI (비디오+피드백 동기화, 이벤트 마커)
6. STT 변환 (Web Speech API / Whisper 폴백)
7. AI 피드백 생성 (Claude API)
8. 종합 리포트 (요약 점수 + 개선포인트)

## MVP 스코프 (DON'T)

- 기업별 질문 DB, 면접이력 대시보드, 피어리뷰, 모바일 앱, 결제, 코딩 테스트 IDE

---

## 금지 사항

- `any` 타입 사용
- `console.log` 커밋 (디버깅 후 반드시 제거)
- 프론트엔드에서 직접 Claude API 호출 (API Key 노출 — 반드시 백엔드 경유)
- MVP DON'T 범위의 기능 구현
- 불필요한 라이브러리 추가 (브라우저 네이티브 API 우선)

---

## 멀티 에이전트 팀 운용법

### 팀 구성

| 역할 | 에이전트 | 담당 |
|------|---------|------|
| PM | `orchestrator` | 태스크 분해, 순서 조율, 에이전트 디스패치 |
| 기획 | `planner` | 요구사항 정의, 유저 스토리, 수용 기준 |
| 디자인 | `designer` | UI/UX 설계, 디자인 토큰, 컴포넌트 구조 |
| 프론트엔드 | `frontend` | React 구현, 상태 관리, API 연동 |
| 백엔드 | `backend` | Spring Boot API, DB, 비즈니스 로직 |
| QA | `qa` | 테스트 실행, 버그 탐지, 리그레션 체크 |
| 인프라 | `devops` | 빌드, Docker, CI/CD |

### 기능 개발 워크플로우 (혼합 실행 모드)

```
1. 사용자: "OO 기능 만들어줘" → orchestrator 호출
2. PM이 태스크 분해 후 순차 실행:
   - Designer → UI 설계 → ⏸️ 사용자 확인 (Slack 알림)
   - Backend → API 설계+구현 → ⏸️ 사용자 확인 (Slack 알림)
   - Frontend → 구현 → 자동 진행
   - QA → 테스트 → 자동 진행
   - DevOps → 빌드 확인 → Slack 완료 보고
```

**⏸️ 수동 확인 대상**: Designer 결과물, Backend API 설계
**▶️ 자동 진행 대상**: Frontend, QA, DevOps

### 팀 문서

| 파일 | 용도 |
|------|------|
| `.omc/notepads/team/handoffs.md` | 에이전트 간 핸드오프 기록 |
| `.omc/notepads/team/issues.md` | 버그/이슈 트래킹 |
| `.omc/notepads/team/api-contracts.md` | Backend↔Frontend API 계약 |
| `.omc/notepads/team/design-tokens.md` | 디자인 시스템 토큰 |
| `.omc/notepads/team/decisions.md` | 아키텍처 결정 기록 (ADR) |

### Slack 알림

- 설정 파일: `~/.claude/.omc-config.json`
- 알림 시점: 세션 종료, 사용자 입력 대기, 세션 시작, 세션 유지
- 활성화: `omc --slack` 플래그로 세션별 활성화
