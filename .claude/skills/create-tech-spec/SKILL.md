---
name: create-tech-spec
description: "기존 product-spec 기반 tech-spec.md 생성. 영향범위 / 정합성 / 실시간성 / 성능 / 동시성 / 마이그레이션 / 외부의존 / 보안 / 롤백 메타인지 질문으로 Architecture / API contract / Trade-off / Verification 채움. docs/plans/{N}-{slug}/tech-spec.md."
---

# Create Tech Spec

`product-spec.md` 1개 → `tech-spec.md` 1개. 기술 메타인지를 강제하는 대화형 spec 생성.

## 전제 (Read 필수, Blocking)

스킬 시작 직후 다음 2개 문서 `Read`. 미로드 시 진행 금지.

- `docs/plans/AGENTS.md` — 워크플로우 / 승인 게이트 / BE+FE 분리 룰 / 안티패턴
- `docs/plans/_templates/tech-spec.md` — 출력 파일 템플릿 구조

추가 전제:

- 사용자가 product-spec 을 이미 작성했어야 함. 부재 시 → "먼저 `/create-product-spec` 호출 권장." 종료.
- tech-spec 은 product-spec 의 WHY/WHAT 을 받아 **HOW** 만 다룬다. 요구사항 새로 정의 X.
- 모든 단계에서 `docs/plans/AGENTS.md` 룰 준수. 충돌 시 AGENTS.md 우선.

## 핵심 원칙

- **한 번에 1 질문** (brainstorming 패턴).
- **`AskUserQuestion` 우선** — 메타인지 결정은 다중선택. 자유서술은 구조 / 데이터 모델 / API contract 같이 자연어 필요한 필드.
- **Trade-off 명시 강제** — 결정마다 "왜 A 가 아닌 B?" 답 받아낸다.
- **승인 게이트** — preview → confirm → write (Blocking).

## Step 1 — product-spec 선택

후보 폴더 자동 수집:

```bash
# handoff.md 존재 = 진행 중 plan (최우선)
find docs/plans -maxdepth 2 -name handoff.md

# product-spec.md 존재 폴더 + 최근 수정 정렬
find docs/plans -maxdepth 2 -name product-spec.md -exec stat -f "%m %N" {} \; | sort -rn | head -5
```

`AskUserQuestion` 으로 제시 (최대 4개):

```
question: "어떤 product-spec 의 tech-spec 작성할까요?"
options:
  - "042-interview-quality (handoff 진행중, 추천)"
  - "048-payment-intro (최근 수정 2026-05-04)"
  - "051-resume-preview (최근 수정 2026-05-02)"
  - "다른 폴더 — 직접 경로 입력"
```

선택된 폴더의 `product-spec.md` Read → 컨텍스트 파악. tech-spec.md 이미 존재 시:

```
question: "tech-spec.md 이미 존재. 어떻게?"
options:
  - "갱신 — 기존 내용 보여주고 수정"
  - "덮어쓰기 — 처음부터"
  - "취소"
```

## Step 2 — 기술 메타인지 질문 (순서대로)

각 답변은 tech-spec 섹션에 매핑. 모호 답 = 재질문.

### 2-1. 영향 범위

`AskUserQuestion`:

```
question: "영향 범위?"
options:
  - "BE only"
  - "FE only"
  - "BE+FE 동시 (API contract 필요)"
  - "BE+FE+lambda"
```

→ implement.md 단일 / -be / -fe 분리 결정. BE+FE 선택 시 이후 단계에서 API contract 필수.

### 2-2. 정합성 (consistency) 중요도

`AskUserQuestion`:

```
question: "데이터 정합성 요구 수준?"
options:
  - "강한 정합성 — 트랜잭션 / 락 / 즉시 반영 필수"
  - "이벤트적 정합성 — 결국 일치 OK (이벤트 / 비동기)"
  - "낮음 — 캐시 / 통계 / 일시 불일치 허용"
```

→ Trade-off 섹션, 트랜잭션 경계 결정 근거.

### 2-3. 실시간성 (latency 민감도)

`AskUserQuestion`:

```
options:
  - "P95 < 200ms 필수 (사용자 직접 대기)"
  - "P95 < 1s 권장 (인터랙티브)"
  - "P95 < 5s 허용 (백그라운드 / 비동기 OK)"
  - "분~시간 단위 OK (배치 / 분석)"
```

### 2-4. 부하 / 성능 고려?

`AskUserQuestion`:

```
options:
  - "고부하 — N+1 / 쿼리 최적화 / 캐싱 필수"
  - "중간 — 일반적 인덱스 / 페이징 충분"
  - "저부하 — 운영 / 어드민 류, 최적화 우선순위 낮음"
```

### 2-5. 동시성 시나리오?

`AskUserQuestion`:

```
question: "동시 요청 / race condition 가능?"
options:
  - "있음 + 중요 — 락 / 큐 / 이벤트 직렬화 설계 필요"
  - "있음 + 약함 — 낙관락 / retry 로 충분"
  - "없음 — 단일 사용자 / 단일 트랜잭션"
```

→ 있음 시 후속: 어느 자원? 어떤 충돌 형태? (자유서술)

### 2-6. 데이터 마이그레이션 / 스키마 변경?

`AskUserQuestion`:

```
options:
  - "스키마 변경 + 백필 필요"
  - "스키마 변경만 (백필 X)"
  - "스키마 변경 없음"
```

→ 마이그레이션 시: Flyway DDL 만 (DML 금지 — `backend/.claude/rules/conventions.md`). 백필 = 별도 SQL 운영 스크립트.

### 2-7. 외부 의존?

`AskUserQuestion` (복수 선택 자유서술):

