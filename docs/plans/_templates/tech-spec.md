# Tech Spec — {제목}

> **작성자**: 구현 agent (backend / frontend)
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄 미러)

product-spec.md 의 Why/Goal 1줄 요약. (중복 복붙 금지)

## Evidence

근거 / 리서치 / 기존 코드 분석. 추측은 명시.

- 현재 구조: (관련 파일 / 클래스 / 모듈)
- 외부 레퍼런스:
- 사용자 발화 (특정 결정 근거):
- 추정 / 미확인 가정:

## Trade-offs

선택지 비교. 각 옵션 = 장 / 단 / 채택 사유.

### Option A (채택)
- 장점:
- 단점:
- 사유:

### Option B (폐기)
- 장점:
- 단점:
- 폐기 사유:

## Architecture

구조도 / 데이터 흐름 / 시퀀스. 텍스트 다이어그램 권장.

```
(예시 시퀀스)
[Client] → [API] → [Service] → [Repo] → [DB]
              ↓
          [Event Bus] → [Lambda]
```

## Data Model

DB 스키마 변경 / Entity / DTO 정의.

```sql
-- (예시)
ALTER TABLE interviews ADD COLUMN intent_score DECIMAL(3,2);
```

## API Contract

> BE+FE 공통 작업 시 **필수**. contract 합의 = 사용자 승인 게이트.

### Endpoint

`POST /api/v1/...`

### Request
```json
{ "field": "value" }
```

### Response (200)
```json
{ "id": 1, "status": "ok" }
```

### Error
- 400: validation 실패 코드 매핑
- 404: 리소스 없음
- 409: 충돌 (낙관락)

## Verification (완료 판정)

구현 완료 = 아래 모두 통과.

- [ ] 단위 테스트: (구체 케이스)
- [ ] 통합 테스트: (Testcontainers / E2E)
- [ ] 빌드 / 린트: `./gradlew build`, `npm run lint`
- [ ] 관찰 가능 동작: (구체 시나리오 / 명령)
- [ ] 회귀 체크: (영향 범위 영역)

## Pre / Post State

### Pre (현재)
- 파일 / 동작 / 스키마 현재 상태

### Post (구현 후)
- 변경된 파일 / 동작 / 스키마

diff 형태 비교 가능하게.

## 위험 / 마이그레이션 / 롤백

- 위험: (데이터 정합성 / 다운타임 / 호환성)
- 마이그레이션 전략: (zero-downtime / read-then-write / dual-write)
- 롤백 시나리오: (실패 시 복구 절차)

## 분기 결정

이번 작업이 **BE+FE 분리** 인가 단일 영역인가:

- [ ] 단일 영역 → `implement.md` 1개
- [ ] BE+FE 동시 → `implement-be.md` + `implement-fe.md` (API contract 합의 후 병렬)
- [ ] BE 선행 강제 (강결합) → `implement-be.md` 머지 후 `implement-fe.md`
