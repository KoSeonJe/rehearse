# Standard Workflows

## 1. Feature Development (기능 개발)

```
[사용자 요청]
     ↓
  Orchestrator (분석 & 태스크 분해)
     ↓
  Planner (요구사항 수집 & 스펙 작성)
     ↓                              📄 .omc/plans/{feature}.md
  ┌─────────────────────────┐
  │  Designer (UI 설계)      │  ← 병렬 실행
  │  Frontend (프론트 로직)   │
  └─────────────────────────┘
     ↓                              📄 design-tokens.md 업데이트
  Backend (백엔드 API 구현)
     ↓                              📄 api-contracts.md 업데이트
  QA (QA 검증)
     ↓                              📄 issues.md 업데이트
  DevOps (빌드 & 배포)
```

### 단계별 상세

| 단계 | 에이전트 | 작업 | 문서화 |
|------|---------|------|--------|
| 1 | Orchestrator | 태스크 분해, 에이전트 할당 | - |
| 2 | Planner | 유저 스토리, 수용 기준 | `.omc/plans/{feature}.md` |
| 3 | Designer | 컴포넌트 구조, UI 코드 | `design-tokens.md` |
| 3 | Frontend | 상태 관리, API 연동 로직 | `handoffs.md` |
| 4 | Backend | API 엔드포인트, DB 스키마 | `api-contracts.md` |
| 5 | QA | 테스트 실행, 버그 리포트 | `issues.md`, QA Report |
| 6 | DevOps | 빌드 확인, 배포 설정 | `.env.example` |

> 각 단계 완료 시 `handoffs.md`에 핸드오프 기록.

---

## 2. Bug Fix (버그 수정)

```
[버그 리포트]
     ↓
  QA (조사 & 원인 분석)
     ↓                              📄 issues.md에 기록
  Backend 또는 Frontend (수정)
     ↓                              📄 handoffs.md에 기록
  QA (재검증)
     ↓                              📄 issues.md 상태 업데이트
```

### 단계별 상세

| 단계 | 에이전트 | 작업 | 문서화 |
|------|---------|------|--------|
| 1 | QA | 버그 재현, 원인 분석, 리포트 | `issues.md` |
| 2 | Backend/Frontend | 버그 수정 | `handoffs.md` |
| 3 | QA | 수정 확인, 리그레션 체크 | `issues.md` (Resolved) |

---

## 3. Refactoring (리팩토링)

```
  QA (현재 코드 분석)
     ↓
  Orchestrator (리팩토링 계획)
     ↓
  Frontend 또는 Backend (리팩토링 실행)
     ↓
  QA (리그레션 검증)
```

---

## 4. Sprint (스프린트)

여러 기능을 동시에 진행할 때:

```
  Orchestrator (전체 계획)
     ↓
  Planner (각 기능 스펙 작성)
     ↓
  Swarm (Frontend + Backend + Designer 병렬 실행)
     ↓
  QA (전체 QA)
     ↓
  DevOps (통합 배포)
```

---

## 5. Project Setup (프로젝트 초기 세팅)

```
  Planner (기술 요구사항 정리)
     ↓                              📄 docs/tech-stack.md
  DevOps (프로젝트 스캐폴딩, 빌드 설정)
     ↓
  Backend (백엔드 기본 구조)
     ↓
  Frontend (프론트 기본 구조)
     ↓
  Designer (디자인 시스템 초기화)
     ↓                              📄 design-tokens.md
```
