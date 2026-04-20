# Tech Stack

팀의 기본 기술 스택 정의. 모든 개발자(Frontend, Backend, DevOps)는 작업 전 이 문서를 참조한다.

> **변경 규칙**: 스택 변경 시 반드시 `.omc/notepads/team/decisions.md`에 ADR 기록 후 변경.

---

## Frontend

| 영역 | 기술 | 버전 |
|------|------|------|
| Framework | React + TypeScript | React 18.3, TS 5.9 |
| Styling | Tailwind CSS | 3.4 |
| Build | Vite | 7.3 |
| State (Client) | Zustand | 4.5 |
| State (Server) | TanStack Query | 5.90 |
| Testing | Vitest | 4.0 |

### 선택 근거

> **문제**: 초기 스타트업에서 빠른 UI 개발과 에이전트 간 타입 안전 협업이 필요하다.
>
> **상황**: AI 에이전트가 코드를 작성하므로 타입 시스템이 계약(contract) 역할을 해야 한다.
>
> **선택지**:
> | 옵션 | 장점 | 단점 |
> |------|------|------|
> | React + TS | 최대 생태계, 채용 풀, AI 학습 데이터 풍부 | 보일러플레이트 |
> | Vue + TS | 간결, 러닝커브 낮음 | 생태계 상대적 소규모 |
> | Next.js | SSR, 풀스택 | 초기엔 오버스펙 |
>
> **결정**: React + TypeScript. 생태계 크기와 AI 에이전트의 코드 생성 품질이 가장 높다.

---

## Backend

| 영역 | 기술 | 버전 |
|------|------|------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.4 |
| Build | Gradle (Kotlin DSL) | 8.x |
| Database | MySQL | 8.0 |
| ORM | Spring Data JPA + Hibernate | - |
| Validation | Jakarta Bean Validation | - |
| Testing | JUnit 5 + Mockito + Spring Boot Test | - |

### 선택 근거

> **문제**: 엔터프라이즈급 안정성, 타입 안전성, 확장 가능한 백엔드가 필요하다.
>
> **상황**: 한국 IT 시장 타겟, 장기적으로 대규모 트래픽 대응 가능해야 한다.
>
> **선택지**:
> | 옵션 | 장점 | 단점 |
> |------|------|------|
> | Java Spring | 한국 표준, DI/AOP 성숙, 대규모 검증 | 초기 설정 무거움 |
> | Node.js Express | 빠른 프로토타이핑, JS 풀스택 | 타입 안전성 약함, 대규모 불안 |
> | Kotlin Spring | 현대적 문법, Null Safety | 채용 풀 상대적 소규모 |
>
> **결정**: Java 21 + Spring Boot 3. 한국 IT 시장 표준이며, DI/AOP 등 아키텍처 패턴이 가장 성숙하고, 토스/카카오/네이버 등 대규모 서비스에서 검증됨.

---

## Database

| 영역 | 기술 | 용도 |
|------|------|------|
| Production | MySQL 8.0 | 운영 환경 |
| Development | H2 In-Memory | 로컬 개발 (빠른 시작) |

### 선택 근거

> **문제**: 신뢰할 수 있고 성능 좋은 RDBMS가 필요하다.
>
> **상황**: CRUD 중심 웹 서비스, Java Spring 백엔드, 한국 시장 타겟.
>
> **선택지**:
> | 옵션 | 장점 | 단점 |
> |------|------|------|
> | MySQL | 읽기 성능 우수, 한국 기업 표준, Spring+JPA 최적 궁합, 자료 풍부 | 복잡한 쿼리/JSON에서 PostgreSQL보다 약함 |
> | PostgreSQL | 복잡한 쿼리, JSON, 확장성 우수 | 초기 스타트업 CRUD에는 오버스펙, 한국 자료 상대적 부족 |
> | MongoDB | 스키마리스, 빠른 프로토타이핑 | ACID 트랜잭션 약함, JPA 미지원 |
>
> **결정**: MySQL 8.0. 우리는 CRUD 중심 웹 서비스이고, Spring Boot + JPA와 가장 검증된 조합이며, 토스/카카오/네이버 등 한국 IT 기업 표준이다.

---

## Build & Tooling

