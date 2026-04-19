# Rehearse (리허설)

> 모의면접을 녹화한 뒤, AI 피드백을 영상의 **정확한 시점**에 고정합니다 — 점수가 아닌, 타임스탬프로 복기하는 개발자 면접 연습 플랫폼.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?style=flat&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=flat&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?style=flat&logo=typescript&logoColor=white)

**🌐 Live Demo → [www.rehearse.co.kr](https://www.rehearse.co.kr)** · 베타 전 기능 무료

![Rehearse 랜딩](docs/images/landing-hero.png)

---

## Why Rehearse?

기존 AI 면접 서비스는 면접이 끝나면 **텍스트 점수표**만 제공합니다.
"표정이 불안했습니다" 같은 피드백을 받아도, 어느 시점에서 그랬는지 확인할 방법이 없습니다.

Rehearse는 녹화된 면접 영상 위에 피드백을 **타임스탬프**로 연결합니다.
피드백을 클릭하면 해당 장면으로 점프해, 자신의 말투·표정·답변을 직접 보며 복기할 수 있습니다.

|  | 기존 서비스 | Rehearse |
|--|-----------|----------|
| 피드백 | 면접 종료 후 텍스트 점수표 | **타임스탬프 기반 영상+피드백 동기화** |
| 비언어 분석 | 없음 또는 추상적 점수 | **GPT-4o Vision 시선·표정·자세 분석** |
| 개발자 특화 | 범용 BQ 중심 | **이력서 PDF 기반 CS·시스템 설계·행동 면접** |
| 후속 질문 | 고정 질문 목록 | **답변 맥락 기반 AI 실시간 생성** |

---

## 핵심 기능

### 1. 이력서 기반 맞춤 질문

이력서 PDF 한 장을 업로드하면, 직무·경력·기술스택을 파싱해 **내 프로젝트에 파고드는 CS·시스템 설계·행동 질문**을 자동 생성합니다. 범용 BQ 템플릿이 아닌, 내가 실제로 썼다고 적은 기술에 대한 질문이 나옵니다.

### 2. AI 면접관과 실시간 대화

AI 면접관이 내 답변을 듣고 **심화·보충·반론** 후속 질문을 즉석에서 생성합니다. 최대 3라운드까지, 고정된 질문 리스트가 아니라 대화가 이어집니다. Google Meet 스타일 1:1 면접 UI로 실제 면접 긴장감까지 재현합니다.

### 3. 타임스탬프 동기화 피드백 *(핵심 차별점)*

피드백 카드를 클릭하면 영상이 정확히 그 순간으로 점프합니다. "0:42 — 시선 흔들림" 같은 추상 문구 대신, **몇 분 몇 초 구간에 무엇이 일어났는지** 영상 위에서 직접 확인합니다. 타임라인 · 피드백 · 영상이 하나의 시간축으로 묶입니다.

### 4. 비언어 분석 (GPT-4o Vision)

시선 회피 · 표정 경직 · 자세 불안정 · 말하기 속도 · 습관어("어…", "그…") 횟수를 **시점별로** 집계합니다. 면접 종료 즉시 `S3 업로드 → EventBridge → Lambda (Whisper STT + GPT-4o Vision)` 파이프라인이 자동으로 돌아갑니다.

### 5. 종합 리포트 & 모범 답안

100점 만점 종합 점수, 강점·보완점 요약, 그리고 **각 질문별 모범 답안**을 함께 제공합니다. "다음에 어떻게 말하면 더 좋을지"를 같은 화면에서 바로 확인할 수 있습니다.

---

**👉 지금 사용해보기 → [www.rehearse.co.kr](https://www.rehearse.co.kr)** · 로그인 후 이력서 PDF만 있으면 3분 안에 시작됩니다.

---

## 사용자 플로우

### 0. 대시보드

![대시보드](docs/images/dashboard.png)

지금까지 본 면접이 한 화면에 정리됩니다. 이번 주에 얼마나 연습했는지, 어떤 면접이 아직 끝나지 않았는지를 바로 확인하고, 이어서 볼지 · 다시 돌아볼지를 고를 수 있습니다. 오래 연습할수록 "내가 무엇을 얼마나 준비했는지"가 쌓입니다.

### 1. 면접 설정

![면접 설정](docs/images/setup.png)

내가 지원하려는 자리를 골라 면접을 맞춤으로 구성합니다. 직무와 연차를 고르고, 이번에 집중해서 연습할 주제(CS · 언어/프레임워크 · 경험/협업 · 시스템 설계 등)와 시간을 정한 뒤, 이력서 한 장을 올리면 **내 프로젝트와 기술 스택을 파고드는 질문**이 만들어집니다. 범용 질문이 아니라 "내 이력서에서 나올 만한 질문"입니다.

### 2. 면접 준비

![장치 확인](docs/images/ready.png)

맞춤 질문이 만들어지는 동안, 카메라 · 마이크 · 스피커를 한 번씩 체크합니다. 3가지를 모두 확인하면 면접을 시작할 수 있어, 실제 화상 면접 직전의 긴장감을 그대로 재현하는 워밍업이 됩니다.

### 3. AI 면접 진행

![면접 진행](docs/images/conduct.png)

화상 면접 화면 그대로, AI 면접관과 마주 앉아 대화합니다. 내가 답변을 끝내면 면접관이 바로 이어서 **심화·보충·반론** 질문을 던지기 때문에, 고정된 질문 리스트가 아니라 실제 면접처럼 대화가 흘러갑니다. 긴장한 상태에서 얼마나 논리적으로 말할 수 있는지 직접 부딪혀볼 수 있습니다.

### 4. 분석 대기 & 모범답변

![분석 대기 & 모범답변](docs/images/analysis.png)

면접이 끝나자마자 분석을 기다릴 필요 없이, 내가 방금 받은 질문에 대한 **모범답변**을 바로 읽을 수 있습니다. "그 자리에서는 이렇게 답했어야 하는구나"를 기억이 생생할 때 확인하는 게 핵심입니다. 상세 피드백이 준비되면 알림이 뜹니다.

### 5. 타임스탬프 피드백 리뷰

![피드백 리뷰](docs/images/feedback.png)

피드백을 클릭하면 영상이 정확히 그 순간으로 이동합니다. "0:58에 시선이 흔들렸어요" 같은 말을, **그 시점 영상을 직접 보면서** 복기할 수 있습니다.

- **내 답변은 어땠을까** — 잘한 점 · 아쉬운 점 · 이렇게 말하면 더 좋아요
- **어떤 인상을 줬을까** — 시선·표정·자세·말하기 속도
- **모범 답변** — 같은 질문을 더 잘 답하는 예시
- **복습 목록에 담기** — 아쉬웠던 답변만 따로 모아, 시간 날 때 다시 확인
- **복습 목록에 담기** — 아쉬웠던 답변을 저장해 사이드바 `복습 목록`에서 모아 재확인

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Frontend** | React 18, TypeScript 5.9, Vite, Tailwind CSS |
| **상태관리** | Zustand (client), TanStack Query (server) |
| **Backend** | Java 21, Spring Boot 3.4, Spring Data JPA |
| **Database** | MySQL 8.0 (prod) · H2 (local dev) |
| **AI — 질문/피드백** | Claude API (`claude-sonnet-4-20250514`) |
| **AI — 분석** | OpenAI Whisper (STT), GPT-4o (Vision + LLM) |
| **Infra** | AWS S3, EventBridge, Lambda (Python 3.12), CloudFront |
| **영상** | MediaRecorder (WebM), FFmpeg, MediaConvert |
| **배포** | EC2, Docker Compose, Nginx, ECR |

---

## Quick Start

### Prerequisites

- Java 21+
- Node.js 20+
- Git

### Backend

```bash
git clone https://github.com/KoSeonJe/devlens.git
cd devlens/backend
./gradlew bootRun
# 기본 프로필: local (H2 인메모리 DB, API 키 불필요)
# http://localhost:8080/actuator/health 로 상태 확인
```

### Frontend

```bash
cd devlens/frontend
npm install
npm run dev
# http://localhost:5173 에서 접속
```

> **참고**: Local 프로필에서는 H2 인메모리 DB를 사용하며, Claude/OpenAI API 키 없이도 기본 흐름을 확인할 수 있습니다. AI 질문 생성·분석 기능을 사용하려면 환경변수를 설정하세요.

---

## 환경변수

### Backend (`dev` / `prod` 프로필)

| Variable | Required | Description | Default |
|----------|----------|-------------|---------|
| `DB_URL` | Yes | MySQL JDBC URL | — |
| `DB_USERNAME` | Yes | DB 사용자명 | — |
| `DB_PASSWORD` | Yes | DB 비밀번호 | — |
| `CLAUDE_API_KEY` | Yes | Claude API 키 (질문 생성, 피드백) | — |
| `CLAUDE_MODEL` | No | Claude 모델 ID | `claude-sonnet-4-20250514` |
| `OPENAI_API_KEY` | No | OpenAI API 키 (후속질문 음성 분석) | — |
| `OPENAI_MODEL` | No | OpenAI 모델 ID | `gpt-4o-mini` |
| `AWS_ACCESS_KEY_ID` | Yes | AWS 액세스 키 | — |
| `AWS_SECRET_ACCESS_KEY` | Yes | AWS 시크릿 키 | — |
| `AWS_REGION` | No | AWS 리전 | `ap-northeast-2` |
| `AWS_S3_BUCKET` | No | S3 버킷명 | `rehearse-videos-dev` |
| `INTERNAL_API_KEY` | Yes | Lambda↔Backend 내부 API 키 | — |
| `CORS_ALLOWED_ORIGINS` | Yes | CORS 허용 도메인 | — |

> `local` 프로필에서는 H2 인메모리 DB를 사용하므로 위 환경변수가 불필요합니다.

