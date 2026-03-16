# Plans 작성 가이드

이 문서는 `docs/plans/` 스펙 문서를 작성할 때 따라야 할 지침입니다.

---

## 원칙

1. **스펙 먼저, 구현은 나중** — 모든 작업은 반드시 스펙 문서를 먼저 작성한 뒤 구현
2. **Why부터 시작** — 왜 이 작업이 필요한지 명확히 정의
3. **에이전트 지정 필수** — 태스크마다 담당 에이전트를 명시하지 않으면 불완전한 플랜

---

## 디렉토리 구조

```
docs/plans/{topic}/
├── requirements.md        # 요구사항 정의 (Why, 목표, 아키텍처, 제약조건)
├── plan-01-{subtopic}.md  # 개별 태스크 플랜
├── plan-02-{subtopic}.md
├── ...
└── progress.md            # 진행 상황 추적 테이블
```

- **topic**: 기능/프로젝트 단위 kebab-case 폴더명 (예: `cicd`, `sprint-1`, `auth-system`)
- **plan 파일**: `plan-{순번}-{subtopic}.md` (예: `plan-01-flyway.md`, `plan-02-dockerfile.md`)
- **requirements.md**: 전체 맥락과 요구사항을 정의하는 최상위 문서
- **progress.md**: 태스크별 상태를 추적하는 테이블

---

## requirements.md 작성 기준

```markdown
# {프로젝트/기능명} — 요구사항 정의

> 상태: Draft | In Progress | Completed
> 작성일: YYYY-MM-DD

## Why

{이 작업이 필요한 이유}

## 목표

{구체적인 결과물과 성공 기준}

## 아키텍처 / 설계

{전체 구조, 다이어그램, 기술 선택 근거}

## Scope

- **In**: {포함 범위}
- **Out**: {제외 범위}

## 제약조건 / 환경

{환경변수, 보안 요구사항, 인프라 제약 등}
```

### Decision Framework (필수)

requirements.md의 Why 섹션은 다음 4가지에 답해야 합니다:

1. **Why?** — 어떤 문제를 해결하는가?
2. **Goal** — 구체적인 결과물은? 성공 기준은?
3. **Evidence** — 근거 데이터나 리서치는?
4. **Trade-offs** — 포기하는 것은? 고려한 대안은?

---

## plan-NN-{subtopic}.md 작성 기준

```markdown
# Plan {NN}: {제목}

> 상태: Draft | In Progress | Completed
> 작성일: YYYY-MM-DD

## Why

{이 태스크가 필요한 이유 (requirements.md의 큰 Why 중 어떤 부분을 해결하는가)}

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `path/to/file` | {무엇을 하는가} |

## 상세

{구체적인 구현 내용, 필요 시 코드 예시}

## 담당 에이전트

- Implement: `{agent_name}` — {역할}
- Review: `{agent_name}` — {검증 포커스}

## 검증

- {검증 방법 1}
- {검증 방법 2}
- `progress.md` 상태 업데이트 (Task N → Completed)
```

---

## progress.md 작성 기준

```markdown
# {프로젝트/기능명} — 진행 상황

## 태스크 상태

| # | 태스크 | 상태 | 비고 |
|---|--------|------|------|
| 1 | {태스크명} | Draft / In Progress / Completed | |
| 2 | {태스크명} | Draft | |

## 진행 로그

### YYYY-MM-DD
- {작업 내용 요약}
- 생성 파일: {파일 목록}
- 수정 파일: {파일 목록}
```

---

## 에이전트 지정 규칙

### 구현 에이전트 (Implement)

| 영역 | 에이전트 | 용도 |
|------|---------|------|
| Backend API/로직 | `backend` | API 엔드포인트, 서비스 로직, DB 스키마 |
| Backend 아키텍처 | `backend-architect` | 시스템 설계, 마이크로서비스 경계 |
| Frontend 로직 | `frontend` | 컴포넌트, 상태 관리, API 연동 |
| Frontend 앱 | `frontend-developer` | 멀티 페이지, 복합 기능 |
| UI/UX 디자인 | `designer` | 레이아웃, 디자인 시스템, 반응형 |
| 인프라/CI/CD | `devops-engineer` | 파이프라인, Docker, 배포 |
| 테스트 | `test-engineer` | 테스트 전략, 자동화, 커버리지 |
| 디버깅 | `debugger` | 버그 원인 분석, 수정 |

### 리뷰 에이전트 (Review)

| 에이전트 | 검증 포커스 |
|---------|-----------|
| `architect-reviewer` | 아키텍처 일관성, SOLID, 레이어링 |
| `code-reviewer` | 코드 품질, 보안, 성능, 기술 부채 |
| `designer` | UI/UX 일관성, 디자인 토큰 준수 |
| `qa` | 기능 검증, 회귀 테스트, 엣지 케이스 |

### 태그

- `[parallel]` — 다른 태스크와 병렬 실행 가능
- `[blocking]` — 이 태스크 완료 전 다음 태스크 불가
- `[optional]` — 시간 여유가 있을 때만 진행

---

## 좋은 플랜 vs 나쁜 플랜

### 좋은 예

```markdown
# Plan 01: paths-filter로 변경 감지 job 추가

> 상태: Draft
> 작성일: 2026-03-16

## Why
BE만 수정해도 FE 빌드가 돌아 불필요한 리소스 낭비 발생.

## 생성/수정 파일
| 파일 | 작업 |
|------|------|
| `.github/workflows/deploy-dev.yml` | dorny/paths-filter 추가, 조건부 job 실행 |

## 담당 에이전트
- Implement: `devops-engineer` — 워크플로우 수정
- Review: `architect-reviewer` — 파이프라인 구조 검증

## 검증
- actionlint YAML 검증
- BE만 변경 시 FE 빌드 미실행 확인
```

### 나쁜 예

```markdown
# CI/CD 수정
- 파이프라인 고치기
```

나쁜 이유: 에이전트 미지정, 파일 미명시, 작업 내용 모호, 구조 미준수

---

## 체크리스트

플랜 작성 후 아래 항목을 확인하세요:

- [ ] `requirements.md`에 Why/Goal/Evidence/Trade-offs가 있는가?
- [ ] 각 `plan-NN-*.md`에 담당 에이전트(Implement/Review)가 지정되었는가?
- [ ] 생성/수정 파일 경로가 명시되었는가?
- [ ] `progress.md`에 전체 태스크 목록이 있는가?
- [ ] 검증 방법이 포함되었는가?
