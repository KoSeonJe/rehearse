# API: {액션 이름}

> Endpoint: `METHOD /api/...`
> Action: {1줄 요약 — "사용자가 무엇을 할 수 있는가"}
> 관련 테이블: `{tableA}` (read) / `{tableB}` (write)
> 관련 외부 의존: {OpenAI / Claude / Gemini / S3 / Lambda — 없으면 생략}

---

## 입력

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| path | `{id}` | Long | required | {의미} |
| body | `{field}` | string | 1..200 | {의미} |
| query | `{flag}` | bool | optional, default=false | {의미} |
| header | `Authorization` | Bearer | required | JWT |

---

## 출력 (200)

| 필드 | 타입 | 의미 |
|------|------|------|
| `id` | Long | 생성 / 갱신된 식별자 |
| `status` | enum | {값 목록} |
| ... | ... | ... |

## 출력 (4xx / 5xx)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 400 | `INVALID_INPUT` | {조건} |
| 403 | `FORBIDDEN` | 본인 리소스 아님 |
| 404 | `NOT_FOUND` | {조건} |
| 409 | `CONFLICT` | {state invariant 위반} |
| 503 | `EXTERNAL_FAILURE` | AI fallback 모두 실패 |

---

## 흐름

### 1. {단계 제목}
- {단계 동작}
- {조건 → 분기}

### 2. 분기: {분기 기준 — 예: resume 존재 여부 / intent 종류}

#### 2-A. {조건 만족 — 예: resume 있음}
1. ...
2. ...

#### 2-B. {조건 미만족 — 예: resume 없음}
1. ...

### 3. 외부 호출 (있을 시)
- Provider: {GPT-4o-mini → Claude Haiku fallback}
- Timeout: {Ns}
- Retry: {N회}
- Parse 실패 시: {동작}

### 4. 저장
- `{table}` INSERT/UPDATE — {필드 / 동시성 처리 (낙관락 / unique / 비관락)}

### 5. 응답
- 200 본문 구성 / 부수 효과 (이벤트 발행 / 캐시 무효화 등)

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| 동시 호출 (같은 사용자, 같은 리소스) | {예: 낙관락 → 409 CONFLICT} |
| 외부 의존 (AI / S3) 실패 | {예: fallback provider 호출 → 모두 실패 시 503} |
| 권한 mismatch (다른 사용자 리소스) | 403 FORBIDDEN |
| state invariant 위반 (예: COMPLETED 후 PUT) | 409 CONFLICT |
| 입력 schema 위반 | 400 INVALID_INPUT |
| ... | ... |

---

## 상태 전이 (있을 시)

```
{INITIAL} → {NEXT_A}  (조건: ...)
         → {NEXT_B}  (조건: ...)
{NEXT_A} → {FINAL}    (조건: ...)
```

---

## 관찰성

- **로그**: `{logger}` — key fields: `userId`, `sessionId`, `intent`, `provider`, `latencyMs`
- **메트릭**: `{metric_name}{labels}` — 예: `interview_turn_total{intent="follow_up"}`
- **알람**: {조건} → {채널} — 예: `503 비율 > 5% (5분)` → Slack `#alerts-be`

---

## 연관 의존성

이 액션이 호출 / 청취하는 도메인 외부 클래스. `import` / 호출 그래프 근거. 임계값·디폴트는 클래스 직접 인용.

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.{other}.{Class}` | {1줄 역할} | calls / event-publisher / event-listener / cache / persister |
| `com.rehearse.api.infra.ai.{Class}` | {AI 클라이언트} | calls — primary / fallback |

---

## 정책 출처

- 비즈니스 룰: `docs/domain/{name}/schema.md` `{관련 invariant}`
- 임계값 / fallback 룰: §메타인지 보완 결과 (가능 시 연관 의존성 클래스 상수 직접 인용)
- ❓TODO(사용자 확인): {코드에서 추론 불가 항목 명시 — 정책 결정 필요한 것만}