```
question: "외부 시스템 의존?"
options:
  - "AI (OpenAI / Claude / Gemini) — 비용 + 실패 케이스"
  - "AWS (S3 / Lambda / EventBridge / MediaConvert)"
  - "3rd party (Google OAuth / 결제 등)"
  - "없음"
```

→ 의존 있음 시: 실패 시 대응 (fallback / retry / 사용자 알림)?

### 2-8. 보안 영역?

`AskUserQuestion`:

```
question: "보안 / OWASP 고려 필요?"
options:
  - "인증 / 권한 변경 (A01)"
  - "암호화 / 민감 데이터 (A02)"
  - "사용자 입력 → 쿼리 / 명령 (A03 SQLi / Command)"
  - "외부 URL fetch (A10 SSRF)"
  - "특별 영향 없음"
```

→ 영향 시 `.claude/rules/security.md` 항목 명시 + 검증 방법 tech-spec 에 기록.

### 2-9. 관찰성 / 모니터링?

> "운영 중 어떻게 알 수 있나? 로그 / 메트릭 / 알람? 실패 탐지 방법?"

자유서술. "없음" 답 = "정말 안 봐도 되나? 실패 시 어떻게 알지?" 재질문.

### 2-10. 롤백 / 호환성?

`AskUserQuestion`:

```
options:
  - "변경 후 즉시 롤백 가능 (feature flag / 환경변수)"
  - "롤백 어려움 (마이그레이션 / 데이터 변경)"
  - "롤백 불필요 (신규 기능, 기존 영향 없음)"
```

### 2-11. 검증 방법

> "어떻게 '구현 됐다' 판정? 단위 / 통합 / E2E 테스트, 수동 시나리오, 메트릭 등."

자유서술. testing.md (BE / FE) 카테고리 명시 권장.

## Step 3 — 구조 / 데이터 / API contract (자유서술 단계)

### 3-1. Architecture 개요

> "BE 어떤 도메인 / 어떤 서비스 추가 / 수정? FE 어떤 페이지 / 컴포넌트 / 훅? 데이터 흐름 한 줄로."

자유서술. 모호 시 ("interview 영역" 류) = 재질문 ("구체 클래스 / 파일?").

### 3-2. 데이터 모델 (필요 시)

> "신규 테이블 / 컬럼 / VO / DTO? 기존 schema 변경?"

스키마 변경 답변 (2-6) Yes 시 강제. No 시 생략 가능.

### 3-3. API contract (BE+FE 시 필수)

> "엔드포인트 / 메서드 / 요청 schema / 응답 schema / 에러 코드?"

`POST /api/x` 형태 + JSON 예시 받기. 모호 시 재질문.

## Step 4 — Trade-off 명시

> "이 설계 외 검토한 대안? 왜 이걸 선택?"

최소 1개 trade-off 강제. "없음" 답 = "정말 한 가지 길? 다른 도메인 사례 / 라이브러리 / 패턴?" 재질문.

## Step 5 — preview

수집된 답변으로 tech-spec.md 초안. 템플릿: `docs/plans/_templates/tech-spec.md` 섹션 / 헤더 / 메타데이터 블록 그대로 따름 (자의적 변형 X). `docs/plans/AGENTS.md` Section 3 (파일 역할) + Section 5 (BE/FE 분리 룰 / API contract 필수 케이스) 위반 X.

```markdown
# Tech Spec — {title}

> Issue: #{N}
> product-spec: ./product-spec.md
> 작성일: {YYYY-MM-DD}

## Why
(product-spec 요약 1-2줄)

## Goal
(product-spec Goal 그대로)

## Evidence
{현 코드베이스 분석 / 사용자 발화 / 리서치}

## Architecture
{3-1 답변}

## Data Model
{3-2 답변, 없으면 "변경 없음"}

## API Contract
{3-3 답변, 없으면 생략}

## NF 결정
- 영향 범위: {2-1}
- 정합성: {2-2}
- 실시간성: {2-3}
- 부하: {2-4}
- 동시성: {2-5}
- 마이그레이션: {2-6}
- 외부 의존: {2-7}
- 보안: {2-8}
- 관찰성: {2-9}
- 롤백: {2-10}

## Trade-offs
{Step 4 답변}

## Verification
{2-11 답변}

## Pre / Post
- Pre: {현재 상태}
- Post: {구현 후 기대 상태}
```

`AskUserQuestion`:

```
options:
  - "생성 — 그대로 진행"
  - "수정 — 특정 섹션"
  - "취소"
```

## Step 6 — 파일 작성

`Write` 로 `$PLAN_DIR/tech-spec.md` 작성.

## Step 7 — 후속 안내

- "tech-spec 작성 완료. **사용자 명시 승인** 후 implement.md 작성 단계 (`docs/plans/AGENTS.md` Section 4 승인 게이트)."
- BE+FE 시: "API contract 합의 후 병렬 시작 가능 (implement-be.md / implement-fe.md 분리)."
- 강결합 시: "BE 선행 명시. FE 는 BE 머지 후 시작."
- 커밋은 별도. 사용자 결정.

## 안티 패턴

- product-spec 부재인데 tech-spec 진행 (요구사항 추측).
- 한 메시지에 메타인지 질문 여러 개.
- Trade-off "없음" 그대로 수용.
- API contract BE+FE 작업인데 생략.
- NF 결정 (정합성 / 실시간성 등) 사용자 확인 없이 자율 결정.
- preview 없이 파일 작성.
- product-spec WHY / WHAT 침범 (HOW 영역만).
- Verification "테스트 작성" 만 (통과 기준 없음 — `.claude/rules/plan-mode.md` 안티패턴).