| 영역 | 기술 | 근거 |
|------|------|------|
| Backend Build | Gradle (Kotlin DSL) | Maven보다 유연, 빌드 스크립트 가독성 |
| Frontend Build | Vite | 빠른 HMR, 간단한 설정, ESM 네이티브 |
| Linter (JS/TS) | ESLint + Prettier | 코드 스타일 자동 포맷 |

---

## AI / LLM

면접 서비스 특성상 LLM을 **용도별로 다른 공급자**로 분리 운영한다.

### Backend (질문 생성 · 꼬리질문) — `ResilientAiClient`

| 역할 | 공급자 / 모델 | 용도 |
|------|--------------|------|
| **Primary** | OpenAI **GPT-4o-mini** | 초기 질문 생성 (temp 0.9), 꼬리질문 (temp 0.7) |
| Primary (audio) | OpenAI **gpt-4o-mini-audio-preview** | 오디오 직접 입력 꼬리질문 (STT 불필요) |
| **Fallback** | Anthropic **Claude Sonnet** | 질문 생성 fallback |
| Fallback | Anthropic **Claude Haiku** | 꼬리질문 fallback (Whisper STT 선행) |

- 활성 조건: `openai.api-key` 존재 → primary, 없으면 Claude only, 둘 다 없으면 `MockAiClient`
- 프롬프트 빌더(`QuestionGenerationPromptBuilder`, `FollowUpPromptBuilder`)는 **양쪽 공용** — 템플릿 1벌로 관리
- Claude는 Prompt Caching(`SystemContent.withCaching()`) 활성

### Analysis (Lambda) — Python 3.12

| 역할 | 공급자 / 모델 | 파일 |
|------|--------------|------|
| **Audio 통합 분석 (주력)** | Google **Gemini** (`google.generativeai`) | `gemini_analyzer.py` — verbal+technical+vocal 통합 |
| 언어 분석 (보조) | OpenAI GPT | `verbal_analyzer.py` |
| 비언어 (Vision) | OpenAI **GPT-4o Vision** | `vision_analyzer.py` — 프레임 기반 |
| STT (fallback 경로) | OpenAI **Whisper** | `stt_analyzer.py` |
| 미디어 처리 | FFmpeg (static) 7.x | - |

- 프롬프트 팩토리 `verbal_prompt_factory.py` 는 모델 중립으로 설계 → Gemini/OpenAI 양쪽이 공유
- handler 가 4개 analyzer 모두 import. 실제 주력 경로는 `analyze_answer_audio` (Gemini)

### 선택 근거

> **문제**: 면접 질문 생성/꼬리질문, 녹화 영상의 STT·언어·비언어 분석을 안정적으로 처리해야 한다.
>
> **상황**: 단일 벤더 종속 리스크와 장애 내성이 필요. 언어·비언어 분석은 오디오 직접 이해 능력이 중요.
>
> **결정**:
> - Backend: GPT-4o-mini primary + Claude fallback 이중화로 벤더 장애 내성 확보. 동일 템플릿 공유로 운영 복잡도 최소.
> - Lambda: Gemini 의 long-context 오디오 분석 + OpenAI Vision 의 프레임 해석력 조합이 비용·품질 최적.

---

## Infrastructure

| 영역 | 기술 | 사양 |
|------|------|------|
| Compute | EC2 | t3.small (2 vCPU, 2GB) |
| Container | Docker Compose | Backend + MySQL |
| Object Storage | S3 | rehearse-videos-dev |
| Event | EventBridge | S3 PutObject → Lambda 트리거 |
| Serverless | Lambda | Convert (256MB/5min) + Analysis (2048MB/15min) |
| Video Convert | MediaConvert | WebM→MP4 (H.264+AAC) |
| CDN | CloudFront | Frontend SPA 배포 |
| Registry | ECR | Backend Docker 이미지 |

### 선택 근거

> **문제**: 영상 업로드 후 분석/변환을 비동기로 처리하는 파이프라인이 필요하다.
>
> **상황**: 분석은 15분까지 소요될 수 있으며, EC2 부하를 분리해야 한다.
>
> **결정**: S3 → EventBridge → Lambda 이벤트 드리븐 아키텍처. 영상 업로드 시 자동으로 분석+변환 Lambda가 병렬 트리거되어 서버 부하 없이 처리.
